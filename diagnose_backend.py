import requests
import json
import uuid
import time

BASE_URL = "https://codekhoda-sentinel-brain.liara.run"
DEVICE_ID = str(uuid.uuid4())

def test_endpoint(name, method, path, json_data=None, params=None):
    url = f"{BASE_URL}{path}"
    headers = {"X-Device-ID": DEVICE_ID}
    print(f"[*] Testing {name}: {method} {path}...", end=" ", flush=True)
    try:
        if method == "GET":
            response = requests.get(url, params=params, headers=headers)
        else:
            response = requests.post(url, json=json_data, headers=headers)
        
        if response.status_code == 200:
            print("OK ✅")
            return response.json()
        else:
            print(f"FAILED ❌ (Status: {response.status_code})")
            print(f"    Error: {response.text}")
            return None
    except Exception as e:
        print(f"ERROR 🛑 ({str(e)})")
        return None

def run_diagnostics():
    print(f"=== Sentinel Backend Diagnostics ===")
    print(f"Target: {BASE_URL}")
    print(f"Device ID: {DEVICE_ID}")
    print("-" * 40)

    # 1. Analyze Single App
    analyze_payload = {
        "package_name": "com.malware.test",
        "version_code": 1,
        "signature": "suspicious_signature",
        "permissions": ["android.permission.READ_SMS", "android.permission.INTERNET"],
        "intents": ["android.intent.action.BOOT_COMPLETED"],
        "has_reflection": True
    }
    test_endpoint("Analyze App", "POST", "/api/v1/scan/analyze", json_data=analyze_payload)

    # 2. Batch Scan
    batch_payload = {
        "packages": [
            {"package_name": "com.safe.app", "version_code": 10, "signature": "safe_sig", "permissions": []},
            {"package_name": "com.danger.app", "version_code": 5, "signature": "evil_sig", "permissions": ["android.permission.SEND_SMS"]}
        ]
    }
    test_endpoint("Batch Scan", "POST", "/api/v1/scan/batch", json_data=batch_payload)

    # 3. Threat Feed
    test_endpoint("Threat Feed", "GET", "/api/v1/threats/feed", params={"limit": 5})

    # 4. Network Analysis
    network_payload = {
        "flows": [
            {
                "source_app": "com.browser.app",
                "destination_ip": "1.2.3.4",
                "destination_port": 80,
                "protocol": "TCP",
                "domain": "malicious-site.com",
                "bytes_sent": 1024,
                "bytes_received": 2048,
                "timestamp": int(time.time())
            }
        ]
    }
    test_endpoint("Network Analysis", "POST", "/api/v1/network/analyze", json_data=network_payload)

    # 5. Reputation Check
    test_endpoint("Reputation Check", "GET", "/api/v1/reputation/com.malware.test")

    # 6. Current Model
    test_endpoint("Current Model", "GET", "/api/v1/models/current")

    print("-" * 40)
    print("Diagnostics Completed.")

if __name__ == "__main__":
    run_diagnostics()

