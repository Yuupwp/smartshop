package com.example.smartshop

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.smartshop.database.SmartShopRepository
import com.google.accompanist.permissions.*

val AzulPrincipal = Color(0xFF2962FF)
val AzulOscuro = Color(0xFF17233D)
val FondoApp = Color(0xFFF3F6FB)
val TextoGris = Color(0xFF7A8499)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NuevaVentaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SmartShopRepository(context) }
    var mostrarCamara by remember { mutableStateOf(false) }
    var mostrarLector by remember { mutableStateOf(false) }
    var codigoLector by remember { mutableStateOf("") }
    var mostrarTicket by remember { mutableStateOf(false) }
    var ticketDetalle by remember { mutableStateOf("") }
    var ticketTotal by remember { mutableStateOf(0) }
    val productos = remember { mutableStateListOf<ProductoVenta>() }

    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val db = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    val userId = auth.currentUser?.uid

    // Cargar productos desde Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("tiendas").document(userId).collection("productos")
                .get() // Usamos get() en vez de snapshot para que la lista no brinque mientras vendes
                .addOnSuccessListener { snapshot ->
                    productos.clear()
                    snapshot.documents.forEach { doc ->
                        productos.add(
                            ProductoVenta(
                                nombre = doc.getString("nombre") ?: "",
                                precio = doc.getDouble("precio")?.toInt() ?: 0,
                                stockInicial = doc.getLong("stock")?.toInt() ?: 0,
                                codigo = doc.id, // Guardamos el ID larguísimo de Firebase aquí
                                codigoBarras = doc.getString("codigoBarras") ?: ""
                            )
                        )
                    }
                }
        }
    }

    var total by remember { mutableStateOf(0) }
    var cantidadProductos by remember { mutableStateOf(0) }

    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    // Configuración del Escáner de Google (ML Kit)
    val scannerOptions = remember {
        com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
            .enableAutoZoom()
            .build()
    }
    val scanner = remember { com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context, scannerOptions) }

    // Función Cerebro: Busca el código y hace la matemática
    val procesarCodigoEscaneado = { codigoBuscado: String ->
        val prod = productos.find { it.codigoBarras == codigoBuscado }
        if (prod != null) {
            if (prod.stock > 0) {
                prod.stock--
                total += prod.precio
                cantidadProductos++
                android.widget.Toast.makeText(context, "Agregado: ${prod.nombre}", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "¡Sin stock de ${prod.nombre}!", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "Producto no encontrado", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = FondoApp
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Regresar",
                        tint = Color(0xFF2A3950),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Nueva venta",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulOscuro
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            ResumenVenta(total, cantidadProductos)

            Spacer(modifier = Modifier.height(16.dp))

            MetodoEscaneoModerno(
                onCamaraClick = {
                    // Abrimos el escáner de Google directo
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            val rawValue = barcode.rawValue ?: ""
                            procesarCodigoEscaneado(rawValue)
                        }
                        .addOnFailureListener { e ->
                            android.widget.Toast.makeText(context, "Error cámara: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                },
                onLectorClick = { mostrarLector = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            BuscadorModerno()

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Productos disponibles",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulOscuro
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn {
                        items(productos) { producto ->
                            ProductoItemModerno(producto) {
                                if (producto.stock > 0) {
                                    producto.stock--
                                    total += producto.precio
                                    cantidadProductos++
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (userId != null) {
                        // Creamos un "Lote" de operaciones (Batch)
                        val batch = db.batch()

                        // 1. Preparamos el registro de la nueva venta
                        val nuevaVentaRef = db.collection("tiendas").document(userId)
                            .collection("ventas").document() // Documento vacío con ID automático

                        val ventaData = hashMapOf(
                            "total" to total.toDouble(),
                            "fecha" to System.currentTimeMillis(),
                            "cantidadProductos" to cantidadProductos
                        )
                        batch.set(nuevaVentaRef, ventaData) // Agregamos la creación al lote

                        // 2. Preparamos los descuentos de stock
                        // Filtramos solo los productos a los que les dimos click en "+"
                        productos.filter { it.stock < it.stockInicial }.forEach { prod ->
                            val cantidadVendida = prod.stockInicial - prod.stock

                            val prodRef = db.collection("tiendas").document(userId)
                                .collection("productos").document(prod.codigo)

                            // Le decimos a Firebase: "Réscale esta cantidad al stock actual"
                            batch.update(
                                prodRef,
                                "stock",
                                com.google.firebase.firestore.FieldValue.increment(-cantidadVendida.toLong())
                            )
                        }

                        // 3. ¡Ejecutamos todo el lote de un jalón!
                        batch.commit()
                            .addOnSuccessListener {
                                // En lugar de regresarnos, preparamos el texto del ticket
                                val textoTicket = productos.filter { it.stock < it.stockInicial }
                                    .joinToString(separator = "\n") { prod ->
                                        val cant = prod.stockInicial - prod.stock
                                        val subtotal = cant * prod.precio
                                        "$cant x ${prod.nombre} - $$subtotal"
                                    }

                                ticketDetalle = textoTicket
                                ticketTotal = total
                                mostrarTicket = true // Esto hace que salte la ventana
                            }
                            .addOnFailureListener { e ->
                                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp),
                enabled = cantidadProductos > 0,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2A3950),
                    disabledContainerColor = Color(0xFFB7C7F5)
                )
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null)

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Confirmar venta - $$total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }

    if (mostrarCamara) {
        ModalBottomSheet(
            onDismissRequest = { mostrarCamara = false }
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Escanear código de barras",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = AzulOscuro
                )

                Text(
                    "Usando cámara del dispositivo",
                    color = TextoGris
                )

                Spacer(modifier = Modifier.height(20.dp))

                CameraPreview()

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A3950)
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar escaneo")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (mostrarLector) {
        ModalBottomSheet(
            onDismissRequest = { mostrarLector = false }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Escanear código de barras", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = AzulOscuro)
                Text("Usando lector externo", color = TextoGris)

                Spacer(modifier = Modifier.height(20.dp))

                // CAMPO DE TEXTO
                OutlinedTextField(
                    value = codigoLector,
                    onValueChange = { codigoLector = it },
                    placeholder = { Text("Código aparecerá aquí...") }, // Las llaves { } son vitales aquí
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // BOTÓN DE CONFIRMACIÓN
                Button(
                    onClick = {
                        if (codigoLector.isNotBlank()) {
                            procesarCodigoEscaneado(codigoLector.trim())
                            codigoLector = "" // Limpia el campo para el siguiente producto
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulOscuro)
                ) {
                    Text("Confirmar código")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
    // --- VENTANA DEL TICKET DIGITAL ---
    if (mostrarTicket) {
        AlertDialog(
            onDismissRequest = { /* Lo dejamos vacío para obligarlo a picarle a Cerrar */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF35C76F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("¡Venta Exitosa!", fontWeight = FontWeight.Bold, color = AzulOscuro)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp)).padding(16.dp)
                ) {
                    Text("--- TICKET DE COMPRA ---", fontWeight = FontWeight.Bold, color = TextoGris, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(ticketDetalle, fontSize = 16.sp, color = AzulOscuro)

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL:", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AzulOscuro)
                        Text("$$ticketTotal", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AzulPrincipal)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarTicket = false
                        onBack() // AHORA SÍ nos regresamos al inicio
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
                ) {
                    Text("Cerrar y Volver")
                }
            },
            containerColor = Color.White
        )
    }
}

class ProductoVenta(
    val nombre: String,
    val precio: Int,
    val stockInicial: Int,
    val codigo: String,
    val codigoBarras: String = ""
) {
    var stock by mutableStateOf(stockInicial)
}

@Composable
fun ResumenVenta(total: Int, cantidad: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Resumen de la venta",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AzulOscuro
            )

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color(0xFFE8F8EF), RoundedCornerShape(35.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF35C76F),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column {
                    Text(
                        text = "$$total",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulOscuro
                    )

                    Text(
                        text = "Total a pagar",
                        fontSize = 18.sp,
                        color = TextoGris
                    )
                }
            }
        }
    }
}

@Composable
fun MetodoEscaneoModerno(
    onCamaraClick: () -> Unit,
    onLectorClick: () -> Unit
) {
    var metodoActivo by remember { mutableStateOf("camara") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Método de escaneo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AzulOscuro
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        metodoActivo = "camara"
                        onCamaraClick()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.dp, Color.Transparent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor =
                            if (metodoActivo == "camara") Color(0xFF94A8C7)
                            else Color(0xFFE8E8E8),
                        contentColor =
                            if (metodoActivo == "camara") Color(0xFF2A3950)
                            else TextoGris
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Cámara", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(14.dp))

                OutlinedButton(
                    onClick = {
                        metodoActivo = "lector"
                        onLectorClick()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.dp, Color.Transparent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor =
                            if (metodoActivo == "lector") Color(0xFF94A8C7)
                            else Color(0xFFE8E8E8),
                        contentColor =
                            if (metodoActivo == "lector") Color(0xFF2A3950)
                            else TextoGris
                    )
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Lector", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BuscadorModerno() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = {
                Text(
                    "Buscar producto...",
                    color = TextoGris
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = TextoGris
                )
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
    }
}

@Composable
fun ProductoItemModerno(producto: ProductoVenta, onAgregar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Color(0xFFEAF0FF), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = Color(0xFF2A3950)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = producto.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulOscuro
                )

                Row {
                    Text(
                        text = "$${producto.precio}",
                        color = Color(0xFF2A3950),
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "  •  Stock: ${producto.stock}",
                        color = TextoGris
                    )
                }
            }

            IconButton(
                onClick = onAgregar,
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFF94A8C7), RoundedCornerShape(25.dp))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Agregar",
                    tint = Color(0xFF2A3950)
                )
            }
        }
    }
}

@Composable
fun CameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}
