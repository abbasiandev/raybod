import pytest
from app.engine.network_heuristics import network_heuristic_engine
from app.schemas.network_schema import NetworkFlow
from fastapi.testclient import TestClient
from app.main import app

@pytest.fixture
def client():
    return TestClient(app)

def test_dga_detection():
    """Verify that high-entropy domains trigger DGA alerts."""
    # Normal domain
    normal_flow = NetworkFlow(
        source_app="com.google.android",
        destination_ip="8.8.8.8",
        destination_port=443,
        domain="google.com",
        bytes_sent=100,
        bytes_received=200,
        protocol="TCP",
        timestamp=1000
    )
    
    # Random domain (High entropy)
    suspicious_flow = NetworkFlow(
        source_app="com.unknown.app",
        destination_ip="1.2.3.4",
        destination_port=443,
        domain="xkjh123asdlkjzxc.com",
        bytes_sent=100,
        bytes_received=200,
        protocol="TCP",
        timestamp=1001
    )
    
    alerts, blocklist = network_heuristic_engine.analyze_flows([normal_flow, suspicious_flow])
    
    dga_alerts = [a for a in alerts if a.threat_type == "DGA Detected"]
    assert len(dga_alerts) > 0
    assert dga_alerts[0].destination == "xkjh123asdlkjzxc.com"
    
    # Verify blocklist entry
    assert any(b.pattern == "xkjh123asdlkjzxc.com" for b in blocklist)
    
    # Verify normal domain didn't trigger
    assert not any(a.destination == "google.com" for a in alerts)

def test_suspicious_port_detection():
    """Verify that connection to known malware ports triggers CRITICAL alert."""
    flow = NetworkFlow(
        source_app="com.test.app",
        destination_ip="1.1.1.1",
        destination_port=1337, # Leet port
        bytes_sent=100,
        bytes_received=100,
        protocol="TCP",
        timestamp=2000
    )
    
    alerts, _ = network_heuristic_engine.analyze_flows([flow])
    
    assert len(alerts) == 1
    assert alerts[0].threat_type == "Suspicious Port"
    assert alerts[0].risk_level == "CRITICAL"
    assert "1337" in alerts[0].description

def test_data_exfiltration_detection():
    """Verify that large data transfers to unknown domains trigger alerts."""
    flow = NetworkFlow(
        source_app="com.data.thief",
        destination_ip="5.5.5.5",
        destination_port=443,
        domain="evil-server.xyz",
        bytes_sent=2 * 1024 * 1024, # 2MB
        bytes_received=100,
        protocol="TCP",
        timestamp=3000
    )
    
    alerts, _ = network_heuristic_engine.analyze_flows([flow])
    
    exfil_alerts = [a for a in alerts if a.threat_type == "Potential Data Exfiltration"]
    assert len(exfil_alerts) > 0
    assert exfil_alerts[0].risk_level == "MEDIUM"

def test_calculation_entropy():
    """Unit test for entropy calculation helper."""
    # Low entropy
    assert network_heuristic_engine._calculate_entropy("aaaaa") == 0
    # Higher entropy
    assert network_heuristic_engine._calculate_entropy("abcde") > 2.0
    # Empty string
    assert network_heuristic_engine._calculate_entropy("") == 0

def test_network_analyze_endpoint(client):
    payload = {
        "flows": [
            {
                "source_app": "com.test.app",
                "destination_ip": "8.8.8.8",
                "destination_port": 6667, # Suspicious (IRC)
                "protocol": "TCP",
                "bytes_sent": 100,
                "bytes_received": 100,
                "timestamp": 123456789
            }
        ]
    }
    response = client.post("/api/v1/network/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert len(data["alerts"]) > 0
    assert data["alerts"][0]["threat_type"] == "Suspicious Port"

