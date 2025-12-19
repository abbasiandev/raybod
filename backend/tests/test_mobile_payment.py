import pytest
from fastapi.testclient import TestClient
from app.main import app

@pytest.fixture
def client():
    return TestClient(app)

def test_mobile_pay_redirect(client):
    """Test the GET /api/v1/public/pay?mobile=true endpoint."""
    response = client.get("/api/v1/public/pay?mobile=true", follow_redirects=False)
    assert response.status_code == 303
    location = response.headers["location"]
    assert "/payment/sandbox/" in location
    
    # Check if session is marked as mobile
    session_id = location.split("/")[-1]
    response = client.get(location)
    assert response.status_code == 200
    # We can't easily check the internal session state without mocking or exposing it, 
    # but we can check if the template received the mobile flag if it was rendered.
    assert "Paid" in response.text

def test_mobile_payment_process_success_redirect(client):
    """Test successful mobile payment redirects to codekhoda:// deep link."""
    # 1. Create a mobile session
    response = client.get("/api/v1/public/pay?mobile=true", follow_redirects=False)
    session_id = response.headers["location"].split("/")[-1]
    
    # 2. Process success
    response = client.post(
        "/api/v1/public/payment/process",
        data={"session_id": session_id, "success": "true"},
        follow_redirects=False
    )
    assert response.status_code == 303
    assert response.headers["location"] == "codekhoda://payment-result?status=success"

def test_mobile_payment_process_failure_redirect(client):
    """Test failed mobile payment redirects to codekhoda:// deep link."""
    # 1. Create a mobile session
    response = client.get("/api/v1/public/pay?mobile=true", follow_redirects=False)
    session_id = response.headers["location"].split("/")[-1]
    
    # 2. Process failure
    response = client.post(
        "/api/v1/public/payment/process",
        data={"session_id": session_id, "success": "false"},
        follow_redirects=False
    )
    assert response.status_code == 303
    assert response.headers["location"] == "codekhoda://payment-result?status=failed"

def test_non_mobile_payment_process_success_stay_on_web(client):
    """Test non-mobile payment stays on web (HTML response)."""
    # 1. Create a non-mobile session
    response = client.post(
        "/api/v1/public/checkout",
        data={"plan_id": "FEATURED"},
        follow_redirects=False
    )
    session_id = response.headers["location"].split("/")[-1]
    
    # 2. Process success
    response = client.post(
        "/api/v1/public/payment/process",
        data={"session_id": session_id, "success": "true"},
        follow_redirects=False
    )
    assert response.status_code == 200
    assert "Payment Successful" in response.text
    assert "codekhoda://" not in response.text

def test_mobile_checkout_post_redirect(client):
    """Test the POST /api/v1/public/checkout endpoint with mobile=true."""
    # 1. Checkout session via POST
    response = client.post(
        "/api/v1/public/checkout",
        data={"plan_id": "FEATURED", "mobile": "true"},
        follow_redirects=False
    )
    assert response.status_code == 303
    session_id = response.headers["location"].split("/")[-1]
    
    # 2. Process success
    response = client.post(
        "/api/v1/public/payment/process",
        data={"session_id": session_id, "success": "true"},
        follow_redirects=False
    )
    assert response.status_code == 303
    assert response.headers["location"] == "codekhoda://payment-result?status=success"

