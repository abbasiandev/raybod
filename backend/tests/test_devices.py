import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.database import SessionLocal
from app.models.device import Device

@pytest.fixture
def client():
    return TestClient(app)

def test_device_registration(client):
    """POST /api/v1/devices/register creates or updates a device"""
    payload = {
        "device_id": "test-device-123",
        "device_model": "Pixel 6",
        "os_version": "13",
        "app_version": "1.0.0"
    }
    response = client.post("/api/v1/devices/register", json=payload)
    assert response.status_code == 200
    assert response.json()["device_id"] == "test-device-123"

