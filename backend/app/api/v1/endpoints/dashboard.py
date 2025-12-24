"""
Dashboard web endpoints with Jinja2 templates.
"""
from datetime import datetime, timedelta
from typing import Optional
from fastapi import APIRouter, Request, Depends, Form, HTTPException, Response
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session, joinedload
from sqlalchemy import func
import json

from app.core.database import get_db, AllowlistEntry, BlocklistEntry
from app.models.user import User, Role
from app.models.scan_log import ScanLog
from app.services.auth import (
    verify_password,
    create_access_token,
    get_token_from_request,
    verify_token,
    hash_password
)

router = APIRouter()
templates = Jinja2Templates(directory="app/templates")


def get_optional_user(request: Request, db: Session = Depends(get_db)) -> Optional[User]:
    """Get current user if authenticated, None otherwise."""
    token = get_token_from_request(request)
    if not token:
        return None
    try:
        payload = verify_token(token)
        user_id = payload.get("user_id")
        if user_id:
            return db.query(User).options(joinedload(User.role)).filter(User.id == user_id).first()
    except:
        pass
    return None


def require_auth(request: Request, db: Session = Depends(get_db)) -> User:
    """Require authentication, redirect to login if not authenticated."""
    user = get_optional_user(request, db)
    if not user:
        raise HTTPException(status_code=302, headers={"Location": "/dashboard/login"})
    return user


def require_permission(permission: str):
    """Factory for permission-based access control."""
    def checker(request: Request, db: Session = Depends(get_db)) -> User:
        user = require_auth(request, db)
        if not user.has_permission(permission):
            raise HTTPException(status_code=403, detail="Permission denied")
        return user
    return checker


# --------------------------------------------------------------------------
# Authentication Pages
# --------------------------------------------------------------------------

@router.get("/login", response_class=HTMLResponse)
async def login_page(request: Request, db: Session = Depends(get_db)):
    """Render login page."""
    user = get_optional_user(request, db)
    if user:
        return RedirectResponse(url="/dashboard/", status_code=302)
    return templates.TemplateResponse("login.html", {
        "request": request,
        "current_user": None,
        "error": None
    })


@router.post("/login")
async def login_submit(
    request: Request,
    response: Response,
    username: str = Form(...),
    password: str = Form(...),
    db: Session = Depends(get_db)
):
    """Handle login form submission."""
    user = db.query(User).options(joinedload(User.role)).filter(User.username == username).first()
    
    if not user or not verify_password(password, user.hashed_password):
        return templates.TemplateResponse("login.html", {
            "request": request,
            "current_user": None,
            "error": "Invalid username or password"
        }, status_code=401)
    
    if not user.is_active:
        return templates.TemplateResponse("login.html", {
            "request": request,
            "current_user": None,
            "error": "Account is inactive"
        }, status_code=401)
    
    # Ensure role is loaded and handle None case
    if not user.role:
        return templates.TemplateResponse("login.html", {
            "request": request,
            "current_user": None,
            "error": "User role not configured. Please contact administrator."
        }, status_code=500)
    
    token = create_access_token(
        user_id=user.id,
        username=user.username,
        role=user.role.name
    )
    
    redirect = RedirectResponse(url="/dashboard/", status_code=302)
    redirect.set_cookie(
        key="access_token",
        value=f"Bearer {token}",
        httponly=True,
        max_age=3600,
        samesite="lax"
    )
    return redirect


@router.get("/logout")
async def logout(response: Response):
    """Handle logout and redirect to home page."""
    redirect = RedirectResponse(url="/", status_code=302)
    redirect.delete_cookie(key="access_token")
    return redirect


# --------------------------------------------------------------------------
# Dashboard Overview
# --------------------------------------------------------------------------

