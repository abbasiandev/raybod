import json
import os
import time

DEBUG_LOG_PATH = os.getenv(
    "DEBUG_LOG_PATH",
    "/Users/mahdiabbasian/Documents/Android/Workspace/raybod/.cursor/debug-e7b765.log",
)
SESSION_ID = "e7b765"


def agent_debug_log(
    hypothesis_id: str,
    location: str,
    message: str,
    data: dict | None = None,
    run_id: str = "pre-fix",
) -> None:
    entry = {
        "sessionId": SESSION_ID,
        "hypothesisId": hypothesis_id,
        "location": location,
        "message": message,
        "data": data or {},
        "timestamp": int(time.time() * 1000),
        "runId": run_id,
    }
    line = json.dumps(entry)
    try:
        with open(DEBUG_LOG_PATH, "a", encoding="utf-8") as handle:
            handle.write(line + "\n")
    except OSError:
        pass
