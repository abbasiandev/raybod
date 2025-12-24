# 📡 API Reference

This document describes the Cloud Brain REST API endpoints, request/response formats, and usage examples.

---

## Table of Contents

1. [Overview](#overview)
2. [Base URL](#base-url)
3. [Authentication](#authentication)
4. [Endpoints](#endpoints)
5. [Data Schemas](#data-schemas)
6. [Error Handling](#error-handling)
7. [Examples](#examples)

---

## Overview

The Cloud Brain API is a RESTful service that accepts app metadata from Android clients and returns threat analysis results. It uses JSON for data exchange and follows OpenAPI 3.0 specification.

### Key Features

- **Asynchronous Processing**: Built on FastAPI for high throughput
- **Type Safety**: Pydantic schemas ensure data validation
- **Auto Documentation**: Swagger UI available at `/docs`

---

## Base URL

| Environment | URL |
|-------------|-----|
| **Local Development** | `http://localhost:8000` |
| **Docker** | `http://host.docker.internal:8000` |
| **Production** | `https://codekhoda-sentinel.liara.run` |

### API Versioning

All endpoints are versioned under `/api/v1/`.

```
Full endpoint: {base_url}/api/v1/{endpoint}
```

---

## Authentication

Authentication is implemented for all non-public endpoints.

- **API Key**: Required for device interaction. Header: `X-API-Key: your-api-key`
- **JWT Auth**: Required for admin dashboard and user-specific data. Header: `Authorization: Bearer <token>`

---

## Endpoints

### 🛡️ Scan Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/scan/analyze` | Submit app for analysis (Rate-limited) |
| `GET` | `/api/v1/scan/history` | Get recent scan logs |

### 🧠 Model & ML Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/models/current` | Get metadata for the latest active TFLite model |
| `GET` | `/api/v1/models/download/{version}` | Download specific model version |
| `POST` | `/api/v1/models/retrain` | Trigger automated model retraining |

### 🔍 Threat Intel Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `https://raw.githubusercontent.com/codekhoda/threat-intel/main/package_lists.json` | Remote package lists (Whitelist/Blacklist) |
| `GET` | `/api/v1/allowlist/check/{package}` | Check if a package is in the global allowlist |
| `GET` | `/api/v1/reputation/{package}` | Get global reputation score for a package |

### 👤 User & Subscription Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/auth/login` | Authenticate and get JWT |
| `GET` | `/api/v1/auth/me` | Get current user info (including `plan`) |
| `POST` | `/dashboard/billing/checkout` | Create a mock payment session |
| `POST` | `/dashboard/billing/verify` | Verify mock payment result |

### 📊 Admin & Analytics Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/analytics/overview` | Aggregate threat statistics |
| `GET` | `/api/v1/devices/list` | List registered devices |
| `POST` | `/api/v1/allowlist/add` | Add package to global allowlist |

---

## Data Schemas

### AppMetadata

App information sent from Android client.

```python
class AppMetadata(BaseModel):
    package_name: str              # e.g., "com.example.app"
    version_name: str              # e.g., "1.2.3"
    version_code: int              # e.g., 123
    signature_hash: str            # SHA-256 of signing certificate
    permissions: List[str]         # List of requested permissions
    install_source: Optional[str]  # Where app was installed from
    first_install_time: int        # Unix timestamp in milliseconds
    last_update_time: int          # Unix timestamp in milliseconds
```

### RiskAssessment

Threat analysis result returned to client.

```python
class RiskAssessment(BaseModel):
    package_name: str
    risk_level: RiskLevel          # Enum: SAFE, LOW, MEDIUM, HIGH, CRITICAL
    risk_score: int                # 0-100
    threats_detected: List[Threat]
    analysis_timestamp: datetime
    cached: bool                   # Whether result was from cache
```

### Threat

Individual threat detection.

```python
class Threat(BaseModel):
    type: ThreatType               # Enum: MALWARE, SPYWARE, ADWARE, etc.
    severity: Severity             # Enum: LOW, MEDIUM, HIGH, CRITICAL
    description: str               # Human-readable explanation
    recommendation: str            # Suggested action
```

---

## Error Handling

### Error Response Format

All errors follow this format:

```json
{
    "detail": "Error message here",
    "error_code": "ERROR_CODE",
    "timestamp": "2024-01-15T10:30:00Z"
}
```

### HTTP Status Codes

| Code | Meaning | When |
|------|---------|------|
| `200` | OK | Successful request |
| `201` | Created | Resource created |
| `400` | Bad Request | Invalid input data |
| `401` | Unauthorized | Missing/invalid auth |
| `404` | Not Found | Resource doesn't exist |
| `422` | Validation Error | Schema validation failed |
| `429` | Too Many Requests | Rate limit exceeded |
| `500` | Internal Error | Server-side failure |

### Validation Errors

```json
{
    "detail": [
        {
            "loc": ["body", "package_name"],
            "msg": "field required",
            "type": "value_error.missing"
        }
    ]
}
```

---

## Examples

### cURL Examples

**Scan an app:**
```bash
curl -X POST "http://localhost:8000/api/v1/scan" \
     -H "Content-Type: application/json" \
     -d '{
       "package_name": "com.suspicious.app",
       "version_name": "1.0.0",
       "version_code": 1,
       "signature_hash": "abc123def456",
       "permissions": [
         "android.permission.CAMERA",
         "android.permission.RECORD_AUDIO",
         "android.permission.INTERNET"
       ],
       "install_source": "unknown",
       "first_install_time": 1705312800000,
       "last_update_time": 1705312800000
     }'
```

**Get cached result:**
```bash
curl "http://localhost:8000/api/v1/scan/abc123def456"
```

### Python Example

```python
import httpx

async def scan_app(app_metadata: dict) -> dict:
    async with httpx.AsyncClient() as client:
        response = await client.post(
            "http://localhost:8000/api/v1/scan",
            json=app_metadata
        )
        response.raise_for_status()
        return response.json()
```

### Kotlin (Android) Example

```kotlin
interface CloudBrainApi {
    @POST("api/v1/scan")
    suspend fun scanApp(
        @Body metadata: AppMetadataDto
    ): Response<RiskAssessmentDto>

    @GET("api/v1/scan/{hash}")
    suspend fun getCachedResult(
        @Path("hash") signatureHash: String
    ): Response<RiskAssessmentDto>
}
```

---

## Rate Limiting (Planned)

| Tier | Requests/minute | Description |
|------|-----------------|-------------|
| Free | 60 | Basic protection |
| Premium | 600 | Real-time scanning |
| Enterprise | Unlimited | Full protection |

---

## Swagger Documentation

Access interactive API documentation:

- **Swagger UI**: `https://codekhoda-sentinel.liara.run/docs` (or `http://localhost:8000/docs`)
- **ReDoc**: `https://codekhoda-sentinel.liara.run/redoc` (or `http://localhost:8000/redoc`)
- **OpenAPI JSON**: `https://codekhoda-sentinel.liara.run/openapi.json` (or `http://localhost:8000/openapi.json`)
