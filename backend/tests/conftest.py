import os
import sys
import pytest
from fastapi.testclient import TestClient

# Ensure backend root is on sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from main import app
from app.config import settings

@pytest.fixture(scope="session")
def client():
    """Provides a synchronous TestClient for testing HTTP endpoints."""
    with TestClient(app) as c:
        yield c

@pytest.fixture
def sample_valid_whatsapp_payload():
    """Standard valid Meta WhatsApp Cloud API incoming text message payload."""
    return {
        "object": "whatsapp_business_account",
        "entry": [
            {
                "id": "1234567890",
                "changes": [
                    {
                        "field": "messages",
                        "value": {
                            "messaging_product": "whatsapp",
                            "metadata": {
                                "display_phone_number": "+16505551234",
                                "phone_number_id": "100200300400"
                            },
                            "contacts": [
                                {
                                    "profile": {"name": "Sharma Kirana"},
                                    "wa_id": "919876543210"
                                }
                            ],
                            "messages": [
                                {
                                    "from": "919876543210",
                                    "id": "wamid.HBgLMjA2M...",
                                    "timestamp": "1711100000",
                                    "type": "text",
                                    "text": {
                                        "body": "Bhaiya, send 10 peti maggi, 5 patta paracetamol, and 20 philip led bulb 9 watt today evening quickly."
                                    }
                                }
                            ]
                        }
                    }
                ]
            }
        ]
    }
