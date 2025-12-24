"""
Authentication API endpoints.
"""
from fastapi import APIRouter, HTTPException, Depends, Response, Request, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session, joinedload
from pydantic import BaseModel
from typing import Optional

from app.core.database import get_db
from app.models.user import User
from app.core.audit import log_audit_event
from app.services.auth import (
    hash_password,
    verify_password,
    create_access_token,
    get_current_user
)

router = APIRouter()


class LoginResponse(BaseModel):
    """Response model for login endpoint."""
    access_token: str
    token_type: str = "bearer"
    username: str
    role: str


class UserResponse(BaseModel):
    """Response model for current user."""
    id: int
    username: str
    role: str
    plan: str
    permissions: dict
    is_active: bool
    
    class Config:
        from_attributes = True


class MessageResponse(BaseModel):
    """Simple message response."""
    message: str


@router.post("/login", response_model=LoginResponse)
async def login(
    request: Request,
    response: Response,
    form_data: OAuth2PasswordRequestForm = Depends(),
    db: Session = Depends(get_db)
):
    """
    Authenticate user and return JWT token.
    Sets token in HTTP-only cookie for browser clients.
    """
    # Find user with role relationship loaded
    user = db.query(User).options(joinedload(User.role)).filter(User.username == form_data.username).first()
    
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password"
        )
    
    # Verify password
    if not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password"
        )
    
    # Check if active
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User account is inactive"
        )
    
    # Ensure role is loaded
    if not user.role:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="User role not configured"
        )
    
    # Create token
    token = create_access_token(
        user_id=user.id,
        username=user.username,
        role=user.role.name
    )
    
    # Log audit event
    log_audit_event(
        db=db,
        request=request,
        user_id=user.id,
        action="login",
        resource_type="auth",
        details={"username": user.username}
    )
    
    # Set HTTP-only cookie for browser clients
    response.set_cookie(
        key="access_token",
        value=f"Bearer {token}",
        httponly=True,
        max_age=3600,  # 1 hour
        samesite="lax",
        secure=False  # Set to True in production with HTTPS
    )
    
    return LoginResponse(
        access_token=token,
        username=user.username,
        role=user.role.name if user.role else "unknown"
    )


@router.post("/logout", response_model=MessageResponse)
async def logout(response: Response):
    """
    Logout user by clearing the access token cookie.
    """
    response.delete_cookie(
        key="access_token",
        httponly=True,
        samesite="lax"
    )
    
    return MessageResponse(message="Successfully logged out")


@router.get("/me", response_model=UserResponse)
async def get_me(current_user: User = Depends(get_current_user)):
    """
    Get current authenticated user's information.
    """
    return UserResponse(
        id=current_user.id,
        username=current_user.username,
        role=current_user.role.name,
        plan=current_user.plan,
        permissions=current_user.role.permissions,
        is_active=current_user.is_active
    )
