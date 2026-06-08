package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val scannerNotification by viewModel.scannerNotification.collectAsStateWithLifecycle()
    val checkoutSuccessMessage by viewModel.checkoutSuccessMessage.collectAsStateWithLifecycle()

    // Dialog flags
    var showBarcodeModal by remember { mutableStateOf(false) }
    var showNewMemberModal by remember { mutableStateOf(false) }
    var showNewProductModal by remember { mutableStateOf(false) }
    var showInvoiceDetailModal by remember { mutableStateOf<TransactionWithItemsRelation?>(null) }
    var showQuickTransactionCartModal by remember { mutableStateOf(false) }

    // Clear notifications automatically after a delay
    LaunchedEffect(scannerNotification) {
        if (scannerNotification != null) {
            delay(3500)
            viewModel.clearScannerNotification()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WarmBackground,
        bottomBar = {
            BottomNavBar(
                currentTab = currentTab,
                onTabSelect = { tab ->
                    viewModel.setTab(tab)
                },
                onCartClick = {
                    showQuickTransactionCartModal = true
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of GlowPoint POS
                HeaderSection()

                // Static summary metrics panel
                MetricsOverviewPanel(viewModel = viewModel)

                // Quick Action Bar
                QuickActionsBar(
                    onScanClick = { showBarcodeModal = true },
                    onNewMemberClick = { showNewMemberModal = true },
                    onCheckStockClick = { viewModel.setTab("INVENTORY") },
                    onLaporanClick = { viewModel.setTab("HISTORY") }
                )

                Divider(
                    color = Color(0xFFF0EAEA),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Content switching based on selected tab state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentTab) {
                        "CASHIER" -> CashierContentSection(
                            viewModel = viewModel,
                            onOpenCartCheckout = { showQuickTransactionCartModal = true }
                        )
                        "HISTORY" -> HistoryContentSection(
                            viewModel = viewModel,
                            onInvoiceClick = { showInvoiceDetailModal = it }
                        )
                        "INVENTORY" -> InventoryContentSection(
                            viewModel = viewModel,
                            onAddNewProduct = { showNewProductModal = true }
                        )
                        "MEMBERS" -> MembersContentSection(viewModel = viewModel)
                        "SETTINGS" -> SettingsContentSection()
                    }
                }
            }

            // Notification Popups Floating
            scannerNotification?.let { msg ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .fillMaxWidth(0.9f)
                        .testTag("scanner_toast"),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPink),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner Match",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearScannerNotification() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismis",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            checkoutSuccessMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .fillMaxWidth(0.85f)
                        .testTag("checkout_success_popup"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF2E7D32), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Berhasil",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Checkout Berhasil",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            fontSize = 14.sp,
                            color = AccentGreen,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.dismissCheckoutSuccess() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dismiss_success_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Selesai")
                        }
                    }
                }
            }
        }
    }

    // --- MODAL DIALOGS ---

    // 1. Barcode Manual & Quick presets
    if (showBarcodeModal) {
        BarcodeScannerSimulatorDialog(
            viewModel = viewModel,
            onDismiss = { showBarcodeModal = false }
        )
    }

    // 2. Add New Member Dialog
    if (showNewMemberModal) {
        RegisterMemberDialog(
            viewModel = viewModel,
            onDismiss = { showNewMemberModal = false }
        )
    }

    // 3. Add / Edit Product Dialog
    if (showNewProductModal || viewModel.editingProduct.collectAsStateWithLifecycle().value != null) {
        AddEditProductDialog(
            viewModel = viewModel,
            onDismiss = {
                showNewProductModal = false
                viewModel.resetProductInputs()
            }
        )
    }

    // 4. Invoice Detail Modal
    showInvoiceDetailModal?.let { rel ->
        InvoiceDetailDialog(
            relation = rel,
            viewModel = viewModel,
            onDismiss = { showInvoiceDetailModal = null }
        )
    }

    // 5. Complete shopping cart dialog
    if (showQuickTransactionCartModal) {
        CartCheckoutDialog(
            viewModel = viewModel,
            onDismiss = { showQuickTransactionCartModal = false }
        )
    }
}

