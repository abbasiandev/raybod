from datetime import datetime
from fastapi import APIRouter, HTTPException, Request, Depends
from sqlalchemy.orm import Session
from app.schemas.scan_schema import AppMetadata, ScanResult
from app.engine.heuristics import engine
from app.core.database import get_db
from app.models.scan_log import ScanLog
from slowapi.errors import RateLimitExceeded

router = APIRouter()

@router.post("/analyze", response_model=ScanResult)
async def analyze_app(
    metadata: AppMetadata, 
    request: Request,
    db: Session = Depends(get_db)
):
    try:
        # Rate limit is handled by the limiter in app state
        from app.main import limiter
        @limiter.limit("10/minute")
        async def limited_analyze(request: Request, metadata: AppMetadata):
            return engine.analyze(metadata)
        
        result = await limited_analyze(request, metadata)
        
        # Log scan for analytics dashboard
        log_entry = ScanLog(
            package_name=metadata.package_name,
            version_code=metadata.version_code,
            signature=metadata.signature,
            risk_level=result.risk_level.value,
            threat_type=result.threat_type,
            heuristics_used=result.heuristics_used,
            device_id=request.headers.get("X-Device-ID"),
            ip_address=request.client.host if request.client else None,
            timestamp=datetime.utcnow()
        )
        db.add(log_entry)
        db.commit()
        
        return result
    except RateLimitExceeded:
        raise HTTPException(status_code=429, detail="Too many requests")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

