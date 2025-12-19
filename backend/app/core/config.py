import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    APP_NAME: str = "Hybrid Cloud Sentinel Brain"
    VERSION: str = "1.0.0"
    DEBUG: bool = False
    PORT: int = 8000
    HOST: str = "0.0.0.0"
    
    # Package List URL
    PACKAGE_LIST_URL: str = "https://raw.githubusercontent.com/codekhoda/threat-intel/main/package_lists.json"

    class Config:
        env_file = ".env"

settings = Settings()
