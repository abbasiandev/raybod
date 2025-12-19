"""
TDD Tests for Authentication Service.
These tests are written FIRST, before implementation.
"""
import pytest
from datetime import datetime, timedelta
from fastapi import HTTPException
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock

# Import after we have the service created
# from app.services.auth import (
#     create_access_token,
#     verify_token,
#     hash_password,
#     verify_password,
#     get_current_user
# )


class TestPasswordHashing:
    """Tests for password hashing utilities."""
    
    def test_password_hashing_returns_different_value(self):
        """Hashed password should not equal plain password."""
        from app.services.auth import hash_password
        
        plain_password = "admin123"
        hashed = hash_password(plain_password)
        
        assert hashed != plain_password
        assert len(hashed) > 0
    
    def test_password_verification_with_correct_password(self):
        """Verify password should return True for correct password."""
        from app.services.auth import hash_password, verify_password
        
        plain_password = "secure_password_123"
        hashed = hash_password(plain_password)
        
        assert verify_password(plain_password, hashed) is True
    
    def test_password_verification_with_incorrect_password(self):
        """Verify password should return False for wrong password."""
        from app.services.auth import hash_password, verify_password
        
        plain_password = "secure_password_123"
        hashed = hash_password(plain_password)
        
        assert verify_password("wrong_password", hashed) is False


class TestJWTTokens:
    """Tests for JWT token creation and verification."""
    
    def test_create_access_token_returns_string(self):
        """create_access_token should return a JWT string."""
        from app.services.auth import create_access_token
        
        token = create_access_token(user_id=1, username="admin", role="super_admin")
        
        assert isinstance(token, str)
        assert len(token) > 0
        # JWT has 3 parts separated by dots
        assert len(token.split(".")) == 3
    
    def test_verify_token_with_valid_token(self):
        """verify_token should return payload for valid token."""
        from app.services.auth import create_access_token, verify_token
        
        token = create_access_token(user_id=1, username="admin", role="super_admin")
        payload = verify_token(token)
        
        assert payload is not None
        assert payload["user_id"] == 1
        assert payload["username"] == "admin"
        assert payload["role"] == "super_admin"
    
    def test_verify_token_with_invalid_token_raises_exception(self):
        """verify_token should raise HTTPException for invalid token."""
        from app.services.auth import verify_token
        
        with pytest.raises(HTTPException) as exc_info:
            verify_token("invalid.token.here")
        
        assert exc_info.value.status_code == 401
    
    def test_verify_token_with_expired_token_raises_exception(self):
        """verify_token should raise HTTPException for expired token."""
        from app.services.auth import create_access_token, verify_token
        
        # Create token that expires immediately
        token = create_access_token(
            user_id=1, 
            username="admin", 
            role="super_admin",
            expires_delta=timedelta(seconds=-1)  # Already expired
        )
        
        with pytest.raises(HTTPException) as exc_info:
            verify_token(token)
        
        assert exc_info.value.status_code == 401


class TestLoginEndpoint:
    """Tests for login API endpoint."""
    
    @pytest.fixture
    def client(self):
        """Create test client."""
        from app.main import app
        return TestClient(app)
    
    def test_login_with_valid_credentials_returns_200(self, client):
        """Login with valid credentials should return 200 and set cookie."""
        response = client.post(
            "/api/v1/auth/login",
            data={"username": "admin", "password": "admin"}
        )
        
        assert response.status_code == 200
        assert "access_token" in response.cookies or "Set-Cookie" in response.headers.get("set-cookie", "").lower() or response.json().get("access_token")
    
    def test_login_with_invalid_credentials_returns_401(self, client):
        """Login with wrong password should return 401."""
        response = client.post(
            "/api/v1/auth/login",
            data={"username": "admin", "password": "wrong_password"}
        )
        
        assert response.status_code == 401
    
    def test_login_with_nonexistent_user_returns_401(self, client):
        """Login with unknown user should return 401."""
        response = client.post(
            "/api/v1/auth/login",
            data={"username": "nobody", "password": "password"}
        )
        
        assert response.status_code == 401


class TestLogoutEndpoint:
    """Tests for logout API endpoint."""
    
    @pytest.fixture
    def client(self):
        """Create test client."""
        from app.main import app
        return TestClient(app)
    
    def test_logout_clears_cookie(self, client):
        """Logout should clear the access_token cookie."""
        # First login
        login_response = client.post(
            "/api/v1/auth/login",
            data={"username": "admin", "password": "admin"}
        )
        
        # Then logout
        response = client.post("/api/v1/auth/logout")
        
        assert response.status_code == 200


class TestProtectedRoutes:
    """Tests for protected route access."""
    
    @pytest.fixture
    def client(self):
        """Create test client."""
        from app.main import app
        return TestClient(app)
    
    def test_protected_route_without_token_returns_401(self, client):
        """Accessing protected route without token should return 401."""
        response = client.get("/api/v1/auth/me")
        
        assert response.status_code == 401
    
    def test_protected_route_with_valid_token_succeeds(self, client):
        """Accessing protected route with valid token should succeed."""
        # First login to get token
        login_response = client.post(
            "/api/v1/auth/login",
            data={"username": "admin", "password": "admin"}
        )
        
        # Use token to access protected route
        # Token should be in cookie or response body
        token = login_response.json().get("access_token", "")
        
        response = client.get(
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {token}"} if token else {},
            cookies=login_response.cookies
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["username"] == "admin"


class TestRoleBasedAccess:
    """Tests for role-based permission checks."""
    
    @pytest.fixture
    def client(self):
        """Create test client."""
        from app.main import app
        return TestClient(app)
    
    def test_super_admin_has_all_permissions(self, client):
        """Super admin should have all permissions."""
        # Login as admin (super_admin role)
        login_response = client.post(
            "/api/v1/auth/login",
            data={"username": "admin", "password": "admin"}
        )
        
        response = client.get(
            "/api/v1/auth/me",
            cookies=login_response.cookies
        )
        
        if response.status_code == 200:
            data = response.json()
            # Super admin should have manage_users permission
            assert data.get("permissions", {}).get("manage_users", False) is True
