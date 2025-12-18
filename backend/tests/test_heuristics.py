"""
Test suite for the HeuristicEngine that performs cloud-based threat analysis.
"""
import pytest
from app.engine.heuristics import HeuristicEngine, engine
from app.schemas.scan_schema import AppMetadata, RiskLevel


class TestHeuristicEngine:
    """Tests for the HeuristicEngine class."""

    @pytest.fixture
    def heuristic_engine(self):
        """Create a fresh HeuristicEngine instance for each test."""
        return HeuristicEngine()

    # ========== Known Malware Detection Tests ==========

    def test_known_malware_returns_critical(self, heuristic_engine):
        """Known malware packages should be flagged as CRITICAL."""
        metadata = AppMetadata(
            package_name="com.example.virus",
            version_code=1,
            signature="malware_hash",
            permissions=[]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.CRITICAL
        assert result.threat_type == "Known Malware"
        assert "blocklist" in result.description.lower()
        assert "Blocklist" in result.heuristics_used

    def test_spyware_tracker_returns_critical(self, heuristic_engine):
        """Another known malware package should return CRITICAL."""
        metadata = AppMetadata(
            package_name="com.spyware.tracker",
            version_code=1,
            signature="spyware_hash",
            permissions=[]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.CRITICAL
        assert result.threat_type == "Known Malware"

    # ========== Dangerous Permission Combination Tests ==========

    def test_three_dangerous_permissions_returns_high(self, heuristic_engine):
        """Apps with 3+ dangerous permissions should be flagged as HIGH risk."""
        metadata = AppMetadata(
            package_name="com.suspicious.app",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.HIGH
        assert result.threat_type == "Potential Spyware"
        assert "PermissionCombo" in result.heuristics_used
        assert "CAMERA" in result.description
        assert "RECORD_AUDIO" in result.description
        assert "ACCESS_FINE_LOCATION" in result.description

    def test_four_dangerous_permissions_returns_high(self, heuristic_engine):
        """Apps with 4 dangerous permissions should also be HIGH risk."""
        metadata = AppMetadata(
            package_name="com.very.suspicious",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.READ_SMS"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.HIGH
        assert result.threat_type == "Potential Spyware"

    # ========== Safe App Tests ==========

    def test_clean_app_returns_safe(self, heuristic_engine):
        """Apps with no suspicious indicators should be SAFE."""
        metadata = AppMetadata(
            package_name="com.legitimate.app",
            version_code=1,
            signature="hash",
            permissions=[]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.SAFE
        assert result.threat_type == ""
        assert "no threats" in result.description.lower()
        assert "Clean" in result.heuristics_used

    def test_one_dangerous_permission_is_safe(self, heuristic_engine):
        """Single dangerous permission should not trigger HIGH risk."""
        metadata = AppMetadata(
            package_name="com.camera.app",
            version_code=1,
            signature="hash",
            permissions=["android.permission.CAMERA"]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.SAFE

    def test_two_dangerous_permissions_is_safe(self, heuristic_engine):
        """Two dangerous permissions should not trigger HIGH risk (threshold is 3)."""
        metadata = AppMetadata(
            package_name="com.audio.recorder",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.SAFE

    def test_non_dangerous_permissions_are_safe(self, heuristic_engine):
        """Apps with many non-dangerous permissions should be SAFE."""
        metadata = AppMetadata(
            package_name="com.normal.app",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.INTERNET",
                "android.permission.VIBRATE",
                "android.permission.WAKE_LOCK",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.FOREGROUND_SERVICE"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.SAFE

    # ========== Edge Cases ==========

    def test_empty_permissions_list(self, heuristic_engine):
        """Empty permissions list should be SAFE."""
        metadata = AppMetadata(
            package_name="com.minimal.app",
            version_code=1,
            signature="hash",
            permissions=[]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.SAFE

    def test_result_package_name_matches_input(self, heuristic_engine):
        """Result should contain the same package name as input."""
        package_name = "com.test.matching"
        metadata = AppMetadata(
            package_name=package_name,
            version_code=1,
            signature="hash",
            permissions=[]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.package_name == package_name

    def test_blocklist_takes_priority_over_permissions(self, heuristic_engine):
        """Known malware should be CRITICAL even without dangerous permissions."""
        metadata = AppMetadata(
            package_name="com.example.virus",
            version_code=1,
            signature="hash",
            permissions=[]  # No permissions, but known malware
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.CRITICAL
        assert "Blocklist" in result.heuristics_used

    def test_mixed_dangerous_and_safe_permissions(self, heuristic_engine):
        """Mix of dangerous and safe permissions with <3 dangerous should be SAFE."""
        metadata = AppMetadata(
            package_name="com.mixed.app",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.INTERNET",
                "android.permission.CAMERA",  # Dangerous
                "android.permission.VIBRATE",
                "android.permission.RECORD_AUDIO",  # Dangerous
                "android.permission.WAKE_LOCK"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        # Only 2 dangerous permissions, should be SAFE
        assert result.risk_level == RiskLevel.SAFE


class TestGlobalEngineInstance:
    """Tests for the module-level engine instance."""

    def test_global_engine_exists(self):
        """The module should export a global engine instance."""
        assert engine is not None
        assert isinstance(engine, HeuristicEngine)

    def test_global_engine_works(self):
        """Global engine should function correctly."""
        metadata = AppMetadata(
            package_name="com.test.global",
            version_code=1,
            signature="hash",
            permissions=[]
        )

        result = engine.analyze(metadata)

        assert result.risk_level == RiskLevel.SAFE
