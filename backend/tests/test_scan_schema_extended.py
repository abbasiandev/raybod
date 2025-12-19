import pytest
from app.schemas.scan_schema import AppMetadata

def test_app_metadata_accepts_optional_fields():
    """AppMetadata should accept optional intents, version_name, timestamps"""
    metadata = AppMetadata(
        package_name="com.test",
        version_code=1,
        signature="hash",
        permissions=["CAMERA"],
        intents=["android.intent.action.VIEW"],  # NEW
        version_name="1.0.0",  # NEW
        install_time=1234567890,  # NEW
        last_update_time=1234567890  # NEW
    )
    assert metadata.intents == ["android.intent.action.VIEW"]
    assert metadata.version_name == "1.0.0"
    assert metadata.install_time == 1234567890
    assert metadata.last_update_time == 1234567890

def test_app_metadata_backward_compatible():
    """Old clients without new fields should still work"""
    metadata = AppMetadata(
        package_name="com.test",
        version_code=1,
        signature="hash",
        permissions=[]
    )
    # Check default values
    assert metadata.intents == []
    assert metadata.version_name is None
    assert metadata.install_time is None
    assert metadata.last_update_time is None

