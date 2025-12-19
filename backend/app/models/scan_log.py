"""
ScanLog model for analytics and threat tracking.
"""
from datetime import datetime
from sqlalchemy import Column, Integer, String, DateTime, JSON, BigInteger
from app.core.database import Base


class ScanLog(Base):
    """Log of all scan requests for analytics."""
    __tablename__ = "scan_logs"
    
    id = Column(Integer, primary_key=True, index=True)
    package_name = Column(String(255), nullable=False, index=True)
    version_code = Column(Integer, nullable=False, index=True)
    version_name = Column(String(100), nullable=True)
    signature = Column(String(255), nullable=True, index=True)
    risk_level = Column(String(20), nullable=False, index=True)
    threat_type = Column(String(100), nullable=True, index=True)
    heuristics_used = Column(JSON, default=[])
    intents = Column(JSON, nullable=True)
    install_time = Column(BigInteger, nullable=True)
    last_update_time = Column(BigInteger, nullable=True)
    has_reflection = Column(Integer, nullable=True) # Store as 0/1 or NULL
    has_dynamic_loading = Column(Integer, nullable=True)
    device_id = Column(String(100), nullable=True, index=True)
    ip_address = Column(String(45), nullable=True)
    timestamp = Column(DateTime, default=datetime.utcnow, index=True)
    
    def __repr__(self):
        return f"<ScanLog(package='{self.package_name}', risk='{self.risk_level}')>"
