from fastapi import APIRouter, Request, Depends
from fastapi.responses import HTMLResponse
from typing import List, Optional
from app.schemas.scan_schema import AppMetadata, RiskLevel
from app.engine.heuristics import engine
from app.models.user import User
from app.api.v1.endpoints.auth import get_current_user

router = APIRouter()

@router.get("/debug", response_class=HTMLResponse)
async def debug_page(request: Request, current_user: User = Depends(get_current_user)):
    """Render the debug APK analyzer page"""
    from fastapi.templating import Jinja2Templates
    templates = Jinja2Templates(directory="app/templates")
    return templates.TemplateResponse("dashboard/debug.html", {
        "request": request,
        "current_user": current_user,
        "active_page": "debug"
    })

@router.post("/debug")
async def analyze_debug(
    request: Request,
    current_user: User = Depends(get_current_user)
):
    """Analyze app with custom inputs"""
    form_data = await request.form()
    
    package_name = form_data.get("package_name", "com.example.app")
    app_size = int(form_data.get("app_size", 500000))
    permissions = form_data.getlist("permissions")
    intents = form_data.getlist("intents")
    has_reflection = form_data.get("has_reflection") == "true"
    has_dynamic_loading = form_data.get("has_dynamic_loading") == "true"
    
    # Create metadata object
    metadata = AppMetadata(
        package_name=package_name,
        version_code=1,
        signature="debug_signature",
        permissions=permissions,
        intents=intents,
        app_size=app_size,
        has_reflection=has_reflection,
        has_dynamic_loading=has_dynamic_loading
    )
    
    # Analyze using heuristic engine
    result = engine.analyze(metadata)
    
    # Generate HTML response
    risk_color = {
        RiskLevel.SAFE: "safe",
        RiskLevel.LOW: "primary",
        RiskLevel.MEDIUM: "warning",
        RiskLevel.HIGH: "danger",
        RiskLevel.CRITICAL: "danger"
    }.get(result.risk_level, "secondary")
    
    html = f"""
    <div class="card">
        <div class="card-header">
            <h3 class="card-title">Analysis Results</h3>
        </div>
        <div class="card-body">
            <div class="row mb-3">
                <div class="col-md-6">
                    <h5>Risk Assessment</h5>
                    <span class="badge badge-{risk_color}" style="font-size: 1.2rem; padding: 0.5rem 1rem;">
                        {result.risk_level.value}
                    </span>
                </div>
                <div class="col-md-6">
                    <h5>Threat Type</h5>
                    <p>{result.threat_type or 'None detected'}</p>
                </div>
            </div>
            
            <div class="mb-3">
                <h5>Description</h5>
                <p>{result.description}</p>
            </div>
            
            <div class="mb-3">
                <h5>Heuristics Triggered</h5>
                <div>
                    {"".join([f'<span class="badge badge-info me-1">{h}</span>' for h in result.heuristics_used])}
                </div>
            </div>
            
            <div class="mb-3">
                <h5>Drebin Features Analysis</h5>
                <div class="row">
                    <div class="col-md-6">
                        <strong>Requested Permissions ({len(result.drebin_features.s2_requested_permissions)}):</strong>
                        <ul class="small">
                            {"".join([f'<li>{p.split(".")[-1]}</li>' for p in result.drebin_features.s2_requested_permissions[:10]])}
                        </ul>
                    </div>
                    <div class="col-md-6">
                        <strong>Suspicious APIs ({len(result.drebin_features.s7_suspicious_apis)}):</strong>
                        <ul class="small">
                            {"".join([f'<li>{api}</li>' for api in result.drebin_features.s7_suspicious_apis])}
                        </ul>
                    </div>
                </div>
            </div>
            
            <div class="alert alert-info">
                <strong>💡 Debug Info:</strong><br>
                Package Size: {app_size / 1024:.1f} KB<br>
                Permissions: {len(permissions)}<br>
                Intents: {len(intents)}<br>
                Reflection: {'Yes' if has_reflection else 'No'}<br>
                Dynamic Loading: {'Yes' if has_dynamic_loading else 'No'}
            </div>
        </div>
    </div>
    """
    
    return HTMLResponse(content=html)
