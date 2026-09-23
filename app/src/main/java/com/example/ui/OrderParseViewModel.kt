package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.JsonHelper
import com.example.data.SamplePresets
import com.example.data.local.AppDatabase
import com.example.data.model.CatalogItem
import com.example.data.model.ParsedOrder
import com.example.data.model.ParsedOrderItem
import com.example.data.repository.OrderRepository
import com.example.parser.GeminiOrderParser
import com.example.parser.OrderParserEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class OrderParseUiState(
    val inputText: String = "",
    val selectedFormat: String = "text_message", // "text_message", "voice_transcript", "handwritten_slip"
    val selectedImageUri: String? = null,
    val selectedImageBase64: String? = null,
    val isParsing: Boolean = false,
    val parsingStatusMessage: String = "",
    val parsedOrder: ParsedOrder? = null,
    val savedOrders: List<ParsedOrder> = emptyList(),
    val catalogProducts: List<CatalogItem> = CatalogItem.DEFAULT_CATALOG,
    val itemFilter: String = "ALL", // "ALL", "MATCHED", "UNMATCHED"
    val showJsonModal: Boolean = false,
    val jsonContent: String = "",
    val showInvoiceModal: Boolean = false,
    val showWebhookModal: Boolean = false,
    val editingItemIndex: Int? = null,
    val searchQuery: String = "",
    val toastMessage: String? = null,
    val isGeminiAvailable: Boolean = false,
    val useAiEngineIfAvailable: Boolean = true,
    val selectedAiModel: String = "gemini-2.5-flash" // "gemini-2.5-flash" or "gemini-3.1-pro-preview"
)

class OrderParseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: OrderRepository
    private val geminiParser = GeminiOrderParser()

    private val _uiState = MutableStateFlow(OrderParseUiState())
    val uiState: StateFlow<OrderParseUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = OrderRepository(database)

        // Pre-fill input text with the first sample preset so user has immediate rich data to test
        val defaultPreset = SamplePresets.ALL_PRESETS.first()
        _uiState.update {
            it.copy(
                inputText = defaultPreset.text,
                selectedFormat = defaultPreset.inputFormat,
                isGeminiAvailable = geminiParser.isApiKeyConfigured()
            )
        }

        viewModelScope.launch {
            repository.initializeCatalogIfNeeded()
        }

        viewModelScope.launch {
            repository.getAllCatalogProducts().collect { list ->
                if (list.isNotEmpty()) {
                    _uiState.update { it.copy(catalogProducts = list) }
                }
            }
        }

        viewModelScope.launch {
            repository.getAllOrders().collect { list ->
                _uiState.update { it.copy(savedOrders = list) }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onFormatSelected(format: String) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun loadPreset(presetText: String, format: String) {
        _uiState.update {
            it.copy(
                inputText = presetText,
                selectedFormat = format,
                selectedImageUri = null,
                selectedImageBase64 = null
            )
        }
    }

    fun clearInput() {
        _uiState.update {
            it.copy(
                inputText = "",
                selectedImageUri = null,
                selectedImageBase64 = null
            )
        }
    }

    fun onImagePicked(uri: Uri?) {
        if (uri == null) return
        val uriStr = uri.toString()
        _uiState.update { it.copy(selectedImageUri = uriStr, selectedFormat = "handwritten_slip") }

        viewModelScope.launch {
            val base64 = withContext(Dispatchers.IO) {
                try {
                    val context = getApplication<Application>()
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    null
                }
            }
            _uiState.update { it.copy(selectedImageBase64 = base64) }
        }
    }

    fun loadSampleOrderSlip() {
        _uiState.update {
            it.copy(
                selectedFormat = "handwritten_slip",
                inputText = """
                    ORDER SLIP - Verma Medical & Kirana
                    - 10 peti maggi noodles
                    - 4 bag surf big
                    - 5 strip paracetamol 500
                    - 2 bottle cough syrup healthplus
                    - 12 pcs philips led bulb 9w
                    - 4 tube fair n lovly 50g
                    - 1 roll havells copper wire 1.5mm
                    Shop: Verma General Store, Main Road
                    Phone: 9811223344
                """.trimIndent(),
                selectedImageUri = "sample_slip_preview"
            )
        }
    }

    fun parseCurrentInput() {
        val text = _uiState.value.inputText
        val imageBase64 = _uiState.value.selectedImageBase64

        if (text.isBlank() && imageBase64 == null) {
            _uiState.update { it.copy(toastMessage = "Please enter text or select an order slip photo") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isParsing = true,
                    parsingStatusMessage = "Extracting line items and cross-referencing catalog..."
                )
            }

            val catalog = _uiState.value.catalogProducts
            val format = _uiState.value.selectedFormat
            val model = _uiState.value.selectedAiModel

            val order: ParsedOrder = if (_uiState.value.useAiEngineIfAvailable && geminiParser.isApiKeyConfigured()) {
                val res = geminiParser.parseWithGemini(
                    rawInput = text,
                    imageBase64 = imageBase64,
                    imageMimeType = "image/jpeg",
                    catalog = catalog,
                    inputFormat = format,
                    selectedModel = model
                )
                res.getOrDefault(OrderParserEngine.parse(text, catalog, format))
            } else {
                // High precision offline deterministic NLP engine
                OrderParserEngine.parse(text, catalog, format)
            }

            val json = JsonHelper.toJson(order)

            _uiState.update {
                it.copy(
                    isParsing = false,
                    parsedOrder = order,
                    jsonContent = json,
                    toastMessage = "Parsed ${order.items.size} items (${order.summary.catalogMatchedCount} matched, ${order.summary.unmatchedCount} flagged)"
                )
            }

            // Auto-save parsed order to Room
            repository.saveOrder(order)
        }
    }

    fun simulateIncomingWebhook(orderText: String) {
        onInputTextChanged(orderText)
        onFormatSelected("text_message")
        parseCurrentInput()
    }

    fun setItemFilter(filter: String) {
        _uiState.update { it.copy(itemFilter = filter) }
    }

    fun openJsonModal() {
        val order = _uiState.value.parsedOrder
        if (order != null) {
            val json = JsonHelper.toJson(order)
            _uiState.update { it.copy(showJsonModal = true, jsonContent = json) }
        }
    }

    fun closeJsonModal() {
        _uiState.update { it.copy(showJsonModal = false) }
    }

    fun openInvoiceModal() {
        _uiState.update { it.copy(showInvoiceModal = true) }
    }

    fun closeInvoiceModal() {
        _uiState.update { it.copy(showInvoiceModal = false) }
    }

    fun openWebhookModal() {
        _uiState.update { it.copy(showWebhookModal = true) }
    }

    fun closeWebhookModal() {
        _uiState.update { it.copy(showWebhookModal = false) }
    }

    fun selectAiModel(model: String) {
        _uiState.update { it.copy(selectedAiModel = model) }
    }

    fun startEditingItem(index: Int) {
        _uiState.update { it.copy(editingItemIndex = index) }
    }

    fun cancelEditingItem() {
        _uiState.update { it.copy(editingItemIndex = null) }
    }

    fun updateItem(index: Int, updatedItem: ParsedOrderItem) {
        val currentOrder = _uiState.value.parsedOrder ?: return
        val currentItems = currentOrder.items.toMutableList()
        if (index in currentItems.indices) {
            currentItems[index] = updatedItem
            val summary = ParsedOrder.calculateSummary(currentItems)
            val updatedOrder = currentOrder.copy(
                items = currentItems,
                summary = summary
            )
            val json = JsonHelper.toJson(updatedOrder)
            _uiState.update {
                it.copy(
                    parsedOrder = updatedOrder,
                    jsonContent = json,
                    editingItemIndex = null,
                    toastMessage = "Updated ${updatedItem.standardName}"
                )
            }
            viewModelScope.launch {
                repository.saveOrder(updatedOrder)
            }
        }
    }

    fun deleteItem(index: Int) {
        val currentOrder = _uiState.value.parsedOrder ?: return
        val currentItems = currentOrder.items.toMutableList()
        if (index in currentItems.indices) {
            val removed = currentItems.removeAt(index)
            val summary = ParsedOrder.calculateSummary(currentItems)
            val updatedOrder = currentOrder.copy(
                items = currentItems,
                summary = summary
            )
            val json = JsonHelper.toJson(updatedOrder)
            _uiState.update {
                it.copy(
                    parsedOrder = updatedOrder,
                    jsonContent = json,
                    toastMessage = "Removed ${removed.standardName}"
                )
            }
            viewModelScope.launch {
                repository.saveOrder(updatedOrder)
            }
        }
    }

    fun viewPastOrder(order: ParsedOrder) {
        val json = JsonHelper.toJson(order)
        _uiState.update {
            it.copy(
                parsedOrder = order,
                jsonContent = json,
                itemFilter = "ALL"
            )
        }
    }

    fun deletePastOrder(orderId: String) {
        viewModelScope.launch {
            repository.deleteOrder(orderId)
            if (_uiState.value.parsedOrder?.orderId == orderId) {
                _uiState.update { it.copy(parsedOrder = null) }
            }
            _uiState.update { it.copy(toastMessage = "Order $orderId deleted") }
        }
    }

    fun toggleUseAi(useAi: Boolean) {
        _uiState.update { it.copy(useAiEngineIfAvailable = useAi) }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
