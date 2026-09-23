package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class OrderSummary(
    @Json(name = "total_items_requested")
    val totalItemsRequested: Int,

    @Json(name = "catalog_matched_count")
    val catalogMatchedCount: Int,

    @Json(name = "unmatched_count")
    val unmatchedCount: Int,

    @Json(name = "subtotal_inr")
    val subtotalInr: Double,

    @Json(name = "tax_gst_inr")
    val taxGstInr: Double,

    @Json(name = "grand_total_inr")
    val grandTotalInr: Double
)

@JsonClass(generateAdapter = true)
data class ParsedOrder(
    @Json(name = "order_id")
    val orderId: String,

    @Json(name = "retailer_name")
    val retailerName: String = "Retail Shop",

    @Json(name = "retailer_phone")
    val retailerPhone: String = "",

    @Json(name = "retailer_address")
    val retailerAddress: String = "",

    @Json(name = "order_date")
    val orderDate: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),

    @Json(name = "input_format")
    val inputFormat: String = "text_message", // "text_message", "voice_transcript", "handwritten_slip", "manual_entry"

    @Json(name = "raw_input")
    val rawInput: String,

    @Json(name = "items")
    val items: List<ParsedOrderItem>,

    @Json(name = "summary")
    val summary: OrderSummary,

    @Json(name = "status")
    val status: String = "PARSED" // "PARSED", "REVIEWED", "INVOICED", "DISPATCHED"
) {
    companion object {
        fun calculateSummary(items: List<ParsedOrderItem>): OrderSummary {
            val matchedCount = items.count { it.matchedInCatalog }
            val unmatchedCount = items.size - matchedCount
            val subtotal = items.sumOf { it.totalPriceInr }
            val gst = Math.round(subtotal * 0.12 * 100.0) / 100.0 // 12% standard wholesale GST
            val grandTotal = Math.round((subtotal + gst) * 100.0) / 100.0
            return OrderSummary(
                totalItemsRequested = items.size,
                catalogMatchedCount = matchedCount,
                unmatchedCount = unmatchedCount,
                subtotalInr = Math.round(subtotal * 100.0) / 100.0,
                taxGstInr = gst,
                grandTotalInr = grandTotal
            )
        }
    }
}
