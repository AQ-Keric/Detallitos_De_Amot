package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close // <--- ¡IMPORTANTE!
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.dominio.Producto

@Composable
fun PantallaInventario(
    productos: List<Producto>,
    onNuevoProducto: () -> Unit,
    onEditarProducto: (Producto) -> Unit
) {
    val GrisCarbon = Color(0xFF444444)
    val FondoSuave = Color(0xFFFAFAFA)

    var textoBusqueda by remember { mutableStateOf("") }
    var filtroStock by remember { mutableStateOf("En Stock") }

    val productosFiltrados = remember(textoBusqueda, productos, filtroStock) {
        productos.filter { producto ->
            val coincideNombre = producto.nombre.contains(textoBusqueda, ignoreCase = true)
            val coincideStock = when (filtroStock) {
                "En Stock" -> producto.stock > 0
                "Agotados" -> producto.stock == 0
                else -> true
            }
            coincideNombre && coincideStock
        }
    }

    var productoParaVerFoto by remember { mutableStateOf<Producto?>(null) }
    if (productoParaVerFoto != null) {
        DialogFotoGrande(producto = productoParaVerFoto!!, onDismiss = { productoParaVerFoto = null })
    }

    Scaffold(
        backgroundColor = FondoSuave,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevoProducto,
                backgroundColor = GrisCarbon,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "MIS ARTESANÍAS",
                style = MaterialTheme.typography.h6,
                color = GrisCarbon,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            BarraBusqueda(
                texto = textoBusqueda,
                onTextoCambio = { textoBusqueda = it },
                placeholder = "Buscar producto..."
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipFiltro("En Stock", filtroStock == "En Stock") { filtroStock = "En Stock" }
                ChipFiltro("Agotados", filtroStock == "Agotados") { filtroStock = "Agotados" }
                ChipFiltro("Todos", filtroStock == "Todos") { filtroStock = "Todos" }
            }

            if (productosFiltrados.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, null, tint = Color.LightGray, modifier = Modifier.size(50.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        val mensaje = if (filtroStock == "Agotados") "¡Todo tiene stock! 🎉" else "No se encontraron productos"
                        Text(mensaje, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(productosFiltrados) { producto ->
                        CardProducto(
                            producto = producto,
                            onEditar = { onEditarProducto(producto) },
                            onVerFoto = { productoParaVerFoto = producto }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardProducto(producto: Producto, onEditar: () -> Unit, onVerFoto: () -> Unit) {
    val imagenBitmap = recordarImagenDesdeRuta(producto.rutaImagen)
    val GrisCarbon = Color(0xFF444444)
    val stockColor = if (producto.stock == 0) Color(0xFFD32F2F) else Color.Gray

    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable { onVerFoto() },
                contentAlignment = Alignment.Center
            ) {
                if (imagenBitmap != null) {
                    Image(imagenBitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.CardGiftcard, null, tint = Color.LightGray, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                Text(
                    text = if (producto.stock == 0) "¡AGOTADO!" else "Stock: ${producto.stock}",
                    style = MaterialTheme.typography.caption,
                    color = stockColor,
                    fontWeight = if (producto.stock == 0) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("$${producto.precioVenta}", style = MaterialTheme.typography.subtitle1, color = GrisCarbon, fontWeight = FontWeight.ExtraBold)
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, "Editar", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun DialogFotoGrande(producto: Producto, onDismiss: () -> Unit) {
    val imagenBitmap = recordarImagenDesdeRuta(producto.rutaImagen)
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), elevation = 8.dp, modifier = Modifier.fillMaxWidth().height(400.dp)) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (imagenBitmap != null) Image(imagenBitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else Icon(Icons.Default.CardGiftcard, null, Modifier.align(Alignment.Center).size(80.dp), tint = Color.LightGray)
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(0.5f), androidx.compose.foundation.shape.CircleShape)) {
                    Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                }
            }
        }
    }
}