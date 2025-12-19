from sqlalchemy.orm import Session
from app.models.audit_log import AuditLog
from fastapi import Request
from typing import Optional, Any

def log_audit_event(
    db: Session,
    request: Request,
    user_id: Optional[int],
    action: str,
    resource_type: str,
    resource_id: Optional[str] = None,
    details: Optional[Any] = None
):
    """Utility to log audit events."""
    audit_log = AuditLog(
        user_id=user_id,
        action=action,
        resource_type=resource_type,
        resource_id=resource_id,
        details=details,
        ip_address=request.client.host if request.client else None
    )
    db.add(audit_log)
    db.commit()

