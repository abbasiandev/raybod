"""
Billing and subscription endpoints with mock payment integration.
"""
from fastapi import APIRouter, Request, Depends, Form, HTTPException, Response
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
from typing import Optional

from app.core.database import get_db
from app.models.user import User
from app.api.v1.endpoints.dashboard import require_auth

router = APIRouter()
templates = Jinja2Templates(directory="app/templates")

@router.get("/", response_class=HTMLResponse)
async def billing_page(
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_auth)
):
    """Render billing management page."""
    return templates.TemplateResponse("dashboard/billing.html", {
        "request": request,
        "current_user": current_user,
        "active_page": "billing"
    })

@router.post("/checkout")
async def checkout(
    request: Request,
    plan: str = Form(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_auth)
):
    """Simulate a checkout session by redirecting to the sandbox."""
    if plan not in ["FREEMIUM", "FEATURED"]:
        raise HTTPException(status_code=400, detail="Invalid plan")
    
    # In a real app, this would create a Stripe session.
    # Here, we just redirect to our mock sandbox.
    return RedirectResponse(
        url=f"/dashboard/billing/sandbox?plan={plan}",
        status_code=303
    )

@router.get("/sandbox", response_class=HTMLResponse)
async def payment_sandbox(
    request: Request,
    plan: str,
    current_user: User = Depends(require_auth)
):
    """Render the mock payment sandbox page."""
    return templates.TemplateResponse("dashboard/payment_sandbox.html", {
        "request": request,
        "current_user": current_user,
        "plan": plan
    })

@router.post("/verify")
async def verify_payment(
    request: Request,
    success: bool = Form(...),
    plan: str = Form(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_auth)
):
    """Process the result of the mock payment."""
    if success:
        current_user.plan = plan
        db.commit()
        return RedirectResponse(url="/dashboard/billing?status=success", status_code=303)
    else:
        return RedirectResponse(url="/dashboard/billing?status=failed", status_code=303)

