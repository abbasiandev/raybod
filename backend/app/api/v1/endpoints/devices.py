from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.device import Device
from pydantic import BaseModel
from datetime import datetime
from typing import List, Optional

router = APIRouter()

class DeviceRegistrationRequest(BaseModel):
    device_id: str
    device_model: str
    os_version: str
    app_version: str

class DeviceResponse(BaseModel):
    device_id: str
    device_model: str
    os_version: str
    app_version: str
    last_seen: datetime

    class Config:
        from_attributes = True

@router.post("/register", response_model=DeviceResponse)
async def register_device(
    request: DeviceRegistrationRequest,
    db: Session = Depends(get_db)
):
    """Register or update device info."""
    device = db.query(Device).filter(Device.device_id == request.device_id).first()
    if device:
        device.device_model = request.device_model
        device.os_version = request.os_version
        device.app_version = request.app_version
        device.last_seen = datetime.utcnow()
    else:
        device = Device(
            device_id=request.device_id,
            device_model=request.device_model,
            os_version=request.os_version,
            app_version=request.app_version
        )
        db.add(device)
    
    db.commit()
    db.refresh(device)
    return device

