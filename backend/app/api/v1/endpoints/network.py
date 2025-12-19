from fastapi import APIRouter, Depends, HTTPException
from app.schemas.network_schema import NetworkAnalysisRequest, NetworkAnalysisResult
from app.engine.network_heuristics import network_heuristic_engine

router = APIRouter()

@router.post("/analyze", response_model=NetworkAnalysisResult)
async def analyze_network(request: NetworkAnalysisRequest):
    """
    Analyze a batch of network flows for threats.
    """
    try:
        alerts, blocklist = network_heuristic_engine.analyze_flows(request.flows)
        return NetworkAnalysisResult(
            alerts=alerts,
            blocklist=blocklist
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

