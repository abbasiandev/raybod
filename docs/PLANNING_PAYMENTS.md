# 💰 Phase 9: Commercialization & Payment Infrastructure

> **Status**: Complete
> **Context**: Implemented Freemium/Featured tiers with a Sandbox Payment Gateway.

## 1.0 Overview
To sustain the development of **Raybod**, we have introduced a tiered subscription model. This phase involved creating a "Featured" (Premium) plan and a "Freemium" plan, supported by a custom Sandbox Payment Gateway for testing and demonstration purposes.

## 2.0 Subscription Tiers

| Feature | Freemium (Free) | Featured (Premium) |
| :--- | :--- | :--- |
| **On-Device Scanning** | ✅ Unlimited | ✅ Unlimited |
| **Cloud Analysis** | ⚠️ Limited (e.g., 5/day) | ✅ Unlimited |
| **Threat Details** | ❌ Basic Verdict Only | ✅ Full Forensics & Explainability |
| **Real-Time Protection** | ❌ Manual Scans Only | ✅ Background Monitoring |
| **Support** | Community | Priority |

## 3.0 System Implementation

### 3.1 Backend (Python FastAPI)
**Models Implemented:**
*   `Plan`: Definitions of available plans.
*   `Subscription`: Links `User` to `Plan` with status.
*   `Payment`: Transaction logs.
*   `APIKey`: For secure device access.

**Endpoints Implemented:**
*   `GET /api/v1/public/plans`: List public plans.
*   `POST /api/v1/scan/analyze`: Rate-limited scan endpoint.
*   `GET /payment/sandbox/{session_id}`: HTML Page for the sandbox gateway.
*   `POST /api/v1/websocket`: Real-time updates for scan results.

### 3.2 Web Frontend (Landing & Dashboard)
**Pages Implemented:**
*   `landing.html`: Cyberpunk-themed high-conversion homepage.
*   `sandbox_pay.html`: Simulated credit card form with success/fail toggles.
*   `dashboard/`: Complete admin interface for user and threat management.

### 3.3 Android App
**Features Implemented:**
*   **PremiumScreen**: Benefits visualization and subscription trigger.
*   **Feature Gating**: Explainability and real-time monitoring locked behind premium.
*   **WebView Integration**: Seamless transition to sandbox payment gateway.

## 4.0 Verification
The end-to-end flow from subscription trigger in the Android app to payment completion in the sandbox gateway and subsequent feature unlocking has been verified.