// --- SUB-COMPONENTS & LAYOUTS ---

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "GlowPoint POS",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                fontFamily = FontFamily.SansSerif
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(PrimaryPink, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cabang Mall Jakarta • Aktif",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryMuted
                )
            }
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(SecondaryPink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = "Cosmetic Storefront",
                tint = PrimaryPink,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun MetricsOverviewPanel(viewModel: MainViewModel) {
    val todayRevenue by viewModel.todayTotalRevenue.collectAsStateWithLifecycle()
    val todaySalesCount by viewModel.todayTransactionCount.collectAsStateWithLifecycle()
    val avgBasketSize by viewModel.averageBasketSize.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left item: Today's revenue
        Card(
            modifier = Modifier
                .weight(1f)
                .height(115.dp),
            colors = CardDefaults.cardColors(containerColor = TertiaryPink),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFFAD2D8))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "OMZET HARI INI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF883244),
                    letterSpacing = 0.5.sp
                )
                Column {
                    Text(
                        text = "Rp ${viewModel.numberFormat(todayRevenue)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "+12% vs Kemarin",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }
            }
        }

        // Right item: Total transaction count
        Card(
            modifier = Modifier
                .weight(1f)
                .height(115.dp),
            colors = CardDefaults.cardColors(containerColor = AccentBlueLight),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFD0E6F5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TOTAL STRUK",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0),
                    letterSpacing = 0.5.sp
                )
                Column {
                    Text(
                        text = "$todaySalesCount Transaksi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1A237E)
                    )
                    Text(
                        text = "Avg: Rp ${viewModel.numberFormat(avgBasketSize)}",
                        fontSize = 10.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsBar(
    onScanClick: () -> Unit,
    onNewMemberClick: () -> Unit,
    onCheckStockClick: () -> Unit,
    onLaporanClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Aksi Cepat",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Action 1: Barcode Scan
                QuickActionButton(
                    icon = Icons.Default.QrCodeScanner,
                    label = "Scan Barcode",
                    containerColor = SecondaryPink,
                    iconColor = PrimaryPink,
                    borderColor = Color(0xFFF8BBD0),
                    onClick = onScanClick,
                    testTag = "quick_scan_button"
                )

                // Action 2: Add member
                QuickActionButton(
                    icon = Icons.Default.PersonAdd,
                    label = "Member Baru",
                    containerColor = Color(0xFFF3E5F5),
                    iconColor = Color(0xFF7B1FA2),
                    borderColor = Color(0xFFE1BEE7),
                    onClick = onNewMemberClick,
                    testTag = "quick_member_button"
                )

                // Action 3: Check Stock
                QuickActionButton(
                    icon = Icons.Default.Warehouse,
                    label = "Cek Stok",
                    containerColor = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF2E7D32),
                    borderColor = Color(0xFFC8E6C9),
                    onClick = onCheckStockClick,
                    testTag = "quick_stock_button"
                )

                // Action 4: Reports
                QuickActionButton(
                    icon = Icons.Default.Assessment,
                    label = "Laporan",
                    containerColor = Color(0xFFFFF3E0),
                    iconColor = Color(0xFFE65100),
                    borderColor = Color(0xFFFFE0B2),
                    onClick = onLaporanClick,
                    testTag = "quick_reports_button"
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    iconColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(containerColor)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )
    }
}