@router.get("/", response_class=HTMLResponse)
async def dashboard_index(
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_auth)
):
    """Render dashboard overview page."""
    # Calculate stats
    total_scans = db.query(func.count(ScanLog.id)).scalar() or 0
    today = datetime.utcnow().date()
    scans_today = db.query(func.count(ScanLog.id)).filter(
        func.date(ScanLog.timestamp) == today
    ).scalar() or 0
    
    safe_count = db.query(func.count(ScanLog.id)).filter(ScanLog.risk_level == "SAFE").scalar() or 0
    risky_count = db.query(func.count(ScanLog.id)).filter(ScanLog.risk_level.in_(["MEDIUM", "HIGH"])).scalar() or 0
    threat_count = db.query(func.count(ScanLog.id)).filter(ScanLog.risk_level.in_(["HIGH", "CRITICAL"])).scalar() or 0
    
    stats = {
        "total_scans": total_scans,
        "scans_today": scans_today,
        "safe_count": safe_count,
        "safe_percentage": (safe_count / total_scans * 100) if total_scans else 0,
        "risky_count": risky_count,
        "risky_percentage": (risky_count / total_scans * 100) if total_scans else 0,
        "threat_count": threat_count,
        "threat_percentage": (threat_count / total_scans * 100) if total_scans else 0,
    }
    
    # Get last 7 days trend
    trends_labels = []
    trends_values = []
    for i in range(6, -1, -1):
        day = datetime.utcnow().date() - timedelta(days=i)
        trends_labels.append(day.strftime("%b %d"))
        count = db.query(func.count(ScanLog.id)).filter(
            func.date(ScanLog.timestamp) == day
        ).scalar() or 0
        trends_values.append(count)
    
    trends_data = {"labels": trends_labels, "values": trends_values}
    
    # Risk distribution
    risk_counts = {
        "SAFE": db.query(func.count(ScanLog.id)).filter(ScanLog.risk_level == "SAFE").scalar() or 0,
        "LOW": db.query(func.count(ScanLog.id)).filter(ScanLog.risk_level == "LOW").scalar() or 0,
        "MEDIUM": db.query(func.count(ScanLog.id)).filter(ScanLog.risk_level == "MEDIUM").scalar() or 0,
        "HIGH": db.query(func.count(ScanLog.id)).filter(ScanLog.risk_level == "HIGH").scalar() or 0,
        "CRITICAL": db.query(func.count(ScanLog.id)).filter(ScanLog.risk_level == "CRITICAL").scalar() or 0,
    }
    risk_data = {
        "labels": list(risk_counts.keys()),
        "values": list(risk_counts.values())
    }
    
    # Recent scans
    recent_scans = db.query(ScanLog).order_by(ScanLog.timestamp.desc()).limit(10).all()
    
    return templates.TemplateResponse("dashboard/index.html", {
        "request": request,
        "current_user": current_user,
        "active_page": "overview",
        "stats": stats,
        "trends_data": trends_data,
        "risk_data": risk_data,
        "recent_scans": recent_scans
    })


# --------------------------------------------------------------------------
# Allowlist Management
# --------------------------------------------------------------------------

@router.get("/allowlist", response_class=HTMLResponse)
async def allowlist_page(
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_permission("manage_allowlist"))
):
    """Render allowlist management page."""
    entries = db.query(AllowlistEntry).order_by(AllowlistEntry.id.desc()).all()
    return templates.TemplateResponse("dashboard/allowlist.html", {
        "request": request,
        "current_user": current_user,
        "active_page": "allowlist",
        "entries": entries
    })


@router.post("/allowlist")
async def add_to_allowlist(
    request: Request,
    package_name: str = Form(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_permission("manage_allowlist"))
):
    """Add package to allowlist."""
    existing = db.query(AllowlistEntry).filter(AllowlistEntry.package_name == package_name).first()
    if existing:
        raise HTTPException(status_code=400, detail="Package already in allowlist")
    
    entry = AllowlistEntry(package_name=package_name)
    db.add(entry)
    db.commit()
    
    # Return updated table for HTMX
    entries = db.query(AllowlistEntry).order_by(AllowlistEntry.id.desc()).all()
    return templates.TemplateResponse("dashboard/partials/allowlist_table.html", {
        "request": request,
        "entries": entries
    })


@router.delete("/allowlist/{entry_id}")
async def delete_from_allowlist(
    entry_id: int,
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_permission("manage_allowlist"))
):
    """Remove package from allowlist."""
    entry = db.query(AllowlistEntry).filter(AllowlistEntry.id == entry_id).first()
    if entry:
        db.delete(entry)
        db.commit()
    
    entries = db.query(AllowlistEntry).order_by(AllowlistEntry.id.desc()).all()
    return templates.TemplateResponse("dashboard/partials/allowlist_table.html", {
        "request": request,
        "entries": entries
    })


# --------------------------------------------------------------------------
# Blocklist Management
# --------------------------------------------------------------------------

@router.get("/blocklist", response_class=HTMLResponse)
async def blocklist_page(
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_permission("manage_blocklist"))
):
    """Render blocklist management page."""
    entries = db.query(BlocklistEntry).order_by(BlocklistEntry.id.desc()).all()
    return templates.TemplateResponse("dashboard/blocklist.html", {
        "request": request,
        "current_user": current_user,
        "active_page": "blocklist",
        "entries": entries
    })


