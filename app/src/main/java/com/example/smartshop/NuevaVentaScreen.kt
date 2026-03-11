package com.example.smartshop

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.smartshop.database.SmartShopRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*

import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NuevaVentaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SmartShopRepository(context) }

    var mostrarCamara by remember { mutableStateOf(false) }
    var mostrarLector by remember { mutableStateOf(false) }

    val productos = remember {
        mutableStateListOf<ProductoVenta>()
    }

    LaunchedEffect(Unit) {

        val productosDB = repo.obtenerProductos()

        productos.clear()

        productosDB.forEach {

            productos.add(
                ProductoVenta(
                    nombre = it["nombre"] as String,
                    precio = (it["precio"] as Double).toInt(),
                    stockInicial = it["stock"] as Int,
                    codigo = (it["id"] as Int).toString()
                )
            )
        }
    }

    var total by remember { mutableStateOf(0) }
    var cantidadProductos by remember { mutableStateOf(0) }

    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva venta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2962FF),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            MetodoEscaneo(
                onCamaraClick = {

                    if (cameraPermissionState.status.isGranted) {
                        mostrarCamara = true
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }

                },
                onLectorClick = { mostrarLector = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TarjetaTotal(total, cantidadProductos)

            Spacer(modifier = Modifier.height(16.dp))

            Buscador()

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(productos) { producto ->

                    ProductoItem(producto) {

                        if (producto.stock > 0) {
                            producto.stock--
                            total += producto.precio
                            cantidadProductos++
                        }
                    }
                }
            }

            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                enabled = cantidadProductos > 0
            ) {
                Text("Confirmar venta - $$total")
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
                    fontSize = 20.sp
                )

                Text("Usando cámara del dispositivo")

                Spacer(modifier = Modifier.height(20.dp))

                CameraPreview()

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
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

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    "Escanear código de barras",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Text("Usando lector externo")

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Color.LightGray,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.Blue,
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Lector externo listo")

                    Text(
                        "Escanee el código con su lector Bluetooth o USB",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Código aparecerá aquí...") }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirmar código")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}


class ProductoVenta(
    val nombre: String,
    val precio: Int,
    stockInicial: Int,
    val codigo: String
) {
    var stock by mutableStateOf(stockInicial)
}

@Composable
fun MetodoEscaneo(
    onCamaraClick: () -> Unit,
    onLectorClick: () -> Unit
) {

    var metodoActivo by remember { mutableStateOf("camara") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE9EAEC)
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "Método de escaneo activo:",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Button(
                    onClick = {
                        metodoActivo = "camara"
                        onCamaraClick()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (metodoActivo == "camara")
                                Color(0xFF2962FF)
                            else
                                Color.LightGray
                    )
                ) {

                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Camara",
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    Text("Cámara")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        metodoActivo = "lector"
                        onLectorClick()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (metodoActivo == "lector")
                                Color(0xFF2962FF)
                            else
                                Color.LightGray
                    )
                ) {

                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Lector"
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text("Lector")
                }
            }
        }
    }
}

@Composable
fun TarjetaTotal(total: Int, cantidad: Int) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2962FF)
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "Total a pagar",
                color = Color.White
            )

            Text(
                text = "$$total",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "$cantidad productos en el carrito",
                color = Color.White
            )
        }
    }
}

@Composable
fun Buscador() {
    OutlinedTextField(
        value = "",
        onValueChange = {},
        placeholder = { Text("Buscar producto...") }
    )
}

@Composable
fun ProductoItem(producto: ProductoVenta, onAgregar: () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column {
            Text(producto.nombre)
            Text("$${producto.precio} - Stock ${producto.stock}")
            Text(producto.codigo)
        }

        IconButton(onClick = onAgregar) {
            Icon(Icons.Default.Add, contentDescription = "Agregar")
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
