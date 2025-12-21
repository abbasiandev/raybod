from sqlalchemy.orm import Session
from app.models.scan_log import ScanLog
from sqlalchemy import func

class ReputationService:
    """
    Service for calculating and tracking package reputation scores.
    Uses historical scan data to build confidence in package safety.
    """
    
    # Reputation thresholds
    CONFIDENCE_HIGH_THRESHOLD = 50
    CONFIDENCE_MEDIUM_THRESHOLD = 10
    
    def calculate_reputation(self, db: Session, package_name: str):
        """
        Calculate reputation score for a package based on scan history.
        
        Returns a score from 0.0 (all malicious) to 1.0 (all safe),
        along with confidence level based on number of scans.
        """
        total_scans = db.query(func.count(ScanLog.id)).filter(
            ScanLog.package_name == package_name
        ).scalar() or 0
        
        if total_scans == 0:
            return {
                "package_name": package_name,
                "reputation_score": 0.5,
                "confidence": "none",
                "total_scans": 0
            }
        
        safe_scan_count = db.query(func.count(ScanLog.id)).filter(
            ScanLog.package_name == package_name,
            ScanLog.risk_level == "SAFE"
        ).scalar() or 0
        
        reputation_score = safe_scan_count / total_scans
        
        # Determine confidence level based on sample size
        if total_scans >= self.CONFIDENCE_HIGH_THRESHOLD:
            confidence_level = "high"
        elif total_scans >= self.CONFIDENCE_MEDIUM_THRESHOLD:
            confidence_level = "medium"
        else:
            confidence_level = "low"
            
        return {
            "package_name": package_name,
            "reputation_score": reputation_score,
            "confidence": confidence_level,
            "total_scans": total_scans
        }

reputation_service = ReputationService()

