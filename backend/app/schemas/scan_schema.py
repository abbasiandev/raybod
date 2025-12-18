from enum import Enum
from typing import List, Optional
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
    
class ScanResult(BaseModel):
    package_name: str
    risk_level: RiskLevel
    threat_type: str = ""
    description: str
    heuristics_used: List[str] = []
