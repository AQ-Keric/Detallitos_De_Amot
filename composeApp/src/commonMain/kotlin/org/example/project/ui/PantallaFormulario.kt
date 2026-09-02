package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.dominio.PersistenciaLocal
import org.example.project.dominio.Producto
import org.example.project.dominio.nuevoId

@Composable
fun PantallaFormulario(
    productoAEditar: Producto? = null,
    onGuardar: (Producto) -> Unit,
    onEliminar: () -> Unit,
    onVolver: () -> Unit
) {
    // --- PALETA DE COLORES "BOUTIQUE" ---
    val GrisCarbon = Color(0xFF444444)
    val RojoAlerta = Color(0xFFB71C1C)
    val FondoSuave = Color(0xFFFAFAFA)
    val GrisClaro = Color(0xFFF5F5F5)

    // Variables de estado
    // Si es edición conservamos el ID; los productos nuevos reciben un UUID estable.
    val idActual = remember { productoAEditar?.id ?: nuevoId() }

    var nombre by remember { mutableStateOf(productoAEditar?.nombre ?: "") }
    var precioVentaTxt by remember { mutableStateOf(productoAEditar?.precioVenta?.toString() ?: "") }
    var costoProduccionTxt by remember { mutableStateOf(productoAEditar?.costoProduccion?.toString() ?: "") }
    var stockTxt by remember { mutableStateOf(productoAEditar?.stock?.toString() ?: "") }
    var rutaImagenTemporal by remember { mutableStateOf(productoAEditar?.rutaImagen) }

    var mostrarMenuFoto by remember { mutableStateOf(false) }
    var mostrarAlertaBorrar by remember { mutableStateOf(false) }

    val esEdicion = productoAEditar != null
    val titulo = if (esEdicion) "EDITAR ARTESANÍA" else "NUEVA ARTESANÍA"

    // Controladores de imagen
    val controladorImagen = rememberControladorImagen { nuevaRuta ->
        rutaImagenTemporal = nuevaRuta
        mostrarMenuFoto = false
    }
    val imagenBitmap = recordarImagenDesdeRuta(rutaImagenTemporal)

    // --- ALERTA BORRAR ---
    if (mostrarAlertaBorrar) {
        AlertDialog(
            onDismissRequest = { mostrarAlertaBorrar = false },
            title = { Text("¿Eliminar Producto?", color = GrisCarbon) },
            text = { Text("Se borrará '$nombre' del inventario permanentemente.") },
            confirmButton = {
                TextButton(onClick = { onEliminar(); mostrarAlertaBorrar = false }) {
                    Text("ELIMINAR", color = RojoAlerta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAlertaBorrar = false }) { Text("Cancelar", color = GrisCarbon) }
            }
        )
    }

    // --- MENÚ FOTO ---
    if (mostrarMenuFoto) {
        AlertDialog(
            onDismissRequest = { mostrarMenuFoto = false },
            title = { Text("Seleccionar imagen", color = GrisCarbon) },
            buttons = {
                Column(Modifier.padding(16.dp)) {
                    Button(
                        onClick = { controladorImagen.lanzarCamara() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = GrisCarbon)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Usar Cámara", color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { controladorImagen.lanzarGaleria() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.LightGray)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir Galería", color = Color.Black)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { mostrarMenuFoto = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GrisCarbon)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                backgroundColor = GrisCarbon,
                contentColor = Color.White,
                elevation = 0.dp
            )
        },
        backgroundColor = FondoSuave
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // --- 1. ÁREA DE LA FOTO ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(GrisClaro)
                    .clickable { mostrarMenuFoto = true },
                contentAlignment = Alignment.Center
            ) {
                if (imagenBitmap != null) {
                    Image(
                        bitmap = imagenBitmap,
                        contentDescription = "Foto Producto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(0.6f), androidx.compose.foundation.shape.CircleShape)
                            .padding(8.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Agregar Foto", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Divider(color = Color.LightGray)

            // --- 2. CAMPOS DE TEXTO ---
            val inputColors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = GrisCarbon,
                focusedLabelColor = GrisCarbon,
                cursorColor = GrisCarbon,
                textColor = Color.Black
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del producto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = inputColors
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = precioVentaTxt,
                    onValueChange = { if (it.all { c -> c.isDigit() }) precioVentaTxt = it },
                    label = { Text("Precio Venta") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = GrisCarbon) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = inputColors
                )
                OutlinedTextField(
                    value = stockTxt,
                    onValueChange = { if (it.all { c -> c.isDigit() }) stockTxt = it },
                    label = { Text("Stock") },
                    leadingIcon = { Icon(Icons.Default.Inventory2, null, tint = GrisCarbon) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = inputColors
                )
            }

            OutlinedTextField(
                value = costoProduccionTxt,
                onValueChange = { if (it.all { c -> c.isDigit() }) costoProduccionTxt = it },
                label = { Text("Costo Producción (Opcional)") },
                leadingIcon = { Icon(Icons.Default.MoneyOff, null, tint = Color.Gray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = inputColors
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. BOTÓN GUARDAR ---
            Button(
                onClick = {
                    if (nombre.isNotEmpty() && precioVentaTxt.isNotEmpty() && stockTxt.isNotEmpty()) {

                        // 1. ASEGURAMOS LA FOTO
                        val rutaSegura = PersistenciaLocal.prepararImagen(rutaImagenTemporal)

                        // 2. CREAMOS EL PRODUCTO
                        val producto = Producto(
                            id = idActual,
                            nombre = nombre,
                            precioVenta = precioVentaTxt.toIntOrNull() ?: 0,
                            costoProduccion = costoProduccionTxt.toIntOrNull() ?: 0,
                            stock = stockTxt.toIntOrNull() ?: 0,
                            rutaImagen = rutaSegura
                        )
                        onGuardar(producto)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = GrisCarbon),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                enabled = nombre.isNotEmpty()
            ) {
                Icon(Icons.Default.Save, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (esEdicion) "GUARDAR CAMBIOS" else "CREAR PRODUCTO", color = Color.White, fontWeight = FontWeight.Bold)
            }

            // --- 4. BOTÓN ELIMINAR ---
            if (esEdicion) {
                OutlinedButton(
                    onClick = { mostrarAlertaBorrar = true },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RojoAlerta),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RojoAlerta),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ELIMINAR PRODUCTO")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
