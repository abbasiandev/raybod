from fastapi import FastAPI, Request
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
import os
from app.core.config import settings
print("DEBUG: Loading all routers...")
from app.api.v1.endpoints import scan, auth, dashboard, allowlist, threats, reputation, devices, models, analytics, websocket, keys, public, billing, network
print("DEBUG: Routers loaded successfully")

from app.core.database import init_db

limiter = Limiter(key_func=get_remote_address, enabled=os.getenv("TESTING") != "1")
app = FastAPI(
    title=settings.APP_NAME,
    description="Central Intelligence for Mobile Threat Defense",
    version=settings.VERSION
)
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

templates = Jinja2Templates(directory="app/templates")

# Mount static files
app.mount("/static", StaticFiles(directory="app/static"), name="static")

# Initialize database unless explicitly disabled (e.g. in some test environments)
if os.getenv("SKIP_INIT_DB") != "1":
    init_db()

# Include routers
app.include_router(scan.router, prefix="/api/v1/scan", tags=["scan"])
app.include_router(allowlist.router, prefix="/api/v1/allowlist", tags=["allowlist"])
app.include_router(threats.router, prefix="/api/v1/threats", tags=["threats"])
app.include_router(reputation.router, prefix="/api/v1/reputation", tags=["reputation"])
app.include_router(devices.router, prefix="/api/v1/devices", tags=["devices"])
app.include_router(models.router, prefix="/api/v1/models", tags=["models"])
app.include_router(analytics.router, prefix="/api/v1/analytics", tags=["analytics"])
app.include_router(network.router, prefix="/api/v1/network", tags=["network"])
app.include_router(websocket.router, tags=["websocket"])
app.include_router(keys.router, prefix="/api/v1/keys", tags=["keys"])
app.include_router(auth.router, prefix="/api/v1/auth", tags=["auth"])
app.include_router(dashboard.router, prefix="/dashboard", tags=["dashboard"])
app.include_router(billing.router, prefix="/dashboard/billing", tags=["billing"])
app.include_router(public.router, tags=["public"])

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "Hybrid Cloud Sentinel Brain"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=settings.DEBUG)

