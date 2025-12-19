import pytest
from app.engine.heuristics import HeuristicEngine
from app.schemas.scan_schema import AppMetadata, RiskLevel
from app.core.database import init_db

# Initialize database for tests
init_db()

class TestSizeAnomaly:
    """Tests for the Size-Permission Anomaly Rule."""

    @pytest.fixture
    def heuristic_engine(self):
        """Create a fresh HeuristicEngine instance for each test."""
        return HeuristicEngine()

    def test_small_app_with_dangerous_permissions_returns_high(self, heuristic_engine):
        """Tiny app (<500KB) with dangerous permissions should be HIGH risk."""
        metadata = AppMetadata(
            package_name="com.tiny.spyware",
            version_code=1,
            signature="hash",
            app_size=100 * 1024,  # 100KB
            permissions=["android.permission.CAMERA"]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.HIGH
        assert result.threat_type == "Suspicious Lightweight App"
        assert "SizeAnomaly" in result.heuristics_used
        assert "small app" in result.description.lower()

    def test_normal_sized_app_with_camera_is_safe(self, heuristic_engine):
        """Normal sized app with Camera should be SAFE (unless other rules trigger)."""
        metadata = AppMetadata(
            package_name="com.normal.photo.app",
            version_code=1,
            signature="hash",
            app_size=10 * 1024 * 1024,  # 10MB
            permissions=["android.permission.CAMERA"]
        )

        result = heuristic_engine.analyze(metadata)

        # Should be SAFE because size > 500KB and < 3 dangerous permissions
        assert result.risk_level == RiskLevel.SAFE

    def test_small_app_with_safe_permissions_is_safe(self, heuristic_engine):
        """Tiny app with only safe permissions should be SAFE."""
        metadata = AppMetadata(
            package_name="com.tiny.safe",
            version_code=1,
            signature="hash",
            app_size=100 * 1024,  # 100KB
            permissions=["android.permission.INTERNET"]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.SAFE

    def test_multiple_dangerous_perms_in_small_app(self, heuristic_engine):
        """Tiny app with multiple dangerous permissions should still be HIGH (or arguably CRITICAL, but rule sets logic)."""
        metadata = AppMetadata(
            package_name="com.tiny.multispy",
            version_code=1,
            signature="hash",
            app_size=200 * 1024,  # 200KB
            permissions=[
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.HIGH
        assert "SizeAnomaly" in result.heuristics_used

    def test_size_exactly_500kb_should_be_safe(self, heuristic_engine):
        """Boundary condition: 500KB (approx) check."""
        # The logic is < 500,000
        metadata = AppMetadata(
            package_name="com.boundary.test",
            version_code=1,
            signature="hash",
            app_size=500_000,
            permissions=["android.permission.CAMERA"]
        )

        result = heuristic_engine.analyze(metadata)
        
        # 500,000 is not < 500,000, so valid
        assert result.risk_level == RiskLevel.SAFE
