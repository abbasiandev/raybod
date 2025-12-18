# 🛡️ Hybrid Cloud Sentinel

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0--alpha-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Backend](https://img.shields.io/badge/backend-Python%20FastAPI-orange.svg)
![License](https://img.shields.io/badge/license-MIT-purple.svg)

**"The smartphone is too weak to fight alone. We brought the brain to the cloud."**

*A next-generation mobile security solution that offloads complex threat analysis to a centralized "Cloud Brain"*

[📖 Documentation](./docs/) • [🏗️ Architecture](./docs/ARCHITECTURE.md) • [🚀 Setup Guide](./docs/SETUP.md) • [🧪 Testing](./docs/TESTING.md)

</div>

---

## 🎯 Overview

**Hybrid Cloud Sentinel (HCS)** is a mobile security MVP that combines a lightweight on-device Android agent with a powerful Python-based backend analysis engine. The solution provides comprehensive threat detection while maintaining minimal battery impact on the user's device.

### Key Value Proposition

| Feature | Description |
|---------|-------------|
| **⚡ Lightweight Endpoint** | Minimal battery drain through cloud-offloaded processing |
| **🧠 Deep Analysis** | Cloud-based heuristics & ML for advanced threat detection |
| **🔴 Real-time Protection** | Instantaneous feedback and blocking capabilities |
| **🌐 Offline Support** | Local caching ensures protection even without connectivity |

---

## 🏗️ Architecture

We prioritize **Clean Architecture** with **MVVM** to ensure scalability and testability.

```
┌──────────────────────────────────────────────────────────────┐
│                     HYBRID CLOUD SENTINEL                    │
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

## 🚀 Quick Start

### Prerequisites

- **Android Development**: Android Studio Arctic Fox+, JDK 17
- **Backend Development**: Python 3.10+, pip

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/hybrid-cloud-sentinel.git
cd hybrid-cloud-sentinel
```

### 2. Start the Backend (Cloud Brain)

```bash
cd backend
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

*Server runs at `http://127.0.0.1:8000`*  
*API Documentation available at `http://127.0.0.1:8000/docs`*

### 3. Run the Android App

1. Open the `android/` folder in **Android Studio**
2. Sync Gradle dependencies
3. Configure the Cloud Brain URL in your local properties:
   ```properties
   # android/local.properties
   cloud.brain.url=http://10.0.2.2:8000
   ```
4. Build & Run the `:app` module on an emulator or device

> **Note**: Use `10.0.2.2` for Android Emulator to reach `localhost`. For physical devices, use your machine's local IP.

---

## 📁 Project Structure

```
hybrid-cloud-sentinel/
├── 📂 android/                    # Android Application
│   ├── 📂 app/                    # Main application module
│   ├── 📂 domain/                 # Business logic (Pure Kotlin)
│   │   ├── model/                 # Entities (AppPackage, RiskAssessment)
│   │   ├── repository/            # Repository interfaces
│   │   └── usecase/               # Use cases (ScanAppUseCase)
│   ├── 📂 data/                   # Data layer
│   │   ├── local/                 # Room database, DAOs
│   │   ├── remote/                # Retrofit API, DTOs
│   │   └── repository/            # Repository implementations
│   ├── 📂 presentation/           # UI Layer (Jetpack Compose)
│   │   ├── theme/                 # Design system
│   │   ├── dashboard/             # Main dashboard screen
│   │   └── scan/                  # Scanning screens
│   └── 📂 agent/                  # System services
│       ├── service/               # Foreground service
│       └── scanner/               # Package analyzer
├── 📂 backend/                    # Python Backend (Cloud Brain)
│   └── 📂 app/
│       ├── api/                   # REST endpoints
│       ├── engine/                # Heuristics & ML
│       ├── models/                # Database models
│       └── schemas/               # Pydantic schemas
├── 📂 docs/                       # Documentation
├── 📂 references/                 # Reference repositories
└── 📂 samples/                    # Test samples
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
- [x] **Heuristic Detection** - Permission-based risk analysis
- [x] **Modern UI** - Cyberpunk aesthetic with animations
- [x] **Clean Architecture** - MVVM with separation of concerns
- [ ] **ML Classification** - TensorFlow model integration (planned)
- [ ] **Network Monitoring** - Packet analysis (planned)
- [ ] **Premium Features** - Subscription model (planned)

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
