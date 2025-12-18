"""
Test suite for the FastAPI application main module.
"""
import pytest
from fastapi.testclient import TestClient
from fastapi import FastAPI
from app.main import app
from app.core.config import settings


class TestAppInitialization:
    """Tests for application initialization."""

    def test_app_is_fastapi_instance(self):
        """App should be an instance of FastAPI."""
        assert isinstance(app, FastAPI)

    def test_app_title(self):
        """App should have correct title."""
        assert app.title == settings.APP_NAME

    def test_app_version(self):
        """App should have correct version."""
        assert app.version == settings.VERSION

    def test_app_description(self):
        """App should have description."""
        assert app.description == "Central Intelligence for Mobile Threat Defense"

    def test_app_includes_scan_router(self):
        """App should include the scan router."""
        routes = [route.path for route in app.routes]
        assert any("/api/v1/scan/analyze" in route or route == "/api/v1/scan/analyze" for route in routes)

    def test_app_has_health_endpoint(self):
        """App should have health check endpoint."""
        routes = [route.path for route in app.routes]
        assert "/health" in routes

    def test_app_routes_registered(self):
        """All expected routes should be registered."""
        routes = {route.path: route for route in app.routes}
        
        assert "/health" in routes
        # Check for scan routes
        scan_routes = [route.path for route in routes.values() if "/scan" in route.path]
        assert len(scan_routes) > 0


class TestHealthEndpointIntegration:
    """Integration tests for health endpoint via main app."""

    @pytest.fixture
    def client(self):
        """Create a test client for the FastAPI application."""
        return TestClient(app)

    def test_health_endpoint_via_app(self, client):
        """Health endpoint should work through main app."""
        response = client.get("/health")
        
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert data["service"] == "Hybrid Cloud Sentinel Brain"


class TestAppConfiguration:
    """Tests for app configuration and metadata."""

    def test_app_openapi_schema_available(self):
        """App should generate OpenAPI schema."""
        schema = app.openapi()
        
        assert "openapi" in schema
        assert "info" in schema
        assert schema["info"]["title"] == settings.APP_NAME
        assert schema["info"]["version"] == settings.VERSION

    def test_app_tags_configured(self):
        """App should have tags configured for routers."""
        # Check if scan tag exists in routes
        scan_routes = [route for route in app.routes if hasattr(route, "tags") and "scan" in route.tags]
        assert len(scan_routes) > 0

