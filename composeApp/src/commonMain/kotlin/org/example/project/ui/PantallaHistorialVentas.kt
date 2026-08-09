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
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.dominio.Venta

@Composable
fun PantallaHistorialVentas(
    ventas: List<Venta>,
    onEliminarVenta: (Venta) -> Unit
) {
    val GrisCarbon = Color(0xFF444444)
    val FondoSuave = Color(0xFFFAFAFA)

    var textoBusqueda by remember { mutableStateOf("") }
    var filtroMetodoPago by remember { mutableStateOf("Todos") }

    var ventaParaBorrar by remember { mutableStateOf<Venta?>(null) }
    var ventaParaVerFoto by remember { mutableStateOf<Venta?>(null) }

    // BUG FIX: derivedStateOf para que detecte cambios en la lista
    val ventasFiltradas by remember(textoBusqueda, filtroMetodoPago, ventas) {
        derivedStateOf {
            ventas.filter { venta ->
                val coincideNombre = venta.productoNombre.contains(textoBusqueda, ignoreCase = true)
                val coincidePago = if (filtroMetodoPago == "Todos") true else venta.metodoPago == filtroMetodoPago
                coincideNombre && coincidePago
            }
        }
    }

    val ventasAgrupadas by remember(ventasFiltradas) {
        derivedStateOf {
            ventasFiltradas.groupBy { it.fecha.take(5) }
        }
    }

    if (ventaParaBorrar != null) {
        AlertDialog(
            onDismissRequest = { ventaParaBorrar = null },
            title = { Text("¿Anular Venta?", color = GrisCarbon) },
            text = { Text("Se devolverán las unidades al inventario.") },
            confirmButton = {
                TextButton(onClick = { onEliminarVenta(ventaParaBorrar!!); ventaParaBorrar = null }) {
                    Text("ANULAR", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { ventaParaBorrar = null }) { Text("Cancelar", color = GrisCarbon) } }
        )
    }

    if (ventaParaVerFoto != null) {
        DialogFotoGrandeHistoria(ventaParaVerFoto!!) { ventaParaVerFoto = null }
    }

    Scaffold(backgroundColor = FondoSuave) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "HISTORIAL DE VENTAS",
                style = MaterialTheme.typography.h6,
                color = GrisCarbon,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            BarraBusqueda(
                texto = textoBusqueda,
                onTextoCambio = { textoBusqueda = it },
                placeholder = "Buscar venta..."
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipFiltro("Todos", filtroMetodoPago == "Todos") { filtroMetodoPago = "Todos" }
                ChipFiltro("Efectivo", filtroMetodoPago == "Efectivo") { filtroMetodoPago = "Efectivo" }
                ChipFiltro("Transf.", filtroMetodoPago == "Transferencia") { filtroMetodoPago = "Transferencia" }
            }

            if (ventasAgrupadas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, tint = Color.LightGray, modifier = Modifier.size(50.dp))
                        Text("No se encontraron ventas", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ventasAgrupadas.forEach { (fecha, listaVentasDelDia) ->
                        item {
                            Text(
                                text = "📅 Fecha: $fecha",
                                color = GrisCarbon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(listaVentasDelDia) { venta ->
                            val imagenBitmap = recordarImagenDesdeRuta(venta.rutaImagen)
                            CardVenta(venta, imagenBitmap, { ventaParaBorrar = venta }, { ventaParaVerFoto = venta })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardVenta(
    venta: Venta,
    imagenBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    onBorrar: () -> Unit,
    onVerFoto: () -> Unit
) {
    val GrisCarbon = Color(0xFF444444)
    val esEfectivo = venta.metodoPago == "Efectivo"
    val colorPago = if (esEfectivo) Color(0xFF388E3C) else Color(0xFF1976D2)
    val iconoPago = if (esEfectivo) Icons.Default.Money else Icons.Default.Smartphone
    val textoPago = if (esEfectivo) "Efectivo" else "Transf."

    Card(elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)).clickable { onVerFoto() },
                contentAlignment = Alignment.Center
            ) {
                if (imagenBitmap != null) Image(bitmap = imagenBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Default.ShoppingBag, null, tint = Color.LightGray)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(venta.productoNombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GrisCarbon)
                Text("${venta.cantidad} un. • ${venta.fecha}", style = MaterialTheme.typography.caption, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(iconoPago, null, tint = colorPago, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(textoPago, fontSize = 12.sp, color = colorPago, fontWeight = FontWeight.Bold)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$${venta.total}", style = MaterialTheme.typography.subtitle1, color = GrisCarbon, fontWeight = FontWeight.ExtraBold)
                IconButton(onClick = onBorrar) { Icon(Icons.Default.Delete, contentDescription = "Anular", tint = Color.LightGray) }
            }
        }
    }
}

@Composable
fun DialogFotoGrandeHistoria(venta: Venta, onDismiss: () -> Unit) {
    val imagenBitmap = recordarImagenDesdeRuta(venta.rutaImagen)
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), elevation = 8.dp, modifier = Modifier.fillMaxWidth().height(400.dp)) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (imagenBitmap != null) Image(bitmap = imagenBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BrokenImage, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                        Text("Sin imagen", color = Color.Gray)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(0.5f), androidx.compose.foundation.shape.CircleShape)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
                Text(text = venta.productoNombre, modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(0.6f)).fillMaxWidth().padding(8.dp), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}