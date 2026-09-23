"""
WhatsApp Cloud API Outbound Messaging Service
"""
import logging
import httpx
from app.config import settings
from app.models import OrderResult

logger = logging.getLogger("DistriParse.WhatsAppService")

class WhatsAppService:
    def format_confirmation_message(self, order: OrderResult) -> str:
        """Constructs an accessible, WhatsApp-formatted order confirmation message."""
        retailer_display = order.retailer_name or "Valued Partner"
        lines = [
            "✅ *Order Received & Logged*",
            "━━━━━━━━━━━━━━━━━━━",
            f"🏪 *Retailer:* {retailer_display}",
            f"📦 *Total Items:* {len(order.line_items)} item(s)",
            "━━━━━━━━━━━━━━━━━━━",
            "*Order Breakdown:*"
        ]

        if not order.line_items:
            lines.append("⚠️ _No recognized order items found in the message._")
        else:
            for idx, item in enumerate(order.line_items, 1):
                icon = "✓" if item.matched_in_catalog else "⚠️"
                sku_badge = f" `[{item.item_code}]`" if item.item_code else " _(Review)_"
                lines.append(
                    f"{idx}. {icon} *{item.normalized_name}*{sku_badge}\n"
                    f"   ↳ *Qty:* {item.quantity:g} {item.unit_of_measure} (Raw: _{item.original_text}_)"
                )

        if order.order_notes:
            lines.append("━━━━━━━━━━━━━━━━━━━")
            lines.append(f"📝 *Notes:* {order.order_notes}")

        lines.append("━━━━━━━━━━━━━━━━━━━")
        lines.append("🚀 *Status:* Queued for ERP purchase order export.")
        lines.append("🙏 _Thank you for doing business with us!_")
        return "\n".join(lines)

    async def send_text_message(self, to_phone: str, phone_number_id: str, message_text: str) -> bool:
        """
        Sends outbound WhatsApp message using Meta Graph API.
        Returns True if successful, False otherwise.
        """
        if not settings.WHATSAPP_TOKEN:
            logger.info(
                f"[SIMULATION MODE - NO WHATSAPP_TOKEN]\n"
                f"To: {to_phone} (via phone_number_id: {phone_number_id})\n"
                f"Content:\n{message_text}"
            )
            return True

        url = f"https://graph.facebook.com/{settings.WHATSAPP_API_VERSION}/{phone_number_id}/messages"
        headers = {
            "Authorization": f"Bearer {settings.WHATSAPP_TOKEN}",
            "Content-Type": "application/json"
        }
        payload = {
            "messaging_product": "whatsapp",
            "to": to_phone,
            "type": "text",
            "text": {"body": message_text}
        }

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.post(url, headers=headers, json=payload)
                if response.status_code in (200, 201):
                    logger.info(f"WhatsApp reply delivered to {to_phone}. Response: {response.text}")
                    return True
                else:
                    logger.error(
                        f"Meta Graph API error ({response.status_code}): {response.text}"
                    )
                    return False
        except httpx.TimeoutException:
            logger.error(f"Timeout connecting to Meta Graph API for recipient {to_phone}.")
            return False
        except Exception as e:
            logger.error(f"Unexpected error sending WhatsApp message: {e}")
            return False

# Global service singleton
whatsapp_service = WhatsAppService()
