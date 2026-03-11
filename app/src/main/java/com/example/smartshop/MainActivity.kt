package com.example.smartshop

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartshop.database.SmartShopRepository
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartShopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}


@Composable
fun SmartShopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2962FF),
            background = Color(0xFFF5F5F5)
        ),
        content = content
    )
}


data class Producto(
    val id: Int = 0,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val stockMinimo: Int = 5
)


enum class Pantalla { HOME, INVENTARIO, AGREGAR_PRODUCTO, REPORTES }

@Composable
fun AppNavigation() {
    var pantallaActual by remember { mutableStateOf(Pantalla.HOME) }

    when (pantallaActual) {
        Pantalla.HOME -> HomeScreen(
            onVerInventario = { pantallaActual = Pantalla.INVENTARIO },
            onVerReportes = { pantallaActual = Pantalla.REPORTES }
        )
        Pantalla.INVENTARIO -> InventarioScreen(
            onBack = { pantallaActual = Pantalla.HOME },
            onAgregarProducto = { pantallaActual = Pantalla.AGREGAR_PRODUCTO }
        )
        Pantalla.AGREGAR_PRODUCTO -> AgregarProductoScreen(
            onBack = { pantallaActual = Pantalla.INVENTARIO }
        )
        Pantalla.REPORTES -> ReportesScreen(
            onBack = { pantallaActual = Pantalla.HOME }
        )
    }
}


@Composable
fun HomeScreen(onVerInventario: () -> Unit, onVerReportes: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SmartShopRepository(context) }

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
    val inicioDia = calendar.timeInMillis
    val finDia = inicioDia + 86_400_000L

    val totalVentas by remember { mutableDoubleStateOf(repo.totalVentasDelDia(inicioDia, finDia)) }
    val cantVentas by remember { mutableIntStateOf(repo.contarVentasDelDia(inicioDia, finDia)) }
    val productosConPocoStock by remember {
        mutableStateOf(
            repo.obtenerProductos()
                .filter { (it["stock"] as Int) <= (it["stock_minimo"] as Int) }
                .map { Producto(
                    id = it["id"] as Int,
                    nombre = it["nombre"] as String,
                    precio = it["precio"] as Double,
                    stock = it["stock"] as Int,
                    stockMinimo = it["stock_minimo"] as Int
                )}
        )
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFF5F5F5))
    ) {
        HeaderSection()
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            SummarySection(total = totalVentas, cantidad = cantVentas)
            Spacer(modifier = Modifier.height(24.dp))
            if (productosConPocoStock.isNotEmpty()) {
                LowStockSection(productos = productosConPocoStock)
                Spacer(modifier = Modifier.height(24.dp))
            }
            QuickActionsSection(onVerInventario = onVerInventario, onVerReportes = onVerReportes)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
fun InventarioScreen(onBack: () -> Unit, onAgregarProducto: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SmartShopRepository(context) }

    var productos by remember { mutableStateOf(cargarProductos(repo)) }
    var productoAEditar by remember { mutableStateOf<Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Inventario", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2962FF),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAgregarProducto,
                containerColor = Color(0xFF2962FF)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
            }
        }
    ) { paddingValues ->

        if (productos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null,
                        modifier = Modifier.size(64.dp), tint = Color(0xFFBBBBBB))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Sin productos aún", color = Color(0xFF888888), fontSize = 16.sp)
                    Text("Presiona + para agregar uno", color = Color(0xFFAAAAAA), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(productos, key = { it.id }) { producto ->
                    ProductoCard(
                        producto = producto,
                        onEditar = { productoAEditar = producto },
                        onEliminar = { productoAEliminar = producto }
                    )
                }
            }
        }
    }

    productoAEditar?.let { prod ->
        ProductoDialog(
            titulo = "Editar producto",
            productoExistente = prod,
            onDismiss = { productoAEditar = null },
            onConfirmar = { nombre, precio, stock, stockMin ->
                repo.actualizarProducto(prod.id, nombre, precio, stock, stockMin)
                productos = cargarProductos(repo)
                productoAEditar = null
            }
        )
    }

    productoAEliminar?.let { prod ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Deseas eliminar \"${prod.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        repo.eliminarProducto(prod.id)
                        productos = cargarProductos(repo)
                        productoAEliminar = null
                    }
                ) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}


