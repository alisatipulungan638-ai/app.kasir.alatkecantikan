package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: POSRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = POSRepository(database)
        
        // Seed initial products/members/transactions immediately on launch!
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Active Screen Tab
    // "CASHIER", "HISTORY", "INVENTORY", "MEMBERS"
    private val _currentTab = MutableStateFlow("CASHIER")
    val currentTab: StateFlow<String> = _currentTab

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    // Query states
    val productSearchQuery = MutableStateFlow("")
    val memberSearchQuery = MutableStateFlow("")

    // Observing list data reactively
    val products = productSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allProducts else repository.searchProducts(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val members = memberSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allMembers else repository.searchMembers(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts = repository.allProducts
        .map { list -> list.filter { it.stock <= 5 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart Management
    private val _cart = MutableStateFlow<List<Pair<Product, Int>>>(emptyList())
    val cart: StateFlow<List<Pair<Product, Int>>> = _cart

    fun addToCart(product: Product) {
        if (product.stock <= 0) return
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.first.id == product.id }
        if (index >= 0) {
            val currentQty = currentList[index].second
            if (currentQty < product.stock) {
                currentList[index] = Pair(product, currentQty + 1)
            }
        } else {
            currentList.add(Pair(product, 1))
        }
        _cart.value = currentList
    }

    fun decreaseQuantity(product: Product) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.first.id == product.id }
        if (index >= 0) {
            val currentQty = currentList[index].second
            if (currentQty > 1) {
                currentList[index] = Pair(product, currentQty - 1)
            } else {
                currentList.removeAt(index)
            }
        }
        _cart.value = currentList
    }

    fun removeFromCart(product: Product) {
        val currentList = _cart.value.filter { it.first.id != product.id }
        _cart.value = currentList
    }

    fun clearCart() {
        _cart.value = emptyList()
        resetCheckoutFields()
    }

    // Checkout Form state
    val checkoutMemberPhone = MutableStateFlow("")
    val checkoutMemberName = MutableStateFlow("")
    val checkoutCashAmount = MutableStateFlow("")
    val autoRegisterMember = MutableStateFlow(true)
    
    // Status states
    val isMemberFound = MutableStateFlow(false)
    val foundMemberName = MutableStateFlow<String?>(null)

    init {
        // Automatically look up member as user types phone
        viewModelScope.launch {
            checkoutMemberPhone.collect { phone ->
                val cleaned = phone.trim()
                if (cleaned.length >= 4) {
                    val member = repository.getMemberByPhone(cleaned)
                    if (member != null) {
                        isMemberFound.value = true
                        foundMemberName.value = member.name
                        checkoutMemberName.value = member.name
                    } else {
                        isMemberFound.value = false
                        foundMemberName.value = null
                    }
                } else {
                    isMemberFound.value = false
                    foundMemberName.value = null
                    checkoutMemberName.value = ""
                }
            }
        }
    }

    fun selectCheckoutMember(member: Member) {
        checkoutMemberPhone.value = member.phone
        checkoutMemberName.value = member.name
        isMemberFound.value = true
        foundMemberName.value = member.name
    }

    private fun resetCheckoutFields() {
        checkoutMemberPhone.value = ""
        checkoutMemberName.value = ""
        checkoutCashAmount.value = ""
        isMemberFound.value = false
        foundMemberName.value = null
    }

    // Calculations
    val cartSubtotal = cart.map { list ->
        list.sumOf { it.first.price * it.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartDiscount = combine(cartSubtotal, isMemberFound) { subtotal, memberFound ->
        if (memberFound) subtotal * 0.05 else 0.0 // 5% discount for verified loyalty members!
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTotal = combine(cartSubtotal, cartDiscount) { subtotal, discount ->
        subtotal - discount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val checkoutChange = combine(cartTotal, checkoutCashAmount) { total, cashStr ->
        val cash = cashStr.toDoubleOrNull() ?: 0.0
        if (cash >= total) cash - total else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Invoice Status Popup
    private val _checkoutSuccessMessage = MutableStateFlow<String?>(null)
    val checkoutSuccessMessage: StateFlow<String?> = _checkoutSuccessMessage

    fun dismissCheckoutSuccess() {
        _checkoutSuccessMessage.value = null
    }

    fun completeCheckout(): Boolean {
        val total = cartTotal.value
        val cash = checkoutCashAmount.value.toDoubleOrNull() ?: 0.0
        if (cash < total || total <= 0) return false

        val itemsSnap = _cart.value
        val phoneSnap = if (checkoutMemberPhone.value.isNotBlank()) checkoutMemberPhone.value.trim() else null
        val nameSnap = if (checkoutMemberName.value.isNotBlank()) checkoutMemberName.value.trim() else null

        viewModelScope.launch {
            val success = repository.executeCheckout(
                memberPhone = phoneSnap,
                memberName = nameSnap,
                totalPrice = cartSubtotal.value,
                discountPrice = cartDiscount.value,
                finalPrice = total,
                cashAmount = cash,
                changeAmount = checkoutChange.value,
                cartItems = itemsSnap
            )
            if (success) {
                _checkoutSuccessMessage.value = "Transaksi Berhasil! Kembalian: Rp ${numberFormat(checkoutChange.value)}"
                clearCart()
            }
        }
        return true
    }

    // Simulated scanner helper
    private val _scannerNotification = MutableStateFlow<String?>(null)
    val scannerNotification: StateFlow<String?> = _scannerNotification

    fun triggerBarcodeScan(barcode: String) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode.trim())
            if (product != null) {
                if (product.stock > 0) {
                    addToCart(product)
                    _scannerNotification.value = "Barcode Match: ${product.name} ditambahkan ke keranjang🛒"
                } else {
                    _scannerNotification.value = "Stok ${product.name} kosong!"
                }
            } else {
                _scannerNotification.value = "Barcode $barcode tidak dikenal!"
            }
        }
    }

    fun clearScannerNotification() {
        _scannerNotification.value = null
    }

    // Reports Dashboard calculations
    val todayStartTimestamp: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val dailyTransactions = repository.getDailyTransactions(todayStartTimestamp)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTotalRevenue = dailyTransactions.map { list ->
        list.sumOf { it.transaction.finalPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayTransactionCount = dailyTransactions.map { list ->
        list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageBasketSize = combine(todayTotalRevenue, todayTransactionCount) { revenue, count ->
        if (count > 0) revenue / count else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Inventory operations inputs
    val newProdName = MutableStateFlow("")
    val newProdBarcode = MutableStateFlow("")
    val newProdPrice = MutableStateFlow("")
    val newProdStock = MutableStateFlow("")
    val newProdCategory = MutableStateFlow("Kosmetik")

    fun resetProductInputs() {
        newProdName.value = ""
        newProdBarcode.value = ""
        newProdPrice.value = ""
        newProdStock.value = ""
        newProdCategory.value = "Kosmetik"
        editingProduct.value = null
    }

    val editingProduct = MutableStateFlow<Product?>(null)

    fun startEditProduct(product: Product) {
        editingProduct.value = product
        newProdName.value = product.name
        newProdBarcode.value = product.barcode
        newProdPrice.value = product.price.toInt().toString()
        newProdStock.value = product.stock.toString()
        newProdCategory.value = product.category
    }

    fun saveProduct(): String? {
        val name = newProdName.value.trim()
        val barcode = newProdBarcode.value.trim()
        val price = newProdPrice.value.toDoubleOrNull() ?: 0.0
        val stock = newProdStock.value.toIntOrNull() ?: 0
        val category = newProdCategory.value

        if (name.isBlank() || barcode.isBlank() || price <= 0) {
            return "Mohon isi nama barang, barcode, dan harga secara benar!"
        }

        viewModelScope.launch {
            val currentEditing = editingProduct.value
            if (currentEditing != null) {
                val updated = currentEditing.copy(
                    name = name,
                    barcode = barcode,
                    price = price,
                    stock = stock,
                    category = category
                )
                repository.updateProduct(updated)
            } else {
                val newProduct = Product(
                    name = name,
                    barcode = barcode,
                    price = price,
                    stock = stock,
                    category = category
                )
                repository.insertProduct(newProduct)
            }
            resetProductInputs()
        }
        return null
    }

    fun quickStockAdjust(product: Product, adjustBy: Int) {
        viewModelScope.launch {
            repository.updateStockDirect(product.id, product.stock + adjustBy)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Members management additions
    val newMemberName = MutableStateFlow("")
    val newMemberPhone = MutableStateFlow("")

    fun saveMember(): String? {
        val name = newMemberName.value.trim()
        val phone = newMemberPhone.value.trim()

        if (name.isBlank() || phone.isBlank()) {
            return "Mohon isi nama lengkap dan nomor telepon member!"
        }

        viewModelScope.launch {
            val newMember = Member(phone = phone, name = name, points = 0)
            repository.insertMember(newMember)
            newMemberName.value = ""
            newMemberPhone.value = ""
        }
        return null
    }

    // Number formatting helper
    fun numberFormat(amount: Double): String {
        return "%,.0f".format(amount).replace(",", ".")
    }
}
