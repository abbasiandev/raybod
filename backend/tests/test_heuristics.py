"""
Test suite for the HeuristicEngine that performs cloud-based threat analysis.
"""
import pytest
from app.engine.heuristics import HeuristicEngine, engine
from app.schemas.scan_schema import AppMetadata, RiskLevel
from app.core.database import init_db

# Initialize database for tests
init_db()


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

    def test_three_dangerous_permissions_returns_safe(self, heuristic_engine):
        """Apps with 3 dangerous permissions are now SAFE (threshold increased to 5)."""
        metadata = AppMetadata(
            package_name="com.legitimate.app",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        # With new threshold of 5, this should be SAFE (reduces false positives)
        assert result.risk_level == RiskLevel.SAFE

    def test_four_dangerous_permissions_returns_safe(self, heuristic_engine):
        """Apps with 4 dangerous permissions are now SAFE (threshold increased to 5)."""
        metadata = AppMetadata(
            package_name="com.video.call.app",
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

        # With new threshold of 5, this should be SAFE (prevents false positives for video call apps)
        assert result.risk_level == RiskLevel.SAFE

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

    def test_exactly_five_dangerous_permissions_boundary(self, heuristic_engine):
        """Exactly 5 dangerous permissions should trigger HIGH risk (new boundary)."""
        metadata = AppMetadata(
            package_name="com.boundary.test",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.HIGH
        assert result.threat_type == "Potential Spyware"

    def test_six_dangerous_permissions(self, heuristic_engine):
        """Six dangerous permissions should definitely return HIGH risk."""
        metadata = AppMetadata(
            package_name="com.all.dangerous",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS",
                "android.permission.RECEIVE_SMS"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        assert result.risk_level == RiskLevel.HIGH
        assert result.threat_type == "Potential Spyware"

    def test_duplicate_dangerous_permissions(self, heuristic_engine):
        """Duplicate dangerous permissions - need 5+ unique to trigger."""
        metadata = AppMetadata(
            package_name="com.duplicate.test",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.CAMERA",
                "android.permission.CAMERA",  # Duplicate
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        # Should be HIGH because we have 5 unique dangerous permissions
        assert result.risk_level == RiskLevel.HIGH
        assert result.threat_type == "Potential Spyware"

    def test_malware_with_dangerous_permissions(self, heuristic_engine):
        """Known malware should be CRITICAL even with dangerous permissions."""
        metadata = AppMetadata(
            package_name="com.example.virus",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        # Blocklist should take priority
        assert result.risk_level == RiskLevel.CRITICAL
        assert result.threat_type == "Known Malware"
        assert "Blocklist" in result.heuristics_used

    def test_case_sensitivity_package_name(self, heuristic_engine):
        """Package name matching should be case-sensitive."""
        metadata = AppMetadata(
            package_name="COM.EXAMPLE.VIRUS",  # Uppercase version
            version_code=1,
            signature="hash",
            permissions=[]
        )

        result = heuristic_engine.analyze(metadata)

        # Should be SAFE because case doesn't match
        assert result.risk_level == RiskLevel.SAFE

    def test_permission_case_sensitivity(self, heuristic_engine):
        """Permission matching should be case-sensitive."""
        metadata = AppMetadata(
            package_name="com.case.test",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.camera",  # Lowercase
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION"
            ]
        )

        result = heuristic_engine.analyze(metadata)

        # Should be SAFE because lowercase permission doesn't match
        assert result.risk_level == RiskLevel.SAFE

    def test_result_structure_completeness(self, heuristic_engine):
        """Result should have all required fields with proper types."""
        metadata = AppMetadata(
            package_name="com.structure.test",
            version_code=1,
            signature="hash",
            permissions=[]
        )

        result = heuristic_engine.analyze(metadata)

        assert hasattr(result, "package_name")
        assert hasattr(result, "risk_level")
        assert hasattr(result, "threat_type")
        assert hasattr(result, "description")
        assert hasattr(result, "heuristics_used")
        
        assert isinstance(result.package_name, str)
        assert isinstance(result.risk_level, RiskLevel)
        assert isinstance(result.threat_type, str)
        assert isinstance(result.description, str)
        assert isinstance(result.heuristics_used, list)

    def test_heuristics_used_content(self, heuristic_engine):
        """Heuristics_used should contain meaningful values."""
        # Test for blocklist
        malware_metadata = AppMetadata(
            package_name="com.example.virus",
            version_code=1,
            signature="hash",
            permissions=[]
        )
        malware_result = heuristic_engine.analyze(malware_metadata)
        assert len(malware_result.heuristics_used) > 0
        assert "Blocklist" in malware_result.heuristics_used

        # Test for permission combo
        perm_metadata = AppMetadata(
            package_name="com.perms.test",
            version_code=1,
            signature="hash",
            permissions=[
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS"  # Need 5+ for PermissionCombo
            ]
        )
        perm_result = heuristic_engine.analyze(perm_metadata)
        assert len(perm_result.heuristics_used) > 0
        assert "PermissionCombo" in perm_result.heuristics_used

        # Test for clean
        clean_metadata = AppMetadata(
            package_name="com.clean.test",
            version_code=1,
            signature="hash",
            permissions=[]
        )
        clean_result = heuristic_engine.analyze(clean_metadata)
        assert len(clean_result.heuristics_used) > 0
        assert "Clean" in clean_result.heuristics_used


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


class TestEnsembleMetadataSync:
    """On-device ML scores from phone batch sync should appear in cloud results."""

    def test_high_ensemble_score_returns_critical(self):
        heuristic_engine = HeuristicEngine()
        from app.schemas.scan_schema import DrebinFeatures
        metadata = AppMetadata(
            package_name="com.phone.sync.test",
            version_code=1,
            signature="hash",
            permissions=["android.permission.INTERNET"],
            ensemble_metadata={
                "final_boosted_score": 0.95,
                "calibrated_score": 0.92,
            },
        )
        result = heuristic_engine._assess_from_ensemble_metadata(metadata, DrebinFeatures())
        assert result is not None
        assert result.risk_level == RiskLevel.CRITICAL
        assert result.threat_type == "Malware Detected"
        assert "OnDeviceEnsemble" in result.heuristics_used

    def test_analyze_uses_ensemble_when_cloud_heuristics_clean(self):
        heuristic_engine = HeuristicEngine()
        metadata = AppMetadata(
            package_name="com.phone.sync.clean.heuristics",
            version_code=1,
            signature="hash",
            permissions=["android.permission.INTERNET"],
            ensemble_metadata={"final_boosted_score": 0.82},
        )
        result = heuristic_engine.analyze(metadata)
        assert result.risk_level == RiskLevel.CRITICAL
        assert "OnDeviceEnsemble" in result.heuristics_used
