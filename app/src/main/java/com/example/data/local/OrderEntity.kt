package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parsed_orders")
data class OrderEntity(
    @PrimaryKey
    val orderId: String,
    val retailerName: String,
    val retailerPhone: String,
    val retailerAddress: String,
    val orderDate: String,
    val inputFormat: String,
    val rawInput: String,
    val itemsJson: String,
    val totalItems: Int,
    val matchedCount: Int,
    val unmatchedCount: Int,
    val subtotalInr: Double,
    val grandTotalInr: Double,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "catalog_products")
data class CatalogEntity(
    @PrimaryKey
    val itemCode: String,
    val standardName: String,
    val brand: String,
    val unitType: String,
    val priceInr: Double,
    val category: String,
    val aliasesCsv: String
)
