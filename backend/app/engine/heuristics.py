from app.schemas.scan_schema import AppMetadata, ScanResult, RiskLevel

class HeuristicEngine:
    
    def analyze(self, metadata: AppMetadata) -> ScanResult:
        known_malware = [
            "com.example.virus",
            "com.spyware.tracker"
        ]
        
        allow_list = [
            "com.android.chrome",
            "com.google.android.apps.maps",
            "com.whatsapp"
        ]

        # Rule 0: Global Allowlist (False Positive Mitigation)
        if metadata.package_name in allow_list:
            return ScanResult(
                package_name=metadata.package_name,
                risk_level=RiskLevel.SAFE,
                threat_type="",
                description="Verified safe package via Cloud Allowlist.",
                heuristics_used=["Allowlist"]
            )
        
        # Rule 1: Known Bad Packages
        if metadata.package_name in known_malware:
            return ScanResult(
                package_name=metadata.package_name,
                risk_level=RiskLevel.CRITICAL,
                threat_type="Known Malware",
                description="This package is in our global blocklist.",
                heuristics_used=["Blocklist"]
            )
            
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

engine = HeuristicEngine()
