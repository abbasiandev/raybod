from fastapi import FastAPI
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from app.core.config import settings
from app.api.v1.endpoints import scan
from app.core.database import init_db

limiter = Limiter(key_func=get_remote_address)
app = FastAPI(
    title=settings.APP_NAME,
    description="Central Intelligence for Mobile Threat Defense",
    version=settings.VERSION
)
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# Initialize database
init_db()

# Include routers
app.include_router(scan.router, prefix="/api/v1/scan", tags=["scan"])

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "Hybrid Cloud Sentinel Brain"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=settings.DEBUG)
