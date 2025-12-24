"""
Test suite for the scan API endpoints.
"""
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.schemas.scan_schema import RiskLevel


@pytest.fixture
def client():
    """Create a test client for the FastAPI application."""
    return TestClient(app)


class TestHealthEndpoint:
    """Tests for the health check endpoint."""

    def test_health_check_returns_200(self, client):
        """Health endpoint should return 200 OK."""
        response = client.get("/health")

        assert response.status_code == 200

    def test_health_check_returns_healthy_status(self, client):
        """Health endpoint should return healthy status."""
        response = client.get("/health")
        data = response.json()

        assert data["status"] == "healthy"
        assert "service" in data


class TestScanAnalyzeEndpoint:
    """Tests for the /api/v1/scan/analyze endpoint."""

    def test_analyze_clean_app(self, client):
        """Clean app should return SAFE risk level."""
        payload = {
            "package_name": "com.clean.app",
            "version_code": 1,
            "signature": "hash123",
            "permissions": []
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert data["package_name"] == "com.clean.app"
        assert data["risk_level"] == "SAFE"

    def test_analyze_known_malware(self, client):
        """Known malware should return CRITICAL risk level."""
        payload = {
            "package_name": "com.example.virus",
            "version_code": 1,
            "signature": "virus_hash",
            "permissions": []
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert data["risk_level"] == "CRITICAL"
        assert data["threat_type"] == "Known Malware"

    def test_analyze_suspicious_permissions(self, client):
        """App with 5+ dangerous permissions should return HIGH risk."""
        payload = {
            "package_name": "com.suspicious.app",
            "version_code": 1,
            "signature": "hash",
            "permissions": [
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS"  # Need 5+ for HIGH risk
            ]
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert data["risk_level"] == "HIGH"
        assert data["threat_type"] == "Potential Spyware"

    def test_analyze_returns_heuristics_used(self, client):
        """Response should include which heuristics were used."""
        payload = {
            "package_name": "com.test.heuristics",
            "version_code": 1,
            "signature": "hash",
            "permissions": []
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert "heuristics_used" in data
        assert isinstance(data["heuristics_used"], list)

    def test_analyze_returns_description(self, client):
        """Response should include a description."""
        payload = {
            "package_name": "com.test.description",
            "version_code": 1,
            "signature": "hash",
            "permissions": []
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert "description" in data
        assert len(data["description"]) > 0

    # ========== Validation Tests ==========

    def test_missing_package_name_returns_422(self, client):
        """Missing required field should return 422."""
        payload = {
            "version_code": 1,
            "signature": "hash",
            "permissions": []
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 422

    def test_missing_version_code_returns_422(self, client):
        """Missing version_code should return 422."""
        payload = {
            "package_name": "com.test.app",
            "signature": "hash",
            "permissions": []
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 422

    def test_missing_signature_returns_422(self, client):
        """Missing signature should return 422."""
        payload = {
            "package_name": "com.test.app",
            "version_code": 1,
            "permissions": []
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 422

    def test_empty_body_returns_422(self, client):
        """Empty request body should return 422."""
        response = client.post("/api/v1/scan/analyze", json={})

        assert response.status_code == 422

    def test_invalid_json_returns_422(self, client):
        """Invalid JSON should return 422."""
        response = client.post(
            "/api/v1/scan/analyze",
            content="not valid json",
            headers={"Content-Type": "application/json"}
        )

        assert response.status_code == 422

    # ========== Edge Cases ==========

    def test_permissions_defaults_to_empty_list(self, client):
        """Permissions should default to empty list if not provided."""
        payload = {
            "package_name": "com.minimal.app",
            "version_code": 1,
            "signature": "hash"
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert data["risk_level"] == "SAFE"

    def test_large_permissions_list(self, client):
        """Should handle large permissions lists."""
        permissions = [f"android.permission.TEST_{i}" for i in range(100)]
        payload = {
            "package_name": "com.many.permissions",
            "version_code": 1,
            "signature": "hash",
            "permissions": permissions
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 200

    def test_unicode_package_name(self, client):
        """Should handle unicode in package names."""
        payload = {
            "package_name": "com.test.app_αβγ",
            "version_code": 1,
            "signature": "hash",
            "permissions": []
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert data["package_name"] == "com.test.app_αβγ"

    def test_large_version_code(self, client):
        """Should handle large version codes."""
        payload = {
            "package_name": "com.large.version",
            "version_code": 999999999,
            "signature": "hash",
            "permissions": []
        }

        response = client.post("/api/v1/scan/analyze", json=payload)

        assert response.status_code == 200


class TestResponseFormat:
    """Tests for response format compliance."""

    def test_response_contains_all_fields(self, client):
        """Response should contain all required fields."""
        payload = {
            "package_name": "com.test.format",
            "version_code": 1,
            "signature": "hash",
            "permissions": []
        }

        response = client.post("/api/v1/scan/analyze", json=payload)
        data = response.json()

        required_fields = ["package_name", "risk_level", "threat_type", "description", "heuristics_used"]
        for field in required_fields:
            assert field in data, f"Missing field: {field}"

    def test_risk_level_is_valid_enum(self, client):
        """Risk level should be a valid RiskLevel enum value."""
        payload = {
            "package_name": "com.test.enum",
            "version_code": 1,
            "signature": "hash",
            "permissions": []
        }

        response = client.post("/api/v1/scan/analyze", json=payload)
        data = response.json()

        valid_levels = [level.value for level in RiskLevel]
        assert data["risk_level"] in valid_levels

    def test_exception_handling_returns_500(self, client, monkeypatch):
        """Exception in engine should return 500 error."""
        def mock_analyze(*args, **kwargs):
            raise RuntimeError("Engine failure")
        
        from app.engine import heuristics
        monkeypatch.setattr(heuristics.engine, "analyze", mock_analyze)
        
        payload = {
            "package_name": "com.test.error",
            "version_code": 1,
            "signature": "hash",
            "permissions": []
        }
        
        response = client.post("/api/v1/scan/analyze", json=payload)
        
        assert response.status_code == 500
        assert "error" in response.json()["detail"].lower() or "failure" in response.json()["detail"].lower()
