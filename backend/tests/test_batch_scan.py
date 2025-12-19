import pytest
from fastapi.testclient import TestClient
from app.main import app

@pytest.fixture
def client():
    return TestClient(app)

def test_batch_scan_processes_multiple_packages(client):
    """POST /api/v1/scan/batch processes array of packages"""
    payload = {
        "packages": [
            {"package_name": "com.app1", "version_code": 1, "signature": "hash1", "permissions": []},
            {"package_name": "com.app2", "version_code": 2, "signature": "hash2", "permissions": []}
        ]
    }
    response = client.post("/api/v1/scan/batch", json=payload)
    assert response.status_code == 200
    results = response.json()["results"]
    assert len(results) == 2
    assert results[0]["package_name"] == "com.app1"
    assert results[1]["package_name"] == "com.app2"