fun cargarProductos(repo: SmartShopRepository): List<Producto> =
    repo.obtenerProductos().map {
        Producto(
            id = it["id"] as Int,
            nombre = it["nombre"] as String,
            precio = it["precio"] as Double,
            stock = it["stock"] as Int,
            stockMinimo = it["stock_minimo"] as Int
        )
    }


@Composable
fun ProductoCard(
    producto: Producto,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val stockBajo = producto.stock <= producto.stockMinimo
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(4.dp))
                Text("$${String.format(Locale.getDefault(), "%.2f", producto.precio)}",
                    fontSize = 15.sp, color = Color(0xFF2962FF), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Stock: ${producto.stock} unidades",
                    fontSize = 13.sp,
                    color = if (stockBajo) Color(0xFFFF9800) else Color(0xFF666666),
                    fontWeight = if (stockBajo) FontWeight.Medium else FontWeight.Normal
                )
            }
            Row {
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar",
                        tint = Color(0xFF2962FF))
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                        tint = Color(0xFFE53935))
                }
            }
        }
    }
}

@Composable
fun ProductoDialog(
    titulo: String,
    productoExistente: Producto? = null,
    onDismiss: () -> Unit,
    onConfirmar: (String, Double, Int, Int) -> Unit
) {
    var nombre by remember { mutableStateOf(productoExistente?.nombre ?: "") }
    var precio by remember { mutableStateOf(productoExistente?.precio?.toString() ?: "") }
    var stock by remember { mutableStateOf(productoExistente?.stock?.toString() ?: "") }
    var stockMin by remember { mutableStateOf(productoExistente?.stockMinimo?.toString() ?: "5") }
    var error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del producto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = precio,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            precio = input
                        }
                    },
                    label = { Text("Precio ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = stock,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            stock = input
                        }
                    },
                    label = { Text("Stock actual") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = stockMin,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            stockMin = input
                        }
                    },
                    label = { Text("Stock mínimo") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                if (error.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error, color = Color.Red, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val precioVal = precio.toDoubleOrNull()
                            val stockVal = stock.toIntOrNull()
                            val stockMinVal = stockMin.toIntOrNull()
                            when {
                                nombre.isBlank() -> error = "El nombre es obligatorio"
                                precioVal == null || precioVal < 0 -> error = "Precio inválido"
                                stockVal == null || stockVal < 0 -> error = "Stock inválido"
                                stockMinVal == null || stockMinVal < 0 -> error = "Stock mínimo inválido"
                                else -> onConfirmar(nombre.trim(), precioVal, stockVal, stockMinVal)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
                    ) { Text("Guardar") }
                }
            }
        }
    }
}


@Composable
fun HeaderSection() {
    val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    val currentDate = dateFormat.format(Date())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF2962FF),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(24.dp)
    ) {
        Column {
            Text("SmartShop", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Gestión de negocio", fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(currentDate, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun SummarySection(total: Double, cantidad: Int) {
    Column {
        Text("Resumen del día", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,
                iconColor = Color(0xFF4CAF50),
                value = "$${String.format(Locale.getDefault(), "%.2f", total)}",
                label = "Ventas totales"
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ShoppingCart,
                iconColor = Color(0xFF2962FF),
                value = "$cantidad",
                label = "Ventas realizadas"
            )
        }
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null,
                tint = iconColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 14.sp, color = Color(0xFF666666))
        }
    }
}

