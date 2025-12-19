from sqlalchemy import Column, Integer, String, DateTime
from app.core.database import Base
from datetime import datetime

class Device(Base):
    __tablename__ = "devices"
    id = Column(Integer, primary_key=True, index=True)
    device_id = Column(String(100), unique=True, index=True)
    device_model = Column(String(100))
    os_version = Column(String(50))
    app_version = Column(String(50))
    registered_at = Column(DateTime, default=datetime.utcnow)
    last_seen = Column(DateTime, default=datetime.utcnow)

