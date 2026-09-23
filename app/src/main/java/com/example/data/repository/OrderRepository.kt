package com.example.data.repository

import com.example.data.JsonHelper
import com.example.data.local.AppDatabase
import com.example.data.local.CatalogEntity
import com.example.data.local.OrderEntity
import com.example.data.model.CatalogItem
import com.example.data.model.OrderSummary
import com.example.data.model.ParsedOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OrderRepository(private val database: AppDatabase) {
    private val orderDao = database.orderDao()
    private val catalogDao = database.catalogDao()

    suspend fun initializeCatalogIfNeeded() = withContext(Dispatchers.IO) {
        if (catalogDao.getProductCount() == 0) {
            val entities = CatalogItem.DEFAULT_CATALOG.map { item ->
                CatalogEntity(
                    itemCode = item.itemCode,
                    standardName = item.standardName,
                    brand = item.brand,
                    unitType = item.unitType,
                    priceInr = item.priceInr,
                    category = item.category,
                    aliasesCsv = item.aliases.joinToString(",")
                )
            }
            catalogDao.insertProducts(entities)
        }
    }

    fun getAllOrders(): Flow<List<ParsedOrder>> {
        return orderDao.getAllOrders().map { entities ->
            entities.map { entity ->
                val items = JsonHelper.deserializeItems(entity.itemsJson)
                val summary = OrderSummary(
                    totalItemsRequested = entity.totalItems,
                    catalogMatchedCount = entity.matchedCount,
                    unmatchedCount = entity.unmatchedCount,
                    subtotalInr = entity.subtotalInr,
                    taxGstInr = Math.round((entity.grandTotalInr - entity.subtotalInr) * 100.0) / 100.0,
                    grandTotalInr = entity.grandTotalInr
                )
                ParsedOrder(
                    orderId = entity.orderId,
                    retailerName = entity.retailerName,
                    retailerPhone = entity.retailerPhone,
                    retailerAddress = entity.retailerAddress,
                    orderDate = entity.orderDate,
                    inputFormat = entity.inputFormat,
                    rawInput = entity.rawInput,
                    items = items,
                    summary = summary,
                    status = entity.status
                )
            }
        }
    }

    suspend fun saveOrder(order: ParsedOrder) = withContext(Dispatchers.IO) {
        val itemsJson = JsonHelper.serializeItems(order.items)
        val entity = OrderEntity(
            orderId = order.orderId,
            retailerName = order.retailerName,
            retailerPhone = order.retailerPhone,
            retailerAddress = order.retailerAddress,
            orderDate = order.orderDate,
            inputFormat = order.inputFormat,
            rawInput = order.rawInput,
            itemsJson = itemsJson,
            totalItems = order.items.size,
            matchedCount = order.summary.catalogMatchedCount,
            unmatchedCount = order.summary.unmatchedCount,
            subtotalInr = order.summary.subtotalInr,
            grandTotalInr = order.summary.grandTotalInr,
            status = order.status
        )
        orderDao.insertOrder(entity)
    }

    suspend fun deleteOrder(orderId: String) = withContext(Dispatchers.IO) {
        orderDao.deleteOrderById(orderId)
    }

    fun getAllCatalogProducts(): Flow<List<CatalogItem>> {
        return catalogDao.getAllProducts().map { entities ->
            entities.map { entity ->
                CatalogItem(
                    itemCode = entity.itemCode,
                    standardName = entity.standardName,
                    brand = entity.brand,
                    unitType = entity.unitType,
                    priceInr = entity.priceInr,
                    category = entity.category,
                    aliases = entity.aliasesCsv.split(",").filter { it.isNotBlank() }
                )
            }
        }
    }

    suspend fun addCatalogProduct(item: CatalogItem) = withContext(Dispatchers.IO) {
        catalogDao.insertProduct(
            CatalogEntity(
                itemCode = item.itemCode,
                standardName = item.standardName,
                brand = item.brand,
                unitType = item.unitType,
                priceInr = item.priceInr,
                category = item.category,
                aliasesCsv = item.aliases.joinToString(",")
            )
        )
    }
}
