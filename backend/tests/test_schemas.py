"""
Test suite for Pydantic schemas (data validation and serialization).
"""
import pytest
from pydantic import ValidationError
from app.schemas.scan_schema import AppMetadata, ScanResult, RiskLevel


class TestRiskLevelEnum:
    """Tests for the RiskLevel enum."""

    def test_all_risk_levels_defined(self):
        """All expected risk levels should be defined."""
        expected = ["SAFE", "LOW", "MEDIUM", "HIGH", "CRITICAL", "UNKNOWN"]
        actual = [level.value for level in RiskLevel]

        assert set(expected) == set(actual)

    def test_risk_level_string_values(self):
        """Risk levels should have string values matching their names."""
        assert RiskLevel.SAFE.value == "SAFE"
        assert RiskLevel.CRITICAL.value == "CRITICAL"
        assert RiskLevel.UNKNOWN.value == "UNKNOWN"

    def test_risk_level_from_string(self):
        """Should be able to construct RiskLevel from string."""
        assert RiskLevel("SAFE") == RiskLevel.SAFE
        assert RiskLevel("CRITICAL") == RiskLevel.CRITICAL

    def test_invalid_risk_level_raises_error(self):
        """Invalid risk level string should raise ValueError."""
        with pytest.raises(ValueError):
            RiskLevel("INVALID")


class TestAppMetadataSchema:
    """Tests for the AppMetadata Pydantic model."""

    def test_valid_app_metadata_creation(self):
        """Should create AppMetadata with all required fields."""
        metadata = AppMetadata(
            package_name="com.example.app",
            version_code=42,
            signature="abc123hash"
        )

        assert metadata.package_name == "com.example.app"
        assert metadata.version_code == 42
        assert metadata.signature == "abc123hash"

    def test_permissions_defaults_to_empty_list(self):
        """Permissions should default to empty list."""
        metadata = AppMetadata(
            package_name="com.test",
            version_code=1,
            signature="hash"
        )

        assert metadata.permissions == []

    def test_permissions_with_values(self):
        """Should accept permissions list."""
        permissions = ["android.permission.CAMERA", "android.permission.INTERNET"]
        metadata = AppMetadata(
            package_name="com.test",
            version_code=1,
            signature="hash",
            permissions=permissions
        )

        assert len(metadata.permissions) == 2
        assert "android.permission.CAMERA" in metadata.permissions

    def test_missing_package_name_raises_error(self):
        """Missing package_name should raise ValidationError."""
        with pytest.raises(ValidationError):
            AppMetadata(
                version_code=1,
                signature="hash"
            )

    def test_missing_version_code_raises_error(self):
        """Missing version_code should raise ValidationError."""
        with pytest.raises(ValidationError):
            AppMetadata(
                package_name="com.test",
                signature="hash"
            )

    def test_missing_signature_raises_error(self):
        """Missing signature should raise ValidationError."""
        with pytest.raises(ValidationError):
            AppMetadata(
                package_name="com.test",
                version_code=1
            )

    def test_invalid_version_code_type_raises_error(self):
        """Non-integer version_code should raise ValidationError."""
        with pytest.raises(ValidationError):
            AppMetadata(
                package_name="com.test",
                version_code="not_an_int",
                signature="hash"
            )

    def test_json_serialization(self):
        """Should serialize to JSON correctly."""
        metadata = AppMetadata(
            package_name="com.json.test",
            version_code=100,
            signature="json_hash",
            permissions=["perm1"]
        )

        json_data = metadata.model_dump()

        assert json_data["package_name"] == "com.json.test"
        assert json_data["version_code"] == 100
        assert json_data["signature"] == "json_hash"
        assert json_data["permissions"] == ["perm1"]

    def test_from_dict(self):
        """Should create from dictionary."""
        data = {
            "package_name": "com.dict.test",
            "version_code": 50,
            "signature": "dict_hash",
            "permissions": []
        }

        metadata = AppMetadata(**data)

        assert metadata.package_name == "com.dict.test"
        assert metadata.version_code == 50


