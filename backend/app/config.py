import os
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    """
    Application configuration loaded from environment variables or .env file.
    """
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # Meta WhatsApp Cloud API credentials
    WHATSAPP_TOKEN: str = os.getenv("WHATSAPP_TOKEN", "")
    VERIFY_TOKEN: str = os.getenv("VERIFY_TOKEN", "parchai_secure_token")
    WHATSAPP_API_VERSION: str = "v20.0"

    # Google Gemini API
    GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "")
    GEMINI_MODEL: str = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
    GEMINI_TIMEOUT_SECONDS: float = 15.0

    # Google Sheets Configuration
    SHEETS_SPREADSHEET_NAME: str = os.getenv("SHEETS_SPREADSHEET_NAME", "ParchAI_Data")
    SERVICE_ACCOUNT_FILE: str = os.getenv("SERVICE_ACCOUNT_FILE", "service_account.json")
    GOOGLE_SERVICE_ACCOUNT_JSON: str = os.getenv("GOOGLE_SERVICE_ACCOUNT_JSON", "")

    # Server config
    PORT: int = int(os.getenv("PORT", 8000))
    HOST: str = os.getenv("HOST", "0.0.0.0")
    DEBUG: bool = os.getenv("DEBUG", "false").lower() == "true"

settings = Settings()
