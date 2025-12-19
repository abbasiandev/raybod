from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.services.reputation import reputation_service
from pydantic import BaseModel

router = APIRouter()

class ReputationResponse(BaseModel):
    package_name: str
    reputation_score: float
    confidence: str
    total_scans: int

@router.get("/{package_name}", response_model=ReputationResponse)
async def get_reputation(
    package_name: str,
    db: Session = Depends(get_db)
):
    """Get reputation score for a package."""
    return reputation_service.calculate_reputation(db, package_name)

