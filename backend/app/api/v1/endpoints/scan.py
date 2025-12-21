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

# Rate limiting configuration
RATE_LIMIT_PER_MINUTE = "10/minute"

@router.post("/analyze", response_model=ScanResult)
async def analyze_app(
    metadata: AppMetadata, 
    request: Request,
    db: Session = Depends(get_db)
):
    """
    Analyze a single app package for malware and security threats.
    
    Applies heuristic analysis and returns risk assessment.
    Rate limited to prevent abuse.
    """
    try:
        # Apply rate limiting
        from app.main import limiter
        @limiter.limit(RATE_LIMIT_PER_MINUTE)
        async def limited_analyze(request: Request, metadata: AppMetadata):
            return engine.analyze(metadata)
        
        scan_result = await limited_analyze(request, metadata)
        
        # Create audit log entry for analytics
        scan_log_entry = ScanLog(
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
        db.add(scan_log_entry)
        db.commit()
        
        # Broadcast critical threats to connected clients via WebSocket
        if scan_result.risk_level in ["HIGH", "CRITICAL"]:
            threat_notification = {
                "type": "NEW_THREAT",
                "package_name": scan_result.package_name,
                "risk_level": scan_result.risk_level.value,
                "threat_type": scan_result.threat_type,
                "timestamp": datetime.utcnow().isoformat()
            }
            await manager.broadcast(json.dumps(threat_notification))
        
        return scan_result
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
    @limiter.limit(RATE_LIMIT_PER_MINUTE)
    async def limited_batch(request: Request):
        scan_results = []
        
        for package_metadata in batch_data.packages:
            analysis_result = engine.analyze(package_metadata)
            
            # Create audit log for each package in batch
            scan_log_entry = ScanLog(
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
            db.add(scan_log_entry)
            scan_results.append(analysis_result)
        
        db.commit()
        return scan_results

    try:
        batch_scan_results = await limited_batch(request)
        return BatchScanResult(results=batch_scan_results)
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
    user_feedback = ThreatFeedback(
        package_name=report.package_name,
        is_false_positive=report.is_false_positive,
        user_comment=report.user_comment,
        original_risk_level=report.original_risk_level.value
    )
    db.add(user_feedback)
    db.commit()
    
    return {
        "status": "success", 
        "message": "Feedback received. Thank you for helping us improve!"
    }
