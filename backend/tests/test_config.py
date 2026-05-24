"""
Test suite for application configuration settings.
"""
import os
import pytest
from pydantic import ValidationError


class TestSettings:
    """Tests for the Settings configuration class."""

    def test_default_settings(self):
        """Settings should have default values."""
        from app.core.config import Settings
        
        # Create a new instance to test defaults
        # We need to clear any existing env vars first
        test_settings = Settings(_env_file=None)
        
        assert test_settings.APP_NAME == "Raybod Brain"
        assert test_settings.VERSION == "1.0.0"
        assert test_settings.DEBUG is False
        assert test_settings.PORT == 8000
        assert test_settings.HOST == "0.0.0.0"

    def test_settings_from_environment_variables(self, monkeypatch):
        """Settings should load from environment variables."""
        monkeypatch.setenv("APP_NAME", "Test App")
        monkeypatch.setenv("VERSION", "2.0.0")
        monkeypatch.setenv("DEBUG", "true")
        monkeypatch.setenv("PORT", "9000")
        monkeypatch.setenv("HOST", "127.0.0.1")
        
        # Create new Settings instance which should pick up env vars
        from app.core.config import Settings
        test_settings = Settings(_env_file=None)
        
        assert test_settings.APP_NAME == "Test App"
        assert test_settings.VERSION == "2.0.0"
        assert test_settings.DEBUG is True
        assert test_settings.PORT == 9000
        assert test_settings.HOST == "127.0.0.1"

    def test_port_must_be_integer(self, monkeypatch):
        """PORT must be a valid integer."""
        monkeypatch.setenv("PORT", "not_an_int")
        
        from app.core.config import Settings
        
        with pytest.raises(ValidationError):
            Settings(_env_file=None)

    def test_debug_must_be_boolean(self, monkeypatch):
        """DEBUG must be a valid boolean."""
        monkeypatch.setenv("DEBUG", "not_a_bool")
        
        from app.core.config import Settings
        
        with pytest.raises(ValidationError):
            Settings(_env_file=None)

    def test_settings_instance_exists(self):
        """Module should export a settings instance."""
        from app.core.config import settings
        
        assert settings is not None
        assert hasattr(settings, "APP_NAME")
        assert hasattr(settings, "VERSION")
        assert hasattr(settings, "DEBUG")
        assert hasattr(settings, "PORT")
        assert hasattr(settings, "HOST")

    def test_settings_can_be_serialized(self):
        """Settings should support dictionary conversion."""
        from app.core.config import Settings
        
        test_settings = Settings(_env_file=None)
        settings_dict = test_settings.model_dump()
        
        assert isinstance(settings_dict, dict)
        assert "APP_NAME" in settings_dict
        assert "VERSION" in settings_dict
        assert "DEBUG" in settings_dict
        assert "PORT" in settings_dict
        assert "HOST" in settings_dict

