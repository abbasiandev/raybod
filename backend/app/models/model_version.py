from sqlalchemy import Column, Integer, String, DateTime, Boolean, ForeignKey
from app.core.database import Base
from datetime import datetime

class ModelVersion(Base):
    __tablename__ = "model_versions"
    id = Column(Integer, primary_key=True, index=True)
    version = Column(String(50), unique=True, index=True)  # e.g., "1.2.0"
    file_path = Column(String(255))
    file_size = Column(Integer)
    checksum_sha256 = Column(String(64))
    is_active = Column(Boolean, default=False)
    uploaded_at = Column(DateTime, default=datetime.utcnow)
    uploaded_by = Column(Integer, ForeignKey("users.id"))

