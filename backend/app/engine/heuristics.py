from typing import Optional, List
from app.schemas.scan_schema import AppMetadata, ScanResult, RiskLevel, DrebinFeatures
from app.core.database import SessionLocal, AllowlistEntry, BlocklistEntry
from app.services.threat_intel import threat_intel_service

class HeuristicEngine:
    """
    Cloud-based heuristic analysis engine for malware detection.
    Applies rule-based threat detection before returning scan results.
    """
    
    # Configuration constants
    APP_SIZE_ANOMALY_THRESHOLD_BYTES = 500_000  # 500KB
    DANGEROUS_PERMISSION_COMBO_MIN_COUNT = 3
    SUSPICIOUS_INTENT_COMBO_MIN_COUNT = 3
    
    def analyze(self, metadata: AppMetadata) -> ScanResult:
        """Analyze app metadata and return risk assessment."""
        db = SessionLocal()
        drebin = DrebinFeatures(
            s2_requested_permissions=metadata.permissions,
            s4_filtered_intents=metadata.intents
        )
        
        try:
            # Rule 0: Check global allowlist for verified safe packages
            allowed = db.query(AllowlistEntry).filter(AllowlistEntry.package_name == metadata.package_name).first()
            if allowed:
                return ScanResult(
                    package_name=metadata.package_name,
                    risk_level=RiskLevel.SAFE,
                    threat_type="",
                    description="Verified safe package via Cloud Allowlist.",
                    heuristics_used=["Allowlist"],
                    drebin_features=drebin
                )
            
            # Rule 1: Check global blocklist for known malicious packages
            blocked = db.query(BlocklistEntry).filter(BlocklistEntry.package_name == metadata.package_name).first()
            if blocked:
                drebin.s7_suspicious_apis.append("Known Malicious Signature")
                return ScanResult(
                    package_name=metadata.package_name,
                    risk_level=RiskLevel.CRITICAL,
                    threat_type=blocked.threat_type,
                    description="This package is in our global blocklist.",
                    heuristics_used=["Blocklist"],
                    drebin_features=drebin
                )
            
            # Rule 1.1: Cross-check with external threat intelligence (VirusTotal)
            if metadata.signature:
                external_threat = threat_intel_service.check_virus_total(metadata.signature)
                if external_threat:
                    drebin.s7_suspicious_apis.append(f"VT Detections: {external_threat.get('positives', 0)}")
                    return ScanResult(
                        package_name=metadata.package_name,
                        risk_level=RiskLevel.CRITICAL,
                        threat_type="Malware (External)",
                        description=f"External threat intelligence (VirusTotal) flags this app.",
                        heuristics_used=["ExternalIntel"],
                        drebin_features=drebin
                    )
        finally:
            db.close()
            
        # Rule 1.5: Analyze suspicious intent patterns
        intent_result = self._analyze_intents(metadata, drebin)
        if intent_result:
            return intent_result

        # Rule 2: Size-Permission Anomaly Detection
        # Small apps (<500KB) requesting sensitive permissions are highly suspicious
        if metadata.app_size and metadata.app_size < self.APP_SIZE_ANOMALY_THRESHOLD_BYTES:
            dangerous_permissions_for_small_apps = {"READ_EXTERNAL_STORAGE", "CAMERA", "RECORD_AUDIO", "READ_SMS"}
            
            # Extract short permission names and check for dangerous ones
            has_dangerous = False
            suspicious_permissions = []
            for permission in metadata.permissions:
                permission_short_name = permission.split(".")[-1]
                if permission_short_name in dangerous_permissions_for_small_apps:
                    has_dangerous = True
                    suspicious_permissions.append(permission_short_name)
            
            if has_dangerous:
                drebin.s7_suspicious_apis.append("Size-Permission Anomaly")
                return ScanResult(
                    package_name=metadata.package_name,
                    risk_level=RiskLevel.HIGH,
                    threat_type="Suspicious Lightweight App",
                    description=f"Very small app (<500KB) requesting sensitive permissions: {', '.join(suspicious_permissions)}.",
                    heuristics_used=["SizeAnomaly"],
                    drebin_features=drebin
                )

        # Rule 3: Suspicious permission combinations (high-risk data access)
        dangerous_permission_set = {
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS"
        }
        
        requested_dangerous_permissions = [perm for perm in metadata.permissions if perm in dangerous_permission_set]
        
        if len(requested_dangerous_permissions) >= self.DANGEROUS_PERMISSION_COMBO_MIN_COUNT:
             drebin.s7_suspicious_apis.append("High-Risk Permission Combination")
             permission_list = ", ".join([perm.split(".")[-1] for perm in requested_dangerous_permissions])
             return ScanResult(
                package_name=metadata.package_name,
                risk_level=RiskLevel.HIGH,
                threat_type="Potential Spyware",
                description=f"High risk permission combination detected: {permission_list}",
                heuristics_used=["PermissionCombo"],
                drebin_features=drebin
            )

        # Rule 4: Banking trojan/ransomware detection (2025 threat pattern)
        # Combination of Accessibility Service + Device Admin is a critical indicator
        if "android.permission.BIND_ACCESSIBILITY_SERVICE" in metadata.permissions and \
           "android.permission.BIND_DEVICE_ADMIN" in metadata.permissions:
            drebin.s7_suspicious_apis.append("Accessibility + Device Admin Combination")
            return ScanResult(
                package_name=metadata.package_name,
                risk_level=RiskLevel.CRITICAL,
                threat_type="Banking Trojan / Ransomware",
                description="Combination of Accessibility Service and Device Admin is highly indicative of 2025-era financial malware.",
                heuristics_used=["AccessAdminSynergy"],
                drebin_features=drebin
            )
        
        # No threats detected - return clean result
        return ScanResult(
            package_name=metadata.package_name,
            risk_level=RiskLevel.SAFE,
            threat_type="",
            description="No threats detected by cloud brain.",
            heuristics_used=["Clean"],
            drebin_features=drebin
        )

    def _analyze_intents(self, metadata: AppMetadata, drebin: DrebinFeatures) -> Optional[ScanResult]:
        """
        Analyze intent patterns for suspicious combinations.
        Intents reveal what actions an app can perform (send SMS, capture images, etc.)
        """
        if not metadata.intents:
            return None
        
        suspicious_intent_patterns = {
            "android.intent.action.SENDTO",
            "android.provider.Telephony.SMS_RECEIVED",
            "android.intent.action.VIEW",
            "android.media.action.IMAGE_CAPTURE"
        }
        
        detected_suspicious_intents = [intent for intent in metadata.intents if intent in suspicious_intent_patterns]
        
        if len(detected_suspicious_intents) >= self.SUSPICIOUS_INTENT_COMBO_MIN_COUNT:
            drebin.s7_suspicious_apis.append("Suspicious Intent Pattern")
            return ScanResult(
                package_name=metadata.package_name,
                risk_level=RiskLevel.HIGH,
                threat_type="Potential Spyware",
                description="Suspicious intent combination detected.",
                heuristics_used=["IntentAnalysis"],
                drebin_features=drebin
            )
        return None

engine = HeuristicEngine()
