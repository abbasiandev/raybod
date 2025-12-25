import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    APP_NAME: str = "Hybrid Cloud Sentinel Brain"
    VERSION: str = "1.0.0"
    DEBUG: bool = False
    PORT: int = 8000
    HOST: str = "0.0.0.0"
    
    # Database
    # Use /data directory for persistent storage in Liara
    DATABASE_URL: str = os.getenv("DATABASE_URL", "sqlite:////data/sentinel_brain.db")
    
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
