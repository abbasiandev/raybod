# 🧪 Hybrid Cloud Sentinel - Test Suite

This document describes the comprehensive test suite for the Hybrid Cloud Sentinel project.

---

## Test Coverage Summary

| Module | Test File | Tests | Status |
|--------|-----------|-------|--------|
| **Backend (Python)** | | **56** | ✅ |
| - Heuristics Engine | `test_heuristics.py` | 14 | ✅ |
| - Scan API Endpoint | `test_scan_endpoint.py` | 18 | ✅ |
| - Pydantic Schemas | `test_schemas.py` | 24 | ✅ |
| **Android (Kotlin)** | | **~50+** | 📝 |
| - Domain: RiskLevel | `RiskLevelTest.kt` | 4 | 📝 |
| - Domain: AppPackage | `AppPackageTest.kt` | 6 | 📝 |
| - Domain: RiskAssessment | `RiskAssessmentTest.kt` | 6 | 📝 |
| - Domain: Repository Contract | `ThreatRepositoryContractTest.kt` | 5 | 📝 |
| - Domain: ScanAppUseCase | `ScanAppUseCaseTest.kt` | 1 | 📝 |
| - Data: CachedRiskEntity | `CachedRiskEntityTest.kt` | 7 | 📝 |
| - Data: Mappers | `MappersTest.kt` | 8 | 📝 |
| - Data: DTOs | `DtosTest.kt` | 10 | 📝 |
| - Data: MalwareScanner | `MalwareScannerScoreInterpretationTest.kt` | 14 | 📝 |
| - Data: Repository | `ThreatRepositoryImplTest.kt` | 8 | 📝 |
| - Presentation: ViewModel | `ScanViewModelTest.kt` | 7 | 📝 |

> **Legend**: ✅ = Verified, 📝 = Written (needs Gradle build to verify)

---

## Running Tests

### Backend (Python)

```bash
cd backend
pip install -r requirements.txt
pytest tests/ -v
```

**With coverage report:**
```bash
pytest tests/ --cov=app --cov-report=html
```

### Android (Kotlin)

```bash
cd android
./gradlew test
```

**Run specific module tests:**
```bash
./gradlew :domain:test
./gradlew :data:test
./gradlew :presentation:test
```

---

## Test Architecture

### Backend Tests

#### `test_heuristics.py`
Tests the `HeuristicEngine` threat detection:
- ✅ Known malware blocklist detection
- ✅ Dangerous permission combination detection (3+ triggers HIGH risk)
- ✅ Clean app classification
- ✅ Edge cases (empty permissions, single dangerous permission)

#### `test_scan_endpoint.py`
Tests the FastAPI `/api/v1/scan/analyze` endpoint:
- ✅ Clean app returns SAFE
- ✅ Known malware returns CRITICAL
- ✅ Suspicious permissions return HIGH
- ✅ Request validation (missing fields → 422)
- ✅ Response format compliance

#### `test_schemas.py`
Tests Pydantic models:
- ✅ `RiskLevel` enum values
- ✅ `AppMetadata` validation
- ✅ `ScanResult` serialization

---

### Android Tests

#### Domain Layer
| Test Class | Purpose |
|------------|---------|
| `RiskLevelTest` | Verify enum values and ordinal order |
| `AppPackageTest` | Data class equality, copying, defaults |
| `RiskAssessmentTest` | Assessment creation for all risk levels |
| `ThreatRepositoryContractTest` | Interface contract verification |
| `ScanAppUseCaseTest` | Use case invocation with mocks |

#### Data Layer
| Test Class | Purpose |
|------------|---------|
| `CachedRiskEntityTest` | Room entity field handling |
| `MappersTest` | Entity ↔ Domain bidirectional mapping |
| `DtosTest` | API DTOs and serialization |
| `MalwareScannerScoreInterpretationTest` | TFLite score thresholds |
| `ThreatRepositoryImplTest` | Repository flow (cache → AI → cloud) |

#### Presentation Layer
| Test Class | Purpose |
|------------|---------|
| `ScanViewModelTest` | ViewModel state management, scan flow |

---

## Test Dependencies

### Backend
```
pytest==7.4.4
pytest-cov==4.1.0
httpx==0.26.0  # For FastAPI TestClient
```

### Android
```kotlin
testImplementation(libs.junit)
testImplementation(libs.mockk)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.androidx.arch.core.testing)
```

---

## Key Test Patterns

### 1. Mocking with MockK (Kotlin)
```kotlin
private val threatRepository: ThreatRepository = mockk()
coEvery { threatRepository.scanApp(any()) } returns expectedAssessment
coVerify(exactly = 1) { threatRepository.scanApp(appPackage) }
```

### 2. Coroutines Testing
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
}
```

### 3. FastAPI Testing (Python)
```python
from fastapi.testclient import TestClient

@pytest.fixture
def client():
    return TestClient(app)

def test_endpoint(client):
    response = client.post("/api/v1/scan/analyze", json=payload)
    assert response.status_code == 200
```

---

## Coverage Goals

| Layer | Target Coverage |
|-------|-----------------|
| Domain | 90%+ |
| Data (Mappers, DTOs) | 90%+ |
| Data (Repository) | 80%+ |
| Presentation (ViewModel) | 80%+ |
| Backend API | 85%+ |

---

## Next Steps

1. **Run Android tests**: `./gradlew test` after Gradle sync
2. **Add UI Tests**: Compose UI tests with `androidTestImplementation`
3. **Add Integration Tests**: End-to-end flow with real TFLite model
4. **CI/CD Integration**: GitHub Actions workflow for automated testing
