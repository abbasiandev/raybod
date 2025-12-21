from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.core.database import get_db, AllowlistEntry
from pydantic import BaseModel

router = APIRouter()

class AllowlistCheckResponse(BaseModel):
    """Response schema for allowlist verification."""
    package_name: str
    is_allowed: bool

@router.get("/check/{package_name}", response_model=AllowlistCheckResponse)
async def check_allowlist(
    package_name: str,
    db: Session = Depends(get_db)
):
    """
    Check if a package is in the global allowlist.
    
    This is a fast-path check used to skip scanning for known safe packages.
    Returns immediately without performing any analysis.
    """
    allowlist_entry = db.query(AllowlistEntry).filter(
        AllowlistEntry.package_name == package_name
    ).first()
    
    return AllowlistCheckResponse(
        package_name=package_name,
        is_allowed=allowlist_entry is not None
    )

