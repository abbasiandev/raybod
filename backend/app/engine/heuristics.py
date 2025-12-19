from typing import Optional
from app.schemas.scan_schema import AppMetadata, ScanResult, RiskLevel
from app.core.database import SessionLocal, AllowlistEntry, BlocklistEntry
from app.services.threat_intel import threat_intel_service

class HeuristicEngine:
    
    def analyze(self, metadata: AppMetadata) -> ScanResult:
        db = SessionLocal()
        try:
            # Rule 0: Global Allowlist (False Positive Mitigation)
            allowed = db.query(AllowlistEntry).filter(AllowlistEntry.package_name == metadata.package_name).first()
            if allowed:
                return ScanResult(
                    package_name=metadata.package_name,
                    risk_level=RiskLevel.SAFE,
                    threat_type="",
                    description="Verified safe package via Cloud Allowlist.",
                    heuristics_used=["Allowlist"]
                )
            
            # Rule 1: Known Bad Packages
            blocked = db.query(BlocklistEntry).filter(BlocklistEntry.package_name == metadata.package_name).first()
            if blocked:
                return ScanResult(
                    package_name=metadata.package_name,
                    risk_level=RiskLevel.CRITICAL,
                    threat_type=blocked.threat_type,
                    description="This package is in our global blocklist.",
                    heuristics_used=["Blocklist"]
                )
            
            # Rule 1.1: External Threat Intel (New)
            if metadata.signature:
                external_threat = threat_intel_service.check_virus_total(metadata.signature)
                if external_threat:
                    return ScanResult(
                        package_name=metadata.package_name,
                        risk_level=RiskLevel.CRITICAL,
                        threat_type="Malware (External)",
                        description=f"External threat intelligence (VirusTotal) flags this app: {external_threat.get('positives', 0)} detections.",
                        heuristics_used=["ExternalIntel"]
                    )
        finally:
            db.close()
            
        # Rule 1.5: Intent-based Detection (New)
        intent_result = self._analyze_intents(metadata)
        if intent_result:
            return intent_result

        # Rule 2: Suspicious Permission Combinations
        dangerous_perms = {
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_SMS"
        }
        
        requested_dangerous = [p for p in metadata.permissions if p in dangerous_perms]
        
        if len(requested_dangerous) >= 3:
             return ScanResult(
                package_name=metadata.package_name,
                risk_level=RiskLevel.HIGH,
                threat_type="Potential Spyware",
                description=f"High risk permission combination: {', '.join(requested_dangerous)}",
                heuristics_used=["PermissionCombo"]
            )
            
        return ScanResult(
            package_name=metadata.package_name,
            risk_level=RiskLevel.SAFE,
            threat_type="",
            description="No threats detected by cloud brain.",
            heuristics_used=["Clean"]
        )

    def _analyze_intents(self, metadata: AppMetadata) -> Optional[ScanResult]:
        """Analyze intents for suspicious patterns."""
        if not metadata.intents:
            return None
        
        # Suspicious intent combinations
        spyware_intents = {
            "android.intent.action.SENDTO",  # SMS
            "android.provider.Telephony.SMS_RECEIVED",
            "android.intent.action.VIEW",  # Location
            "android.media.action.IMAGE_CAPTURE",  # Camera
            "android.media.action.VIDEO_CAPTURE"
        }
        
        detected_intents = [i for i in metadata.intents if i in spyware_intents]
        
        if len(detected_intents) >= 3:
            return ScanResult(
                package_name=metadata.package_name,
                risk_level=RiskLevel.HIGH,
                threat_type="Potential Spyware",
                description=f"Suspicious intent combination detected: {', '.join(detected_intents)}",
                heuristics_used=["IntentAnalysis"]
            )
        return None

engine = HeuristicEngine()
