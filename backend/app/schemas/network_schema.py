from pydantic import BaseModel
from typing import List, Optional

class NetworkFlow(BaseModel):
    source_app: str
    destination_ip: str
    destination_port: int
    protocol: str
    domain: Optional[str] = None
    bytes_sent: int
    bytes_received: int
    timestamp: int

class NetworkAnalysisRequest(BaseModel):
    flows: List[NetworkFlow]

class NetworkAlert(BaseModel):
    package_name: str
    destination: str
    threat_type: str
    risk_level: str
    description: str
    timestamp: int

class BlocklistEntry(BaseModel):
    pattern: str
    type: str
    reason: str
    timestamp: int

class NetworkAnalysisResult(BaseModel):
    alerts: List[NetworkAlert]
    blocklist: List[BlocklistEntry]




