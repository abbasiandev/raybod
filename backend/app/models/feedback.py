from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey
from sqlalchemy.sql import func
from app.core.database import Base

class ThreatFeedback(Base):
    __tablename__ = "threat_feedback"

    id = Column(Integer, primary_key=True, index=True)
    package_name = Column(String, index=True)
    is_false_positive = Column(Boolean, default=True)
    user_comment = Column(String, nullable=True)
    original_risk_level = Column(String)
    timestamp = Column(DateTime(timezone=True), server_default=func.now())


