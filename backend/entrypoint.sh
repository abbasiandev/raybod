#!/bin/sh
set -e

echo "==================================="
echo "Raybod - Startup"
echo "==================================="

# Ensure /data directory exists and is writable
if [ -d "/data" ]; then
    echo "✓ /data directory exists"
    
    # Try to make it writable
    if chmod -R 777 /data 2>/dev/null; then
        echo "✓ /data permissions set successfully"
    else
        echo "⚠ Could not set /data permissions (may already be correct)"
    fi
    
    # Test write access
    if touch /data/.write_test 2>/dev/null; then
        rm -f /data/.write_test
        echo "✓ /data is writable"
    else
        echo "✗ /data is NOT writable - database may fail"
        echo "  Falling back to local directory"
    fi
else
    echo "⚠ /data directory does not exist"
    echo "  Creating local data directory"
    mkdir -p /app/data
fi

echo "==================================="
echo "Starting application..."
echo "==================================="

# Start the application
exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}
