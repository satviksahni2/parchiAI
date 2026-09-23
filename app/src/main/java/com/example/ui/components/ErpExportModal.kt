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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
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
import com.example.data.model.ParsedOrder

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ErpExportModal(
    order: ParsedOrder,
    jsonString: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: JSON, 1: Tally XML, 2: Excel CSV, 3: WhatsApp

    val tallyXml = remember(order) { ErpExportHelper.generateTallyXml(order) }
    val excelCsv = remember(order) { ErpExportHelper.generateExcelCsv(order) }
    val whatsAppMsg = remember(order) { ErpExportHelper.generateWhatsAppConfirmation(order) }

    val tabs = listOf(
        Pair("JSON Schema", Icons.Default.DataObject),
        Pair("Tally XML", Icons.Default.Description),
        Pair("Excel CSV", Icons.Default.TableChart),
        Pair("WhatsApp Reply", Icons.AutoMirrored.Filled.Chat)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp)
                .testTag("erp_export_modal"),
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
                    Column {
                        Text(
                            text = "ERP Export & Integration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tally Prime • Busy ERP • Excel • WhatsApp Cloud API",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Row
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, (title, icon) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 11.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(icon, contentDescription = title, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_export_$index")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Info banner per tab
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (selectedTab) {
                            1 -> Color(0xFFFFFBEB) // Tally Amber
                            2 -> Color(0xFFF0FDF4) // Excel Green
                            3 -> Color(0xFFECFDF5) // WhatsApp Emerald
                            else -> Color(0xFFEFF6FF) // JSON Blue
                        }
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Info",
                            tint = when (selectedTab) {
                                1 -> Color(0xFFB45309)
                                2 -> Color(0xFF16A34A)
                                3 -> Color(0xFF059669)
                                else -> Color(0xFF2563EB)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (selectedTab) {
                                1 -> "Tally Prime Purchase Order XML with inventory vouchers & ledger lines"
                                2 -> "Comma-separated values formatted for Excel & Busy ERP stock import"
                                3 -> "Automated retailer WhatsApp confirmation message per PRD Sec 2.2"
                                else -> "Strict B2B JSON schema with item_codes, rates & matched_in_catalog flags"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (selectedTab) {
                                1 -> Color(0xFF92400E)
                                2 -> Color(0xFF166534)
                                3 -> Color(0xFF065F46)
                                else -> Color(0xFF1E40AF)
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payload text viewer box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()

                    val currentText = when (selectedTab) {
                        1 -> tallyXml
                        2 -> excelCsv
                        3 -> whatsAppMsg
                        else -> jsonString
                    }

                    val textColor = when (selectedTab) {
                        1 -> Color(0xFFFDE047) // XML Yellow/Amber
                        2 -> Color(0xFF86EFAC) // CSV Green
                        3 -> Color(0xFF6EE7B7) // WhatsApp Mint
                        else -> Color(0xFF38BDF8) // JSON Cyan
                    }

                    Text(
                        text = currentText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        color = textColor,
                        lineHeight = 17.sp,
                        modifier = Modifier
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                            .testTag("export_payload_text")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectedTab == 3) {
                            // WhatsApp direct send
                            Button(
                                onClick = {
                                    ErpExportHelper.sendWhatsAppMessage(
                                        context = context,
                                        phoneNumber = order.retailerPhone,
                                        messageText = whatsAppMsg
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.testTag("send_whatsapp_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send via WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                val label = when (selectedTab) {
                                    1 -> "Tally XML"
                                    2 -> "Excel CSV"
                                    3 -> "WhatsApp Message"
                                    else -> "B2B Order JSON"
                                }
                                val contentToCopy = when (selectedTab) {
                                    1 -> tallyXml
                                    2 -> excelCsv
                                    3 -> whatsAppMsg
                                    else -> jsonString
                                }
                                ErpExportHelper.copyToClipboard(context, label, contentToCopy)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("copy_payload_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                when (selectedTab) {
                                    1 -> "Copy Tally XML"
                                    2 -> "Copy CSV"
                                    3 -> "Copy Message"
                                    else -> "Copy JSON"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
