import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.models.user import User
from app.core.database import SessionLocal

@pytest.fixture
def client():
    return TestClient(app)

@pytest.fixture
def auth_header(client):
    """Get auth header for admin user."""
    response = client.post(
        "/api/v1/auth/login",
        data={"username": "admin", "password": "admin"}
    )
    token = response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}

def test_landing_page_accessible(client):
    """The landing page should be accessible at the root."""
    response = client.get("/")
    assert response.status_code == 200
    assert "CODEKHODA SENTINEL" in response.text

def test_billing_page_requires_auth(client):
    """Billing page should redirect to login if not authenticated."""
    response = client.get("/dashboard/billing/", follow_redirects=False)
    assert response.status_code == 302
    assert "/dashboard/login" in response.headers["location"]

def test_billing_page_accessible_with_auth(client, auth_header):
    """Billing page should be accessible for authenticated users."""
    response = client.get("/dashboard/billing/", headers=auth_header)
    assert response.status_code == 200
    assert "Billing & Subscription" in response.text

def test_checkout_redirects_to_sandbox(client, auth_header):
    """Checkout should redirect to the payment sandbox."""
    response = client.post(
        "/dashboard/billing/checkout",
        data={"plan": "FEATURED"},
        headers=auth_header,
        follow_redirects=False
    )
    assert response.status_code == 303
    assert "/dashboard/billing/sandbox" in response.headers["location"]
    assert "plan=FEATURED" in response.headers["location"]

def test_verify_payment_success_updates_plan(client, auth_header):
    """Verifying a successful payment should update the user's plan."""
    # Check current plan (admin is FEATURED by default in seeds, let's check or change it)
    db = SessionLocal()
    user = db.query(User).filter(User.username == "admin").first()
    user.plan = "FREEMIUM"
    db.commit()
    
    response = client.post(
        "/dashboard/billing/verify",
        data={"success": "true", "plan": "FEATURED"},
        headers=auth_header,
        follow_redirects=False
    )
    
    assert response.status_code == 303
    assert "status=success" in response.headers["location"]
    
    db.refresh(user)
    assert user.plan == "FEATURED"
    db.close()

def test_verify_payment_failure_does_not_update_plan(client, auth_header):
    """Verifying a failed payment should not update the user's plan."""
    db = SessionLocal()
    user = db.query(User).filter(User.username == "admin").first()
    user.plan = "FREEMIUM"
    db.commit()
    
    response = client.post(
        "/dashboard/billing/verify",
        data={"success": "false", "plan": "FEATURED"},
        headers=auth_header,
        follow_redirects=False
    )
    
    assert response.status_code == 303
    assert "status=failed" in response.headers["location"]
    
    db.refresh(user)
    assert user.plan == "FREEMIUM"
    db.close()

