"""
ScanLog model for analytics and threat tracking.
"""
from datetime import datetime
from sqlalchemy import Column, Integer, String, DateTime, JSON
from app.core.database import Base


class ScanLog(Base):
    """Log of all scan requests for analytics."""
    __tablename__ = "scan_logs"
    
    id = Column(Integer, primary_key=True, index=True)
    package_name = Column(String(255), nullable=False, index=True)
    version_code = Column(Integer, nullable=False)
    signature = Column(String(255), nullable=True)
    risk_level = Column(String(20), nullable=False, index=True)
    threat_type = Column(String(100), nullable=True)
    heuristics_used = Column(JSON, default=[])
    device_id = Column(String(100), nullable=True, index=True)
    ip_address = Column(String(45), nullable=True)
    timestamp = Column(DateTime, default=datetime.utcnow, index=True)
    
    def __repr__(self):
        return f"<ScanLog(package='{self.package_name}', risk='{self.risk_level}')>"