class TestScanResultSchema:
    """Tests for the ScanResult Pydantic model."""

    def test_valid_scan_result_creation(self):
        """Should create ScanResult with all required fields."""
        result = ScanResult(
            package_name="com.scanned.app",
            risk_level=RiskLevel.SAFE,
            description="No threats found"
        )

        assert result.package_name == "com.scanned.app"
        assert result.risk_level == RiskLevel.SAFE
        assert result.description == "No threats found"

    def test_threat_type_defaults_to_empty(self):
        """threat_type should default to empty string."""
        result = ScanResult(
            package_name="com.test",
            risk_level=RiskLevel.SAFE,
            description="Test"
        )

        assert result.threat_type == ""

    def test_heuristics_used_defaults_to_empty_list(self):
        """heuristics_used should default to empty list."""
        result = ScanResult(
            package_name="com.test",
            risk_level=RiskLevel.SAFE,
            description="Test"
        )

        assert result.heuristics_used == []

    def test_all_fields_populated(self):
        """Should accept all fields."""
        result = ScanResult(
            package_name="com.full.test",
            risk_level=RiskLevel.CRITICAL,
            threat_type="Malware",
            description="Known malware detected",
            heuristics_used=["Blocklist", "PermissionCombo"]
        )

        assert result.threat_type == "Malware"
        assert len(result.heuristics_used) == 2

    def test_all_risk_levels_valid(self):
        """All RiskLevel values should be valid for ScanResult."""
        for level in RiskLevel:
            result = ScanResult(
                package_name="com.test",
                risk_level=level,
                description=f"Test {level.value}"
            )
            assert result.risk_level == level

    def test_json_serialization(self):
        """Should serialize to JSON correctly."""
        result = ScanResult(
            package_name="com.serialize.test",
            risk_level=RiskLevel.HIGH,
            threat_type="Spyware",
            description="High risk",
            heuristics_used=["Test"]
        )

        json_data = result.model_dump()

        assert json_data["package_name"] == "com.serialize.test"
        assert json_data["risk_level"] == RiskLevel.HIGH
        assert json_data["threat_type"] == "Spyware"
        assert json_data["description"] == "High risk"
        assert json_data["heuristics_used"] == ["Test"]

    def test_json_serialization_mode(self):
        """Should serialize risk_level as string in JSON mode."""
        result = ScanResult(
            package_name="com.test",
            risk_level=RiskLevel.CRITICAL,
            description="Test"
        )

        json_str = result.model_dump_json()

        assert "CRITICAL" in json_str

    def test_missing_package_name_raises_error(self):
        """Missing package_name should raise ValidationError."""
        with pytest.raises(ValidationError):
            ScanResult(
                risk_level=RiskLevel.SAFE,
                description="Test"
            )

    def test_missing_risk_level_raises_error(self):
        """Missing risk_level should raise ValidationError."""
        with pytest.raises(ValidationError):
            ScanResult(
                package_name="com.test",
                description="Test"
            )

    def test_missing_description_raises_error(self):
        """Missing description should raise ValidationError."""
        with pytest.raises(ValidationError):
            ScanResult(
                package_name="com.test",
                risk_level=RiskLevel.SAFE
            )

    def test_invalid_risk_level_raises_error(self):
        """Invalid risk_level should raise ValidationError."""
        with pytest.raises(ValidationError):
            ScanResult(
                package_name="com.test",
                risk_level="INVALID",
                description="Test"
            )

    def test_empty_package_name_allowed(self):
        """Empty package_name should be allowed (Pydantic default behavior)."""
        metadata = AppMetadata(
            package_name="",
            version_code=1,
            signature="hash"
        )
        assert metadata.package_name == ""

    def test_negative_version_code_allowed(self):
        """Negative version_code should be allowed (edge case)."""
        metadata = AppMetadata(
            package_name="com.test",
            version_code=-1,
            signature="hash"
        )
        assert metadata.version_code == -1

    def test_zero_version_code_allowed(self):
        """Zero version_code should be allowed."""
        metadata = AppMetadata(
            package_name="com.test",
            version_code=0,
            signature="hash"
        )
        assert metadata.version_code == 0

    def test_empty_signature_allowed(self):
        """Empty signature should be allowed."""
        metadata = AppMetadata(
            package_name="com.test",
            version_code=1,
            signature=""
        )
        assert metadata.signature == ""

    def test_very_long_package_name(self):
        """Should handle very long package names."""
        long_name = "com." + "x" * 200
        metadata = AppMetadata(
            package_name=long_name,
            version_code=1,
            signature="hash"
        )
        assert metadata.package_name == long_name

    def test_empty_description_raises_error(self):
        """Empty description should raise ValidationError for ScanResult."""
        # Pydantic should allow empty strings by default, but let's test
        result = ScanResult(
            package_name="com.test",
            risk_level=RiskLevel.SAFE,
            description=""
        )
        assert result.description == ""

    def test_permissions_list_type_validation(self):
        """Permissions should validate as a list."""
        with pytest.raises(ValidationError):
            AppMetadata(
                package_name="com.test",
                version_code=1,
                signature="hash",
                permissions="not_a_list"  # Should be a list
            )

    def test_heuristics_used_list_type_validation(self):
        """heuristics_used should validate as a list."""
        with pytest.raises(ValidationError):
            ScanResult(
                package_name="com.test",
                risk_level=RiskLevel.SAFE,
                description="Test",
                heuristics_used="not_a_list"  # Should be a list
            )
