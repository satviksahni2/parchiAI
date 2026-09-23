"""
Unit tests for Gemini service, Pydantic schemas, and normalization logic
"""
import pytest
import asyncio
from app.models import LineItem, OrderResult
from app.gemini_service import gemini_service
from app.whatsapp_service import whatsapp_service

@pytest.mark.asyncio
async def test_pydantic_schema_strictness():
    """Verify that LineItem and OrderResult enforce field types and defaults."""
    item = LineItem(
        original_text="10 peti maggi",
        normalized_name="Maggi 2-Minute Noodles 70g (Pack of 24)",
        item_code="F002",
        quantity=10.0,
        unit_of_measure="box",
        matched_in_catalog=True
    )
    assert item.quantity == 10.0
    assert item.item_code == "F002"
    assert item.matched_in_catalog is True

    order = OrderResult(
        retailer_name="Sharma Kirana",
        order_date="2026-09-22",
        line_items=[item],
        order_notes="Deliver quickly"
    )
    assert len(order.line_items) == 1
    assert order.retailer_name == "Sharma Kirana"

@pytest.mark.asyncio
async def test_parse_conversational_order_units():
    """Verify normalization of Hinglish units: peti -> box, patta -> strip, bulb -> pieces."""
    text = "Bhaiya, send 10 peti maggi, 5 patta paracetamol, and 20 philip led bulb 9 watt today evening quickly."
    order, source = await gemini_service.parse_order(text)

    assert len(order.line_items) == 3
    # Check units
    units = [i.unit_of_measure for i in order.line_items]
    assert "box" in units
    assert "strip" in units
    assert "pieces" in units

@pytest.mark.asyncio
async def test_uncataloged_item_flagging():
    """Verify that uncataloged products (e.g., '3 kg sugar') are flagged with matched_in_catalog=False."""
    text = "10 peti maggi aur 3 kg sugar bhej do"
    order, source = await gemini_service.parse_order(text)

    sugar_item = next((i for i in order.line_items if "sugar" in i.normalized_name.lower()), None)
    assert sugar_item is not None
    assert sugar_item.matched_in_catalog is False
    assert sugar_item.item_code is None
    assert sugar_item.quantity == 3.0

@pytest.mark.asyncio
async def test_empty_or_whitespace_input():
    """Verify that empty or whitespace messages return clean empty order without crashing."""
    order, source = await gemini_service.parse_order("   ")
    assert len(order.line_items) == 0
    assert order.order_notes is not None
    assert source == "empty_input"

@pytest.mark.asyncio
async def test_whatsapp_message_formatting():
    """Verify formatted confirmation message contains all required emojis, status, and items."""
    item = LineItem(
        original_text="10 peti maggi",
        normalized_name="Maggi 2-Minute Noodles 70g (Pack of 24)",
        item_code="F002",
        quantity=10.0,
        unit_of_measure="box",
        matched_in_catalog=True
    )
    order = OrderResult(
        retailer_name="Gupta Electricals",
        order_date="2026-09-22",
        line_items=[item],
        order_notes="Deliver by 5 PM"
    )
    formatted = whatsapp_service.format_confirmation_message(order)
    assert "Order Received & Logged" in formatted
    assert "Gupta Electricals" in formatted
    assert "Maggi 2-Minute Noodles" in formatted
    assert "[F002]" in formatted
    assert "Deliver by 5 PM" in formatted
