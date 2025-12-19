import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.database import SessionLocal
from app.models.scan_log import ScanLog
from datetime import datetime

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

def test_threat_feed_returns_high_risk_scans(client, db_session):
    """GET /api/v1/threats/feed returns threats with risk_level >= HIGH"""
    # Seed test data
    create_scan_log(db_session, "com.malware1", "HIGH")
    create_scan_log(db_session, "com.safe.app", "SAFE")
    create_scan_log(db_session, "com.malware2", "CRITICAL")
    
    response = client.get("/api/v1/threats/feed?limit=10")
    assert response.status_code == 200
    threats = response.json()["threats"]
    assert len(threats) >= 2
    assert all(t["risk_level"] in ["HIGH", "CRITICAL"] for t in threats if t["package_name"].startswith("com.malware"))