// --- CASHIER TAB CONTENT ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CashierContentSection(
    viewModel: MainViewModel,
    onOpenCartCheckout: () -> Unit
) {
    val productsList by viewModel.products.collectAsStateWithLifecycle()
    val cartList by viewModel.cart.collectAsStateWithLifecycle()
    val rawSearch by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val subtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = rawSearch,
                onValueChange = { viewModel.productSearchQuery.value = it },
                placeholder = { Text("Telusuri produk beauty / kosmetik...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                trailingIcon = {
                    if (rawSearch.isNotBlank()) {
                        IconButton(onClick = { viewModel.productSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 56.dp)
                    .testTag("cashier_product_search"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Katalog Kecantikan (${productsList.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            if (cartList.isNotEmpty()) {
                Surface(
                    onClick = onOpenCartCheckout,
                    color = PrimaryPink,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${cartList.sumOf { it.second }} item (Rp ${viewModel.numberFormat(subtotal)})",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (productsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Cosmetics Empty",
                        tint = TextSecondaryMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Barang kecantikan tidak ditemukan",
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondaryMuted
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(productsList, key = { it.id }) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                            .testTag("product_card_${product.barcode}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Category Badge
                            Box(
                                modifier = Modifier
                                    .background(SecondaryPink, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = product.category.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryPink
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = product.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "BC: ${product.barcode}",
                                fontSize = 10.sp,
                                color = TextSecondaryMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Rp ${viewModel.numberFormat(product.price)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryPink
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Stock level display colored
                            val isLow = product.stock <= 5
                            val bgClr = if (isLow) AlertRedBg else Color(0xFFE8F5E9)
                            val txtClr = if (isLow) AlertRed else AccentGreen
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bgClr, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Stok: ${product.stock}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = txtClr
                                )
                                if (isLow) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Stok Tipis",
                                        tint = AlertRed,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Button(
                                onClick = { viewModel.addToCart(product) },
                                enabled = product.stock > 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("add_to_cart_${product.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryPink,
                                    disabledContainerColor = Color(0xFFF0EAEA)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Tambah",
                                    tint = if (product.stock > 0) Color.White else TextSecondaryMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (product.stock > 0) "Tambah" else "Habis",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (product.stock > 0) Color.White else TextSecondaryMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- HISTORY TAB CONTENT (TRANSACTIONS REPORT) ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryContentSection(
    viewModel: MainViewModel,
    onInvoiceClick: (TransactionWithItemsRelation) -> Unit
) {
    val dailySalesList by viewModel.dailyTransactions.collectAsStateWithLifecycle()
    val totalRevenue by viewModel.todayTotalRevenue.collectAsStateWithLifecycle()
    val transCount by viewModel.todayTransactionCount.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Laporan Penjualan Hari Ini",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Total Omzet", fontSize = 11.sp, color = TextSecondaryMuted)
                        Text(
                            text = "Rp ${viewModel.numberFormat(totalRevenue)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = PrimaryPink
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Struk Tercetak", fontSize = 11.sp, color = TextSecondaryMuted)
                        Text(
                            text = "$transCount kali",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentBlue
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Daftar Struk Penjualan",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (dailySalesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada transaksi kosmetik hari ini.",
                    fontSize = 13.sp,
                    color = TextSecondaryMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dailySalesList, key = { it.transaction.id }) { relation ->
                    val trans = relation.transaction
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                            .clickable { onInvoiceClick(relation) }
                            .testTag("invoice_row_${trans.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(SecondaryPink, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "Resi",
                                    tint = PrimaryPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Struk #${trans.id} • ${trans.memberName ?: "Walk-in Customer"}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "${relation.items.size} barang • ${java.text.SimpleDateFormat("HH:mm").format(trans.timestamp)} WIB",
                                    fontSize = 11.sp,
                                    color = TextSecondaryMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Rp ${viewModel.numberFormat(trans.finalPrice)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                if (trans.discountPrice > 0) {
                                    Text(
                                        text = "Disc member",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- INVENTORY TAB CONTENT (STOCK MANAGEMENT) ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryContentSection(
    viewModel: MainViewModel,
    onAddNewProduct: () -> Unit
) {
    val masterProducts by viewModel.products.collectAsStateWithLifecycle()
    val lowStockList by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    var selectedCategoryTab by remember { mutableStateOf("Semua") }

    val categories = listOf("Semua", "Kosmetik", "Skincare", "Bodycare")
    
    val filteredProducts = remember(masterProducts, selectedCategoryTab) {
        if (selectedCategoryTab == "Semua") masterProducts
        else masterProducts.filter { it.category.equals(selectedCategoryTab, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Warning Stock alerts section if any
        if (lowStockList.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = AlertRedBg),
                border = BorderStroke(1.dp, AlertRedBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Peringatan",
                            tint = AlertRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STOK MENIPIS (${lowStockList.size} item segera restok)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF991B1B)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 100.dp)) {
                        items(lowStockList, key = { "low_${it.id}" }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${item.name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Tersisa ${item.stock} unit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active filters tab
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategoryTab).coerceAtLeast(0),
            edgePadding = 0.dp,
            divider = {},
            indicator = {},
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEach { cat ->
                val isSelected = cat == selectedCategoryTab
                val bg = if (isSelected) PrimaryPink else Color.White
                val txt = if (isSelected) Color.White else TextPrimaryDark
                
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp, bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else Color(0xFFF0EAEA),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedCategoryTab = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(text = cat, color = txt, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Stok Produk (${filteredProducts.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Button(
                onClick = onAddNewProduct,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp).testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Produk Baru", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Daftar barang kosong.",
                    fontSize = 13.sp,
                    color = TextSecondaryMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts, key = { it.id }) { prod ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                            .testTag("inventory_item_${prod.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(SecondaryPink, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            prod.category.uppercase(),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = PrimaryPink
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BC: ${prod.barcode}",
                                        fontSize = 10.sp,
                                        color = TextSecondaryMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prod.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Harga: Rp ${viewModel.numberFormat(prod.price)}",
                                    fontSize = 12.sp,
                                    color = PrimaryPink,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Inventory adjustments and direct control
                            Column(horizontalAlignment = Alignment.End) {
                                // Stock display indicator
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (prod.stock <= 5) AlertRedBg else Color(0xFFE8F5E9),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Stok: ${prod.stock}",
                                        fontSize = 12.sp,
                                        color = if (prod.stock <= 5) AlertRed else AccentGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Quick restock +5
                                    Surface(
                                        onClick = { viewModel.quickStockAdjust(prod, 5) },
                                        color = Color(0xFFE3F2FD),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("refill_5_${prod.id}")
                                    ) {
                                        Text(
                                            text = "+5",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentBlue
                                        )
                                    }

                                    // Edit details button
                                    IconButton(
                                        onClick = { viewModel.startEditProduct(prod) },
                                        modifier = Modifier.size(24.dp).testTag("edit_product_${prod.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = AccentBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = { viewModel.deleteProduct(prod) },
                                        modifier = Modifier.size(24.dp).testTag("delete_product_${prod.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = AlertRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- MEMBERS TAB CONTENT ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MembersContentSection(viewModel: MainViewModel) {
    val membersList by viewModel.members.collectAsStateWithLifecycle()
    val searchVal by viewModel.memberSearchQuery.collectAsStateWithLifecycle()
    val newName by viewModel.newMemberName.collectAsStateWithLifecycle()
    val newPhone by viewModel.newMemberPhone.collectAsStateWithLifecycle()

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Registering Member fast form inside tab
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Registrasi Member Baru",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { viewModel.newMemberName.value = it },
                        label = { Text("Nama Lengkap", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reg_member_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { viewModel.newMemberPhone.value = it },
                        label = { Text("No Telepon", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reg_member_phone_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = AlertRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val result = viewModel.saveMember()
                        if (result != null) {
                            errorMessage = result
                        } else {
                            errorMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("save_member_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Simpan", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Member", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search panel
        TextField(
            value = searchVal,
            onValueChange = { viewModel.memberSearchQuery.value = it },
            placeholder = { Text("Cari member berdasarkan nama / no hp...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_members_input"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Daftar Member Beauty Club (${membersList.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (membersList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tidak ada member terdaftar.",
                    fontSize = 13.sp,
                    color = TextSecondaryMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(membersList, key = { it.phone }) { member ->
                    // Beautiful Cosmetic Member VIP Card representation
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                            .testTag("member_card_${member.phone}"),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFD81B60), Color(0xFFEC407A))
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "GLOWPOINT BEAUTY CLUB",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.8f),
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LOYAL MEMBER",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Text(
                                text = member.name.uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = member.phone,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text("Loyalty Reward Points", fontSize = 8.sp, color = Color.White.copy(alpha = 0.7f))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Stars,
                                            contentDescription = "Poin",
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${member.points} PTS",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                                Text(
                                    "Joined: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(member.createdAt)}",
                                    fontSize = 8.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SETTINGS TAB LAYOUT ---

@Composable
fun SettingsContentSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(SecondaryPink, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Pengaturan",
                tint = PrimaryPink,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Pengaturan Kasir",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "GlowPoint POS Versi 1.0.0 Pro\nSistem Keamanan Database SQLite Terkunci.\nCabang Istana Jakarta Barat.",
            fontSize = 12.sp,
            color = TextSecondaryMuted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// --- BOTTOM NAVIGATION BAR (Professional Polish design style) ---

@Composable
fun BottomNavBar(
    currentTab: String,
    onTabSelect: (String) -> Unit,
    onCartClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.White,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFF0EAEA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Menu / Cashier
            BottomNavItem(
                icon = Icons.Default.GridView,
                label = "Menu",
                selected = currentTab == "CASHIER",
                onClick = { onTabSelect("CASHIER") },
                testTag = "nav_cashier"
            )

            // Tab 2: history / reports
            BottomNavItem(
                icon = Icons.Default.History,
                label = "Riwayat",
                selected = currentTab == "HISTORY",
                onClick = { onTabSelect("HISTORY") },
                testTag = "nav_history"
            )

            // Middle floating cart action button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable { onCartClick() }
                    .background(PrimaryPink)
                    .testTag("nav_cart_middle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = "Shopping Cart",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tab 3: Member
            BottomNavItem(
                icon = Icons.Default.Group,
                label = "Member",
                selected = currentTab == "MEMBERS",
                onClick = { onTabSelect("MEMBERS") },
                testTag = "nav_members"
            )

            // Tab 4: Settings
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "Finansial",
                selected = currentTab == "SETTINGS",
                onClick = { onTabSelect("SETTINGS") },
                testTag = "nav_settings"
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val clr = if (selected) PrimaryPink else TextSecondaryMuted
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = clr,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = clr,
            letterSpacing = 0.5.sp
        )
    }
}

// --- DIALOG BOXES IMPLEMENTATIONS ---

// A. Barcode simulator
@Composable
fun BarcodeScannerSimulatorDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }
    val productList by viewModel.products.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("barcode_scanner_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Pemindai Barcode (Simulasi)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Ketik barcode atau pilih preset produk di bawah untuk disimulasikan sebagai hasil scan kamera laser POS.",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { inputCode = it },
                    placeholder = { Text("Ketik kode barcode manual...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("barcode_simulation_text_field"),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (inputCode.isNotBlank()) {
                                viewModel.triggerBarcodeScan(inputCode)
                                inputCode = ""
                            }
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Scan", tint = PrimaryPink)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cepat Klik Preset Produk Kosmetik:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                    items(productList) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.triggerBarcodeScan(p.barcode)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Text("Barcode: ${p.barcode}", fontSize = 10.sp, color = TextSecondaryMuted)
                            }
                            Box(
                                modifier = Modifier
                                    .background(SecondaryPink, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Klik Scan", fontSize = 9.sp, fontWeight = FontWeight.Black, color = PrimaryPink)
                            }
                        }
                        Divider(color = Color(0xFFFCF8F8))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("close_barcode_dialog_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tutup Pemindai")
                }
            }
        }
    }
}

// B. Member Registration Dialog
@Composable
fun RegisterMemberDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val name by viewModel.newMemberName.collectAsStateWithLifecycle()
    val phone by viewModel.newMemberPhone.collectAsStateWithLifecycle()
    var err by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("register_member_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Registrasi Member Baru",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.newMemberName.value = it },
                    label = { Text("Nama Lengkap Member") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_member_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { viewModel.newMemberPhone.value = it },
                    label = { Text("Nomor Telepon (WhatsApp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_member_phone"),
                    shape = RoundedCornerShape(12.dp)
                )

                err?.let {
                    Text(
                        text = it,
                        color = AlertRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val res = viewModel.saveMember()
                            if (res != null) {
                                err = res
                            } else {
                                err = null
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dialog_save_member_confirm"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// C. Product Add/Edit Dialog
@Composable
fun AddEditProductDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val name by viewModel.newProdName.collectAsStateWithLifecycle()
    val barcode by viewModel.newProdBarcode.collectAsStateWithLifecycle()
    val price by viewModel.newProdPrice.collectAsStateWithLifecycle()
    val stock by viewModel.newProdStock.collectAsStateWithLifecycle()
    val category by viewModel.newProdCategory.collectAsStateWithLifecycle()
    val isEditMode = viewModel.editingProduct.collectAsStateWithLifecycle().value != null

    var err by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("product_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (isEditMode) "Edit Detail Produk" else "Tambah Produk Baru",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.newProdName.value = it },
                    label = { Text("Nama Barang Kecantikan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_product_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { viewModel.newProdBarcode.value = it },
                    label = { Text("Kode Barcode") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_product_barcode"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { viewModel.newProdPrice.value = it },
                        label = { Text("Harga Jual (Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_product_price"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = stock,
                        onValueChange = { viewModel.newProdStock.value = it },
                        label = { Text("Disisikan Stok") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_product_stock"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Kategori Kosmetik:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))

                val categories = listOf("Kosmetik", "Skincare", "Bodycare", "Haircare")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == category
                        val colorBg = if (isSelected) PrimaryPink else Color(0xFFFCF8F8)
                        val colorText = if (isSelected) Color.White else TextPrimaryDark
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorBg)
                                .clickable { viewModel.newProdCategory.value = cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colorText)
                        }
                    }
                }

                err?.let {
                    Text(
                        text = it,
                        color = AlertRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val res = viewModel.saveProduct()
                            if (res != null) {
                                err = res
                            } else {
                                err = null
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dialog_product_save"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// D. Invoice Detail Dialog
@Composable
fun InvoiceDetailDialog(
    relation: TransactionWithItemsRelation,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val trans = relation.transaction

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("invoice_detail_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detail Struk #${trans.id}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Box(
                        modifier = Modifier
                            .background(SecondaryPink, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("TERBAYAR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PrimaryPink)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = java.text.SimpleDateFormat("dd MMM yyyy - HH:mm").format(trans.timestamp) + " WIB",
                    fontSize = 10.sp,
                    color = TextSecondaryMuted
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFFF0EAEA))
                Spacer(modifier = Modifier.height(10.dp))

                // Customer info
                Text(
                    text = "PELANGGAN:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryMuted
                )
                Text(
                    text = trans.memberName ?: "Walk-in Customer (Bukan Member)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                if (trans.memberPhone != null) {
                    Text(
                        text = "Phone: ${trans.memberPhone}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFFF0EAEA))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "DAFTAR BARANG YANG DIBELI:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                    items(relation.items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Text(
                                    text = "${item.quantity} x Rp ${viewModel.numberFormat(item.priceAtPurchase)}",
                                    fontSize = 10.sp,
                                    color = TextSecondaryMuted
                                )
                            }
                            Text(
                                "Rp ${viewModel.numberFormat(item.priceAtPurchase * item.quantity)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFFF0EAEA))
                Spacer(modifier = Modifier.height(10.dp))

                // Calculations list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal:", fontSize = 12.sp, color = TextSecondaryMuted)
                    Text("Rp ${viewModel.numberFormat(trans.totalPrice)}", fontSize = 12.sp, color = TextPrimaryDark)
                }
                if (trans.discountPrice > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Diskon Member (5%):", fontSize = 12.sp, color = AccentGreen)
                        Text("-Rp ${viewModel.numberFormat(trans.discountPrice)}", fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Bersih:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Text(
                        "Rp ${viewModel.numberFormat(trans.finalPrice)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPink
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tunai:", fontSize = 12.sp, color = TextSecondaryMuted)
                    Text("Rp ${viewModel.numberFormat(trans.cashAmount)}", fontSize = 12.sp, color = TextPrimaryDark)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kembalian:", fontSize = 12.sp, color = TextSecondaryMuted)
                    Text(
                        "Rp ${viewModel.numberFormat(trans.changeAmount)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("close_invoice_detail_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tutup Struk")
                }
            }
        }
    }
}

// E. Shopping Cart Checkout panel
@Composable
fun CartCheckoutDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val cartList by viewModel.cart.collectAsStateWithLifecycle()
    val subtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()
    val discount by viewModel.cartDiscount.collectAsStateWithLifecycle()
    val total by viewModel.cartTotal.collectAsStateWithLifecycle()
    val change by viewModel.checkoutChange.collectAsStateWithLifecycle()

    val phone by viewModel.checkoutMemberPhone.collectAsStateWithLifecycle()
    val name by viewModel.checkoutMemberName.collectAsStateWithLifecycle()
    val cashStr by viewModel.checkoutCashAmount.collectAsStateWithLifecycle()
    val isFound by viewModel.isMemberFound.collectAsStateWithLifecycle()
    val foundName by viewModel.foundMemberName.collectAsStateWithLifecycle()
    val autoRegister by viewModel.autoRegisterMember.collectAsStateWithLifecycle()

    var checkoutErrorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("cart_checkout_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                Text(
                    text = "Keranjang Kasir & Pembayaran",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (cartList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .height(150.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Keranjang belanja kosong.",
                            fontSize = 13.sp,
                            color = TextSecondaryMuted
                        )
                    }
                } else {
                    // Cart list scrolling
                    Box(modifier = Modifier.heightIn(max = 140.dp)) {
                        LazyColumn {
                            items(cartList) { (prod, qty) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                        Text("Rp ${viewModel.numberFormat(prod.price)}", fontSize = 10.sp, color = PrimaryPink)
                                    }
                                    
                                    // Adjusters
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.decreaseQuantity(prod) },
                                            modifier = Modifier.size(24.dp).testTag("cart_minus_${prod.id}")
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(14.dp))
                                        }
                                        Text(text = "$qty", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        IconButton(
                                            onClick = { viewModel.addToCart(prod) },
                                            modifier = Modifier.size(24.dp).testTag("cart_plus_${prod.id}")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.removeFromCart(prod) },
                                            modifier = Modifier.size(24.dp).testTag("cart_delete_${prod.id}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = AlertRed, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                Divider(color = Color(0xFFFCF8F8))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFF0EAEA))
                Spacer(modifier = Modifier.height(10.dp))

                // MEMBER DETECTOR (AUTOMATIC SYSTEM)
                Text(
                    text = "PENCATATAN MEMBER OTOMATIS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryMuted
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { viewModel.checkoutMemberPhone.value = it },
                        label = { Text("No HP Member", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("checkout_member_phone"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.checkoutMemberName.value = it },
                        label = { Text("Nama Pelanggan", fontSize = 11.sp) },
                        singleLine = true,
                        enabled = !isFound,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("checkout_member_name"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Member lookup indicator banner
                if (isFound && foundName != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = AccentGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "GlowPoint Member VIP: $foundName (Selamat, Diskon 5% Aktif!)",
                                color = AccentGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (phone.isNotBlank() && phone.length >= 4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "New Phone", tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Phone belum terdaftar. Akan disimpan otomatis setelah checkout!",
                                color = Color(0xFFE65100),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFF0EAEA))
                Spacer(modifier = Modifier.height(10.dp))

                // Calculations recap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal:", fontSize = 12.sp, color = TextSecondaryMuted)
                    Text("Rp ${viewModel.numberFormat(subtotal)}", fontSize = 12.sp, color = TextPrimaryDark)
                }
                if (discount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Diskon Member VIP (5%):", fontSize = 12.sp, color = AccentGreen)
                        Text("-Rp ${viewModel.numberFormat(discount)}", fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Belanja:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Text(
                        "Rp ${viewModel.numberFormat(total)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryPink
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cash input field
                OutlinedTextField(
                    value = cashStr,
                    onValueChange = { viewModel.checkoutCashAmount.value = it },
                    label = { Text("Jumlah Uang Tunai (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_cash_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = Color(0xFFF0EAEA)
                    )
                )

                if (cashStr.isNotBlank()) {
                    val cashVal = cashStr.toDoubleOrNull() ?: 0.0
                    if (cashVal >= total) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kembalian Pelanggan:", fontSize = 12.sp, color = TextSecondaryMuted)
                            Text(
                                "Rp ${viewModel.numberFormat(change)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentBlue
                            )
                        }
                    } else {
                        Text(
                            text = "Uang tunai kurang dari total belanja!",
                            color = AlertRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                checkoutErrorMsg?.let {
                    Text(
                        text = it,
                        color = AlertRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val ok = viewModel.completeCheckout()
                            if (ok) {
                                checkoutErrorMsg = null
                                onDismiss()
                            } else {
                                checkoutErrorMsg = "Gagal checkout. Cek input tunai & isi keranjang!"
                            }
                        },
                        enabled = cartList.isNotEmpty() && (cashStr.toDoubleOrNull() ?: 0.0) >= total,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("checkout_pay_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("BAYAR TUNAI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
