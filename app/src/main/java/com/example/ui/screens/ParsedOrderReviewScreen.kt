package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ParsedOrder
import com.example.data.model.ParsedOrderItem
import com.example.ui.OrderParseUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParsedOrderReviewScreen(
    state: OrderParseUiState,
    onFilterChange: (String) -> Unit,
    onOpenJsonModal: () -> Unit,
    onOpenInvoiceModal: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onNewOrder: () -> Unit
) {
    val order = state.parsedOrder ?: return

    val filteredItems = when (state.itemFilter) {
        "MATCHED" -> order.items.filter { it.matchedInCatalog }
        "UNMATCHED" -> order.items.filter { !it.matchedInCatalog }
        else -> order.items
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("parsed_order_review_screen")
    ) {
        // Retailer Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_header_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = order.orderId,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (order.inputFormat) {
                                    "voice_transcript" -> Color(0xFFEEF2FF)
                                    "handwritten_slip" -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFF1F5F9)
                                }
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = when (order.inputFormat) {
                                    "voice_transcript" -> "Voice Note"
                                    "handwritten_slip" -> "Order Slip"
                                    else -> "WhatsApp"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (order.inputFormat) {
                                    "voice_transcript" -> Color(0xFF4338CA)
                                    "handwritten_slip" -> Color(0xFFB45309)
                                    else -> Color(0xFF334155)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = "Shop",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = order.retailerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (order.retailerPhone.isNotBlank() || order.retailerAddress.isNotBlank()) {
                                Text(
                                    text = listOfNotNull(
                                        order.retailerPhone.takeIf { it.isNotBlank() },
                                        order.retailerAddress.takeIf { it.isNotBlank() }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Summary Metric Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Matched Items
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF0FDF4))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Matched",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Matched",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF166534),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${order.summary.catalogMatchedCount} / ${order.summary.totalItemsRequested}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D)
                        )
                    }
                }

                // Flagged Unmatched Items
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (order.summary.unmatchedCount > 0) Color(0xFFFFFBEB) else Color(0xFFF8FAFC)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Unmatched",
                                tint = if (order.summary.unmatchedCount > 0) Color(0xFFD97706) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Unmatched",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (order.summary.unmatchedCount > 0) Color(0xFF92400E) else Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${order.summary.unmatchedCount}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (order.summary.unmatchedCount > 0) Color(0xFF78350F) else Color(0xFF334155)
                        )
                    }
                }

                // Order Grand Total INR
                ElevatedCard(
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Estimated Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${order.summary.grandTotalInr}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Buttons: ERP & Tally Export, Tax Invoice, WhatsApp Confirmation
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenJsonModal,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("export_json_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.DataObject, contentDescription = "ERP", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ERP & Tally Export", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenInvoiceModal,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(46.dp)
                            .testTag("generate_invoice_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = "Invoice", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tax Invoice", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val msg = com.example.data.ErpExportHelper.generateWhatsAppConfirmation(order)
                        com.example.data.ErpExportHelper.sendWhatsAppMessage(
                            context = context,
                            phoneNumber = order.retailerPhone,
                            messageText = msg
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("reply_whatsapp_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "WhatsApp", modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send WhatsApp Confirmation to Retailer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Filter Chips Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Normalized Line Items (${filteredItems.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = state.itemFilter == "ALL",
                        onClick = { onFilterChange("ALL") },
                        label = { Text("All (${order.items.size})", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_all")
                    )
                    FilterChip(
                        selected = state.itemFilter == "MATCHED",
                        onClick = { onFilterChange("MATCHED") },
                        label = { Text("Matched (${order.summary.catalogMatchedCount})", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_matched")
                    )
                    if (order.summary.unmatchedCount > 0) {
                        FilterChip(
                            selected = state.itemFilter == "UNMATCHED",
                            onClick = { onFilterChange("UNMATCHED") },
                            label = { Text("Flagged (${order.summary.unmatchedCount})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEF3C7),
                                selectedLabelColor = Color(0xFF92400E)
                            ),
                            modifier = Modifier.testTag("filter_unmatched")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // List of Normalized Order Items
        itemsIndexed(filteredItems) { index, item ->
            val originalIndex = order.items.indexOf(item)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .testTag("order_item_card_$index"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (item.matchedInCatalog) Color.White else Color(0xFFFFFBEB)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Top row: Item Code & Match Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.matchedInCatalog) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Matched",
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "CODE: ${item.itemCode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                if (item.brand != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = item.brand,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF475569),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // Unmatched flag badge
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Unmatched",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "UNMATCHED IN CATALOG",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Edit / Delete actions
                        Row {
                            IconButton(
                                onClick = { onEditItem(originalIndex) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteItem(originalIndex) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Standard Product Name
                    Text(
                        text = item.standardName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Requested raw string
                    Text(
                        text = "Raw request: \"${item.requestedRawName}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Price and quantity breakdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (item.matchedInCatalog) Color(0xFFF8FAFC) else Color(0xFFFEF9C3),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Qty: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${item.quantity} ${item.unitType}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.unitPriceInr > 0) {
                                Text(
                                    text = "  (@ ₹${item.unitPriceInr})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = if (item.totalPriceInr > 0) "₹${item.totalPriceInr}" else "Quote Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (item.totalPriceInr > 0) MaterialTheme.colorScheme.primary else Color(0xFFB45309)
                        )
                    }

                    // Normalization note
                    if (!item.normalizationNote.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ℹ ${item.normalizationNote}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.matchedInCatalog) Color(0xFF0369A1) else Color(0xFF92400E),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Bottom new order action
        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onNewOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("parse_another_order_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Order")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Parse Another Order Request")
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
