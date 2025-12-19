import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.database import SessionLocal
from app.models.audit_log import AuditLog
from app.core.audit import log_audit_event
from fastapi import Request

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

def test_audit_log_creation(db_session):
    """log_audit_event utility creates a record in database"""
    # Create a mock request object
    mock_request = type('obj', (object,), {'client': type('obj', (object,), {'host': '127.0.0.1'})})
    
    initial_count = db_session.query(AuditLog).count()
    
    log_audit_event(
        db=db_session,
        request=mock_request,
        user_id=1,
        action="test_action",
        resource_type="test_resource",
        details={"foo": "bar"}
    )
    
    new_count = db_session.query(AuditLog).count()
    assert new_count == initial_count + 1
    
    last_log = db_session.query(AuditLog).order_by(AuditLog.id.desc()).first()
    assert last_log.action == "test_action"
    assert last_log.details == {"foo": "bar"}

