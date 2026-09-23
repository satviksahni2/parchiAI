package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OrderParseUiState

@Composable
fun DistributorSettingsScreen(
    state: OrderParseUiState,
    onToggleUseAi: (Boolean) -> Unit,
    onSelectAiModel: (String) -> Unit = {},
    onOpenWebhookModal: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        // Distributor Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Distributor",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Parchi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "WhatsApp B2B Order & Invoice Middleware",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Fulfillment Hub: Apex Wholesale Distributors Ltd.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("GSTIN: 07AABCA1234F1Z9  |  DL: DL-2024-WHS-9941", style = MaterialTheme.typography.bodySmall)
                Text("ERP Compatibility: Tally Prime, Busy ERP & Universal CSV", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // WhatsApp Cloud API & Webhook Middleware
        Text(
            text = "WhatsApp Cloud API & Webhook (PRD Sec 3.1):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Webhook, contentDescription = "Webhook", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("FastAPI Webhook Pipeline", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Meta Verification: hub.challenge active", fontSize = 11.sp, color = Color(0xFF16A34A))
                        }
                    }

                    Button(
                        onClick = onOpenWebhookModal,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text("Open Simulator", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ingests incoming WhatsApp messages, downloads media (audio/image), and dispatches automated confirmation replies to retailers.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Engine Configuration (PRD Sec 4.1)
        Text(
            text = "AI Parsing Engine Configuration (PRD Sec 4.1):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Use Google Gemini AI",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (state.isGeminiAvailable) "Gemini API connected" else "Offline fallback active (Deterministic Regex)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isGeminiAvailable) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = state.useAiEngineIfAvailable,
                        onCheckedChange = onToggleUseAi
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Active Model Architecture:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                // Primary Model Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.selectedAiModel == "gemini-2.5-flash",
                        onClick = { onSelectAiModel("gemini-2.5-flash") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Gemini 2.5 Flash (Primary)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Optimized for low-latency text & slip image extraction at volume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Fallback Model Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.selectedAiModel == "gemini-3.1-pro-preview",
                        onClick = { onSelectAiModel("gemini-3.1-pro-preview") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Gemini Pro (Fallback Model)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Triggered for highly degraded or messy handwritten images", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Temperature: 0.1 (Strict factual)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("Response: application/json", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phase 1 Scope & Constraints Notice (PRD Sec 5)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Scope", tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Phase 1 (V1.0) Architectural Scope:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Inventory Validation: Handled downstream in Tally/Busy ERP.", fontSize = 11.sp, color = Color(0xFF475569))
                Text("• Payment Processing: Excluded from parser pipeline.", fontSize = 11.sp, color = Color(0xFF475569))
                Text("• Single-Turn Ingestion: Pure factual extraction; no conversational chat.", fontSize = 11.sp, color = Color(0xFF475569))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Normalization Standards Reference
        Text(
            text = "B2B Unit Standardization Reference (PRD Sec 3.2):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                val rules = listOf(
                    Pair("\"peti\" / \"bx\" / \"carton\"", "box"),
                    Pair("\"pcs\" / \"nag\" / \"piece\"", "pieces"),
                    Pair("\"kg\" / \"kilo\"", "kilograms"),
                    Pair("\"bag\" / \"bori\" / \"katta\"", "bag"),
                    Pair("\"bucket\" / \"balti\"", "bucket"),
                    Pair("\"can\" / \"tin\"", "can"),
                    Pair("\"pkt\" / \"packet\"", "packet"),
                    Pair("\"strip\" / \"patti\"", "strip"),
                    Pair("\"btl\" / \"bottle\"", "bottle"),
                    Pair("\"tube\"", "tube"),
                    Pair("\"roll\" / \"bundle\"", "roll")
                )

                rules.forEachIndexed { i, (from, to) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = from, style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Arrow",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = to,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (i < rules.size - 1) {
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
