"""
Gemini AI Service with Strict Pydantic Schema Extraction and Fallback Handling
"""
import re
import json
import logging
import asyncio
from typing import Tuple
from datetime import datetime

from app.config import settings
from app.models import OrderResult, LineItem
from app.catalog import SYSTEM_INSTRUCTION, MASTER_CATALOG

logger = logging.getLogger("DistriParse.GeminiService")

class GeminiService:
    def __init__(self):
        self._client = None
        self._initialize_client()

    def _initialize_client(self):
        """Initializes Google GenAI client if API key is configured."""
        if settings.GEMINI_API_KEY:
            try:
                from google import genai
                self._client = genai.Client(api_key=settings.GEMINI_API_KEY)
                logger.info("Initialized Google GenAI client successfully.")
            except Exception as e:
                logger.error(f"Failed to initialize Google GenAI client: {e}")
                self._client = None
        else:
            logger.warning("GEMINI_API_KEY is not set. Service will use deterministic fallback engine.")
            self._client = None

    async def parse_order(self, text: str) -> Tuple[OrderResult, str]:
        """
        Parses raw text using Gemini 2.5 Flash with strict Pydantic JSON schema.
        Returns a tuple of (OrderResult, source_name).
        Handles timeouts and errors gracefully with fallback parsing.
        """
        if not text or not text.strip():
            return OrderResult(
                retailer_name=None,
                order_date=datetime.now().strftime("%Y-%m-%d"),
                line_items=[],
                order_notes="Empty or blank message received."
            ), "empty_input"

        if not self._client:
            logger.info("No Gemini client available; using deterministic fallback parser.")
            return self._deterministic_fallback_parse(text), "deterministic_fallback"

        # Execute Gemini API call in a worker thread with strict timeout
        try:
            order_result = await asyncio.wait_for(
                asyncio.to_thread(self._sync_call_gemini, text),
                timeout=settings.GEMINI_TIMEOUT_SECONDS
            )
            return order_result, "gemini"
        except asyncio.TimeoutError:
            logger.error(f"Gemini API timed out after {settings.GEMINI_TIMEOUT_SECONDS}s. Running fallback parser.")
            fallback = self._deterministic_fallback_parse(text)
            fallback.order_notes = (fallback.order_notes or "") + " [Processed via offline fallback due to API timeout]"
            return fallback, "timeout_fallback"
        except Exception as e:
            logger.error(f"Gemini API error during generation: {e}. Running fallback parser.")
            fallback = self._deterministic_fallback_parse(text)
            fallback.order_notes = (fallback.order_notes or "") + " [Processed via offline fallback due to API error]"
            return fallback, "error_fallback"

    def _sync_call_gemini(self, text: str) -> OrderResult:
        """Synchronous wrapper for google-genai generate_content."""
        from google.genai import types

        response = self._client.models.generate_content(
            model=settings.GEMINI_MODEL,
            contents=text,
            config=types.GenerateContentConfig(
                system_instruction=SYSTEM_INSTRUCTION,
                temperature=0.0,
                response_mime_type="application/json",
                response_schema=OrderResult,
            ),
        )
        data = json.loads(response.text)
        return OrderResult(**data)

    def _deterministic_fallback_parse(self, text: str) -> OrderResult:
        """
        High-precision deterministic rule-based parser used when Gemini is unreachable.
        Handles Hinglish tokens, B2B wholesale units, and catalog mapping.
        """
        raw_text = text.strip()
        order_date = datetime.now().strftime("%Y-%m-%d")

        # 1. Extract retailer name if present (e.g., 'From: Sharma Kirana', 'Gupta Electricals')
        retailer_name = None
        retailer_match = re.search(
            r"(?:bill to|shop|store|retailer|from|send to|name)[:\s]+([A-Za-z0-9\s&]+?)(?:\n|,|$)",
            raw_text,
            re.IGNORECASE
        )
        if retailer_match:
            retailer_name = retailer_match.group(1).strip()

        # 2. Extract urgency / notes
        notes_match = re.search(
            r"\b(urgent(?:ly)?|today evening|today morning|jaldi|asap|quick(?:ly)?|by tomorrow)\b",
            raw_text,
            re.IGNORECASE
        )
        notes = f"Urgency noted: {notes_match.group(1)}" if notes_match else None

        # 3. Clean leading conversational greetings and split items
        cleaned = re.sub(
            r"^(?:bhaiya|bhai|hello|namaste|sir|please|plz|send|bhejo|bhej dena|order)[\s,]+",
            "",
            raw_text,
            flags=re.IGNORECASE
        )

        parts = re.split(r",|\baur\b|\band\b|\+|\n", cleaned)
        items = []

        unit_mapping = {
            "peti": "box", "bx": "box", "box": "box", "boxes": "box", "carton": "box",
            "patta": "strip", "patti": "strip", "patte": "strip", "strip": "strip", "strips": "strip",
            "nag": "pieces", "pcs": "pieces", "pc": "pieces", "piece": "pieces", "pieces": "pieces", "nos": "pieces",
            "bori": "bag", "katta": "bag", "kattas": "bag", "bag": "bag", "bags": "bag",
            "balti": "bucket", "bucket": "bucket", "buckets": "bucket",
            "can": "can", "cans": "can", "tin": "can",
            "bottle": "bottle", "bottles": "bottle", "btl": "bottle",
            "tube": "tube", "tubes": "tube",
            "roll": "roll", "rolls": "roll", "bundle": "roll",
            "kg": "kilograms", "kgs": "kilograms"
        }

        for raw_part in parts:
            part = raw_part.strip().strip(".")
            if not part:
                continue

            # Strip trailing delivery markers from product segment
            part_cleaned = re.sub(
                r"\b(?:today|evening|morning|quickly|urgent|urgently|jaldi|asap|chahiye|bhej do)\b",
                "",
                part,
                flags=re.IGNORECASE
            ).strip()

            match = re.search(r"^(\d+(?:\.\d+)?)\s*([a-zA-Z]+)?\s*(.*)$", part_cleaned)
            if not match:
                continue

            qty = float(match.group(1))
            candidate_unit = (match.group(2) or "").lower()
            rest = (match.group(3) or "").strip()

            if candidate_unit in unit_mapping:
                unit = unit_mapping[candidate_unit]
                search_term = rest
            else:
                unit = None
                search_term = f"{candidate_unit} {rest}".strip()

            if not search_term:
                continue

            # Find best catalog match
            matched_cat = None
            term_lower = search_term.lower()
            for cat in MASTER_CATALOG:
                for alias in cat["aliases"]:
                    if alias in term_lower or term_lower in alias:
                        matched_cat = cat
                        break
                if matched_cat:
                    break

            if matched_cat:
                items.append(LineItem(
                    original_text=part,
                    normalized_name=matched_cat["standard_name"],
                    item_code=matched_cat["item_code"],
                    quantity=qty,
                    unit_of_measure=unit if unit else matched_cat["unit_of_measure"],
                    matched_in_catalog=True
                ))
            else:
                items.append(LineItem(
                    original_text=part,
                    normalized_name=search_term.title(),
                    item_code=None,
                    quantity=qty,
                    unit_of_measure=unit if unit else "pieces",
                    matched_in_catalog=False
                ))

        return OrderResult(
            retailer_name=retailer_name,
            order_date=order_date,
            line_items=items,
            order_notes=notes
        )

# Global service singleton
gemini_service = GeminiService()
