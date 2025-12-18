# 🚀 Setup Guide

This guide provides detailed instructions for setting up the Hybrid Cloud Sentinel development environment.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Repository Setup](#repository-setup)
3. [Backend Setup](#backend-setup)
4. [Android Setup](#android-setup)
5. [Configuration](#configuration)
6. [Running the Project](#running-the-project)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| **Git** | 2.0+ | Version control |
| **Android Studio** | Arctic Fox+ | Android development IDE |
| **JDK** | 17+ | Java Development Kit |
| **Python** | 3.10+ | Backend runtime |
| **pip** | Latest | Python package manager |

### System Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **RAM** | 8 GB | 16 GB |
| **Disk Space** | 10 GB | 20 GB |
| **OS** | macOS 11+, Windows 10+, Ubuntu 20.04+ | Latest LTS |

---

## Repository Setup

### Clone the Repository

```bash
# Clone the main repository
git clone https://github.com/your-org/hybrid-cloud-sentinel.git
cd hybrid-cloud-sentinel

# Verify project structure
ls -la
# Should show: android/ backend/ docs/ references/ samples/
```

### Reference Repositories (Optional)

The `references/` folder contains cloned repositories used for learning and context. These are already included but can be refreshed:

```bash
# Navigate to references
cd references

# Update reference repos (if needed)
cd PObY-A && git pull origin main && cd ..
cd AI-Malware-Ref && git pull origin main && cd ..
cd LibreAV && git pull origin main && cd ..
```

---

## Backend Setup

### Create Virtual Environment

```bash
cd backend

# Create virtual environment
python3 -m venv venv

# Activate virtual environment
# macOS/Linux:
source venv/bin/activate
# Windows:
.\venv\Scripts\activate
```

### Install Dependencies

```bash
# Install required packages
pip install -r requirements.txt
```

**requirements.txt contents:**
```
fastapi>=0.100.0
uvicorn[standard]>=0.22.0
pydantic>=2.0.0
python-multipart>=0.0.6
pytest>=7.0.0
httpx>=0.24.0
```

### Verify Installation

```bash
# Run health check
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# In another terminal, test the endpoint
curl http://localhost:8000/health
# Expected: {"status": "healthy"}
```

---

## Android Setup

### Open in Android Studio

1. Launch **Android Studio**
2. Select **Open an existing project**
3. Navigate to `hybrid-cloud-sentinel/android/`
4. Click **Open**

### Sync Gradle

1. Android Studio will prompt to sync Gradle
2. Click **Sync Now** in the notification bar
3. Wait for dependencies to download (may take several minutes)

### Configure SDK

If prompted, ensure you have:
- **Android SDK** 34 (or latest)
- **Android SDK Build-Tools** 34.0.0
- **Android SDK Platform-Tools**

### Gradle Version Catalog

The project uses `libs.versions.toml` for dependency management:

```toml
# android/gradle/libs.versions.toml
[versions]
kotlin = "1.9.21"
compose = "1.5.4"
hilt = "2.48"
room = "2.6.1"
retrofit = "2.9.0"

[libraries]
# ... dependency definitions
```

---

## Configuration

### Backend Configuration

Create environment file (optional):

```bash
# backend/.env
DEBUG=true
LOG_LEVEL=INFO
ALLOWED_ORIGINS=*
```

### Android Configuration

1. Create/update `local.properties` in `android/`:

```properties
# android/local.properties
sdk.dir=/path/to/your/Android/Sdk

# Cloud Brain Configuration
# For local development (Emulator):
# cloud.brain.url=http://10.0.2.2:8000

# For production (Liara):
cloud.brain.url=https://codekhoda-sentinel-brain.liara.run
cloud.brain.api.version=v1
```

2. For physical device testing, use your machine's IP:

```properties
# Find your local IP
# macOS: ifconfig | grep "inet " | grep -v 127.0.0.1
# Linux: hostname -I
# Windows: ipconfig

cloud.brain.url=http://192.168.1.100:8000
```

---

## Running the Project

### Step 1: Start the Backend

```bash
cd backend
source venv/bin/activate  # or .\venv\Scripts\activate on Windows
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

**Expected output:**
```
INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
INFO:     Started reloader process [xxxxx]
INFO:     Started server process [xxxxx]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
```

### Step 2: Verify Backend

```bash
# Health endpoint (Local)
curl http://localhost:8000/health

# Health endpoint (Production)
curl https://codekhoda-sentinel-brain.liara.run/health

# API documentation
# Local: http://localhost:8000/docs
# Production: https://codekhoda-sentinel-brain.liara.run/docs
```

### Step 3: Run Android App

1. In Android Studio, select run configuration: `app`
2. Choose target device:
   - **Emulator**: Create AVD with API 30+ (Android 11+)
   - **Physical**: Enable USB debugging
3. Click **Run** (▶️) or press `Shift+F10`

### Step 4: Verify End-to-End

1. App should launch with dashboard
2. Tap "Scan" button
3. Observe apps being analyzed
4. Check backend logs for incoming requests

---

## Troubleshooting

### Common Issues

#### Gradle Sync Failed

```
Error: Could not resolve all dependencies
```

**Solution:**
```bash
cd android
./gradlew clean
./gradlew --refresh-dependencies
```

#### Backend Connection Refused

```
Error: Failed to connect to Cloud Brain
```

**Solutions:**
1. Verify backend is running: `curl localhost:8000/health`
2. Check URL in `local.properties`
3. For emulator, use `10.0.2.2` (not `localhost`)
4. Ensure firewall allows port 8000

#### Room Database Migration Error

```
Error: Room cannot verify the data integrity
```

**Solution:**
```bash
# Clear app data on device/emulator
adb shell pm clear com.codekhoda.hybridcloudsentinel

# Or uninstall and reinstall
adb uninstall com.codekhoda.hybridcloudsentinel
```

#### Python Version Mismatch

```
Error: Python 3.10+ required
```

**Solution:**
```bash
# Check Python version
python3 --version

# If needed, install Python 3.10+
# macOS: brew install python@3.11
# Ubuntu: sudo apt install python3.11
```

### Getting Help

1. Check existing [GitHub Issues](https://github.com/your-org/hybrid-cloud-sentinel/issues)
2. Search documentation in `docs/` folder
3. Create a new issue with:
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, versions)

---

## Next Steps

- 📖 Read the [Architecture Guide](./ARCHITECTURE.md)
- 🧪 Learn about [Testing](./TESTING.md)
- 🛠️ Start [Contributing](./DEVELOPMENT.md)
