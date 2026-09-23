"""
WhatsApp B2B Order & Invoice Parser (Auto-PO / DistriParse)
Production FastAPI Webhook Service for Meta Cloud API & Google Gemini 2.5 Flash
"""
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.webhook_router import router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("ParchAI")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("==================================================")
    logger.info("ParchAI / Parchi WhatsApp Webhook Server Starting...")
    logger.info(f"Target Gemini Model: {settings.GEMINI_MODEL}")
    logger.info(f"Gemini API Key Configured: {'YES' if settings.GEMINI_API_KEY else 'NO (Fallback mode enabled)'}")
    logger.info(f"WhatsApp Token Configured: {'YES' if settings.WHATSAPP_TOKEN else 'NO (Simulation mode enabled)'}")
    logger.info(f"Google Sheets Spreadsheet: {settings.SHEETS_SPREADSHEET_NAME}")
    logger.info(f"Webhook Verify Token: {settings.VERIFY_TOKEN}")
    logger.info("==================================================")
    yield
    # Shutdown
    logger.info("ParchAI WhatsApp Webhook Server Shutting down...")

app = FastAPI(
    title="ParchAI B2B WhatsApp Webhook",
    description="Automated B2B order ingestion pipeline via WhatsApp Cloud API, Google Gemini 2.5 Flash & Google Sheets",
    version="1.1.0",
    lifespan=lifespan
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routes
app.include_router(router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host=settings.HOST, port=settings.PORT, reload=settings.DEBUG)
