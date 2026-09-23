from typing import List, Optional
from pydantic import BaseModel, Field

# ---------------------------------------------------------------------------
# Strict Gemini Output Schema (Enforced via response_schema)
# ---------------------------------------------------------------------------
class LineItem(BaseModel):
    original_text: str = Field(
        ...,
        description="The raw text or transcribed handwriting snippet for this item (e.g. '10 peti maggi')."
    )
    normalized_name: str = Field(
        ...,
        description="The standardized product name matched from the distributor catalog."
    )
    item_code: Optional[str] = Field(
        default=None,
        description="The catalog SKU code (e.g. 'F002'). Must be null if item is not matched in catalog."
    )
    quantity: float = Field(
        ...,
        description="The parsed numeric quantity ordered (e.g. 10.0, 5.0, 2.5)."
    )
    unit_of_measure: str = Field(
        ...,
        description="Standardized wholesale unit (e.g., 'box', 'strip', 'pieces', 'bag', 'bucket', 'can', 'bottle', 'roll')."
    )
    matched_in_catalog: bool = Field(
        ...,
        description="True if successfully mapped to a catalog SKU, false if uncataloged/flagged."
    )

class OrderResult(BaseModel):
    retailer_name: Optional[str] = Field(
        default=None,
        description="Name of the retail store or sender if mentioned in the message (e.g. 'Sharma Kirana Store')."
    )
    order_date: Optional[str] = Field(
        default=None,
        description="Date mentioned or implied in the order in YYYY-MM-DD format, or null."
    )
    line_items: List[LineItem] = Field(
        default_factory=list,
        description="List of extracted, normalized order line items."
    )
    order_notes: Optional[str] = Field(
        default=None,
        description="Special instructions, delivery timeline, or urgency flags (e.g. 'Deliver today evening quickly')."
    )

# ---------------------------------------------------------------------------
# Direct API & Webhook Request/Response Models
# ---------------------------------------------------------------------------
class DirectParseRequest(BaseModel):
    text: str = Field(..., min_length=1, description="Raw WhatsApp message or transcription text to parse")

class DirectParseResponse(BaseModel):
    success: bool
    order: OrderResult
    source: str = Field(default="gemini", description="'gemini' or 'deterministic_fallback'")
    processing_time_ms: float

class HealthResponse(BaseModel):
    status: str
    version: str
    gemini_configured: bool
    whatsapp_configured: bool
