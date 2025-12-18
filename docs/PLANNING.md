# 🦅 Project Sentinel: Strategic & Architectural Blueprint

This document serves as the single source of truth for the **Hybrid Cloud Sentinel** project. It outlines the strategic mandate, system architecture, and the phased implementation roadmap to deliver a next-generation mobile threat defense platform.

---

## 1.0 Strategic Mandate & Market Opportunity

### 1.1 The Evolving Mobile Threat Landscape
Mobile devices are the new perimeter. The convergence of personal and professional data on a single device makes them high-value targets.
*   **85%** of organizations reported increased mobile attacks (Verizon MSI).
*   **4M+** mobile-focused social engineering attacks in 2024.
*   **BYOD** expands the attack surface significantly.

**Primary Attack Vectors:**
1.  **Phishing/Smishing**: Leading threat (48% in Retail, 39% in Healthcare).
2.  **Vulnerable Applications**: Unpatched apps as entry points.
3.  **Surveillanceware**: Stealthy data exfiltration (e.g., BnkRat, KrSpy).
4.  **Sideloaded Apps**: Bypassing store vetting mechanisms.

### 1.2 The Solution: Hybrid Cloud Sentinel
Our response is **"Practical AI"**:
*   **On-Device**: Instant, offline, battery-efficient protection using TFLite.
*   **Cloud**: Deep analysis and global threat intelligence.

**Architectural Pillars:**
| Pillar | Justification |
| :--- | :--- |
| **Platform Focus** | Android first for deep API integration and broad market reach. |
| **Core Intelligence** | Lightweight **TFLite** model to minimize battery drain (a top user complaint). |
| **Feature Engineering** | "DNA" analysis of `AndroidManifest.xml` (Permissions + Intents) vs signatures. |
| **Software Architecture** | **Hybrid Model**: Offline autonomy + Cloud collective intelligence. |

---

## 2.0 System Architecture Blueprint

### 2.1 On-Device Agent: The "Sentinel Brain"
Autonomous, real-time threat detection.
*   **Feature Extractor**: Static analysis of `AndroidManifest.xml` & bytecode. Generates a binary feature vector (DNA) based on risky permissions/intents (inspired by Drebin).
*   **Inference Engine**: Optimized **TensorFlow Lite** model (`saved_model.tflite`). Input: Feature Vector -> Output: Risk Score (0.0 - 1.0).
*   **Verdict System**: Maps Risk Score to **Safe**, **Risky**, or **Malware**.

### 2.2 Cloud Infrastructure: The Global Intelligence Hub
Python FastAPI backend acting as a force multiplier.
*   **Crowdsourced Intelligence**: Aggregates anonymized threat metadata.
*   **False Positive Mitigation**: "Global Allowlist" API to verify risky verdicts.
*   **OTA Model Updates**: Secure delivery of new `.tflite` models.

---

## 3.0 Enriched Implementation Roadmap

### Phase 0-2: Foundational Layers & Core AI Engine (Completed)
Established the technical bedrock and on-device detection.
- [x] **Project & Backend Setup**: Android multi-module + Python FastAPI skeleton.
- [x] **Test Infrastructure**: Comprehensive coverage for Domain, Data, and Presentation layers.
- [x] **On-Device AI Integration**: `saved_model.tflite` integration, "DNA" Feature Extractor with Explainability, and TFLite Service.

### Phase 3: Real-Time Protection (Completed)
Transforming from on-demand to always-on.
- [x] **Develop SentinelService**: Foreground service listening for `PACKAGE_ADDED`.
- [x] **Implement Instant Alerting**: Notifications for "Scanning..." and "Threat Detected!".
- [x] **Optimize Performance**: Enforce < 500ms execution budget via efficient signature hashing and resource parsing.

### Phase 4: UI/UX & Threat Visualization (Completed)
Building trust through transparency and aesthetics.
- [x] **Radar Dashboard**: Visualize the scanning process (Scanning -> Feature Extraction -> Verdict).
- [x] **Threat Details View**: Explainability - Show **WHY** an app was flagged (e.g., "Requests SMS + Camera").
- [x] **Cyberpunk Polish**: Apply "Hacker/Cyberpunk" design system (Neon colors, dark mode, glich effects).

### Phase 5: Cloud Connector & Hybrid Intelligence (Completed)
Activating the hybrid network effects.
- [x] **Enable Metadata Upload**: Send anonymized threat signatures to the cloud.
- [x] **Integrate Global Allowlist API**: Check "Risky" verdicts against the cloud allowlist.

---

## 4.0 Definition of Done & Success Metrics

| Criterion | Metric | Strategic Justification |
| :--- | :--- | :--- |
| **AI-Powered** | Detection driven solely by `.tflite` output (mocked in tests). | Validates the core "Practical AI" value proposition. |
| **Responsive** | UI never blocks. Background scan < 500ms. | Mitigates battery drain constraints and ensures retention. |
| **Accurate** | Correctly flags test virus (EICAR) as Malware. | Proves effective end-to-end detection. |
| **Beautiful** | Adheres to "Cyberpunk/Hacker" aesthetic. | key differentiator; builds brand identity. |
