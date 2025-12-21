from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.device import Device
from pydantic import BaseModel
from datetime import datetime
from typing import List, Optional

router = APIRouter()

class DeviceRegistrationRequest(BaseModel):
    """Request schema for device registration."""
    device_id: str
    device_model: str
    os_version: str
    app_version: str

class DeviceResponse(BaseModel):
    """Response schema for device information."""
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
    """
    Register a new device or update existing device information.
    
    Used for tracking active installations and device fleet management.
    Updates last_seen timestamp on every call.
    """
    existing_device = db.query(Device).filter(Device.device_id == request.device_id).first()
    
    if existing_device:
        # Update existing device information
        existing_device.device_model = request.device_model
        existing_device.os_version = request.os_version
        existing_device.app_version = request.app_version
        existing_device.last_seen = datetime.utcnow()
        device_record = existing_device
    else:
        # Create new device record
        device_record = Device(
            device_id=request.device_id,
            device_model=request.device_model,
            os_version=request.os_version,
            app_version=request.app_version
        )
        db.add(device_record)
    
    db.commit()
    db.refresh(device_record)
    return device_record

