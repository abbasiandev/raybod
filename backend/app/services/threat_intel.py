import requests
import os
from typing import Optional, Dict, Any
import logging

logger = logging.getLogger(__name__)

class ThreatIntelService:
    def __init__(self):
        self.virustotal_api_key = os.getenv("VIRUSTOTAL_API_KEY")
        self.abuse_ch_api_key = os.getenv("ABUSE_CH_API_KEY")
    
    def check_virus_total(self, signature: str) -> Optional[Dict[str, Any]]:
        """
        Check file hash against VirusTotal API.
        Returns detection results if available.
        """
        if not self.virustotal_api_key:
            logger.warning("VirusTotal API key not configured")
            return None
        
        try:
            headers = {
                "x-apikey": self.virustotal_api_key
            }
            url = f"https://www.virustotal.com/api/v3/files/{signature}"
            response = requests.get(url, headers=headers, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                stats = data.get("data", {}).get("attributes", {}).get("last_analysis_stats", {})
                positives = stats.get("malicious", 0)
                total = sum(stats.values())
                
                if positives > 0:
                    return {
                        "positives": positives,
                        "total": total,
                        "permalink": f"https://www.virustotal.com/gui/file/{signature}"
                    }
            elif response.status_code == 404:
                logger.info(f"Hash {signature} not found in VirusTotal")
            else:
                logger.warning(f"VirusTotal API returned status {response.status_code}")
                
        except requests.RequestException as e:
            logger.error(f"VirusTotal API request failed: {e}")
        except Exception as e:
            logger.error(f"VirusTotal check error: {e}")
        
        return None

    def check_abuse_ch(self, package_name: str) -> bool:
        """
        Check package against Abuse.ch malware databases.
        Returns True if flagged as malicious.
        """
        if not self.abuse_ch_api_key:
            logger.warning("Abuse.ch API key not configured")
            return False
        
        try:
            url = "https://mb-api.abuse.ch/api/v1/"
            data = {
                "query": "get_info",
                "hash": package_name
            }
            response = requests.post(url, data=data, timeout=10)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("query_status") == "ok":
                    return True
                    
        except requests.RequestException as e:
            logger.error(f"Abuse.ch API request failed: {e}")
        except Exception as e:
            logger.error(f"Abuse.ch check error: {e}")
        
        return False

threat_intel_service = ThreatIntelService()

