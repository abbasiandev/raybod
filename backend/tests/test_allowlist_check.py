import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.database import SessionLocal, AllowlistEntry

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

def test_allowlist_check_returns_true_for_allowed_package(client, db_session):
    """GET /api/v1/allowlist/check/{package} returns 200 with is_allowed=true"""
    # Ensure a package is in allowlist
    package = "com.android.chrome"
    existing = db_session.query(AllowlistEntry).filter(AllowlistEntry.package_name == package).first()
    if not existing:
        db_session.add(AllowlistEntry(package_name=package))
        db_session.commit()
    
    response = client.get(f"/api/v1/allowlist/check/{package}")
    assert response.status_code == 200
    assert response.json()["is_allowed"] is True
    assert response.json()["package_name"] == package

def test_allowlist_check_returns_false_for_unknown_package(client):
    """GET /api/v1/allowlist/check/{package} returns 200 with is_allowed=false"""
    response = client.get("/api/v1/allowlist/check/com.unknown.app")
    assert response.status_code == 200
    assert response.json()["is_allowed"] is False

