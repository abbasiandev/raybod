from typing import Optional, List
from app.schemas.scan_schema import AppMetadata, ScanResult, RiskLevel, DrebinFeatures
from app.core.database import SessionLocal, AllowlistEntry, BlocklistEntry
from app.services.threat_intel import threat_intel_service

class HeuristicEngine:
    
    def analyze(self, metadata: AppMetadata) -> ScanResult:
        db = SessionLocal()
        drebin = DrebinFeatures(
            s2_requested_permissions=metadata.permissions,
            s4_filtered_intents=metadata.intents
        )
        
        try:
            # Rule 0: Global Allowlist
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
            
            # Rule 1: Known Bad Packages
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
            
            # Rule 1.1: External Threat Intel
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
            
        # Rule 1.5: Intent-based Detection
        intent_result = self._analyze_intents(metadata, drebin)
        if intent_result:
            return intent_result

        # Rule 2: Suspicious Permission Combinations (S2)
        dangerous_perms = {
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS"
        }
        
        requested_dangerous = [p for p in metadata.permissions if p in dangerous_perms]
        
        if len(requested_dangerous) >= 3:
             drebin.s7_suspicious_apis.append("High-Risk Permission Combination")
             perm_list = ", ".join([p.split(".")[-1] for p in requested_dangerous])
             return ScanResult(
                package_name=metadata.package_name,
                risk_level=RiskLevel.HIGH,
                threat_type="Potential Spyware",
                description=f"High risk permission combination detected: {perm_list}",
                heuristics_used=["PermissionCombo"],
                drebin_features=drebin
            )

        # Rule 3: 2025 Trend - Accessibility + Device Admin (Ransomware/Banker)
        if "android.permission.BIND_ACCESSIBILITY_SERVICE" in metadata.permissions and \
           "android.permission.BIND_DEVICE_ADMIN" in metadata.permissions:
            return ScanResult(
                package_name=metadata.package_name,
                risk_level=RiskLevel.CRITICAL,
                threat_type="Banking Trojan / Ransomware",
                description="Combination of Accessibility Service and Device Admin is highly indicative of 2025-era financial malware.",
                heuristics_used=["AccessAdminSynergy"],
                drebin_features=drebin
            )
            
        return ScanResult(
            package_name=metadata.package_name,
            risk_level=RiskLevel.SAFE,
            threat_type="",
            description="No threats detected by cloud brain.",
            heuristics_used=["Clean"],
            drebin_features=drebin
        )

    def _analyze_intents(self, metadata: AppMetadata, drebin: DrebinFeatures) -> Optional[ScanResult]:
        """Analyze intents for suspicious patterns (S4)."""
        if not metadata.intents:
            return None
        
        spyware_intents = {
            "android.intent.action.SENDTO",
            "android.provider.Telephony.SMS_RECEIVED",
            "android.intent.action.VIEW",
            "android.media.action.IMAGE_CAPTURE"
        }
        
        detected_intents = [i for i in metadata.intents if i in spyware_intents]
        
        if len(detected_intents) >= 3:
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
