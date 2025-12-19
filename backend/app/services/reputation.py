from sqlalchemy.orm import Session
from app.models.scan_log import ScanLog
from sqlalchemy import func

class ReputationService:
    def calculate_reputation(self, db: Session, package_name: str):
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
        
        safe_scans = db.query(func.count(ScanLog.id)).filter(
            ScanLog.package_name == package_name,
            ScanLog.risk_level == "SAFE"
        ).scalar() or 0
        
        score = safe_scans / total_scans
        
        confidence = "low"
        if total_scans >= 50:
            confidence = "high"
        elif total_scans >= 10:
            confidence = "medium"
            
        return {
            "package_name": package_name,
            "reputation_score": score,
            "confidence": confidence,
            "total_scans": total_scans
        }

reputation_service = ReputationService()

