import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.database import SessionLocal
from app.models.scan_log import ScanLog
from datetime import datetime, timedelta
from sqlalchemy import func

@pytest.fixture
def client():
    return TestClient(app)

@pytest.fixture
def db_session():
    db = SessionLocal()
    try:
        # Use a transaction for the test
        yield db
        # Rollback after test to keep DB clean if using real DB
        # But for integrated tests, we might want to commit and then cleanup
    finally:
        db.close()

def create_log(db, threat_type, risk_level, timestamp=None):
    log = ScanLog(
        package_name=f"com.test.{threat_type.lower().replace(' ', '_')}.{datetime.utcnow().timestamp()}",
        version_code=1,
        risk_level=risk_level,
        threat_type=threat_type,
        timestamp=timestamp or datetime.utcnow()
    )
    db.add(log)
    db.commit()
    return log

def test_top_threats_aggregation(client, db_session):
    """
    Verify that top-threats endpoint correctly aggregates threat types.
    """
    # Seed data
    for _ in range(3):
        create_log(db_session, "Spyware", "HIGH")
    for _ in range(2):
        create_log(db_session, "Trojan", "CRITICAL")
    create_log(db_session, "Clean", "SAFE") # Should be ignored in top-threats
    
    response = client.get("/api/v1/analytics/top-threats")
    assert response.status_code == 200
    data = response.json()["top_threats"]
    
    # We find our seeded threats in the results (there might be others from other tests)
    spyware_entry = next((item for item in data if item["threat_type"] == "Spyware"), None)
    trojan_entry = next((item for item in data if item["threat_type"] == "Trojan"), None)
    
    assert spyware_entry is not None
    assert spyware_entry["count"] >= 3
    assert trojan_entry is not None
    assert trojan_entry["count"] >= 2

def test_timeseries_aggregation(client, db_session):
    """
    Verify that timeseries endpoint correctly groups scans by day.
    """
    today = datetime.utcnow().date()
    yesterday = today - timedelta(days=1)
    
    # Clear any logs for today/yesterday to have exact numbers
    db_session.query(ScanLog).filter(func.date(ScanLog.timestamp).in_([today, yesterday])).delete(synchronize_session=False)
    db_session.commit()
    
    create_log(db_session, "Threat1", "HIGH", timestamp=datetime.utcnow())
    create_log(db_session, "Threat2", "HIGH", timestamp=datetime.utcnow())
    create_log(db_session, "Threat3", "HIGH", timestamp=datetime.utcnow() - timedelta(days=1))
    
    response = client.get("/api/v1/analytics/timeseries?days=2")
    assert response.status_code == 200
    data = response.json()
    
    assert data["labels"][0] == yesterday.strftime("%Y-%m-%d")
    assert data["values"][0] == 1
    
    assert data["labels"][1] == today.strftime("%Y-%m-%d")
    assert data["values"][1] == 2

