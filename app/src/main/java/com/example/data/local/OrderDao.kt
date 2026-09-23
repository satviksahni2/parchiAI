package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM parsed_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM parsed_orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("DELETE FROM parsed_orders WHERE orderId = :orderId")
    suspend fun deleteOrderById(orderId: String)

    @Query("DELETE FROM parsed_orders")
    suspend fun clearAllOrders()
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_products ORDER BY itemCode ASC")
    fun getAllProducts(): Flow<List<CatalogEntity>>

    @Query("SELECT * FROM catalog_products")
    suspend fun getAllProductsList(): List<CatalogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<CatalogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: CatalogEntity)

    @Query("SELECT COUNT(*) FROM catalog_products")
    suspend fun getProductCount(): Int
}
