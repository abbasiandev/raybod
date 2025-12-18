from fastapi import FastAPI
from app.api.v1.endpoints import scan

app = FastAPI(
    title="Hybrid Cloud Sentinel Brain",
    description="Central Intelligence for Mobile Threat Defense",
    version="1.0.0"
)

# Include routers
app.include_router(scan.router, prefix="/api/v1/scan", tags=["scan"])

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "Hybrid Cloud Sentinel Brain"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
