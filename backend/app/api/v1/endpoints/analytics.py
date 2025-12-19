from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from sqlalchemy import func, desc
from app.core.database import get_db
from app.models.scan_log import ScanLog
from pydantic import BaseModel
from datetime import datetime, timedelta
from typing import List, Dict, Any

router = APIRouter()

class TopThreatResponse(BaseModel):
    top_threats: List[Dict[str, Any]]

class TimeseriesResponse(BaseModel):
    labels: List[str]
    values: List[int]

@router.get("/top-threats", response_model=TopThreatResponse)
async def get_top_threats(
    limit: int = Query(10, ge=1, le=50),
    db: Session = Depends(get_db)
):
    """Get most frequent threats."""
    results = db.query(
        ScanLog.threat_type,
        func.count(ScanLog.id).label("count")
    ).filter(
        ScanLog.risk_level.in_(["HIGH", "CRITICAL"]),
        ScanLog.threat_type != None
    ).group_by(
        ScanLog.threat_type
    ).order_by(
        desc("count")
    ).limit(limit).all()
    
    top_threats = [{"threat_type": r[0], "count": r[1]} for r in results]
    return TopThreatResponse(top_threats=top_threats)

@router.get("/timeseries", response_model=TimeseriesResponse)
async def get_timeseries(
    days: int = Query(7, ge=1, le=90),
    db: Session = Depends(get_db)
):
    """Get scan trend over time."""
    labels = []
    values = []
    for i in range(days - 1, -1, -1):
        day = datetime.utcnow().date() - timedelta(days=i)
        labels.append(day.strftime("%Y-%m-%d"))
        
        count = db.query(func.count(ScanLog.id)).filter(
            func.date(ScanLog.timestamp) == day
        ).scalar() or 0
        values.append(count)
        
    return TimeseriesResponse(labels=labels, values=values)

