from fastapi.testclient import TestClient
from app.main import app
from app.core.database import init_db
import time

# Initialize database for tests
init_db()

client = TestClient(app)

def test_rate_limiting_analyze_endpoint():
    """Verify that the /analyze endpoint returns 429 after exceeding limit."""
    payload = {
        "package_name": "com.test.ratelimit",
        "version_code": 1,
        "signature": "hash",
        "permissions": []
    }
    
    success_count = 0
    limit_hit = False
    
    # Send up to 20 requests
    for i in range(20):
        response = client.post("/api/v1/scan/analyze", json=payload)
        if response.status_code == 200:
            success_count += 1
        elif response.status_code == 429:
            limit_hit = True
            break
            
    assert success_count > 0, "All requests were blocked! Rate limit might be too strict or IP blocked."
    assert limit_hit, "Rate limit was never reached!"
    assert response.json()["detail"] == "Too many requests"
