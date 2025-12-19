from fastapi import APIRouter, Request, Form, HTTPException
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
import uuid

router = APIRouter()
templates = Jinja2Templates(directory="app/templates")

# Mock database for payment sessions
payment_sessions = {}

@router.get("/", response_class=HTMLResponse)
async def landing_page(request: Request):
    """Render the public landing page."""
    return templates.TemplateResponse("landing.html", {"request": request})

@router.post("/api/v1/public/checkout")
async def checkout(plan_id: str = Form(...)):
    """Start a checkout session and redirect to sandbox."""
    session_id = str(uuid.uuid4())
    payment_sessions[session_id] = {"plan_id": plan_id, "status": "pending"}
    
    # Redirect to the sandbox payment page
    return RedirectResponse(url=f"/payment/sandbox/{session_id}", status_code=303)

@router.get("/payment/sandbox/{session_id}", response_class=HTMLResponse)
async def payment_page(request: Request, session_id: str):
    """Render the sandbox payment page."""
    if session_id not in payment_sessions:
        raise HTTPException(status_code=404, detail="Session not found")
        
    return templates.TemplateResponse("sandbox_pay.html", {
        "request": request, 
        "session_id": session_id
    })

@router.post("/api/v1/public/payment/process")
async def process_payment(session_id: str = Form(...), success: str = Form(...)):
    """Process the sandbox payment based on user selection."""
    if session_id not in payment_sessions:
        raise HTTPException(status_code=404, detail="Session not found")
    
    is_success = success.lower() == "true"
    payment_sessions[session_id]["status"] = "paid" if is_success else "failed"
    
    if is_success:
        # success logic (e.g., activate plan)
        return HTMLResponse(content="""
            <html>
                <body style="background:#0d0d12; color:#00ff87; display:flex; justify-content:center; align-items:center; height:100vh; font-family:sans-serif;">
                    <div style="text-align:center;">
                        <h1>Payment Successful!</h1>
                        <p>Thank you for subscribing.</p>
                        <a href="/" style="color:white;">Return to Home</a>
                    </div>
                </body>
            </html>
        """)
    else:
        return HTMLResponse(content="""
            <html>
                <body style="background:#0d0d12; color:#ff006e; display:flex; justify-content:center; align-items:center; height:100vh; font-family:sans-serif;">
                    <div style="text-align:center;">
                        <h1>Payment Failed</h1>
                        <p>Something went wrong.</p>
                        <a href="/" style="color:white;">Return to Home</a>
                    </div>
                </body>
            </html>
        """)
