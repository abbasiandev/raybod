from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.core.database import get_db, AllowlistEntry
from pydantic import BaseModel

router = APIRouter()

class AllowlistCheckResponse(BaseModel):
    package_name: str
    is_allowed: bool

@router.get("/check/{package_name}", response_model=AllowlistCheckResponse)
async def check_allowlist(
    package_name: str,
    db: Session = Depends(get_db)
):
    """Fast allowlist check endpoint."""
    entry = db.query(AllowlistEntry).filter(
        AllowlistEntry.package_name == package_name
    ).first()
    return AllowlistCheckResponse(
        package_name=package_name,
        is_allowed=entry is not None
    )

