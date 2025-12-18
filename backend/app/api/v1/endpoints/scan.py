from fastapi import APIRouter, HTTPException, Request
from app.schemas.scan_schema import AppMetadata, ScanResult
from app.engine.heuristics import engine
from slowapi.errors import RateLimitExceeded

router = APIRouter()

@router.post("/analyze", response_model=ScanResult)
async def analyze_app(metadata: AppMetadata, request: Request):
    try:
        # Rate limit is handled by the limiter in app state
        from app.main import limiter
        @limiter.limit("10/minute")
        async def limited_analyze(request: Request, metadata: AppMetadata):
            return engine.analyze(metadata)
        
        return await limited_analyze(request, metadata)
    except RateLimitExceeded:
        raise HTTPException(status_code=429, detail="Too many requests")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
