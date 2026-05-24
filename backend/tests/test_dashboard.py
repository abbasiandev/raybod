import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.database import SessionLocal, AllowlistEntry, BlocklistEntry
from app.models.user import User
import secrets

@pytest.fixture
def client():
    return TestClient(app)

@pytest.fixture
def admin_auth(client):
    """Login as admin and return cookies."""
    response = client.post(
        "/dashboard/login",
        data={"username": "admin", "password": "admin"},
        follow_redirects=False
    )
    return response.cookies

def test_dashboard_index_requires_auth(client):
    response = client.get("/dashboard/", follow_redirects=False)
    assert response.status_code == 302
    assert "/dashboard/login" in response.headers["location"]

def test_dashboard_index_with_auth(client, admin_auth):
    response = client.get("/dashboard/", cookies=admin_auth)
    assert response.status_code == 200
    assert "Overview" in response.text

def test_allowlist_management(client, admin_auth):
    # 1. View allowlist
    response = client.get("/dashboard/allowlist", cookies=admin_auth)
    assert response.status_code == 200
    
    # 2. Add to allowlist
    pkg = f"com.test.allow.{secrets.token_hex(4)}"
    response = client.post(
        "/dashboard/allowlist",
        data={"package_name": pkg},
        cookies=admin_auth
    )
    assert response.status_code == 200
    assert pkg in response.text
    
    # 3. Verify in DB
    db = SessionLocal()
    entry = db.query(AllowlistEntry).filter(AllowlistEntry.package_name == pkg).first()
    assert entry is not None
    db.close()

def test_blocklist_management(client, admin_auth):
    # 1. View blocklist
    response = client.get("/dashboard/blocklist", cookies=admin_auth)
    assert response.status_code == 200
    
    # 2. Add to blocklist
    pkg = f"com.test.block.{secrets.token_hex(4)}"
    response = client.post(
        "/dashboard/blocklist",
        data={"package_name": pkg, "threat_type": "Spyware"},
        cookies=admin_auth
    )
    assert response.status_code == 200
    assert pkg in response.text
    assert "Spyware" in response.text

def test_analytics_page(client, admin_auth):
    response = client.get("/dashboard/analytics", cookies=admin_auth)
    assert response.status_code == 200
    assert "Scan History" in response.text
    assert "Phone scans only" in response.text

def test_analytics_phone_only_filter(client, admin_auth):
    response = client.get("/dashboard/analytics?phone_only=1", cookies=admin_auth)
    assert response.status_code == 200
    assert "Phone scans only" in response.text

def test_export_csv(client, admin_auth):
    response = client.get("/dashboard/export/csv", cookies=admin_auth)
    assert response.status_code == 200
    assert "text/csv" in response.headers["content-type"]
    assert "package_name" in response.text

