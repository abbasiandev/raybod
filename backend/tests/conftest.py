"""
Test configuration and fixtures for pytest.
"""
import os
import pytest
from fastapi.testclient import TestClient

# Set environment variable to disable rate limiting in tests
os.environ["TESTING"] = "1"

from app.main import app
from app.core.database import init_db

@pytest.fixture(scope="session", autouse=True)
def setup_test_environment():
    """Setup test environment before running tests."""
    # Initialize database with test data
    init_db()

@pytest.fixture
def client():
    """Create a test client for the FastAPI application."""
    return TestClient(app)
