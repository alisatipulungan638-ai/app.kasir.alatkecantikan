package com.example.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class POSRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val memberDao = database.memberDao()
    private val transactionDao = database.transactionDao()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()
    
    fun searchProducts(query: String): Flow<List<Product>> = productDao.searchProducts(query)
    fun searchMembers(query: String): Flow<List<Member>> = memberDao.searchMembers(query)

    suspend fun getProductByBarcode(barcode: String): Product? = productDao.getProductByBarcode(barcode)
    suspend fun getMemberByPhone(phone: String): Member? = memberDao.getMemberByPhone(phone)

    suspend fun insertProduct(product: Product) = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)
    suspend fun updateStockDirect(productId: Int, newStock: Int) = productDao.updateStockDirect(productId, newStock)

    suspend fun insertMember(member: Member) = memberDao.insertMember(member)

    // Reactive flow of completed transactions with details
    val allTransactions: Flow<List<TransactionWithItemsRelation>> = 
        transactionDao.getAllTransactionsWithItems()

    fun getDailyTransactions(startOfDay: Long): Flow<List<TransactionWithItemsRelation>> =
        transactionDao.getDailyTransactionsWithItems(startOfDay)

    /**
     * Executes atomic purchase transaction:
     * 1. Inserts the transaction entity safely.
     * 2. Sets transaction references on all sales items and inserts them.
     * 3. Decreases product stock.
     * 4. Updates or automatically registers a member, giving them purchase reward loyalty points.
     */
    suspend fun executeCheckout(
        memberPhone: String?,
        memberName: String?,
        totalPrice: Double,
        discountPrice: Double,
        finalPrice: Double,
        cashAmount: Double,
        changeAmount: Double,
        cartItems: List<Pair<Product, Int>>
    ): Boolean {
        return try {
            database.withTransaction {
                // If member details are submitted, ensure member exists or register automatically
                var finalizedPhone: String? = null
                var finalizedName: String? = null

                if (!memberPhone.isNullOrBlank()) {
                    finalizedPhone = memberPhone.trim()
                    val existingMember = memberDao.getMemberByPhone(finalizedPhone)
                    
                    val calculatedPoints = (finalPrice / 10000.0).toInt() // 1 point per Rp 10.000 spent

                    if (existingMember != null) {
                        finalizedName = existingMember.name
                        memberDao.addPoints(finalizedPhone, calculatedPoints)
                    } else {
                        // Register member automatically as requested!
                        val fallbackName = if (!memberName.isNullOrBlank()) memberName.trim() else "Member Baru"
                        finalizedName = fallbackName
                        val newMember = Member(
                            phone = finalizedPhone,
                            name = fallbackName,
                            points = calculatedPoints
                        )
                        memberDao.insertMember(newMember)
                    }
                }

                // Create transaction invoice
                val transactionHeader = TransactionEntity(
                    memberPhone = finalizedPhone,
                    memberName = finalizedName,
                    totalPrice = totalPrice,
                    discountPrice = discountPrice,
                    finalPrice = finalPrice,
                    cashAmount = cashAmount,
                    changeAmount = changeAmount,
                    timestamp = System.currentTimeMillis()
                )

                val generatedId = transactionDao.insertTransaction(transactionHeader).toInt()

                // Map items & reduce inventory stock levels
                val itemsToSave = cartItems.map { (product, qty) ->
                    productDao.decreaseStock(product.id, qty)
                    TransactionItem(
                        transactionId = generatedId,
                        productId = product.id,
                        productName = product.name,
                        priceAtPurchase = product.price,
                        quantity = qty
                    )
                }

                transactionDao.insertTransactionItems(itemsToSave)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Seeds the database with rich mock data for cosmetics and sales histories if kosong.
     */
    suspend fun seedDatabaseIfEmpty() {
        val existingProductsList = productDao.getAllProducts().firstOrNull()
        if (existingProductsList.isNullOrEmpty()) {
            // Seed products
            val initialProducts = listOf(
                Product(barcode = "50123456", name = "Loreal Hyaluronic Serum", category = "Skincare", price = 185000.0, stock = 2),
                Product(barcode = "88012345", name = "Laneige Sleeping Mask", category = "Skincare", price = 370000.0, stock = 5),
                Product(barcode = "89912345", name = "Wardah Matte Lip Cream", category = "Kosmetik", price = 65000.0, stock = 28),
                Product(barcode = "34012345", name = "Cetaphil Gentle Cleanser", category = "Skincare", price = 145000.0, stock = 15),
                Product(barcode = "89978912", name = "Somethinc Niacinamide Serum", category = "Skincare", price = 119000.0, stock = 20),
                Product(barcode = "69012345", name = "Maybelline Fit Me Foundation", category = "Kosmetik", price = 165000.0, stock = 3),
                Product(barcode = "88023456", name = "COSRX Snail Mucin Essence", category = "Skincare", price = 210000.0, stock = 9),
                Product(barcode = "89945678", name = "Safi Age Defy Cream", category = "Skincare", price = 95000.0, stock = 12),
                Product(barcode = "50987654", name = "Rose All Day Mascara", category = "Kosmetik", price = 129000.0, stock = 7),
                Product(barcode = "89999111", name = "Luxcrime Blur Powder", category = "Kosmetik", price = 110000.0, stock = 14)
            )
            for (p in initialProducts) {
                productDao.insertProduct(p)
            }

            // Seed members
            val initialMembers = listOf(
                Member(phone = "081234567890", name = "Adinda Safitri", points = 150),
                Member(phone = "087788990011", name = "Budi Santoso", points = 40),
                Member(phone = "089911223344", name = "Clara Monica", points = 290)
            )
            for (m in initialMembers) {
                memberDao.insertMember(m)
            }

            // Seed some past transactions for reports dashboard completeness
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val todayStart = cal.timeInMillis

            // We simulate 3 yesterday transactions and 4 today transactions.
            // Under Professional Polish specifications, we'll design today's sales to match around Rp 4.250.000 (total over transactions).
            // Transaction 1 (Today, Budi Santoso)
            val t1 = TransactionEntity(
                memberPhone = "087788990011",
                memberName = "Budi Santoso",
                totalPrice = 555000.0,
                discountPrice = 27750.0, // 5% loyalty discount
                finalPrice = 527250.0,
                cashAmount = 600000.0,
                changeAmount = 72750.0,
                timestamp = todayStart + 3600000 * 9 // 09:00 AM
            )
            val t1Id = transactionDao.insertTransaction(t1).toInt()
            transactionDao.insertTransactionItems(listOf(
                TransactionItem(transactionId = t1Id, productId = 1, productName = "Loreal Hyaluronic Serum", priceAtPurchase = 185000.0, quantity = 1),
                TransactionItem(transactionId = t1Id, productId = 2, productName = "Laneige Sleeping Mask", priceAtPurchase = 370000.0, quantity = 1)
            ))

            // Transaction 2 (Today, Walk-in)
            val t2 = TransactionEntity(
                memberPhone = null,
                memberName = null,
                totalPrice = 130000.0,
                discountPrice = 0.0,
                finalPrice = 130000.0,
                cashAmount = 150000.0,
                changeAmount = 20000.0,
                timestamp = todayStart + 3600000 * 10 // 10:00 AM
            )
            val t2Id = transactionDao.insertTransaction(t2).toInt()
            transactionDao.insertTransactionItems(listOf(
                TransactionItem(transactionId = t2Id, productId = 3, productName = "Wardah Matte Lip Cream", priceAtPurchase = 65000.0, quantity = 2)
            ))

            // Transaction 3 (Today, Clara Monica)
            val t3 = TransactionEntity(
                memberPhone = "089911223344",
                memberName = "Clara Monica",
                totalPrice = 2895000.0,
                discountPrice = 144750.0,
                finalPrice = 2750250.0,
                cashAmount = 3000000.0,
                changeAmount = 249750.0,
                timestamp = todayStart + 3600000 * 12 // 12:00 PM
            )
            val t3Id = transactionDao.insertTransaction(t3).toInt()
            transactionDao.insertTransactionItems(listOf(
                TransactionItem(transactionId = t3Id, productId = 2, productName = "Laneige Sleeping Mask", priceAtPurchase = 370000.0, quantity = 5),
                TransactionItem(transactionId = t3Id, productId = 7, productName = "COSRX Snail Mucin Essence", priceAtPurchase = 210000.0, quantity = 3),
                TransactionItem(transactionId = t3Id, productId = 4, productName = "Cetaphil Gentle Cleanser", priceAtPurchase = 145000.0, quantity = 2),
                TransactionItem(transactionId = t3Id, productId = 5, productName = "Somethinc Niacinamide Serum", priceAtPurchase = 119000.0, quantity = 10)
            ))

            // Transaction 4 (Today, Adinda Safitri)
            val t4 = TransactionEntity(
                memberPhone = "081234567890",
                memberName = "Adinda Safitri",
                totalPrice = 880000.0,
                discountPrice = 44000.0,
                finalPrice = 836000.0,
                cashAmount = 850000.0,
                changeAmount = 14000.0,
                timestamp = todayStart + 3600000 * 14 // 02:00 PM
            )
            val t4Id = transactionDao.insertTransaction(t4).toInt()
            transactionDao.insertTransactionItems(listOf(
                TransactionItem(transactionId = t4Id, productId = 6, productName = "Maybelline Fit Me Foundation", priceAtPurchase = 165000.0, quantity = 2),
                TransactionItem(transactionId = t4Id, productId = 9, productName = "Rose All Day Mascara", priceAtPurchase = 129000.0, quantity = 2),
                TransactionItem(transactionId = t4Id, productId = 10, productName = "Luxcrime Blur Powder", priceAtPurchase = 110000.0, quantity = 2),
                TransactionItem(transactionId = t4Id, productId = 8, productName = "Safi Age Defy Cream", priceAtPurchase = 95000.0, quantity = 1)
            ))
        }
    }
}
