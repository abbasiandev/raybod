from fastapi import FastAPI, Request
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
import os
import logging
from app.core.config import settings
from app.api.v1.endpoints import scan, auth, dashboard, allowlist, threats, reputation, devices, models, analytics, websocket, keys, public, billing, network, debug
from app.core.database import init_db

logger = logging.getLogger(__name__)

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
# Use startup event to avoid blocking app startup if DB is temporarily unavailable
@app.on_event("startup")
def startup_event():
    """Enhanced startup with detailed logging for Liara debugging."""
    logger.info("="*60)
    logger.info("APPLICATION STARTUP - DIAGNOSTICS")
    logger.info("="*60)
    
    # Check /data directory
    logger.info(f"Storage Directory Check:")
    data_exists = os.path.exists("/data")
    logger.info(f"  /data exists: {data_exists}")
    if data_exists:
        try:
            # Check if writable
            test_file = "/data/.write_test"
            with open(test_file, "w") as f:
                f.write("test")
            os.remove(test_file)
            logger.info(f"  /data is WRITABLE ✓")
        except Exception as e:
            logger.error(f"  /data is NOT writable: {e}")
    
    # Check environment
    logger.info(f"Environment Variables:")
    logger.info(f"  DATABASE_URL: {settings.DATABASE_URL}")
    logger.info(f"  JWT_SECRET set: {bool(settings.JWT_SECRET and len(settings.JWT_SECRET) > 20)}")
    logger.info(f"  DEBUG: {settings.DEBUG}")
    logger.info(f"  PORT: {settings.PORT}")
    logger.info(f"  HOST: {settings.HOST}")
    
    # Check directories
    logger.info(f"Directory Check:")
    template_exists = os.path.exists('app/templates')
    static_exists = os.path.exists('app/static')
    logger.info(f"  app/templates exists: {template_exists}")
    logger.info(f"  app/static exists: {static_exists}")
    
    if template_exists:
        try:
            templates = os.listdir('app/templates')
            logger.info(f"  Templates: {templates}")
        except Exception as e:
            logger.error(f"  Error listing templates: {e}")
    
    if static_exists:
        try:
            static_files = os.listdir('app/static')
            logger.info(f"  Static folders: {static_files}")
        except Exception as e:
            logger.error(f"  Error listing static files: {e}")
    
    if os.getenv("SKIP_INIT_DB") != "1":
        try:
            init_db()
            logger.info("✓ Database initialized successfully")
            
            # Check if admin user exists
            from app.core.database import SessionLocal
            from app.models.user import User
            db = SessionLocal()
            try:
                admin = db.query(User).filter(User.username == "admin").first()
                if admin:
                    logger.info(f"✓ Admin user found (active={admin.is_active})")
                else:
                    logger.warning("⚠ Admin user NOT found - first login will fail")
            finally:
                db.close()
            
        except Exception as e:
            logger.error(f"✗ Database initialization failed: {e}")
            import traceback
            logger.error(traceback.format_exc())
            # Don't crash the app - it will retry on first request
    
    logger.info("="*60)
    logger.info("STARTUP COMPLETE")
    logger.info("="*60)

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
app.include_router(debug.router, prefix="/dashboard", tags=["debug"])
app.include_router(public.router, tags=["public"])

@app.get("/health")
async def health_check():
    """Health check endpoint with diagnostic info."""
    return {
        "status": "healthy",
        "service": "Raybod Brain",
        "debug": settings.DEBUG,
        "templates_exist": os.path.exists('app/templates'),
        "static_exist": os.path.exists('app/static'),
        "jwt_configured": bool(settings.JWT_SECRET and len(settings.JWT_SECRET) > 20)
    }

# Enhanced error handler for debugging
from fastapi.responses import JSONResponse
import traceback

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Catch all unhandled exceptions and log them."""
    logger.error(f"Unhandled exception on {request.url}")
    logger.error(f"Exception type: {type(exc).__name__}")
    logger.error(f"Exception message: {str(exc)}")
    logger.error(f"Traceback:\n{traceback.format_exc()}")
    
    # In production, don't expose internal errors
    if settings.DEBUG:
        return JSONResponse(
            status_code=500,
            content={
                "error": "Internal Server Error",
                "detail": str(exc),
                "type": type(exc).__name__,
                "path": str(request.url)
            }
        )
    else:
        return JSONResponse(
            status_code=500,
            content={"error": "Internal Server Error", "path": str(request.url)}
        )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=settings.DEBUG)

