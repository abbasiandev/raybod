FROM python:3.11-slim

WORKDIR /app

# Install system dependencies for weasyprint
RUN apt-get update && apt-get install -y \
    gcc \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libgdk-pixbuf-2.0-0 \
    libffi-dev \
    shared-mime-info \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app ./app

# Copy and setup entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Create /data directory with proper permissions (will be overridden by Liara mount)
# This ensures the directory exists even if mount fails
RUN mkdir -p /data && chmod 777 /data

# Expose port - Liara will use the PORT environment variable
EXPOSE 8000

# Use entrypoint script for proper initialization
ENTRYPOINT ["/entrypoint.sh"]