@router.post("/blocklist")
async def add_to_blocklist(
    request: Request,
    package_name: str = Form(...),
    threat_type: str = Form("Known Malware"),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_permission("manage_blocklist"))
):
    """Add package to blocklist."""
    existing = db.query(BlocklistEntry).filter(BlocklistEntry.package_name == package_name).first()
    if existing:
        raise HTTPException(status_code=400, detail="Package already in blocklist")
    
    entry = BlocklistEntry(package_name=package_name, threat_type=threat_type)
    db.add(entry)
    db.commit()
    
    entries = db.query(BlocklistEntry).order_by(BlocklistEntry.id.desc()).all()
    return templates.TemplateResponse("dashboard/partials/blocklist_table.html", {
        "request": request,
        "entries": entries
    })


@router.delete("/blocklist/{entry_id}")
async def delete_from_blocklist(
    entry_id: int,
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_permission("manage_blocklist"))
):
    """Remove package from blocklist."""
    entry = db.query(BlocklistEntry).filter(BlocklistEntry.id == entry_id).first()
    if entry:
        db.delete(entry)
        db.commit()
    
    entries = db.query(BlocklistEntry).order_by(BlocklistEntry.id.desc()).all()
    return templates.TemplateResponse("dashboard/partials/blocklist_table.html", {
        "request": request,
        "entries": entries
    })


# --------------------------------------------------------------------------
# User Management
# --------------------------------------------------------------------------

@router.get("/users", response_class=HTMLResponse)
async def users_page(
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_permission("manage_users"))
):
    """Render user management page."""
    users = db.query(User).order_by(User.id).all()
    roles = db.query(Role).all()
    return templates.TemplateResponse("dashboard/users.html", {
        "request": request,
        "current_user": current_user,
        "active_page": "users",
        "users": users,
        "roles": roles
    })


@router.post("/users")
async def create_user(
    request: Request,
    username: str = Form(...),
    password: str = Form(...),
    role_id: int = Form(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_permission("manage_users"))
):
    """Create new user."""
    existing = db.query(User).filter(User.username == username).first()
    if existing:
        raise HTTPException(status_code=400, detail="Username already exists")
    
    user = User(
        username=username,
        hashed_password=hash_password(password),
        role_id=role_id,
        is_active=True
    )
    db.add(user)
    db.commit()
    
    return RedirectResponse(url="/dashboard/users", status_code=302)


@router.post("/users/{user_id}/toggle")
async def toggle_user(
    user_id: int,
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_permission("manage_users"))
):
    """Toggle user active status."""
    user = db.query(User).filter(User.id == user_id).first()
    if user and user.id != current_user.id:
        user.is_active = not user.is_active
        db.commit()
    
    return RedirectResponse(url="/dashboard/users", status_code=302)


@router.delete("/users/{user_id}")
async def delete_user(
    user_id: int,
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_permission("manage_users"))
):
    """Delete a user."""
    user = db.query(User).filter(User.id == user_id).first()
    if user and user.id != current_user.id:
        db.delete(user)
        db.commit()
    
    users = db.query(User).order_by(User.id).all()
    return templates.TemplateResponse("dashboard/partials/users_table.html", {
        "request": request,
        "users": users,
        "current_user": current_user
    })


# --------------------------------------------------------------------------
# Analytics
# --------------------------------------------------------------------------

@router.get("/analytics", response_class=HTMLResponse)
async def analytics_page(
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_auth)
):
    """Render detailed analytics page."""
    logs = db.query(ScanLog).order_by(ScanLog.timestamp.desc()).limit(100).all()
    
    # Device fleet stats
    unique_devices = db.query(func.count(func.distinct(ScanLog.device_id))).scalar() or 0
    
    return templates.TemplateResponse("dashboard/analytics.html", {
        "request": request,
        "current_user": current_user,
        "active_page": "analytics",
        "logs": logs,
        "unique_devices": unique_devices
    })


# --------------------------------------------------------------------------
# Export
# --------------------------------------------------------------------------

@router.get("/export/csv")
async def export_csv(
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_auth)
):
    """Export scan logs as CSV."""
    logs = db.query(ScanLog).order_by(ScanLog.timestamp.desc()).all()
    
    csv_content = "id,package_name,version_code,risk_level,threat_type,device_id,timestamp\n"
    for log in logs:
        csv_content += f"{log.id},{log.package_name},{log.version_code},{log.risk_level},{log.threat_type or ''},{log.device_id or ''},{log.timestamp}\n"
    
    return Response(
        content=csv_content,
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=scan_logs.csv"}
    )
