package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.dominio.Producto
import org.example.project.dominio.Venta
import org.example.project.dominio.nuevoId

@Composable
fun PantallaNuevaVenta(
    productosDisponibles: List<Producto>,
    onVentaRealizada: (Venta) -> Unit,
    onVolver: () -> Unit
) {
    val GrisCarbon = Color(0xFF444444)

    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (productoSeleccionado == null) "SELECCIONAR PRODUCTO" else "DETALLE VENTA",
                        fontSize = 16.sp, letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (productoSeleccionado != null) productoSeleccionado = null else onVolver()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                backgroundColor = GrisCarbon, // Barra Superior Gris
                contentColor = Color.White,
                elevation = 0.dp
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).background(Color(0xFFFAFAFA)).fillMaxSize()) {
            if (productoSeleccionado == null) {
                ListaSeleccionProducto(productosDisponibles, { productoSeleccionado = it })
            } else {
                FormularioVenta(productoSeleccionado!!, onVentaRealizada)
            }
        }
    }
}

@Composable
fun ListaSeleccionProducto(productos: List<Producto>, onSeleccionar: (Producto) -> Unit) {
    // Reutilizamos estilo sobrio
    val GrisCarbon = Color(0xFF444444)

    var busqueda by rememberSaveable { mutableStateOf("") }
    val filtrados = productos.filter { it.nombre.contains(busqueda.trim(), ignoreCase = true) }
    if (productos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay productos con stock. Agrega o repón unidades en Inventario.", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                BarraBusqueda(texto = busqueda, onTextoCambio = { busqueda = it }, placeholder = "Buscar producto…")
            }
            if (filtrados.isEmpty()) item { Text("No se encontraron productos.", color = Color.Gray) }
            items(filtrados, key = { it.id }) { producto ->
                val imagenBitmap = recordarImagenDesdeRuta(producto.rutaImagen)
                Card(elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { onSeleccionar(producto) }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                            if (imagenBitmap != null) Image(imagenBitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(Icons.Default.CardGiftcard, null, tint = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(producto.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GrisCarbon)
                            Text("Stock: ${producto.stock}", color = Color.Gray, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$${producto.precioVenta}", color = GrisCarbon, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormularioVenta(producto: Producto, onConfirmar: (Venta) -> Unit) {
    val GrisCarbon = Color(0xFF444444)
    val VerdeOlivo = Color(0xFF558B2F) // Color de Éxito/Dinero

    var cantidadTxt by rememberSaveable { mutableStateOf("1") }
    var totalCobrarTxt by rememberSaveable { mutableStateOf(producto.precioVenta.toString()) }
    var metodoPago by rememberSaveable { mutableStateOf("Efectivo") }
    val imagenBitmap = recordarImagenDesdeRuta(producto.rutaImagen)

    LaunchedEffect(cantidadTxt) {
        val cant = cantidadTxt.toIntOrNull() ?: 0
        if (cant > 0) totalCobrarTxt = (producto.precioVenta.toLong() * cant).toString()
    }

    Column(modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
            if (imagenBitmap != null) Image(imagenBitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.CardGiftcard, null, Modifier.size(50.dp), tint = Color.LightGray)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(producto.nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GrisCarbon)
        Text("Stock disponible: ${producto.stock}", color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = cantidadTxt,
                onValueChange = { if (it.all { c -> c.isDigit() }) cantidadTxt = it },
                label = { Text("Cantidad") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = GrisCarbon, focusedLabelColor = GrisCarbon)
            )
            OutlinedTextField(
                value = totalCobrarTxt,
                onValueChange = { if (it.all { c -> c.isDigit() }) totalCobrarTxt = it },
                label = { Text("Total a Cobrar") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = GrisCarbon, focusedLabelColor = GrisCarbon, textColor = GrisCarbon),
                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (cantidadTxt.toIntOrNull()?.let { it in 1..producto.stock } != true) {
            Text("Ingresa una cantidad entre 1 y ${producto.stock}.", color = MaterialTheme.colors.error)
        }
        if (totalCobrarTxt.toIntOrNull()?.let { it >= 0 } != true) {
            Text("Ingresa un total válido entre 0 y ${Int.MAX_VALUE.formatoPesos()} pesos.", color = MaterialTheme.colors.error)
        }
        Text("Método de Pago", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start), color = GrisCarbon)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BotonMetodoPago("Efectivo", Icons.Default.Money, metodoPago == "Efectivo") { metodoPago = "Efectivo" }
            BotonMetodoPago("Transferencia", Icons.Default.Smartphone, metodoPago == "Transferencia") { metodoPago = "Transferencia" }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val cant = cantidadTxt.toIntOrNull() ?: 0
                val totalFinal = totalCobrarTxt.toIntOrNull() ?: 0
                if (cant > 0 && cant <= producto.stock && totalFinal >= 0) {
                    onConfirmar(
                        Venta(
                            id = nuevoId(),
                            productoId = producto.id,
                            productoNombre = producto.nombre,
                            cantidad = cant,
                            total = totalFinal,
                            precioUnitario = producto.precioVenta,
                            costoUnitario = producto.costoProduccion,
                            metodoPago = metodoPago,
                            rutaImagen = producto.rutaImagen,
                            fechaEpochMillis = System.currentTimeMillis()
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = VerdeOlivo), // Botón Verde Confirmar
            shape = RoundedCornerShape(12.dp),
            enabled = (cantidadTxt.toIntOrNull() ?: 0) in 1..producto.stock && totalCobrarTxt.toIntOrNull()?.let { it >= 0 } == true
        ) {
            Text("CONFIRMAR VENTA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RowScope.BotonMetodoPago(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector, seleccionado: Boolean, alClick: () -> Unit) {
    val GrisCarbon = Color(0xFF444444)
    // El seleccionado es Gris Oscuro, el no seleccionado es Gris Muy Claro
    val colorFondo = if (seleccionado) GrisCarbon else Color(0xFFEEEEEE)
    val colorTexto = if (seleccionado) Color.White else Color.Black

    Button(
        onClick = alClick,
        modifier = Modifier.weight(1f).height(45.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = colorFondo),
        elevation = ButtonDefaults.elevation(0.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Icon(icono, null, tint = colorTexto, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(texto, color = colorTexto, fontSize = 12.sp, maxLines = 1)
    }
}
