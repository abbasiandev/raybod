import pytest
import json
from fastapi.testclient import TestClient
from app.main import app
from app.core.database import SessionLocal
from app.models.scan_log import ScanLog

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

def test_full_scan_flow_with_websocket(client, db_session):
    """
    Test full flow: 
    1. Connect to WebSocket
    2. POST /api/v1/scan/analyze with suspicious app
    3. Verify DB record exists
    4. Verify WebSocket broadcast received
    """
    import uuid
    package_name = f"com.malware.e2e.{uuid.uuid4().hex[:8]}"
    
    # 1. Connect to WebSocket
    with client.websocket_connect("/ws/threats") as websocket:
        # 2. Trigger a HIGH risk scan
        payload = {
            "package_name": package_name,
            "version_code": 1,
            "signature": "malware_sig",
            "permissions": [
                "android.permission.READ_SMS",
                "android.permission.RECEIVE_SMS",
                "android.permission.SEND_SMS",
                "android.permission.INTERNET"
            ]
        }
        
        response = client.post("/api/v1/scan/analyze", json=payload)
        assert response.status_code == 200
        data = response.json()
        assert data["risk_level"] == "HIGH"
        
        # 3. Check if ScanLog was created in DB
        log = db_session.query(ScanLog).filter(ScanLog.package_name == package_name).first()
        assert log is not None
        assert log.risk_level == "HIGH"
        
        # 4. Check WebSocket broadcast
        ws_msg = websocket.receive_text()
        ws_data = json.loads(ws_msg)
        assert ws_data["type"] == "NEW_THREAT"
        assert ws_data["package_name"] == package_name
        assert ws_data["risk_level"] == "HIGH"

def test_batch_scan_persistence(client, db_session):
    """
    Test batch scan flow:
    1. POST /api/v1/scan/batch
    2. Verify all packages are persisted in ScanLog
    """
    # 1. Trigger a batch scan
    payload = {
        "packages": [
            {
                "package_name": "com.app.batch1",
                "version_code": 10,
                "signature": "sig1",
                "permissions": []
            },
            {
                "package_name": "com.app.batch2",
                "version_code": 20,
                "signature": "sig2",
                "permissions": ["android.permission.CAMERA"]
            }
        ]
    }
    
    response = client.post("/api/v1/scan/batch", json=payload)
    assert response.status_code == 200
    results = response.json()["results"]
    assert len(results) == 2
    
    # 2. Check persistence in DB
    log1 = db_session.query(ScanLog).filter(ScanLog.package_name == "com.app.batch1").first()
    log2 = db_session.query(ScanLog).filter(ScanLog.package_name == "com.app.batch2").first()
    
    assert log1 is not None
    assert log1.version_code == 10
    assert log2 is not None
    assert log2.version_code == 20

