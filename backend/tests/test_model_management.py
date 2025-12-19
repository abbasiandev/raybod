import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.database import SessionLocal
from app.models.model_version import ModelVersion
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

def test_get_current_model_returns_active_version(client, db_session):
    """GET /api/v1/models/current returns active model version"""
    # Use random version to avoid unique constraint issues
    v_name1 = f"test_{secrets.token_hex(4)}"
    v_name2 = f"test_{secrets.token_hex(4)}"
    
    v1 = ModelVersion(version=v_name1, file_path="models/v1.tflite", is_active=False)
    v2 = ModelVersion(version=v_name2, file_path="models/v2.tflite", is_active=True)
    db_session.add(v1)
    db_session.add(v2)
    db_session.commit()
    
    response = client.get("/api/v1/models/current")
    assert response.status_code == 200
    assert response.json()["version"] == v_name2

