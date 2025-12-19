from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.core.database import get_db
from app.models.scan_log import ScanLog
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

router = APIRouter()

class ThreatDto(BaseModel):
    package_name: str
    risk_level: str
    threat_type: Optional[str]
    timestamp: datetime

    class Config:
        from_attributes = True

class ThreatFeedResponse(BaseModel):
    threats: List[ThreatDto]
    total: int

@router.get("/feed", response_model=ThreatFeedResponse)
async def get_threat_feed(
    limit: int = Query(10, ge=1, le=100),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db)
):
    """Get recent threat feed."""
    query = db.query(ScanLog).filter(
        ScanLog.risk_level.in_(["HIGH", "CRITICAL"])
    )
    
    total = query.count()
    threats = query.order_by(ScanLog.timestamp.desc()).offset(offset).limit(limit).all()
    
    return ThreatFeedResponse(
        threats=threats,
        total=total
    )

