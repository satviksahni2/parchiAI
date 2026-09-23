"""
FastAPI Router for WhatsApp Cloud API Webhooks and Parsing Endpoints
"""
import time
import logging
from typing import Optional
from fastapi import APIRouter, Request, HTTPException, Query, Response, status

from app.config import settings
from app.models import DirectParseRequest, DirectParseResponse, HealthResponse, OrderResult
from app.gemini_service import gemini_service
from app.whatsapp_service import whatsapp_service

logger = logging.getLogger("DistriParse.WebhookRouter")

router = APIRouter()

# ---------------------------------------------------------------------------
# 1. Meta Webhook Verification Endpoint (Required by WhatsApp Cloud API)
# ---------------------------------------------------------------------------
@router.get("/webhook")
async def verify_meta_webhook(
    hub_mode: Optional[str] = Query(None, alias="hub.mode"),
    hub_challenge: Optional[str] = Query(None, alias="hub.challenge"),
    hub_verify_token: Optional[str] = Query(None, alias="hub.verify_token"),
):
    """
    Handles Meta WhatsApp Cloud API verification handshake.
    Meta sends GET request with hub.mode, hub.challenge, and hub.verify_token.
    """
    logger.info(f"Webhook verification request: mode={hub_mode}, token={hub_verify_token}")

    if hub_mode == "subscribe" and hub_verify_token == settings.VERIFY_TOKEN:
        if not hub_challenge:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Missing hub.challenge parameter"
            )
        logger.info("Meta webhook verification challenge passed.")
        # Meta accepts integer or raw challenge string
        try:
            return int(hub_challenge)
        except ValueError:
            return Response(content=hub_challenge, media_type="text/plain")

    logger.warning(
        f"Webhook verification failed. Expected token '{settings.VERIFY_TOKEN}', got '{hub_verify_token}'"
    )
    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="Verification failed"
    )

# ---------------------------------------------------------------------------
# 2. Meta Inbound Webhook Event Handler
# ---------------------------------------------------------------------------
@router.post("/webhook")
async def receive_whatsapp_webhook(request: Request):
    """
    Receives incoming WhatsApp Cloud API events (messages, status updates, delivery receipts).
    Always returns 200 EVENT_RECEIVED to prevent Meta from retrying indefinitely.
    """
    try:
        body = await request.json()
    except Exception as e:
        logger.warning(f"Received malformed non-JSON payload: {e}")
        # Return 400 for completely unparseable HTTP bodies so clients know it's invalid
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Malformed JSON payload"
        )

    # Validate top-level Meta object
    if not isinstance(body, dict):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Payload must be a JSON object"
        )

    if body.get("object") != "whatsapp_business_account":
        logger.debug(f"Ignored non-whatsapp_business_account event: {body.get('object')}")
        return {"status": "IGNORED", "reason": "Not a whatsapp_business_account event"}

    # Process entries safely
    entries = body.get("entry")
    if not isinstance(entries, list) or not entries:
        logger.warning("Webhook payload has no valid 'entry' list.")
        return {"status": "EVENT_RECEIVED", "detail": "No entries found"}

    for entry in entries:
        if not isinstance(entry, dict):
            continue

        changes = entry.get("changes")
        if not isinstance(changes, list):
            continue

        for change in changes:
            if not isinstance(change, dict):
                continue

            val = change.get("value")
            if not isinstance(val, dict):
                continue

            # Check if this change contains inbound messages
            messages = val.get("messages")
            metadata = val.get("metadata", {})
            phone_number_id = metadata.get("phone_number_id", "")

            if not isinstance(messages, list) or not messages:
                # Status update event (e.g. read, delivered, sent)
                continue

            for msg in messages:
                if not isinstance(msg, dict):
                    continue

                sender_phone = msg.get("from")
                msg_type = msg.get("type")
                msg_id = msg.get("id")

                logger.info(
                    f"Processing message {msg_id} from {sender_phone} of type '{msg_type}'"
                )

                if msg_type == "text":
                    text_obj = msg.get("text", {})
                    order_text = text_obj.get("body", "")

                    if order_text.strip():
                        # Parse order with Gemini (or fallback engine on error/timeout)
                        order_result, source = await gemini_service.parse_order(order_text)
                        logger.info(
                            f"Order parsed successfully ({source}): {len(order_result.line_items)} items."
                        )

                        # Format reply and send back to retailer
                        if sender_phone:
                            reply_text = whatsapp_service.format_confirmation_message(order_result)
                            await whatsapp_service.send_text_message(
                                to_phone=sender_phone,
                                phone_number_id=phone_number_id,
                                message_text=reply_text
                            )
                else:
                    logger.info(f"Received non-text message type '{msg_type}'. Skipping for now.")

    return {"status": "EVENT_RECEIVED"}

# ---------------------------------------------------------------------------
# 3. Direct Parse API Endpoint (For developer testing and ERP integration)
# ---------------------------------------------------------------------------
@router.post("/parse", response_model=DirectParseResponse)
async def direct_parse_endpoint(payload: DirectParseRequest):
    """
    Direct endpoint for testing order parsing without going through WhatsApp.
    Takes a raw text string and returns the strictly formatted OrderResult JSON.
    """
    start_time = time.time()
    order_result, source = await gemini_service.parse_order(payload.text)
    elapsed_ms = round((time.time() - start_time) * 1000.0, 2)

    return DirectParseResponse(
        success=True,
        order=order_result,
        source=source,
        processing_time_ms=elapsed_ms
    )

# ---------------------------------------------------------------------------
# 4. Health Check Endpoint
# ---------------------------------------------------------------------------
@router.get("/health", response_model=HealthResponse)
async def health_check():
    """Service health and credentials verification endpoint."""
    return HealthResponse(
        status="healthy",
        version="1.0.0",
        gemini_configured=bool(settings.GEMINI_API_KEY),
        whatsapp_configured=bool(settings.WHATSAPP_TOKEN)
    )
