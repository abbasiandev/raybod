# 🧪 Testing Guide

This document outlines the testing strategy for Hybrid Cloud Sentinel.

---

## Testing Philosophy

We follow **Test-Driven Development (TDD)** for domain logic:

1. Write failing test
2. Implement minimal code to pass
3. Refactor

---

## Android Testing

### Unit Tests

Located in `src/test/java/` within each module.

```bash
# Run all unit tests
cd android
./gradlew testDebugUnitTest

# Run specific module tests
./gradlew :domain:testDebugUnitTest
./gradlew :data:testDebugUnitTest
```

### Test Structure

```kotlin
class ScanAppUseCaseTest {

    private lateinit var useCase: ScanAppUseCase
    private lateinit var mockRepository: ThreatRepository

    @BeforeEach
    fun setup() {
        mockRepository = mockk()
        useCase = ScanAppUseCase(mockRepository)
    }

    @Test
    fun `invoke returns risk assessment when repository succeeds`() = runTest {
        // Given
        val expected = RiskAssessment(/*...*/)
        coEvery { mockRepository.getRiskAssessment(any()) } returns Result.success(expected)

        // When
        val result = useCase("com.example.app")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }
}
```

### Instrumented Tests

Located in `src/androidTest/java/`.

```bash
# Run on connected device/emulator
./gradlew connectedDebugAndroidTest
```

---

## Backend Testing

### Run Tests

```bash
cd backend
source venv/bin/activate

# Run all tests
pytest

# With coverage
pytest --cov=app tests/

# Verbose output
pytest -v
```

### Test Structure

```python
# tests/test_heuristics.py
import pytest
from app.engine.heuristics import analyze_permissions

class TestHeuristics:
    def test_detects_spyware_permissions(self):
        # Given
        permissions = ["CAMERA", "RECORD_AUDIO", "INTERNET"]
        
        # When
        result = analyze_permissions(permissions)
        
        # Then
        assert result.risk_level == "HIGH"
        assert "SPYWARE" in [t.type for t in result.threats]
```

---

## Manual Testing

### Test Threat Detection

1. Create test APK with suspicious permissions
2. Install on device
3. Run HCS scan
4. Verify threat detected

### Test Offline Mode

1. Start app with network
2. Perform scan (data cached)
3. Enable airplane mode
4. Verify cached results shown

### Test UI

1. Check radar animation smoothness
2. Verify dark theme consistency
3. Test on multiple screen sizes

---

## Coverage Goals

| Module | Target |
|--------|--------|
| `:domain` | 80%+ |
| `:data` | 70%+ |
| Backend Engine | 80%+ |
