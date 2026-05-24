import os
from pydantic_settings import BaseSettings

def _get_database_url():
    """Determine the appropriate database URL with proper directory setup."""
    # Allow explicit override via environment variable
    env_url = os.getenv("DATABASE_URL")
    if env_url:
        return env_url
    
    # Check if /data directory exists (Liara deployment)
    if os.path.exists("/data"):
        # Ensure the directory is writable
        try:
            test_file = "/data/.write_test"
            with open(test_file, "w") as f:
                f.write("test")
            os.remove(test_file)
            # Directory is writable, use it
            return "sqlite:////data/sentinel_brain.db"
        except (OSError, IOError):
            # Directory exists but not writable, fall back to current directory
            pass
    
    # Default: use current directory
    return "sqlite:///./sentinel_brain.db"

class Settings(BaseSettings):
    APP_NAME: str = "Raybod Brain"
    VERSION: str = "1.0.0"
    DEBUG: bool = False
    PORT: int = 8000
    HOST: str = "0.0.0.0"
    
    # Database
    # Use /data directory for persistent storage in Liara (if it exists and writable)
    # Otherwise use current directory for local development/testing
    DATABASE_URL: str = _get_database_url()
    
    # Package List URL
    PACKAGE_LIST_URL: str = "https://raw.githubusercontent.com/codekhoda/threat-intel/main/package_lists.json"

    # JWT Configuration
    JWT_SECRET: str = "your-secret-key-change-in-production-use-env-var"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRE_MINUTES: int = 1440  # 24 hours

    class Config:
        env_file = ".env"
        extra = "ignore"

settings = Settings()
