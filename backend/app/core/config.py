import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    APP_NAME: str = "Hybrid Cloud Sentinel Brain"
    VERSION: str = "1.0.0"
    DEBUG: bool = False
    PORT: int = 8000
    HOST: str = "0.0.0.0"
    
    # Database
    DATABASE_URL: str = "sqlite:///./sentinel_brain.db"
    
    # Package List URL
    PACKAGE_LIST_URL: str = "https://raw.githubusercontent.com/codekhoda/threat-intel/main/package_lists.json"

    class Config:
        env_file = ".env"
        extra = "ignore"

settings = Settings()
