"""
Unit tests for WhatsApp Cloud API Webhook endpoints and error handling
"""
import pytest
from app.config import settings

def test_health_check(client):
    """Verify health endpoint responds with 200 and valid schema."""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "gemini_configured" in data
    assert "whatsapp_configured" in data

def test_webhook_verification_success(client):
    """Test successful Meta Webhook handshake with correct verification token."""
    challenge = "1158201444"
    params = {
        "hub.mode": "subscribe",
        "hub.challenge": challenge,
        "hub.verify_token": settings.VERIFY_TOKEN
    }
    response = client.get("/webhook", params=params)
    assert response.status_code == 200
    # Must return challenge
    assert str(response.text).strip() == challenge

def test_webhook_verification_invalid_token(client):
    """Test verification rejection when token does not match."""
    params = {
        "hub.mode": "subscribe",
        "hub.challenge": "12345",
        "hub.verify_token": "wrong_invalid_token_999"
    }
    response = client.get("/webhook", params=params)
    assert response.status_code == 403
    assert "Verification failed" in response.json()["detail"]

def test_webhook_verification_missing_challenge(client):
    """Test verification rejection when hub.challenge is omitted."""
    params = {
        "hub.mode": "subscribe",
        "hub.verify_token": settings.VERIFY_TOKEN
    }
    response = client.get("/webhook", params=params)
    assert response.status_code == 400

def test_webhook_valid_incoming_message(client, sample_valid_whatsapp_payload):
    """Test standard incoming text message processing."""
    response = client.post("/webhook", json=sample_valid_whatsapp_payload)
    assert response.status_code == 200
    assert response.json() == {"status": "EVENT_RECEIVED"}

def test_webhook_malformed_non_json_body(client):
    """Verify that completely broken non-JSON body returns 400 Bad Request."""
    response = client.post(
        "/webhook",
        content="This is not JSON content at all!",
        headers={"Content-Type": "application/json"}
    )
    assert response.status_code == 400
    assert "Malformed JSON" in response.json()["detail"]

def test_webhook_empty_payload(client):
    """Verify that empty JSON object does not cause 500 server crash."""
    response = client.post("/webhook", json={})
    assert response.status_code == 200
    # Ignored because object != 'whatsapp_business_account'
    assert response.json()["status"] == "IGNORED"

def test_webhook_wrong_object_type(client):
    """Verify that unexpected webhook events (e.g. instagram or page) are safely ignored."""
    payload = {
        "object": "page",
        "entry": [{"id": "123"}]
    }
    response = client.post("/webhook", json=payload)
    assert response.status_code == 200
    assert response.json()["status"] == "IGNORED"

def test_webhook_missing_messages_status_receipt(client):
    """Verify that delivery receipts (without 'messages' field) are handled without error."""
    payload = {
        "object": "whatsapp_business_account",
        "entry": [
            {
                "id": "123",
                "changes": [
                    {
                        "field": "messages",
                        "value": {
                            "messaging_product": "whatsapp",
                            "statuses": [{"id": "wamid.123", "status": "delivered"}]
                        }
                    }
                ]
            }
        ]
    }
    response = client.post("/webhook", json=payload)
    assert response.status_code == 200
    assert response.json() == {"status": "EVENT_RECEIVED"}

def test_webhook_non_text_message_type(client):
    """Verify that incoming media messages (e.g. location or reaction) are handled gracefully."""
    payload = {
        "object": "whatsapp_business_account",
        "entry": [
            {
                "id": "123",
                "changes": [
                    {
                        "field": "messages",
                        "value": {
                            "messaging_product": "whatsapp",
                            "messages": [
                                {
                                    "from": "919876543210",
                                    "id": "wamid.456",
                                    "type": "location",
                                    "location": {"latitude": 28.6139, "longitude": 77.2090}
                                }
                            ]
                        }
                    }
                ]
            }
        ]
    }
    response = client.post("/webhook", json=payload)
    assert response.status_code == 200
    assert response.json() == {"status": "EVENT_RECEIVED"}

def test_direct_parse_endpoint(client):
    """Verify POST /parse endpoint parses conversational B2B order text."""
    payload = {
        "text": "Bhaiya, send 10 peti maggi, 5 patta paracetamol, and 20 philip led bulb 9 watt today evening quickly."
    }
    response = client.post("/parse", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    order = data["order"]
    assert len(order["line_items"]) == 3

    # Check Maggi
    maggi = next((i for i in order["line_items"] if "maggi" in i["normalized_name"].lower()), None)
    assert maggi is not None
    assert maggi["quantity"] == 10.0
    assert maggi["unit_of_measure"] == "box"
    assert maggi["matched_in_catalog"] is True
    assert maggi["item_code"] == "F002"

    # Check Paracetamol
    pcm = next((i for i in order["line_items"] if "paracetamol" in i["normalized_name"].lower()), None)
    assert pcm is not None
    assert pcm["quantity"] == 5.0
    assert pcm["unit_of_measure"] == "strip"
    assert pcm["matched_in_catalog"] is True
    assert pcm["item_code"] == "P001"

    # Check LED Bulb
    bulb = next((i for i in order["line_items"] if "bulb" in i["normalized_name"].lower()), None)
    assert bulb is not None
    assert bulb["quantity"] == 20.0
    assert bulb["unit_of_measure"] == "pieces"
    assert bulb["matched_in_catalog"] is True
    assert bulb["item_code"] == "E002"
