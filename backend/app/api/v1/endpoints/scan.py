from fastapi import APIRouter, HTTPException
from app.schemas.scan_schema import AppMetadata, ScanResult
from app.engine.heuristics import engine

router = APIRouter()

@router.post("/analyze", response_model=ScanResult)
async def analyze_app(metadata: AppMetadata):
    try:
        result = engine.analyze(metadata)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
