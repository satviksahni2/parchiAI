import os
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    """
    Application configuration loaded from environment variables or .env file.
    """
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # Meta WhatsApp Cloud API credentials
    WHATSAPP_TOKEN: str = os.getenv("WHATSAPP_TOKEN", "")
    VERIFY_TOKEN: str = os.getenv("VERIFY_TOKEN", "my_secure_verify_token")
    WHATSAPP_API_VERSION: str = "v20.0"

    # Google Gemini API
    GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "")
    GEMINI_MODEL: str = "gemini-2.5-flash"
    GEMINI_TIMEOUT_SECONDS: float = 15.0

    # Server config
    PORT: int = int(os.getenv("PORT", 8000))
    HOST: str = "0.0.0.0"
    DEBUG: bool = os.getenv("DEBUG", "false").lower() == "true"

settings = Settings()