@Composable
fun LowStockSection(productos: List<Producto>) {
    val visibles = productos.take(3)
    val extras = productos.size - visibles.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(IntrinsicSize.Min)
                    .background(Color(0xFFFF9800))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null,
                        tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Inventario bajo", fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("${productos.size} producto${if (productos.size != 1) "s" else ""} con poco stock",
                    fontSize = 14.sp, color = Color(0xFF666666))
                Spacer(modifier = Modifier.height(16.dp))
                visibles.forEach { producto ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(producto.nombre, fontSize = 14.sp, color = Color(0xFF333333))
                        Text("${producto.stock} unidades", fontSize = 14.sp,
                            fontWeight = FontWeight.Medium, color = Color(0xFFFF9800))
                    }
                }
                if (extras > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("+$extras más", fontSize = 14.sp,
                        fontWeight = FontWeight.Medium, color = Color(0xFF2962FF))
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(onVerInventario: () -> Unit, onVerReportes: () -> Unit) {
    Column {
        Text("Acciones rápidas", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(12.dp))
        ActionButton(
            title = "Registrar venta",
            subtitle = "Nueva transacción",
            icon = Icons.Default.ShoppingCart,
            backgroundColor = Color(0xFF2962FF),
            contentColor = Color.White,
            showArrow = true,
            onClick = {}
        )
        Spacer(modifier = Modifier.height(12.dp))
        ActionButton(
            title = "Ver inventario",
            subtitle = "Gestiona tus productos",
            icon = Icons.Default.Home,
            backgroundColor = Color.White,
            contentColor = Color(0xFF9C27B0),
            showArrow = true,
            onClick = onVerInventario
        )
        Spacer(modifier = Modifier.height(12.dp))
        ActionButton(
            title = "Reportes",
            subtitle = "Análisis de ventas",
            icon = Icons.Default.Info,
            backgroundColor = Color.White,
            contentColor = Color(0xFF4CAF50),
            showArrow = false,
            onClick = onVerReportes
        )
    }
}

@Composable
fun ActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    showArrow: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null,
                tint = contentColor, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = if (backgroundColor == Color.White) Color(0xFF333333) else contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle, fontSize = 14.sp,
                    color = if (backgroundColor == Color.White) Color(0xFF666666) else contentColor.copy(alpha = 0.8f)
                )
            }
            if (showArrow) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    tint = contentColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}

