package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.ParsedOrderItem

@Composable
fun EditItemDialog(
    item: ParsedOrderItem,
    catalog: List<CatalogItem>,
    onSave: (ParsedOrderItem) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf(item.quantity.toString().removeSuffix(".0")) }
    var unitType by remember { mutableStateOf(item.unitType) }
    var selectedCatalogItem by remember {
        mutableStateOf(catalog.find { it.itemCode == item.itemCode })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Order Item", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Raw Request: \"${item.requestedRawName}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_quantity_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = unitType,
                        onValueChange = { unitType = it },
                        label = { Text("Unit") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_unit_input")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Catalog Match Link:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(catalog) { cat ->
                        val isSelected = selectedCatalogItem?.itemCode == cat.itemCode
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedCatalogItem = if (isSelected) null else cat
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${cat.itemCode} - ${cat.standardName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${cat.brand} • ₹${cat.priceInr} / ${cat.unitType}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedQty = quantityText.toDoubleOrNull() ?: item.quantity
                    val cat = selectedCatalogItem
                    val updated = if (cat != null) {
                        item.copy(
                            itemCode = cat.itemCode,
                            standardName = cat.standardName,
                            brand = cat.brand,
                            quantity = parsedQty,
                            unitType = unitType,
                            unitPriceInr = cat.priceInr,
                            totalPriceInr = Math.round(cat.priceInr * parsedQty * 100.0) / 100.0,
                            matchedInCatalog = true,
                            confidence = 1.0,
                            normalizationNote = "Manual catalog link: ${cat.itemCode}"
                        )
                    } else {
                        item.copy(
                            itemCode = "",
                            quantity = parsedQty,
                            unitType = unitType,
                            totalPriceInr = 0.0,
                            matchedInCatalog = false,
                            normalizationNote = "Unmatched item"
                        )
                    }
                    onSave(updated)
                },
                modifier = Modifier.testTag("save_edit_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
