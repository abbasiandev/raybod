# 🎯 Project Planning: Hybrid Cloud Sentinel (HCS)

This document outlines the strategic roadmap, architectural decisions, and detailed implementation phases for the Hybrid Cloud Sentinel project. It balances high-impact features with practical, proven engineering solutions.

---

## 1. Project Vision & Strategy

### 1.1 The Problem
Modern mobile threats are complex, but many share common structural "DNA" (permissions and intent patterns) that traditional scanners miss. Users need instant protection without battery drain.

### 1.2 The Solution: "Hybrid Cloud Sentinel"
We deploy a **Practical AI** approach:
- **Instant On-Device AI**: A lightweight TFLite model (trained on the Drebin dataset) runs locally to detect 95% of common threats in milliseconds.
- **Deep Cloud Analysis**: A secondary layer for edge cases, handling heavy lifting and global threat correlation.

### 1.3 Strategic Pillars
- **Platform**: Android (Kotlin) + Backend (Python FastAPI).
- **Core Intelligence**: **TensorFlow Lite (TFLite)** for offline inference.
- **Feature Engineering**: Vectorization of `AndroidManifest.xml` (Permissions + Intents).
- **Architecture**: Clean Architecture + MVVM + Modularization.

---

## 2. Architectural Blueprint

### 2.1 The "Sentinel Brain" (Agent Module)
- **Feature Extractor**: Parses installed apps to generate a 2000-dimension binary vector (One-hot encoding of Permissions & Intents).
- **Inference Engine**: Runs the `saved_model.tflite` to output a Risk Score (0.0 - 1.0).
- **Verdict System**: Classifies apps as Safe, Risky, or Malware based on confidence thresholds.

### 2.2 Cloud Integration (Backend)
- **FastAPI**: Receives metadata from the device for secondary validation.
- **Threat Database**: Aggregates community findings (Crowdsourced Intelligence).
- **Model Updates**: Delivers new `.tflite` models to devices OTA (Over-The-Air).

---

## 3. Implementation Roadmap (The Marathon)

### Phase 0: Foundation & Skeleton ✅
- [x] Multi-module Android project setup.
- [x] Backend FastAPI skeleton.
- [x] Version Catalog & CI Foundation.

### Phase 1: Core Domain & Data Layer ✅
- [x] Define **Ubiquitous Language** (RiskAssessment, AppPackage).
- [x] Implement **ThreatRepository** with local shadowing.
- [x] Room caching for offline-first protection.

### Phase 1.5: Test Infrastructure ✅
- [x] **Backend Tests**: 56 pytest tests for API, heuristics, and schemas.
- [x] **Domain Tests**: Unit tests for models, enums, and use cases.
- [x] **Data Tests**: Repository, mapper, DTO, and ML score interpretation tests.
- [x] **Presentation Tests**: ViewModel lifecycle and state management tests.
- [x] See [TESTING.md](./TESTING.md) for full documentation.

### Phase 2: On-Device AI Integration (The Core Win) ✅
- [x] **Asset Integration**: Import `saved_model.tflite` and `features.json` from reference architecture.
- [x] **Feature Extractor**: Implement `PackageInfo` -> `FloatArray` converter (The "DNA" Extractor).
- [x] **TFLite Service**: Create a robust `MalwareScanner` class wrapping the interpreter.
- [x] **Validation**: Verify correct inference against known benign apps.

### Phase 3: Real-Time Protection 🔄
- [ ] **SentinelService**: Wire up the Foreground Service to `PACKAGE_ADDED` broadcasts.
- [ ] **Instant Alerting**: Trigger "Scanning..." notification -> "Threat Detected!" alert.
- [ ] **Performance Tuning**: Ensure background scan takes < 500ms per app.

### Phase 4: UI/UX & Visualization 🎨
- [ ] **Radar Dashboard**: Visualize the scanning process (Scanning -> Feature Extraction -> Verdict).
- [ ] **Threat Details**: Show **WHY** an app was flagged (e.g., "High Risk: Requests SMS + Camera + Internet").
- [ ] **Cyberpunk Polish**: Dark mode, neon accents, smooth transitions.

### Phase 5: Cloud Connector (The "Hybrid" Part) ☁️
- [ ] **Metadata Upload**: Send hashes of scanned apps to the Cloud Brain.
- [ ] **Global Allowlist**: Cloud API verifies if a "Risky" app is actually a known safe app (False Positive Mitigation).

---

## 4. Technical Deep Dive: The Machine Learning Stack

We are using a **proven Malware Detection Pipeline**:

1.  **Input**: `AndroidManifest.xml` of the target app.
2.  **Extraction**:
    -   Compare app permissions against a dictionary of ~500 risky permissions.
    -   Compare app intents against a dictionary of ~1500 suspicious intents.
    -   **Result**: A fixed-size float array (e.g., `float[2000]`).
3.  **Inference**:
    -   Load `.tflite` model.
    -   `model.run(inputVector, outputScore)`.
4.  **Output**: `Float` (0.0 = Safe, 1.0 = Malware).

This approach is **deterministic, explainable, and offline-ready**.

---

## 5. Definition of Done (DoD)
- [ ] **AI-Powered**: The app actually runs a TFLite model, no random number generators.
- [ ] **Responsive**: UI never freezes during analysis.
- [ ] **Accurate**: correctly identifies a test virus sample (EICAR or custom mock).
- [ ] **Beautiful**: The "Hacker/Cyberpunk" aesthetic is consistent.
