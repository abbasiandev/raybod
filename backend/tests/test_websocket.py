import pytest
from fastapi.testclient import TestClient
from app.main import app

def test_websocket_connection():
    """Verify that websocket connection can be established"""
    client = TestClient(app)
    with client.websocket_connect("/ws/threats") as websocket:
        # Just check we can connect and it doesn't immediately close
        assert websocket is not None
        # We can try to send a ping
        websocket.send_text("ping")
        # In our implementation it doesn't reply, but it shouldn't crash

