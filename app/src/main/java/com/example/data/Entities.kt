package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String, // barcode scanning input
    val name: String,
    val category: String, // Kosmetik, Skincare, Haircare, Bodycare, etc.
    val price: Double,
    val stock: Int
)

@Entity(tableName = "members")
data class Member(
    @PrimaryKey val phone: String, // Phone number as ID since phone numbers are standard for beauty shop memberships
    val name: String,
    val points: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memberPhone: String?, // associated member if any
    val memberName: String?,  // snapshot of member name during purchase
    val totalPrice: Double,
    val discountPrice: Double, // optional discount if member
    val finalPrice: Double,
    val cashAmount: Double,
    val changeAmount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "transaction_items")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionId: Int,
    val productId: Int,
    val productName: String,
    val priceAtPurchase: Double,
    val quantity: Int
)

// POJO containing a transaction with its items for report details
data class TransactionWithItems(
    val transaction: TransactionEntity,
    val items: List<TransactionItem>
)
