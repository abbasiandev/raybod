import pytest
from fastapi.testclient import TestClient
from app.main import app

@pytest.fixture
def client():
    return TestClient(app)

def test_get_top_threats(client):
    """GET /api/v1/analytics/top-threats returns list of frequent threats"""
    response = client.get("/api/v1/analytics/top-threats?limit=5")
    assert response.status_code == 200
    assert "top_threats" in response.json()

def test_get_timeseries_analytics(client):
    """GET /api/v1/analytics/timeseries returns trend data"""
    response = client.get("/api/v1/analytics/timeseries?days=7")
    assert response.status_code == 200
    assert "labels" in response.json()
    assert "values" in response.json()

