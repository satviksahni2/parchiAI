package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SamplePresets
import com.example.ui.OrderParseUiState

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OrderInputScreen(
    state: OrderParseUiState,
    onInputTextChanged: (String) -> Unit,
    onFormatSelected: (String) -> Unit,
    onLoadPreset: (String, String) -> Unit,
    onImagePicked: (Uri?) -> Unit,
    onLoadSampleSlip: () -> Unit,
    onOpenWebhookModal: () -> Unit,
    onClearInput: () -> Unit,
    onParseOrder: () -> Unit,
    onNavigateToCatalog: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isVoicePlaying by remember { mutableStateOf(false) }

    // Zero-permission photo picker for handwritten order slips
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        onImagePicked(uri)
    }

    val formatTabs = listOf(
        Triple("text_message", "WhatsApp / Text", Icons.Default.EditNote),
        Triple("voice_transcript", "Voice Note", Icons.Default.RecordVoiceOver),
        Triple("handwritten_slip", "Order Slip Photo", Icons.Default.Image)
    )

    val currentTabIndex = formatTabs.indexOfFirst { it.first == state.selectedFormat }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("order_input_screen")
    ) {
        // Welcome Banner & Webhook Simulator shortcut
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto-PO / DistriParse",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenWebhookModal,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Webhook, contentDescription = "Webhook", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Webhook API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Automated B2B intake for wholesale distributors. Ingests messy WhatsApp messages, voice notes, and handwritten order slips. Normalizes SKUs and formats Tally/Busy ERP imports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Format Tabs
        Text(
            text = "Select Retailer Input Channel:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        PrimaryTabRow(
            selectedTabIndex = currentTabIndex,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        ) {
            formatTabs.forEachIndexed { index, (formatKey, label, icon) ->
                Tab(
                    selected = currentTabIndex == index,
                    onClick = { onFormatSelected(formatKey) },
                    text = {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (currentTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(17.dp)) },
                    modifier = Modifier.testTag("tab_$formatKey")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Media-Specific Helpers for Voice and Slip
        if (state.selectedFormat == "handwritten_slip") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = "Slip", tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Handwritten Slip Photo Ingestion",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }

                        if (state.selectedImageUri != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "IMAGE LOADED",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Securely downloads image (.jpg/.png) from WhatsApp media URL and routes to Gemini Vision for OCR & line item extraction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF78350F),
                        fontSize = 11.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Slip Image", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onLoadSampleSlip,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(38.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "Sample", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load Sample Slip", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        } else if (state.selectedFormat == "voice_transcript") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF4338CA), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WhatsApp Voice Note Audio (.ogg / .mp4)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3730A3)
                            )
                        }

                        IconButton(
                            onClick = { isVoicePlaying = !isVoicePlaying },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isVoicePlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Play/Stop",
                                tint = Color(0xFF4338CA)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Simulated Audio Waveform
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Wave", tint = Color(0xFF4338CA), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isVoicePlaying) "Playing: voice_note_0922.ogg (0:18 / 0:34)..." else "Audio Transcript Ready: Guptaji Voice Order (0:34)",
                            fontSize = 11.5.sp,
                            color = Color(0xFF1E1B4B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Quick Test Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wholesale Presets (All Industries):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Catalog (${state.catalogProducts.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onNavigateToCatalog() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SamplePresets.ALL_PRESETS.forEach { preset ->
                FilterChip(
                    selected = state.inputText.trim() == preset.text.trim(),
                    onClick = { onLoadPreset(preset.text, preset.inputFormat) },
                    label = {
                        Column {
                            Text(
                                text = preset.title,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = preset.category,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.testTag("preset_${preset.title.replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Raw input box header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Order Request Raw Text / Transcript:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val pasted = clip.getItemAt(0).text?.toString() ?: ""
                            if (pasted.isNotBlank()) {
                                onInputTextChanged(pasted)
                            }
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (state.inputText.isNotBlank()) {
                    IconButton(
                        onClick = onClearInput,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Input Field
        OutlinedTextField(
            value = state.inputText,
            onValueChange = onInputTextChanged,
            placeholder = {
                Text(
                    text = "e.g. 10 peti maggi, 4 bag surf big, 5 strip paracetamol 500, 2 bottle cough syrup, 12 pcs philips led bulb. Shop: Sharma Kirana Store (9876543210)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .testTag("order_input_textfield"),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Default,
                lineHeight = 20.sp
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Main Action Button
        Button(
            onClick = onParseOrder,
            enabled = !state.isParsing && (state.inputText.isNotBlank() || state.selectedImageUri != null),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("parse_order_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (state.isParsing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Normalizing & Cross-Referencing Catalog...", fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Parse",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Extract & Normalize Order",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Catalog quick summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Master Catalog Coverage (${state.catalogProducts.size} active SKUs):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• Pharmaceuticals", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                    Text("• FMCG / Detergents", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                    Text("• Electricals", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• Building Materials", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                    Text("• Auto Parts & Spares", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                    Text("• Personal Care", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                }
            }
        }
    }
}
