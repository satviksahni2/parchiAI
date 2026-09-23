package com.example.data

import com.example.data.model.OrderSummary
import com.example.data.model.ParsedOrder
import com.example.data.model.ParsedOrderItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONArray
import org.json.JSONObject

object JsonHelper {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val orderAdapter = moshi.adapter(ParsedOrder::class.java).indent("  ")
    private val itemsListAdapter = moshi.adapter<List<ParsedOrderItem>>(
        Types.newParameterizedType(List::class.java, ParsedOrderItem::class.java)
    )

    fun toJson(order: ParsedOrder): String {
        return try {
            orderAdapter.toJson(order)
        } catch (e: Exception) {
            fallbackManualJson(order)
        }
    }

    fun serializeItems(items: List<ParsedOrderItem>): String {
        return try {
            itemsListAdapter.toJson(items)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun deserializeItems(json: String): List<ParsedOrderItem> {
        return try {
            itemsListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun fromJson(json: String): ParsedOrder? {
        return try {
            orderAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fallback manual JSON generator matching the exact user instructions schema
     */
    fun fallbackManualJson(order: ParsedOrder): String {
        val root = JSONObject()
        root.put("order_id", order.orderId)
        root.put("retailer_name", order.retailerName)
        root.put("retailer_phone", order.retailerPhone)
        root.put("retailer_address", order.retailerAddress)
        root.put("order_date", order.orderDate)
        root.put("input_format", order.inputFormat)
        root.put("raw_input", order.rawInput)
        root.put("status", order.status)

        val itemsArr = JSONArray()
        for (item in order.items) {
            val obj = JSONObject()
            obj.put("item_code", item.itemCode)
            obj.put("standard_name", item.standardName)
            if (item.brand != null) {
                obj.put("brand", item.brand)
            } else {
                obj.put("brand", JSONObject.NULL)
            }
            obj.put("requested_raw_name", item.requestedRawName)
            obj.put("quantity", item.quantity)
            obj.put("unit_type", item.unitType)
            obj.put("unit_price_inr", item.unitPriceInr)
            obj.put("total_price_inr", item.totalPriceInr)
            obj.put("matched_in_catalog", item.matchedInCatalog)
            obj.put("confidence", item.confidence)
            if (item.normalizationNote != null) {
                obj.put("normalization_note", item.normalizationNote)
            }
            itemsArr.put(obj)
        }
        root.put("items", itemsArr)

        val summaryObj = JSONObject()
        summaryObj.put("total_items_requested", order.summary.totalItemsRequested)
        summaryObj.put("catalog_matched_count", order.summary.catalogMatchedCount)
        summaryObj.put("unmatched_count", order.summary.unmatchedCount)
        summaryObj.put("subtotal_inr", order.summary.subtotalInr)
        summaryObj.put("tax_gst_inr", order.summary.taxGstInr)
        summaryObj.put("grand_total_inr", order.summary.grandTotalInr)
        root.put("summary", summaryObj)

        return root.toString(2)
    }
}
