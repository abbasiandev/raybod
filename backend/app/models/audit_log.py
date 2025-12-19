from sqlalchemy import Column, Integer, String, DateTime, JSON, ForeignKey
from app.core.database import Base
from datetime import datetime

class AuditLog(Base):
    __tablename__ = "audit_logs"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    action = Column(String(50))  # "create", "update", "delete", "login"
    resource_type = Column(String(50))  # "allowlist", "blocklist", "user"
    resource_id = Column(String(100))
    details = Column(JSON)
    ip_address = Column(String(45))
    timestamp = Column(DateTime, default=datetime.utcnow)