// Pantalla para agregar un nuevo producto
@Composable
fun AgregarProductoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SmartShopRepository(context) }

    // Estados para los campos de texto
    var nombre by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var stockMin by remember { mutableStateOf("10") }
    var codigoBarras by remember { mutableStateOf("") }

    // Configuración del escáner de códigos de barras de Google
    val scannerOptions = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .enableAutoZoom()
            .build()
    }
    val scanner = remember { GmsBarcodeScanning.getClient(context, scannerOptions) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Nuevo producto", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    // Botón para regresar a la pantalla anterior
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // En estas líneas se implementó el botón que guarda el producto en la base de datos
                Button(
                    onClick = {
                        val p = precio.toDoubleOrNull() ?: 0.0
                        val s = stock.toIntOrNull() ?: 0
                        val sm = stockMin.toIntOrNull() ?: 5
                        if (nombre.isNotBlank()) {
                            repo.insertarProducto(nombre, p, s, sm)
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar producto", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                // En estas líneas se implementó el botón que cancela la operación y regresa
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF333333)),
                    border = null
                ) {
                    Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF8F9FA))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Contenedor del ícono principal
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFE3F2FD), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.cart),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Campo para el nombre del producto
                    FieldWithLabel("Nombre del producto *") {
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            placeholder = { Text("Ej: Coca-Cola 600ml", color = Color.LightGray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8F9FA),
                                focusedContainerColor = Color(0xFFF8F9FA),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color(0xFF2962FF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo para el precio
                    FieldWithLabel("Precio *") {
                        OutlinedTextField(
                            value = precio,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' }) {
                                    precio = input
                                }
                            },
                            placeholder = { Text("0.00", color = Color.LightGray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            prefix = { Text("$ ", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8F9FA),
                                focusedContainerColor = Color(0xFFF8F9FA),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color(0xFF2962FF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo para la cantidad en stock
                    FieldWithLabel("Cantidad en stock *") {
                        OutlinedTextField(
                            value = stock,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    stock = input
                                }
                            },
                            placeholder = { Text("0", color = Color.LightGray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8F9FA),
                                focusedContainerColor = Color(0xFFF8F9FA),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color(0xFF2962FF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo para el stock mínimo
                    FieldWithLabel("Alerta de stock bajo") {
                        OutlinedTextField(
                            value = stockMin,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    stockMin = input
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8F9FA),
                                focusedContainerColor = Color(0xFFF8F9FA),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color(0xFF2962FF)
                            )
                        )
                        Text(
                            "Se mostrará una alerta cuando el stock sea menor o igual a este valor",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo para el código de barras
                    FieldWithLabel("Código de barras") {
                        OutlinedTextField(
                            value = codigoBarras,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    codigoBarras = input
                                }
                            },
                            placeholder = { Text("Ej: 7501055301492", color = Color.LightGray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8F9FA),
                                focusedContainerColor = Color(0xFFF8F9FA),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color(0xFF2962FF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de acción secundaria (Cámara y Lector)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // En estas líneas se implementó el botón para activar la cámara y escanear
                        Button(
                            onClick = {
                                scanner.startScan()
                                    .addOnSuccessListener { barcode ->
                                        codigoBarras = barcode.rawValue ?: ""
                                        Toast.makeText(context, "Código escaneado: $codigoBarras", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Error al escanear: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.camera),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // En estas líneas se implementó la función de escaneo de códigos de barras con la cámara
                            Text("Cámara", fontSize = 14.sp)
                        }
                        // En estas líneas se implementó el botón para activar el lector
                        Button(
                            onClick = { },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA020F0))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.scan),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lector", fontSize = 14.sp)
                        }
                    }
                    Text(
                        "Escanea o ingresa el código de barras del producto",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

// Pantalla que muestra los Reportes de ventas
@Composable
fun ReportesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SmartShopRepository(context) }

    // Configuración para obtener las ventas del día actual
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
    val inicioDia = calendar.timeInMillis
    val finDia = inicioDia + 86_400_000L

    // Obtención de datos reales de la base de datos
    val totalHoy = repo.totalVentasDelDia(inicioDia, finDia)
    val ventasHoy = repo.contarVentasDelDia(inicioDia, finDia)
    val promedioVenta = if (ventasHoy > 0) totalHoy / ventasHoy else 0.0

    // Formateo de la fecha actual en español
    val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    val currentDate = dateFormat.format(Date())

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Reportes", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    // Botón para regresar al Home
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF8F9FA))
                .padding(16.dp)
        ) {
            // Muestra la fecha seleccionada con un ícono de calendario
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(currentDate, color = Color.Gray, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // En esta sección se muestran las tarjetas con los totales del día
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ReportCard(
                    modifier = Modifier.weight(1f),
                    iconContent = {
                        Icon(
                            painter = painterResource(R.drawable.dollar),
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                    }, // Representa la cantidad en dinero vendido
                    backgroundColor = Color(0xFFE8F5E9),
                    value = "$${totalHoy.toInt()}",
                    label = "Total vendido hoy"
                )
                // Representa el número de ventas
                ReportCard(
                    modifier = Modifier.weight(1f),
                    iconContent = {
                        Icon(
                            painter = painterResource(R.drawable.box),
                            contentDescription = null,
                            tint = Color(0xFF2962FF),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    backgroundColor = Color(0xFFE3F2FD),
                    value = "$ventasHoy",
                    label = "Ventas realizadas"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta morada que resalta el promedio por cada venta realizada
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFA020F0))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Promedio por venta", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text("$${String.format(Locale.getDefault(), "%.2f", promedioVenta)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección de gráfico visual de ventas semanales
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Ventas de los últimos 7 días", fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        VentasChart()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de resumen de ventas de la parte inferior
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Resumen de ventas", fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No hay ventas registradas hoy", color = Color(0xFF2962FF))
                }
            }
        }
    }
}

// Componente reutilizable para las tarjetas pequeñas de reportes
@Composable
fun ReportCard(modifier: Modifier, iconContent: @Composable () -> Unit, backgroundColor: Color, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(36.dp).background(backgroundColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                iconContent()
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// Función que dibuja el gráfico de ventas utilizando Canvas
@Composable
fun VentasChart() {
    val days = listOf("mar", "mié", "jue", "vie", "sáb", "dom", "lun")
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val paddingLeft = 60f
        val paddingBottom = 60f
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom
        
        // Se dibujan las líneas horizontales de la cuadrícula (Eje Y: 0 a 4)
        for (i in 0..4) {
            val y = chartHeight - (chartHeight / 4) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(paddingLeft, y),
                end = Offset(width, y),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            // Se dibujan los números del Eje Y
            drawContext.canvas.nativeCanvas.drawText(
                i.toString(),
                10f,
                y + 10f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 30f
                }
            )
        }
        
        // Se dibujan las líneas verticales para los días de la semana (Eje X)
        val stepX = chartWidth / (days.size - 1)
        for (i in days.indices) {
            val x = paddingLeft + stepX * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(x, 0f),
                end = Offset(x, chartHeight),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            // Se dibujan los nombres de los días en el Eje X
            drawContext.canvas.nativeCanvas.drawText(
                days[i],
                x - 20f,
                height - 10f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 30f
                }
            )
        }
    }
}

@Composable
fun FieldWithLabel(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SmartShopTheme {
        HomeScreen(onVerInventario = {}, onVerReportes = {})
    }
}
