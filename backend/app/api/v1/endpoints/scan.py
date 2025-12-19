from datetime import datetime
from fastapi import APIRouter, HTTPException, Request, Depends
from sqlalchemy.orm import Session
from app.schemas.scan_schema import AppMetadata, ScanResult, BatchScanRequest, BatchScanResult, FeedbackReport
from app.models.feedback import ThreatFeedback
from app.engine.heuristics import engine
from app.core.database import get_db
from app.models.scan_log import ScanLog
from app.api.v1.endpoints.websocket import manager
from slowapi.errors import RateLimitExceeded
import json

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
            version_name=metadata.version_name,
            signature=metadata.signature,
            risk_level=result.risk_level.value,
            threat_type=result.threat_type,
            heuristics_used=result.heuristics_used,
            intents=metadata.intents,
            install_time=metadata.install_time,
            last_update_time=metadata.last_update_time,
            has_reflection=1 if metadata.has_reflection else 0 if metadata.has_reflection is not None else None,
            has_dynamic_loading=1 if metadata.has_dynamic_loading else 0 if metadata.has_dynamic_loading is not None else None,
            device_id=request.headers.get("X-Device-ID"),
            ip_address=request.client.host if request.client else None,
            timestamp=datetime.utcnow()
        )
        db.add(log_entry)
        db.commit()
        
        # Broadcast high-risk threats to websocket clients
        if result.risk_level in ["HIGH", "CRITICAL"]:
            await manager.broadcast(json.dumps({
                "type": "NEW_THREAT",
                "package_name": result.package_name,
                "risk_level": result.risk_level.value,
                "threat_type": result.threat_type,
                "timestamp": datetime.utcnow().isoformat()
            }))
        
        return result
    except RateLimitExceeded:
        raise HTTPException(status_code=429, detail="Too many requests")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/batch", response_model=BatchScanResult)
async def batch_scan(
    request: Request,
    batch_data: BatchScanRequest,
    db: Session = Depends(get_db)
):
    """Process multiple packages in single request."""
    results = []
    # Note: Rate limiting for batch can be handled per-request or per-app in batch.
    # For now, we apply it to the whole request.
    from app.main import limiter
    @limiter.limit("10/minute")
    async def limited_batch(request: Request):
        batch_results = []
        for metadata in batch_data.packages:
            result = engine.analyze(metadata)
            
            # Log each scan
            log_entry = ScanLog(
                package_name=metadata.package_name,
                version_code=metadata.version_code,
                version_name=metadata.version_name,
                signature=metadata.signature,
                risk_level=result.risk_level.value,
                threat_type=result.threat_type,
                heuristics_used=result.heuristics_used,
                intents=metadata.intents,
                install_time=metadata.install_time,
                last_update_time=metadata.last_update_time,
                has_reflection=1 if metadata.has_reflection else 0 if metadata.has_reflection is not None else None,
                has_dynamic_loading=1 if metadata.has_dynamic_loading else 0 if metadata.has_dynamic_loading is not None else None,
                device_id=request.headers.get("X-Device-ID"),
                ip_address=request.client.host if request.client else None,
                timestamp=datetime.utcnow()
            )
            db.add(log_entry)
            batch_results.append(result)
        db.commit()
        return batch_results

    try:
        results = await limited_batch(request)
        return BatchScanResult(results=results)
    except RateLimitExceeded:
        raise HTTPException(status_code=429, detail="Too many requests")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/feedback")
async def report_feedback(
    report: FeedbackReport,
    db: Session = Depends(get_db)
):
    """Report false positive or feedback on a scan result to improve model (Concept Drift)."""
    feedback_entry = ThreatFeedback(
        package_name=report.package_name,
        is_false_positive=report.is_false_positive,
        user_comment=report.user_comment,
        original_risk_level=report.original_risk_level.value
    )
    db.add(feedback_entry)
    db.commit()
    return {"status": "success", "message": "Feedback received. Thank you for helping us improve!"}
