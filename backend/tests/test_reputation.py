import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.database import SessionLocal
from app.models.scan_log import ScanLog
from datetime import datetime
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

def create_scan_log(db, package, risk_level):
    log = ScanLog(
        package_name=package,
        version_code=1,
        risk_level=risk_level,
        timestamp=datetime.utcnow()
    )
    db.add(log)
    db.commit()
    return log

def test_reputation_calculation_high_confidence(client, db_session):
    """Package with 10 safe scans should have high reputation"""
    package = f"com.trusted.{secrets.token_hex(4)}"
    for _ in range(10):
        create_scan_log(db_session, package, "SAFE")
    
    response = client.get(f"/api/v1/reputation/{package}")
    assert response.status_code == 200
    data = response.json()
    assert data["reputation_score"] >= 0.9
    assert data["total_scans"] == 10

def test_reputation_calculation_mixed_results(client, db_session):
    """Package with mixed results should have lower reputation"""
    package = f"com.mixed.{secrets.token_hex(4)}"
    create_scan_log(db_session, package, "SAFE")
    create_scan_log(db_session, package, "HIGH")
    create_scan_log(db_session, package, "SAFE")
    
    response = client.get(f"/api/v1/reputation/{package}")
    assert response.status_code == 200
    data = response.json()
    assert data["reputation_score"] < 0.9
    assert data["total_scans"] == 3

