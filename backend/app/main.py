from fastapi import FastAPI
from app.core.config import settings
from app.api.v1.endpoints import scan

app = FastAPI(
    title=settings.APP_NAME,
    description="Central Intelligence for Mobile Threat Defense",
    version=settings.VERSION
)

# Include routers
app.include_router(scan.router, prefix="/api/v1/scan", tags=["scan"])

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "Hybrid Cloud Sentinel Brain"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=settings.DEBUG)
