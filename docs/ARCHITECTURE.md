# 🏗️ Architecture Guide

## Overview

Raybod follows **Clean Architecture** principles combined with **MVVM** pattern and **Multi-module** design. This document provides a comprehensive overview of the system architecture, design decisions, and component interactions.

---

## Table of Contents

1. [High-Level Architecture](#high-level-architecture)
2. [Android Architecture](#android-architecture)
3. [Backend Architecture](#backend-architecture)
4. [Data Flow](#data-flow)
5. [Security Considerations](#security-considerations)
6. [Design Decisions](#design-decisions)

---

## High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           SYSTEM ARCHITECTURE                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌────────────────────────────┐         ┌────────────────────────────┐     │
│   │      ANDROID CLIENT        │         │       CLOUD BRAIN          │     │
│   │                            │         │                            │     │
│   │  ┌──────────────────────┐  │         │  ┌──────────────────────┐  │     │
│   │  │    Presentation      │  │         │  │      API Layer       │  │     │
│   │  │  (Jetpack Compose)   │  │         │  │     (FastAPI)        │  │     │
│   │  └──────────┬───────────┘  │         │  └──────────┬───────────┘  │     │
│   │             │              │         │             │              │     │
│   │  ┌──────────▼───────────┐  │  HTTPS  │  ┌──────────▼───────────┐  │     │
│   │  │       Domain         │  │◄───────►│    Engine Layer      │  │     │
│   │  │   (Pure Kotlin)      │  │ WebSoc. │  (Heuristics + ML)   │  │     │
│   │  └──────────┬───────────┘  │◄───────►│  └──────────┬───────────┘  │     │
│   │             │              │         │             │              │     │
│   │  ┌──────────▼───────────┐  │         │  ┌──────────▼───────────┐  │     │
│   │  │        Data          │  │         │  │    Data Layer        │  │     │
│   │  │   (Room + Retrofit)  │  │         │  │   (SQLAlchemy + DB)  │  │     │
│   │  └──────────────────────┘  │         │  └──────────────────────┘  │     │
│   │                            │         │                            │     │
│   │  ┌──────────────────────┐  │         │  ┌──────────────────────┐  │     │
│   │  │       Agent          │  │         │  │  Retraining Pipeline │  │     │
│   │  │  (System Services)   │  │         │  │  (OTA Model Updates) │  │     │
│   │  └──────────────────────┘  │         │  └──────────────────────┘  │     │
│   │                            │         │                            │     │
│   └────────────────────────────┘         └────────────────────────────┘     │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Android Architecture

### Module Dependency Graph

```
                    ┌─────────┐
                    │  :app   │
                    └────┬────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
   ┌───────────┐  ┌────────────┐  ┌───────────┐
   │:presentat.│  │   :data    │  │  :agent   │
   └─────┬─────┘  └─────┬──────┘  └─────┬─────┘
         │              │               │
         └───────┬──────┴───────────────┘
                 │
                 ▼
           ┌──────────┐
           │ :domain  │
           └──────────┘
           (Pure Kotlin)
```

### Module Descriptions

#### `:app` Module
**Purpose**: Application entry point and Dependency Injection container.

```kotlin
// Key Components
- RaybodApp.kt     // Application class
- MainActivity.kt                // Single Activity host
- Navigation.kt                  // Navigation graph
- di/                           // Hilt modules
```

#### `:domain` Module
**Purpose**: Business logic layer with NO Android dependencies.

```kotlin
// Entities
- AppPackage.kt                 // Installed app representation
- RiskAssessment.kt             // Scan result model
- RiskLevel.kt                  // Risk severity enum

// Use Cases
- ScanAppUseCase.kt             // Orchestrates app scanning

// Repository Interfaces
- ThreatRepository.kt           // Data access abstraction
```

**Key Principle**: This module contains only pure Kotlin code. It defines the "ubiquitous language" of the domain and can be tested without any Android instrumentation.

#### `:data` Module
**Purpose**: Data access implementation.

```kotlin
// Local Storage (Room)
- AppDatabase.kt                // Room database
- RiskDao.kt                    // Data Access Object
- CachedRiskEntity.kt           // Database entity

// Remote Access (Retrofit)
- CloudBrainApi.kt              // API interface
- Dtos.kt                       // Data Transfer Objects

// Implementation
- ThreatRepositoryImpl.kt       // Repository implementation
- Mappers.kt                    // Entity <-> DTO mappers

// DI
- DataModule.kt                 // Hilt data module
```

#### `:presentation` Module
**Purpose**: UI layer using Jetpack Compose.

```kotlin
// Theme
- Theme.kt                      // Material3 theme
- Color.kt                      // Color palette (Cyberpunk aesthetic)
- Typography.kt                 // Font styles

// Screens
- DashboardScreen.kt            // Main dashboard with radar
- ScanningScreen.kt             // Real-time scan feedback
- ThreatReportScreen.kt         // Detailed threat analysis
- PremiumScreen.kt              // Monetization UI

// ViewModels
- ScanViewModel.kt              // Scan state management
```

#### `:agent` Module
**Purpose**: System-level device interaction.

```kotlin
// Services
- SentinelService.kt            // Foreground scanning service

// Scanners
- PackageAnalyzer.kt            // Permission & signature extraction

// DI
- AgentModule.kt                // Hilt agent module
```

---

## Backend Architecture

### Layer Structure

```
backend/app/
├── api/                        # API Layer
│   └── v1/
│       └── endpoints/
│           └── scan.py         # Scan endpoints
├── engine/                     # Business Logic
│   ├── heuristics.py           # Rule-based detection
│   └── classifier.py           # ML model wrapper (future)
├── models/                     # Database Models
│   └── threat.py               # SQLAlchemy models (future)
├── schemas/                    # API Contracts
│   └── scan_schema.py          # Pydantic schemas
├── core/                       # Shared utilities
│   └── config.py               # Configuration
└── main.py                     # FastAPI application
```

### API Design

The backend exposes a RESTful API with the following key endpoints:

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Health check |
| `POST` | `/api/v1/scan` | Submit app metadata for analysis |
| `GET` | `/api/v1/scan/{hash}` | Get cached analysis result |

### Heuristic Engine

The heuristic engine uses a rule-based system to detect threats:

```python
# Example Rule: Suspicious Permission Combination
class SpywareDetectionRule:
    """
    Detects apps with permission combinations typical of spyware.
    """
    dangerous_combinations = [
        {"CAMERA", "RECORD_AUDIO", "INTERNET"},      # Spy tool
        {"READ_SMS", "SEND_SMS", "INTERNET"},         # SMS stealer
        {"ACCESS_FINE_LOCATION", "INTERNET"},         # Location tracker
    ]
```

---

## Data Flow

### Scan Flow Sequence

```
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌───────────┐    ┌───────────┐
│  User   │    │ViewModel │    │ UseCase  │    │Repository │    │Cloud Brain│
└────┬────┘    └────┬─────┘    └────┬─────┘    └─────┬─────┘    └─────┬─────┘
     │              │               │                │                │
     │  Tap Scan    │               │                │                │
     │─────────────►│               │                │                │
     │              │ execute()     │                │                │
     │              │──────────────►│                │                │
     │              │               │ checkCache()   │                │
     │              │               │───────────────►│                │
     │              │               │                │                │
     │              │               │  [Cache Miss]  │                │
     │              │               │                │ POST /scan     │
     │              │               │                │───────────────►│
     │              │               │                │                │
     │              │               │                │  RiskResult    │
     │              │               │                │◄───────────────│
     │              │               │                │                │
     │              │               │  saveToCache() │                │
     │              │               │───────────────►│                │
     │              │               │                │                │
     │              │   Result      │                │                │
     │              │◄──────────────│                │                │
     │  Update UI   │               │                │                │
     │◄─────────────│               │                │                │
     │              │               │                │                │
```

### Caching Strategy

1. **Check Local Cache First**: Before any network call, check Room database
2. **Stale-While-Revalidate**: Show cached result immediately, update in background
3. **Time-Based Invalidation**: Cache entries expire after configurable TTL
4. **Offline Fallback**: If network unavailable, use cached data exclusively

---

## Security Considerations

### Data in Transit
- All communication uses HTTPS
- Certificate pinning recommended for production

### Data at Rest
- Local cache uses Room with encrypted shared preferences for sensitive data
- No PII stored on backend (only app metadata hashes)

### Permission Model
- Agent requires only `QUERY_ALL_PACKAGES` for scanning
- No root access required

---

## Design Decisions

### Why Clean Architecture?

| Decision | Rationale |
|----------|-----------|
| **Testability** | Domain logic can be unit tested without Android framework |
| **Flexibility** | Data sources can be swapped (mock, real, etc.) |
| **Maintainability** | Clear separation of concerns |
| **Scalability** | Easy to add new features in isolation |

### Why Multi-Module?

| Decision | Rationale |
|----------|-----------|
| **Build Speed** | Parallel compilation, incremental builds |
| **Code Isolation** | Enforced boundaries prevent layer leakage |
| **Team Scalability** | Different teams can own different modules |

### Why Cloud-Based Analysis?

| Decision | Rationale |
|----------|-----------|
| **Battery Efficiency** | CPU-intensive ML runs on server |
| **Model Updates** | No app update needed for new detection rules |
| **Collective Intelligence** | Aggregate threat data across all users |

---

## Future Considerations

1. **ML Model Integration**: Replace/augment heuristics with trained models
2. **Network Traffic Analysis**: Monitor suspicious connections
3. **Behavioral Analysis**: Long-term app behavior profiling
4. **Threat Intelligence Feed**: Integration with external threat databases
