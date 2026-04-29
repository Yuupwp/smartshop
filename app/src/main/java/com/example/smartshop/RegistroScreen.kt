package com.example.smartshop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    onBackClick: () -> Unit,
    onCrearCuentaClick: (String, String, String, String, String, String) -> Unit,
    onLoginClick: () -> Unit
) {
    // Estados para guardar los datos del formulario
    var nombreResponsable by remember { mutableStateOf("") }
    var nombreTienda by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Paleta de colores
    val colorFondo = Color(0xFFF8F9FA)
    val colorPrincipal = Color(0xFF2A3950)
    val colorTextoSecundario = Color(0xFF7A8499)
    val colorDivisor = Color(0xFFE8ECF2)
    val colorIconoFondo = Color(0xFFEEF2FF)
    val colorIcono = Color(0xFF5C6BC0)

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFondo)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(24.dp)) // Margen superior seguro

        // --- TOP BAR PERSONALIZADO ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = colorPrincipal
                )
            }
            Text(
                text = "Registra tu tienda",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorPrincipal
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TARJETA DEL FORMULARIO ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícono Superior
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(colorIconoFondo, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = colorIcono,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Completa los siguientes datos para crear la cuenta de tu negocio.",
                    fontSize = 14.sp,
                    color = colorTextoSecundario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- SECCIÓN 1: DATOS GENERALES ---
                FormFieldRegistros(
                    label = "Nombre completo del responsable",
                    placeholder = "Ej: Juan Pérez",
                    value = nombreResponsable,
                    onValueChange = { nombreResponsable = it },
                    icon = Icons.Outlined.Person
                )

                Spacer(modifier = Modifier.height(20.dp))

                FormFieldRegistros(
                    label = "Nombre de la tienda de abarrotes",
                    placeholder = "Ej: Abarrotes La Esperanza",
                    value = nombreTienda,
                    onValueChange = { nombreTienda = it },
                    icon = Icons.Default.Storefront
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = colorDivisor)
                Spacer(modifier = Modifier.height(24.dp))

                // --- SECCIÓN 2: CONTACTO Y UBICACIÓN ---
                FormFieldRegistros(
                    label = "Correo electrónico",
                    placeholder = "tu@correo.com",
                    value = correo,
                    onValueChange = { correo = it },
                    icon = Icons.Outlined.MailOutline,
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(20.dp))

                FormFieldRegistros(
                    label = "Teléfono de contacto",
                    placeholder = "10 dígitos",
                    value = telefono,
                    onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) telefono = it },
                    icon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(20.dp))

                FormFieldRegistros(
                    label = "Dirección completa del local",
                    placeholder = "Calle, número, colonia y código postal",
                    value = direccion,
                    onValueChange = { direccion = it },
                    icon = Icons.Default.LocationOn,
                    singleLine = false,
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = colorDivisor)
                Spacer(modifier = Modifier.height(24.dp))

                // --- SECCIÓN 3: SEGURIDAD ---
                FormFieldRegistros(
                    label = "Contraseña",
                    placeholder = "••••••••",
                    value = password,
                    onValueChange = { password = it },
                    icon = Icons.Default.Lock,
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                FormFieldRegistros(
                    label = "Confirmar contraseña",
                    placeholder = "••••••••",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    icon = Icons.Default.Lock,
                    isPassword = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTÓN CREAR CUENTA ---
        Button(
            onClick = {
                // TODO: Validar que las contraseñas coincidan antes de enviar
                onCrearCuentaClick(correo, password, nombreResponsable, nombreTienda, telefono, direccion)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorPrincipal)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Crear cuenta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- BOTÓN YA TIENES CUENTA ---
        OutlinedButton(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorPrincipal),
            border = androidx.compose.foundation.BorderStroke(1.dp, colorDivisor)
        ) {
            Text("¿Ya tienes cuenta? Inicia sesión aquí", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp)) // Espacio final para que el scroll no quede pegado abajo
    }
}

// COMPONENTE REUTILIZABLE PARA LOS CAMPOS DE TEXTO
@Composable
fun FormFieldRegistros(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    val colorPrincipal = Color(0xFF2A3950)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorPrincipal
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.LightGray) },
            leadingIcon = {
                Icon(icon, contentDescription = null, tint = Color.LightGray)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            minLines = minLines,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE8ECF2),
                focusedBorderColor = colorPrincipal,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RegistroScreenPreview() {
    RegistroScreen(
        onBackClick = {},
        onCrearCuentaClick = { _, _, _, _, _, _ -> },
        onLoginClick = {}
    )
}