from fastapi import APIRouter, Depends, HTTPException, File, UploadFile
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.model_version import ModelVersion
from pydantic import BaseModel
from datetime import datetime
from typing import List, Optional
import os

router = APIRouter()

class ModelVersionResponse(BaseModel):
    version: str
    file_size: int
    checksum_sha256: str
    uploaded_at: datetime
    download_url: str

    class Config:
        from_attributes = True

@router.get("/current", response_model=ModelVersionResponse)
async def get_current_model(
    db: Session = Depends(get_db)
):
    """Get current active model version."""
    model = db.query(ModelVersion).filter(ModelVersion.is_active == True).order_by(ModelVersion.uploaded_at.desc()).first()
    if not model:
        raise HTTPException(status_code=404, detail="No active model found")
    
    # In a real app, download_url would point to S3 or a static file endpoint
    download_url = f"/api/v1/models/download/{model.version}"
    
    return ModelVersionResponse(
        version=model.version,
        file_size=model.file_size or 0,
        checksum_sha256=model.checksum_sha256 or "",
        uploaded_at=model.uploaded_at,
        download_url=download_url
    )

@router.get("/download/{version}")
async def download_model(
    version: str,
    db: Session = Depends(get_db)
):
    """Download model file."""
    model = db.query(ModelVersion).filter(ModelVersion.version == version).first()
    if not model:
        raise HTTPException(status_code=404, detail="Model version not found")
    
    if not os.path.exists(model.file_path):
        raise HTTPException(status_code=404, detail="Model file not found on disk")
    
    return FileResponse(
        path=model.file_path,
        filename=f"sentinel_{version}.tflite",
        media_type="application/octet-stream"
    )

