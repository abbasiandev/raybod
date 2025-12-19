from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.api_key import ApiKey
from app.models.user import User
from app.services.auth import get_current_user, require_permission
from pydantic import BaseModel
from typing import List, Optional
import secrets
import hashlib
from datetime import datetime, timedelta

router = APIRouter()

class ApiKeyCreate(BaseModel):
    name: str
    scopes: List[str] = ["scan:read"]
    expires_in_days: Optional[int] = 30

class ApiKeyResponse(BaseModel):
    id: int
    name: str
    scopes: List[str]
    created_at: datetime
    expires_at: Optional[datetime]
    api_key: Optional[str] = None # Only returned on creation

    class Config:
        from_attributes = True

@router.post("/", response_model=ApiKeyResponse)
async def create_api_key(
    request: ApiKeyCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new API key for the current user."""
    # Generate random key
    raw_key = f"hcs_{secrets.token_urlsafe(32)}"
    key_hash = hashlib.sha256(raw_key.encode()).hexdigest()
    
    expires_at = None
    if request.expires_in_days:
        expires_at = datetime.utcnow() + timedelta(days=request.expires_in_days)
        
    api_key = ApiKey(
        key_hash=key_hash,
        user_id=current_user.id,
        name=request.name,
        scopes=request.scopes,
        expires_at=expires_at
    )
    db.add(api_key)
    db.commit()
    db.refresh(api_key)
    
    response = ApiKeyResponse.from_orm(api_key)
    response.api_key = raw_key # Return raw key only once
    return response

@router.get("/", response_model=List[ApiKeyResponse])
async def list_api_keys(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all API keys for the current user."""
    keys = db.query(ApiKey).filter(ApiKey.user_id == current_user.id).all()
    return keys

@router.delete("/{key_id}")
async def delete_api_key(
    key_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Revoke an API key."""
    key = db.query(ApiKey).filter(ApiKey.id == key_id, ApiKey.user_id == current_user.id).first()
    if not key:
        raise HTTPException(status_code=404, detail="API key not found")
    
    db.delete(key)
    db.commit()
    return {"message": "API key revoked"}

