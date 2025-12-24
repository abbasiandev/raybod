import pytest
from app.engine.heuristics import HeuristicEngine
from app.schemas.scan_schema import AppMetadata, RiskLevel

@pytest.fixture
def heuristic_engine():
    return HeuristicEngine()

def test_spyware_intent_combination_detected(heuristic_engine):
    """App with SMS interception + call monitoring intents should be flagged"""
    metadata = AppMetadata(
        package_name="com.spyware.app",
        version_code=1,
        signature="hash",
        permissions=["android.permission.READ_SMS"],
        intents=[
            "android.provider.Telephony.SMS_RECEIVED",  # SMS interception
            "android.intent.action.PHONE_STATE",  # Call monitoring
            "android.intent.action.BOOT_COMPLETED",  # Auto-start
            "android.intent.action.NEW_OUTGOING_CALL"  # Outgoing call interception
        ]
    )
    result = heuristic_engine.analyze(metadata)
    assert result.risk_level == RiskLevel.HIGH
    assert "IntentAnalysis" in result.heuristics_used
    assert "Potential Spyware" in result.threat_type

def test_safe_app_with_few_intents(heuristic_engine):
    """App with only safe intents should pass"""
    metadata = AppMetadata(
        package_name="com.safe.app",
        version_code=1,
        signature="hash",
        permissions=[],
        intents=["android.intent.action.VIEW"]
    )
    result = heuristic_engine.analyze(metadata)
    assert result.risk_level == RiskLevel.SAFE

