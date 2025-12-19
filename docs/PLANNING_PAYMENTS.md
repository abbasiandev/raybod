# 💰 Phase 9: Commercialization & Payment Infrastructure

> **Status**: Planning
> **Context**: Implementing Freemium/Featured tiers with a Sandbox Payment Gateway.

## 1.0 Overview
To sustain the development of **Hybrid Cloud Sentinel**, we are introducing a tiered subscription model. This phase involves creating a "Featured" (Premium) plan and a "Freemium" plan, supported by a custom Sandbox Payment Gateway for testing and demonstration purposes.

## 2.0 Subscription Tiers

| Feature | Freemium (Free) | Featured (Premium) |
| :--- | :--- | :--- |
| **On-Device Scanning** | ✅ Unlimited | ✅ Unlimited |
| **Cloud Analysis** | ⚠️ Limited (e.g., 5/day) | ✅ Unlimited |
| **Threat Details** | ❌ Basic Verdict Only | ✅ Full Forensics & Explainability |
| **Real-Time Protection** | ❌ Manual Scans Only | ✅ Background Monitoring |
| **Support** | Community | Priority |

## 3.0 System Changes

### 3.1 Backend (Python FastAPI)
**New Models:**
*   `Plan`: Definitions of available plans (id, name, price, limits).
*   `Subscription`: Links `User` to `Plan` with status (active, cancelled, pastel).
*   `Payment`: Transaction logs (id, user_id, amount, status, gateway_ref).

**New Endpoints:**
*   `GET /api/v1/plans`: List public plans.
*   `POST /api/v1/subscriptions/checkout`: Create a payment session.
*   `GET /payment/sandbox/{session_id}`: **HTML Page** for the sandbox gateway.
*   `POST /payment/webhook`: Handle payment success/failure callbacks.
*   `GET /api/v1/users/me/subscription`: Check current plan status.

### 3.2 Web Frontend (Landing & Dashboard)
**New Pages:**
*   `landing.html` (Index):
    *   "Real Security Solution" branding.
    *   Value Proposition.
    *   Pricing Table (Free vs Featured).
    *   "Get Started" / "Download App" CTA.
*   `payment_sandbox.html`:
    *   Simulated credit card form.
    *   Debug Controls: **[Simulate Success]** | **[Simulate Failure]**.
*   **Dashboard Updates**:
    *   Show current plan badge.
    *   "Upgrade to Premium" banners if Free.

### 3.3 Android App
**Architecture Updates:**
*   **Domain**: `SubscriptionRepository` to manage plan state.
*   **Data**: `UserPreferences` to cache plan status (avoid blocking on network).
*   **UI**:
    *   **UpgradeScreen**: Benefits list + "Subscribe" button.
    *   **Locked Features**: Overlay on "Threat Details" and "Background Scan" settings for free users.
    *   **Payment Flow**: Open WebView or Browser to `backend_url/payment/sandbox/...`. Handle deep link return `sentinel://payment_result`.

## 4.0 Implementation Steps

1.  **Backend Models & API**: Implement `Plan`, `Subscription`, `Payment` tables and API.
2.  **Web Landing & Sandbox**: Create the high-quality landing page and the sandbox payment HTML.
3.  **Android Client Logic**: Implement `SubscriptionManager` and lock premium features.
4.  **End-to-End Test**: Verify "Subscribe" -> "Sandbox Pay" -> "App Unlock" flow.
