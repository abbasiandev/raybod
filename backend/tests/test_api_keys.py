import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.database import SessionLocal, init_db
from app.models.user import User, Role
from app.services.auth import create_access_token
import secrets

@pytest.fixture
def client():
    return TestClient(app)

@pytest.fixture
def db_session():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@pytest.fixture
def admin_token(db_session):
    # Ensure admin user exists
    user = db_session.query(User).filter(User.username == "admin").first()
    if not user:
        # This shouldn't happen if init_db worked, but for tests we ensure
        role = db_session.query(Role).filter(Role.name == "super_admin").first()
        user = User(username="admin", hashed_password="hashed", role_id=role.id)
        db_session.add(user)
        db_session.commit()
    
    token = create_access_token(user_id=user.id, username=user.username, role="super_admin")
    return token

def test_create_api_key(client, admin_token):
    """POST /api/v1/keys/ creates a new API key"""
    payload = {
        "name": "Test Key",
        "scopes": ["scan:read"],
        "expires_in_days": 30
    }
    response = client.post(
        "/api/v1/keys/",
        json=payload,
        headers={"Authorization": f"Bearer {admin_token}"}
    )
    assert response.status_code == 200
    assert "api_key" in response.json()
    assert response.json()["name"] == "Test Key"
    assert "scan:read" in response.json()["scopes"]

def test_list_api_keys(client, admin_token):
    """GET /api/v1/keys/ lists user's API keys"""
    response = client.get(
        "/api/v1/keys/",
        headers={"Authorization": f"Bearer {admin_token}"}
    )
    assert response.status_code == 200
    assert isinstance(response.json(), list)

def test_delete_api_key(client, admin_token):
    """DELETE /api/v1/keys/{id} revokes an API key"""
    # Create one first
    create_resp = client.post(
        "/api/v1/keys/",
        json={"name": "To Delete"},
        headers={"Authorization": f"Bearer {admin_token}"}
    )
    key_id = create_resp.json()["id"]
    
    delete_resp = client.delete(
        f"/api/v1/keys/{key_id}",
        headers={"Authorization": f"Bearer {admin_token}"}
    )
    assert delete_resp.status_code == 200
    assert delete_resp.json()["message"] == "API key revoked"

