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
    
class DrebinFeatures(BaseModel):
    s1_hardware: List[str] = []
    s2_requested_permissions: List[str] = []
    s3_app_components: List[str] = []
    s4_filtered_intents: List[str] = []
    s5_restricted_apis: List[str] = []
    s6_used_permissions: List[str] = []
    s7_suspicious_apis: List[str] = []
    s8_network_addresses: List[str] = []

class ScanResult(BaseModel):
    package_name: str
    risk_level: RiskLevel
    threat_type: str = ""
    description: str
    heuristics_used: List[str] = []
    drebin_features: Optional[DrebinFeatures] = None

class BatchScanRequest(BaseModel):
    packages: List[AppMetadata]

class BatchScanResult(BaseModel):
    results: List[ScanResult]
