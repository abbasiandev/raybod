# 🛡️ Raybod

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0--alpha-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Backend](https://img.shields.io/badge/backend-Python%20FastAPI-orange.svg)
![License](https://img.shields.io/badge/license-MIT-purple.svg)

![Live API](https://img.shields.io/badge/API-Live-success)

**"The smartphone is too weak to fight alone. We brought the brain to the cloud."**

*A next-generation mobile security solution that offloads complex threat analysis to a centralized "Cloud Brain"*

[📖 Documentation](./docs/) • [🏗️ Architecture](./docs/ARCHITECTURE.md) • [🚀 Setup Guide](./docs/SETUP.md) • [🧪 Testing](./docs/TESTING.md)

</div>

---

## 🎯 Overview

**Raybod** is a mobile security MVP that combines a lightweight on-device Android agent with a powerful Python-based backend analysis engine. The solution provides comprehensive threat detection while maintaining minimal battery impact on the user's device.

### Key Value Proposition

| Feature | Description |
|---------|-------------|
| **⚡ Lightweight Endpoint** | Minimal battery drain through cloud-offloaded processing |
| **🧠 Deep Analysis** | Cloud-based heuristics & ML for advanced threat detection |
| **🔴 Real-time Protection** | Instantaneous feedback and blocking capabilities |
| **🌐 Hybrid Ensemble** | On-device TFLite + Heuristic Matrix + Cloud Brain Intelligence |
| **🛡️ Trust-First UX** | Educational onboarding and security score gamification |
| **🔄 OTA Model Updates** | Automated ML model retraining and over-the-air updates |
| **📊 Admin Dashboard** | Comprehensive analytics and threat management interface |
| **💳 Flexible Plans** | Freemium and Featured subscription models with integrated billing |

---

## 🏗️ Architecture

We prioritize **Clean Architecture** with **MVVM** to ensure scalability and testability.

```
┌──────────────────────────────────────────────────────────────┐
│                            RAYBOD                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────┐                    ┌─────────────────┐  │
│  │  Android Agent  │◄──── HTTPS ───────►│   Cloud Brain   │  │
│  │                 │                    │                 │  │
│  │  ┌───────────┐  │                    │  ┌───────────┐  │  │
│  │  │:app       │  │                    │  │ FastAPI   │  │  │
│  │  │:domain    │  │    Threat Data     │  │ Engine    │  │  │
│  │  │:data      │  │◄──────────────────►│  │ ML Models │  │  │
│  │  │:present   │  │                    │  │ Heuristics│  │  │
│  │  │:agent     │  │                    │  └───────────┘  │  │
│  │  └───────────┘  │                    │                 │  │
│  └─────────────────┘                    └─────────────────┘  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Android Client (Kotlin)

| Module | Purpose |
|--------|---------|
| **`:app`** | Dependency Injection (Hilt), Application class, Navigation host |
| **`:domain`** | Pure Kotlin entities, Use Cases, Repository interfaces (NO Android deps) |
| **`:data`** | Repository implementations, Room Database, Retrofit API, Mappers |
| **`:presentation`** | Jetpack Compose UI, ViewModels, State holders |
| **`:agent`** | Foreground Services, Permission Analysis, Package Scanning |

### Cloud Brain (Python)

| Component | Purpose |
|-----------|---------|
| **FastAPI** | High-performance async API with automatic OpenAPI docs |
| **Pydantic** | Type safety and shared contracts with Android |
| **Heuristic Engine** | Rule-based detection of semantic threat patterns |
| **ML Classifier** | Extensible interface for TensorFlow/PyTorch models |

---

## 🌐 Live Deployment

The Cloud Brain is deployed and accessible at:

| Endpoint | URL |
|----------|-----|
| **Landing Page** | https://gitr_g6pdx-727.b.jrnm.app/ |
| **Admin Dashboard** | https://gitr_g6pdx-727.b.jrnm.app/dashboard/ |
| **Login Page** | https://gitr_g6pdx-727.b.jrnm.app/dashboard/login |
| **Health Check** | https://gitr_g6pdx-727.b.jrnm.app/health |
| **API Documentation** | https://gitr_g6pdx-727.b.jrnm.app/docs |
| **Scan Endpoint** | https://gitr_g6pdx-727.b.jrnm.app/api/v1/scan/analyze |
| **Threat Intel (Web)**| [Package Lists JSON](https://raw.githubusercontent.com/codekhoda/threat-intel/main/package_lists.json) |

### Infrastructure

| Component | Platform | Details |
|-----------|----------|---------|
| **Backend** | [JustRunMy.App](https://justrunmy.app) | Docker container |
| **Database** | SQLite | Lightweight embedded database |
| **Intel Source** | GitHub | Dynamic threat signatures (OTA) |

---

## 🛡️ Hybrid Security Matrix

We utilize a multi-layered approach to threat detection:

1.  **L1: Local Whitelist (System)**: Fast bypass for verified system/OS apps.
2.  **L2: Local TFLite Model**: On-device AI for instant heuristic flagging.
3.  **L3: Cloud Allow/Blocklist**: Real-time verification against global threat databases.
4.  **L4: External Intelligence**: Dynamic fetching of signatures from GitHub and VirusTotal.
5.  **L5: Contextual Analysis**: Correlating app categories with requested permissions.

---

## 🚀 Quick Start

### Prerequisites

- **Android Development**: Android Studio Arctic Fox+, JDK 17
- **Backend Development**: Python 3.10+, pip

### Option A: Use Live Backend (Recommended)

The Android app is pre-configured to use the live backend at `https://gitr_g6pdx-727.b.jrnm.app/`. Simply:

1. Clone the repository
2. Open `android/` in Android Studio
3. Build & Run on your device

### Option B: Local Development

#### 1. Clone the Repository

```bash
git clone https://github.com/your-org/raybod.git
cd raybod
```

#### 2. Start the Backend (Cloud Brain)

```bash
cd backend
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

*Local Server runs at `http://127.0.0.1:8000`*  
*Production API available at `https://gitr_g6pdx-727.b.jrnm.app`*  
*API Documentation: [Local](http://127.0.0.1:8000/docs) | [Production](https://gitr_g6pdx-727.b.jrnm.app/docs)*

#### 3. Configure Android for Local Backend

1. Open the `android/` folder in **Android Studio**
2. Sync Gradle dependencies
3. Configure the Cloud Brain URL in your `local.properties`:
   ```properties
   # android/local.properties
   # For local development (Emulator):
   cloud.brain.url=http://10.0.2.2:8000
   
   # For production:
   # cloud.brain.url=https://gitr_g6pdx-727.b.jrnm.app
   ```

---

## ☁️ Deployment Guide

### Deploying to JustRunMy.App

1. **Create app** on [JustRunMy.App](https://justrunmy.app/panel) and copy the Git deploy URL.

2. **Push backend only** (from repo root):

   ```bash
   git subtree split --prefix=backend -b justrunmy-deploy
   git push -u YOUR_JUSTRUNMY_GIT_URL justrunmy-deploy:deploy
   ```

3. **Configure in panel**:
   - HTTPS port: `8000`
   - Env: `JWT_SECRET`, `DEBUG=false`
   - Volume mount: `/data`

4. **Verify**:

   ```bash
   curl https://gitr_g6pdx-727.b.jrnm.app/health
   ```

### Database Information

The backend uses **SQLite** as an embedded database, which:
- Requires no external database service
- Stores data in a single file (`sentinel_brain.db`)
- Is perfect for MVP and small-scale deployments
- Automatically initializes on first startup

For production with high traffic, consider migrating to PostgreSQL by:
1. Adding `psycopg2-binary` to `requirements.txt`
2. Setting `DATABASE_URL` environment variable to PostgreSQL connection string
3. The code will automatically detect and use PostgreSQL

### Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | Database connection string | `sqlite:///./sentinel_brain.db` (default) |
| `JWT_SECRET` | Secret key for JWT tokens | `your-secret-key-change-in-production` |
| `DEBUG` | Enable debug mode | `false` |
| `SKIP_INIT_DB` | Skip database seeding on startup | `0` |

---

## 📁 Project Structure

```
raybod/
├── 📂 android/                    # Android Application
│   ├── 📂 app/                    # Main application module
│   ├── 📂 domain/                 # Business logic (Pure Kotlin)
│   │   ├── model/                 # Entities (AppPackage, RiskAssessment)
│   │   ├── repository/            # Repository interfaces
│   │   └── usecase/               # Use cases (ScanAppUseCase)
│   ├── 📂 data/                   # Data layer
│   │   ├── local/                 # Room database, DAOs
│   │   ├── remote/                # Retrofit API, DTOs
│   │   ├── ml/                    # TFLite model, FeatureExtractor
│   │   └── repository/            # Repository implementations
│   ├── 📂 presentation/           # UI Layer (Jetpack Compose)
│   │   ├── theme/                 # Cyberpunk design system
│   │   ├── components/            # Reusable UI components
│   │   ├── scan/                  # Scanning screens
│   │   └── about/                 # About screen
│   └── 📂 agent/                  # System services
│       ├── service/               # Foreground service (SentinelService)
│       └── scanner/               # Package analyzer
├── 📂 backend/                    # Python Backend (Cloud Brain)
│   ├── 📂 app/
│   │   ├── api/v1/endpoints/      # REST endpoints (scan, auth, dashboard)
│   │   ├── core/                  # Config, database, security
│   │   ├── engine/                # Heuristics & ML
│   │   ├── models/                # SQLAlchemy models (User, ScanLog)
│   │   ├── schemas/               # Pydantic schemas
│   │   ├── services/              # Business logic (auth)
│   │   ├── static/                # CSS, JavaScript
│   │   └── templates/             # Jinja2 HTML templates (dashboard)
│   └── 📂 tests/                  # pytest test suite
├── 📂 docs/                       # Documentation
├── 📂 references/                 # Reference ML models & datasets
└── 📂 samples/                    # Test APK samples
```

---

## 🧪 Testing

### Android Tests

```bash
# Unit Tests (Domain logic, ViewModels)
cd android
./gradlew testDebugUnitTest

# Instrumented Tests (Room DB, UI flows)
./gradlew connectedDebugAndroidTest
```

### Backend Tests

```bash
cd backend
pytest --cov=app tests/
```

### Manual Verification

1. **Threat Detection Test**: Install a test app with suspicious permissions
2. **Connectivity Test**: Verify offline mode shows cached results
3. **UI Fluidity**: Test radar animations on real device

---

## 🏆 Features Checklist

- [x] **Core Scanning Loop** - Real-time app analysis
- [x] **Cloud Integration** - Offloaded threat analysis
- [x] **Offline Support** - Local caching with Room
- [x] **ML Classification** - Ensemble TFLite model integration
- [x] **Trust-First Onboarding** - Educational permission dashboard
- [x] **OTA Model Updates** - Background model synchronization
- [x] **Admin Dashboard** - Real-time analytics and management
- [x] **Premium Features** - Subscription model and sandbox payments
- [x] **Network Monitoring** - Packet analysis (implemented)

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Architecture Guide](./docs/ARCHITECTURE.md) | Detailed system architecture and design decisions |
| [Setup Guide](./docs/SETUP.md) | Complete installation and configuration instructions |
| [API Reference](./docs/API.md) | Cloud Brain REST API documentation |
| [Development Guide](./docs/DEVELOPMENT.md) | Contributing guidelines and coding standards |
| [Testing Guide](./docs/TESTING.md) | Testing strategy and test writing guide |

---

## 🤝 Contributing

We welcome contributions! Please see our [Development Guide](./docs/DEVELOPMENT.md) for:

- Coding standards and conventions
- Branch naming and commit messages
- Pull request process
- Code review guidelines

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**Built with ❤️ by AI + Human Collaboration**

*Protecting your digital life, one scan at a time.*

</div>
