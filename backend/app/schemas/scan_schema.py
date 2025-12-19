from enum import Enum
from typing import List, Optional, Dict
from pydantic import BaseModel

class RiskLevel(str, Enum):
    SAFE = "SAFE"
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"
    UNKNOWN = "UNKNOWN"

class AppMetadata(BaseModel):
    package_name: str
    version_code: int
    signature: str
    permissions: List[str] = []
    # NEW OPTIONAL FIELDS (backward compatible)
    intents: List[str] = []
    version_name: Optional[str] = None
    install_time: Optional[int] = None
    last_update_time: Optional[int] = None
    ensemble_metadata: Optional[Dict[str, float]] = None
    
class ScanResult(BaseModel):
    package_name: str
    risk_level: RiskLevel
    threat_type: str = ""
    description: str
    heuristics_used: List[str] = []

class BatchScanRequest(BaseModel):
    packages: List[AppMetadata]

class BatchScanResult(BaseModel):
    results: List[ScanResult]
