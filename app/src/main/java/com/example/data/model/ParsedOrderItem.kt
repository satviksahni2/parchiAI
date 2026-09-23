package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ParsedOrderItem(
    @Json(name = "item_code")
    val itemCode: String = "",

    @Json(name = "standard_name")
    val standardName: String,

    @Json(name = "brand")
    val brand: String? = null,

    @Json(name = "requested_raw_name")
    val requestedRawName: String,

    @Json(name = "quantity")
    val quantity: Double,

    @Json(name = "unit_type")
    val unitType: String,

    @Json(name = "unit_price_inr")
    val unitPriceInr: Double = 0.0,

    @Json(name = "total_price_inr")
    val totalPriceInr: Double = 0.0,

    @Json(name = "matched_in_catalog")
    val matchedInCatalog: Boolean = false,

    @Json(name = "confidence")
    val confidence: Double = 0.0,

    @Json(name = "normalization_note")
    val normalizationNote: String? = null
)
