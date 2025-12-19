from sqlalchemy import Column, Integer, String, DateTime, JSON, ForeignKey
from app.core.database import Base
from datetime import datetime

class ApiKey(Base):
    __tablename__ = "api_keys"
    id = Column(Integer, primary_key=True, index=True)
    key_hash = Column(String(255), unique=True, index=True)  # Hashed API key
    user_id = Column(Integer, ForeignKey("users.id"))
    name = Column(String(100))  # User-friendly name
    scopes = Column(JSON)  # e.g., ["scan:read", "threats:read"]
    expires_at = Column(DateTime, nullable=True)
    last_used = Column(DateTime, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

