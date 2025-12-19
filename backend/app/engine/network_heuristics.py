import math
import time
from typing import List, Tuple
from app.schemas.network_schema import NetworkFlow, NetworkAlert, BlocklistEntry

class NetworkHeuristicEngine:
    
    def analyze_flows(self, flows: List[NetworkFlow]) -> Tuple[List[NetworkAlert], List[BlocklistEntry]]:
        alerts = []
        blocklist = []
        
        for flow in flows:
            # Rule 1: DGA Detection via Entropy
            if flow.domain:
                entropy = self._calculate_entropy(flow.domain)
                if entropy > 3.8: # Threshold for suspicious entropy
                    alerts.append(NetworkAlert(
                        package_name=flow.source_app,
                        destination=flow.domain,
                        threat_type="DGA Detected",
                        risk_level="HIGH",
                        description=f"Suspicious domain entropy ({entropy:.2f}) indicates potential malware C2.",
                        timestamp=int(time.time() * 1000)
                    ))
                    blocklist.append(BlocklistEntry(
                        pattern=flow.domain,
                        type="DOMAIN",
                        reason="High entropy domain (DGA pattern)",
                        timestamp=int(time.time() * 1000)
                    ))

            # Rule 2: Suspicious Ports for Non-Standard Apps
            suspicious_ports = {6667, 1337, 4444, 31337}
            if flow.destination_port in suspicious_ports:
                alerts.append(NetworkAlert(
                    package_name=flow.source_app,
                    destination=f"{flow.destination_ip}:{flow.destination_port}",
                    threat_type="Suspicious Port",
                    risk_level="CRITICAL",
                    description=f"Connection to known malware/IRC port {flow.destination_port}.",
                    timestamp=int(time.time() * 1000)
                ))

            # Rule 3: Data Exfiltration (Low and Slow)
            # This would require stateful analysis, simplified here
            if flow.bytes_sent > 1024 * 1024 and "google" not in (flow.domain or ""):
                alerts.append(NetworkAlert(
                    package_name=flow.source_app,
                    destination=flow.domain or flow.destination_ip,
                    threat_type="Potential Data Exfiltration",
                    risk_level="MEDIUM",
                    description="Large outbound data transfer to unverified destination.",
                    timestamp=int(time.time() * 1000)
                ))
                
        return alerts, blocklist

    def _calculate_entropy(self, domain: str) -> float:
        """Calculate Shannon entropy of a string."""
        if not domain:
            return 0
        prob = [float(domain.count(c)) / len(domain) for c in dict.fromkeys(list(domain))]
        entropy = - sum([p * math.log(p) / math.log(2.0) for p in prob])
        return entropy

network_heuristic_engine = NetworkHeuristicEngine()

