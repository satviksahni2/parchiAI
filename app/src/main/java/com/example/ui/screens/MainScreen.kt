package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.OrderParseViewModel
import com.example.ui.components.AppTopBar
import com.example.ui.components.EditItemDialog
import com.example.ui.components.ErpExportModal
import com.example.ui.components.WebhookSimulatorModal

@Composable
fun MainScreen(viewModel: OrderParseViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedBottomNav by remember { mutableStateOf("intake") }

    // Display toast when message arrives
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissToast()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                isAiAvailable = state.isGeminiAvailable,
                useAi = state.useAiEngineIfAvailable
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedBottomNav == "intake",
                    onClick = { selectedBottomNav = "intake" },
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Intake") },
                    label = { Text("Order Intake", fontSize = 11.sp, fontWeight = if (selectedBottomNav == "intake") FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_intake")
                )
                NavigationBarItem(
                    selected = selectedBottomNav == "catalog",
                    onClick = { selectedBottomNav = "catalog" },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Catalog") },
                    label = { Text("Catalog", fontSize = 11.sp, fontWeight = if (selectedBottomNav == "catalog") FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_catalog")
                )
                NavigationBarItem(
                    selected = selectedBottomNav == "history",
                    onClick = { selectedBottomNav = "history" },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("Orders (${state.savedOrders.size})", fontSize = 11.sp, fontWeight = if (selectedBottomNav == "history") FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    selected = selectedBottomNav == "distributor",
                    onClick = { selectedBottomNav = "distributor" },
                    icon = { Icon(Icons.Default.Business, contentDescription = "Distributor") },
                    label = { Text("Distributor", fontSize = 11.sp, fontWeight = if (selectedBottomNav == "distributor") FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_distributor")
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedBottomNav) {
                "intake" -> {
                    if (state.parsedOrder != null) {
                        ParsedOrderReviewScreen(
                            state = state,
                            onFilterChange = { viewModel.setItemFilter(it) },
                            onOpenJsonModal = { viewModel.openJsonModal() },
                            onOpenInvoiceModal = { viewModel.openInvoiceModal() },
                            onEditItem = { viewModel.startEditingItem(it) },
                            onDeleteItem = { viewModel.deleteItem(it) },
                            onNewOrder = { viewModel.onInputTextChanged("") }
                        )
                    } else {
                        OrderInputScreen(
                            state = state,
                            onInputTextChanged = { viewModel.onInputTextChanged(it) },
                            onFormatSelected = { viewModel.onFormatSelected(it) },
                            onLoadPreset = { text, fmt -> viewModel.loadPreset(text, fmt) },
                            onImagePicked = { uri -> viewModel.onImagePicked(uri) },
                            onLoadSampleSlip = { viewModel.loadSampleOrderSlip() },
                            onOpenWebhookModal = { viewModel.openWebhookModal() },
                            onClearInput = { viewModel.clearInput() },
                            onParseOrder = { viewModel.parseCurrentInput() },
                            onNavigateToCatalog = { selectedBottomNav = "catalog" }
                        )
                    }
                }
                "catalog" -> {
                    CatalogScreen(catalogProducts = state.catalogProducts)
                }
                "history" -> {
                    OrderHistoryScreen(
                        orders = state.savedOrders,
                        onSelectOrder = { order ->
                            viewModel.viewPastOrder(order)
                            selectedBottomNav = "intake"
                        },
                        onDeleteOrder = { viewModel.deletePastOrder(it) }
                    )
                }
                "distributor" -> {
                    DistributorSettingsScreen(
                        state = state,
                        onToggleUseAi = { viewModel.toggleUseAi(it) },
                        onSelectAiModel = { viewModel.selectAiModel(it) },
                        onOpenWebhookModal = { viewModel.openWebhookModal() }
                    )
                }
            }
        }
    }

    // Comprehensive ERP Export Modal (JSON, Tally XML, Excel CSV, WhatsApp)
    if (state.showJsonModal && state.parsedOrder != null) {
        ErpExportModal(
            order = state.parsedOrder!!,
            jsonString = state.jsonContent,
            onDismiss = { viewModel.closeJsonModal() }
        )
    }

    // Tax Invoice Modal Sheet
    if (state.showInvoiceModal && state.parsedOrder != null) {
        InvoiceViewScreen(
            order = state.parsedOrder!!,
            onBack = { viewModel.closeInvoiceModal() }
        )
    }

    // Webhook Simulator Modal
    if (state.showWebhookModal) {
        WebhookSimulatorModal(
            onSimulatePayload = { bodyText ->
                viewModel.simulateIncomingWebhook(bodyText)
                selectedBottomNav = "intake"
            },
            onDismiss = { viewModel.closeWebhookModal() }
        )
    }

    // Edit Item Dialog
    if (state.editingItemIndex != null && state.parsedOrder != null) {
        val idx = state.editingItemIndex!!
        val itm = state.parsedOrder!!.items.getOrNull(idx)
        if (itm != null) {
            EditItemDialog(
                item = itm,
                catalog = state.catalogProducts,
                onSave = { updated -> viewModel.updateItem(idx, updated) },
                onDelete = { viewModel.deleteItem(idx) },
                onDismiss = { viewModel.cancelEditingItem() }
            )
        }
    }
}
