package com.example.smartshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartshop.database.SmartShopRepository
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


enum class Pantalla { HOME, INVENTARIO }

@Composable
fun AppNavigation() {
    var pantallaActual by remember { mutableStateOf(Pantalla.HOME) }
    when (pantallaActual) {
        Pantalla.HOME -> HomeScreen(onVerInventario = { pantallaActual = Pantalla.INVENTARIO })
        Pantalla.INVENTARIO -> InventarioScreen(onBack = { pantallaActual = Pantalla.HOME })
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
fun HomeScreen(onVerInventario: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SmartShopRepository(context) }

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val inicioDia = calendar.timeInMillis
    val finDia = inicioDia + 86_400_000L

    val totalVentas by remember { mutableStateOf(repo.totalVentasDelDia(inicioDia, finDia)) }
    val cantVentas by remember { mutableStateOf(repo.contarVentasDelDia(inicioDia, finDia)) }
    val productosConPocoStock by remember {
        mutableStateOf(
            repo.obtenerProductos()
                .filter { (it["stock"] as Int) <= (it["stock_minimo"] as Int) }
                .map {
                    Producto(
                        id = it["id"] as Int,
                        nombre = it["nombre"] as String,
                        precio = it["precio"] as Double,
                        stock = it["stock"] as Int,
                        stockMinimo = it["stock_minimo"] as Int
                    )
                }
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
            QuickActionsSection(onVerInventario = onVerInventario)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
fun InventarioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SmartShopRepository(context) }

    var todosLosProductos by remember { mutableStateOf(cargarProductos(repo)) }
    var busqueda by remember { mutableStateOf("") }
    var mostrarDialogoAgregar by remember { mutableStateOf(false) }
    var productoAEditar by remember { mutableStateOf<Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }

    val productosFiltrados = remember(todosLosProductos, busqueda) {
        if (busqueda.isBlank()) todosLosProductos
        else todosLosProductos.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Inventario", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF333333),
                    navigationIconContentColor = Color(0xFF333333)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogoAgregar = true },
                containerColor = Color(0xFF2962FF),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text("Buscar producto...", color = Color(0xFFAAAAAA)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFAAAAAA))
                },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { busqueda = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color(0xFFAAAAAA))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color(0xFF2962FF),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "${productosFiltrados.size} producto${if (productosFiltrados.size != 1) "s" else ""}",
                fontSize = 14.sp,
                color = Color(0xFF888888)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (productosFiltrados.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null,
                            modifier = Modifier.size(64.dp), tint = Color(0xFFBBBBBB))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (busqueda.isBlank()) "Sin productos aún" else "Sin resultados para \"$busqueda\"",
                            color = Color(0xFF888888), fontSize = 16.sp
                        )
                        if (busqueda.isBlank()) {
                            Text("Presiona + para agregar uno", color = Color(0xFFAAAAAA), fontSize = 14.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(productosFiltrados, key = { it.id }) { producto ->
                        ProductoCard(
                            producto = producto,
                            onEditar = { productoAEditar = producto },
                            onEliminar = { productoAEliminar = producto }
                        )
                    }
                }
            }
        }
    }

    if (mostrarDialogoAgregar) {
        ProductoDialog(
            titulo = "Agregar producto",
            onDismiss = { mostrarDialogoAgregar = false },
            onConfirmar = { nombre, precio, stock, stockMin ->
                repo.insertarProducto(nombre, precio, stock, stockMin)
                todosLosProductos = cargarProductos(repo)
                mostrarDialogoAgregar = false
            }
        )
    }

    productoAEditar?.let { prod ->
        ProductoDialog(
            titulo = "Editar producto",
            productoExistente = prod,
            onDismiss = { productoAEditar = null },
            onConfirmar = { nombre, precio, stock, stockMin ->
                repo.actualizarProducto(prod.id, nombre, precio, stock, stockMin)
                todosLosProductos = cargarProductos(repo)
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
                TextButton(onClick = {
                    repo.eliminarProducto(prod.id)
                    todosLosProductos = cargarProductos(repo)
                    productoAEliminar = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}


@Composable
fun ProductoCard(
    producto: Producto,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val stockBajo = producto.stock <= producto.stockMinimo

    val (badgeBackground, badgeText) = when {
        producto.stock == 0 -> Color(0xFFFFE0E0) to Color(0xFFE53935)
        stockBajo           -> Color(0xFFFFF3CD) to Color(0xFFFF8F00)
        else                -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (stockBajo) 1.5.dp else 0.dp,
                color = if (stockBajo) Color(0xFFFFCC02) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (stockBajo) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = producto.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF222222),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar",
                        tint = Color(0xFF888888), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                        tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                }
            }

            Text(
                text = "$${"%.2f".format(producto.precio)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2962FF)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⠿", fontSize = 12.sp, color = Color(0xFFBBBBBB))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ID-${producto.id.toString().padStart(10, '0')}",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Stock:", fontSize = 14.sp, color = Color(0xFF666666))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(badgeBackground, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${producto.stock} unidades",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = badgeText
                        )
                    }
                }
                if (stockBajo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null,
                            tint = Color(0xFFFF8F00), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bajo", fontSize = 13.sp,
                            fontWeight = FontWeight.Medium, color = Color(0xFFFF8F00))
                    }
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
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre del producto") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = precio, onValueChange = { precio = it },
                    label = { Text("Precio ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = stock, onValueChange = { stock = it },
                    label = { Text("Stock actual") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = stockMin, onValueChange = { stockMin = it },
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
            SummaryCard(modifier = Modifier.weight(1f), icon = Icons.Default.Star,
                iconColor = Color(0xFF4CAF50), value = "$${"%.2f".format(total)}",
                label = "Ventas totales")
            SummaryCard(modifier = Modifier.weight(1f), icon = Icons.Default.ShoppingCart,
                iconColor = Color(0xFF2962FF), value = "$cantidad",
                label = "Ventas realizadas")
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
                Text(
                    "${productos.size} producto${if (productos.size != 1) "s" else ""} con poco stock",
                    fontSize = 14.sp, color = Color(0xFF666666)
                )
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
fun QuickActionsSection(onVerInventario: () -> Unit) {
    Column {
        Text("Acciones rápidas", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(12.dp))
        ActionButton(
            title = "Registrar venta", subtitle = "Nueva transacción",
            icon = Icons.Default.ShoppingCart, backgroundColor = Color(0xFF2962FF),
            contentColor = Color.White, showArrow = true, onClick = {}
        )
        Spacer(modifier = Modifier.height(12.dp))
        ActionButton(
            title = "Ver inventario", subtitle = "Gestiona tus productos",
            icon = Icons.Default.Home, backgroundColor = Color.White,
            contentColor = Color(0xFF9C27B0), showArrow = true, onClick = onVerInventario
        )
        Spacer(modifier = Modifier.height(12.dp))
        ActionButton(
            title = "Reportes", subtitle = "Análisis de ventas",
            icon = Icons.Default.Info, backgroundColor = Color.White,
            contentColor = Color(0xFF4CAF50), showArrow = false, onClick = {}
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
                Icon(Icons.Default.ArrowForward, contentDescription = null,
                    tint = contentColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SmartShopTheme {
        HomeScreen(onVerInventario = {})
    }
}
