package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ErpExportHelper

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WebhookSimulatorModal(
    onSimulatePayload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Webhook Simulator, 1: FastAPI Python Code, 2: Meta Hub Challenge

    val sampleWhatsAppPayload = """
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "id": "WHATSAPP_BUSINESS_ACCOUNT_ID",
      "changes": [
        {
          "value": {
            "messaging_product": "whatsapp",
            "metadata": {
              "display_phone_number": "+919876543210",
              "phone_number_id": "349182746190284"
            },
            "contacts": [
              {
                "profile": { "name": "Sharma Kirana Store" },
                "wa_id": "919876543210"
              }
            ],
            "messages": [
              {
                "from": "919876543210",
                "id": "wamid.HBgLMjA2OTI0NTY3OAUCABEYEjdBMEU0NEM2MTM1RDNBQUFCAA==",
                "timestamp": "1727038800",
                "type": "text",
                "text": {
                  "body": "Bhaiya urgent order: 10 peti maggi, 4 bag surf big, 5 strip paracetamol 500, 2 bottle cough syrup, 12 pcs philips led bulb, 1 roll copper wire 1.5mm. Shop: Sharma Kirana Store (9876543210)"
                }
              }
            ]
          },
          "field": "messages"
        }
      ]
    }
  ]
}
    """.trimIndent()

    val fastapiPythonCode = """
# FastAPI Webhook Middleware for WhatsApp Cloud API & Gemini 2.5 Flash
# Internal Codename: Auto-PO / DistriParse
from fastapi import FastAPI, Request, Response, HTTPException, Query
import httpx
import os

app = FastAPI(title="WhatsApp B2B Order & Invoice Parser")

VERIFY_TOKEN = os.getenv("META_WEBHOOK_VERIFY_TOKEN", "distriparse_secret_token_2026")
WHATSAPP_TOKEN = os.getenv("WHATSAPP_CLOUD_API_TOKEN")
PHONE_NUMBER_ID = os.getenv("WHATSAPP_PHONE_NUMBER_ID")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

# 1. Meta Webhook Verification (PRD Sec 3.1: hub.challenge)
@app.get("/webhook")
async def verify_webhook(
    hub_mode: str = Query(None, alias="hub.mode"),
    hub_challenge: str = Query(None, alias="hub.challenge"),
    hub_verify_token: str = Query(None, alias="hub.verify_token")
):
    if hub_mode == "subscribe" and hub_verify_token == VERIFY_TOKEN:
        return Response(content=hub_challenge, media_type="text/plain")
    raise HTTPException(status_code=403, detail="Verification token mismatch")

# 2. Ingestion & AI Routing (PRD Sec 2.2 & 3.1)
@app.post("/webhook")
async def handle_whatsapp_message(request: Request):
    payload = await request.json()
    entry = payload.get("entry", [{}])[0]
    changes = entry.get("changes", [{}])[0]
    value = changes.get("value", {})
    messages = value.get("messages", [])
    
    if not messages:
        return {"status": "NO_MESSAGES"}
        
    msg = messages[0]
    sender_phone = msg.get("from")
    msg_type = msg.get("type")
    
    # Handle text, voice note, or handwritten slip photo
    if msg_type == "text":
        raw_order_text = msg["text"]["body"]
        parsed_order = await call_gemini_parser(raw_order_text)
    elif msg_type == "image":
        # Download media using WhatsApp media URL & bearer token
        image_bytes = await download_whatsapp_media(msg["image"]["id"])
        parsed_order = await call_gemini_vision_parser(image_bytes)
        
    # Generate Tally XML & push to ERP (PRD Sec 3.3)
    tally_xml = convert_to_tally_xml(parsed_order)
    await push_to_tally_erp(tally_xml)
    
    # Send confirmation WhatsApp message back to retailer (PRD Sec 2.2)
    confirmation_msg = f"✅ Order received and logged. Total items: {len(parsed_order['items'])}."
    await send_whatsapp_reply(sender_phone, confirmation_msg)
    
    return {"status": "SUCCESS", "order_id": parsed_order.get("order_id")}
    """.trimIndent()

    val hubChallengeResponse = """
HTTP/1.1 200 OK
Content-Type: text/plain; charset=utf-8

1158201444

[Verification Success]
• Meta Mode: subscribe
• Token: distriparse_secret_token_2026
• Handshake verified with WhatsApp Cloud API Graph Webhook
    """.trimIndent()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp)
                .testTag("webhook_simulator_modal"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Webhook, contentDescription = "Webhook", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WhatsApp Cloud API & Webhook",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "FastAPI Middleware & Meta Verification",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Webhook Tester", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Tester", modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_webhook_tester")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("FastAPI Code", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Code, contentDescription = "Code", modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_fastapi_code")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("hub.challenge", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Security, contentDescription = "Security", modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_hub_challenge")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active payload preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()

                    val textToShow = when (selectedTab) {
                        1 -> fastapiPythonCode
                        2 -> hubChallengeResponse
                        else -> sampleWhatsAppPayload
                    }

                    val textColor = when (selectedTab) {
                        1 -> Color(0xFF93C5FD)
                        2 -> Color(0xFF86EFAC)
                        else -> Color(0xFFFDE047)
                    }

                    Text(
                        text = textToShow,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = textColor,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                            .testTag("webhook_code_view")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val label = when (selectedTab) {
                                    1 -> "FastAPI Python Source"
                                    2 -> "hub.challenge Response"
                                    else -> "WhatsApp Cloud API Payload"
                                }
                                val content = when (selectedTab) {
                                    1 -> fastapiPythonCode
                                    2 -> hubChallengeResponse
                                    else -> sampleWhatsAppPayload
                                }
                                ErpExportHelper.copyToClipboard(context, label, content)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }

                        Button(
                            onClick = {
                                val extractedBody = "10 peti maggi, 4 bag surf big, 5 strip paracetamol 500, 2 bottle cough syrup, 12 pcs philips led bulb, 1 roll copper wire 1.5mm. Shop: Sharma Kirana Store (9876543210)"
                                onSimulatePayload(extractedBody)
                                Toast.makeText(context, "Simulated incoming WhatsApp Cloud API order!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("trigger_webhook_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Simulate", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Webhook Pipeline", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
