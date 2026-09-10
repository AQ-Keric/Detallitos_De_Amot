package org.example.project

import org.example.project.ui.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart // <-- NUEVO ÍCONO
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.dominio.PersistenciaLocal
import org.example.project.dominio.Producto
import org.example.project.dominio.Venta
import org.example.project.ui.PantallaFormulario
import org.example.project.ui.PantallaHistorialVentas
import org.example.project.ui.PantallaHome
import org.example.project.ui.PantallaInventario
import org.example.project.ui.PantallaNuevaVenta
import org.example.project.ui.PantallaDashboard // <-- NUEVA PANTALLA
import org.jetbrains.compose.ui.tooling.preview.Preview

// 1. AGREGAMOS "DASHBOARD" A LA LISTA DE PANTALLAS
enum class PantallaActual {
    HOME, INVENTARIO, HISTORIAL_VENTAS, NUEVA_VENTA, FORMULARIO_PRODUCTO, DASHBOARD, RESPALDO
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var pantallaActual by remember { mutableStateOf(PantallaActual.HOME) }
        val listaProductos = remember { mutableStateListOf<Producto>() }
        val listaVentas = remember { mutableStateListOf<Venta>() }
        var productoAEditar by remember { mutableStateOf<Producto?>(null) }

        var archivosOcupados by remember { mutableStateOf(false) }
        var cargado by remember { mutableStateOf(false) }
        var ocupado by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        fun refrescar() {
            listaProductos.clear(); listaProductos.addAll(PersistenciaLocal.obtenerProductos())
            listaVentas.clear(); listaVentas.addAll(PersistenciaLocal.obtenerVentas())
        }
        fun cargar() {
            ocupado = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { PersistenciaLocal.estado() }
                    refrescar(); cargado = true
                    error = PersistenciaLocal.motor?.avisoRecuperacion()
                } catch (e: Exception) { error = e.message ?: "No se pudieron cargar tus datos" }
                finally { ocupado = false }
            }
        }
        fun ejecutar(destino: PantallaActual, accion: () -> Unit) {
            if (ocupado) return
            ocupado = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { accion() }
                    refrescar(); pantallaActual = destino
                } catch (e: Exception) { error = e.message ?: "No se pudo guardar. Inténtalo de nuevo." }
                finally { ocupado = false }
            }
        }
        LaunchedEffect(Unit) { cargar() }
        ManejarVolver(enabled = ocupado || pantallaActual != PantallaActual.HOME) {
            if (!ocupado) pantallaActual = if (pantallaActual == PantallaActual.FORMULARIO_PRODUCTO) PantallaActual.INVENTARIO else PantallaActual.HOME
        }
        error?.let { mensaje ->
            AlertDialog(onDismissRequest = { error = null }, title = { Text("Aviso") },
                text = { Text(mensaje) }, confirmButton = { TextButton(onClick = { error = null }) { Text("Entendido") } })
        }
        if (!cargado && pantallaActual != PantallaActual.RESPALDO) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text(if (ocupado) "Cargando tus datos…" else "No se pudieron abrir tus datos. No se han reemplazado por datos vacíos.")
                if (ocupado) CircularProgressIndicator() else {
                    Button(onClick = { cargar() }) { Text("Reintentar") }
                    TextButton(onClick = { pantallaActual = PantallaActual.RESPALDO }) { Text("Restaurar un respaldo") }
                }
            }
            return@MaterialTheme
        }

        if (ocupado) {
            androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(16.dp)); Text("Guardando…")
                    }
                }
            }
        }
        val GrisCarbon = Color(0xFF444444)
        val BlancoPuro = Color(0xFFFFFFFF)
        val GrisInactivo = Color(0xFFBDBDBD)

        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            bottomBar = {
                // 2. AGREGAMOS EL DASHBOARD A LA CONDICIÓN PARA QUE NO SE OCULTE LA BARRA
                if (pantallaActual in listOf(PantallaActual.HOME, PantallaActual.INVENTARIO, PantallaActual.HISTORIAL_VENTAS, PantallaActual.DASHBOARD, PantallaActual.RESPALDO)) {
                    BottomNavigation(
                        backgroundColor = BlancoPuro,
                        contentColor = GrisCarbon,
                        elevation = 8.dp
                    ) {
                        BottomNavigationItem(
                            enabled = !archivosOcupados,
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("Inicio", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                            selected = pantallaActual == PantallaActual.HOME,
                            onClick = { pantallaActual = PantallaActual.HOME },
                            selectedContentColor = GrisCarbon,
                            unselectedContentColor = GrisInactivo
                        )
                        BottomNavigationItem(
                            enabled = !archivosOcupados,
                            icon = { Icon(Icons.Default.Inventory2, null) },
                            label = { Text("Inventario", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                            selected = pantallaActual == PantallaActual.INVENTARIO,
                            onClick = { pantallaActual = PantallaActual.INVENTARIO },
                            selectedContentColor = GrisCarbon,
                            unselectedContentColor = GrisInactivo
                        )
                        BottomNavigationItem(
                            enabled = !archivosOcupados,
                            icon = { Icon(Icons.Default.History, null) },
                            label = { Text("Historial", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                            selected = pantallaActual == PantallaActual.HISTORIAL_VENTAS,
                            onClick = { pantallaActual = PantallaActual.HISTORIAL_VENTAS },
                            selectedContentColor = GrisCarbon,
                            unselectedContentColor = GrisInactivo
                        )
                        // 3. AGREGAMOS EL BOTÓN FÍSICO A LA BARRA
                        BottomNavigationItem(
                            enabled = !archivosOcupados,
                            icon = { Icon(Icons.Default.BarChart, null) },
                            label = { Text("Resumen", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }, // <--- ¡AQUÍ ESTÁ EL CAMBIO!
                            selected = pantallaActual == PantallaActual.DASHBOARD,
                            onClick = { pantallaActual = PantallaActual.DASHBOARD },
                            selectedContentColor = GrisCarbon,
                            unselectedContentColor = GrisInactivo
                        )
                        BottomNavigationItem(
                            enabled = !archivosOcupados,
                            icon = { Icon(Icons.Default.Backup, null) },
                            label = { Text("Respaldo", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                            selected = pantallaActual == PantallaActual.RESPALDO,
                            onClick = { pantallaActual = PantallaActual.RESPALDO },
                            selectedContentColor = GrisCarbon,
                            unselectedContentColor = GrisInactivo
                        )
                    }
                }
            }
        ) { paddingDelScaffold ->
            Box(modifier = Modifier.padding(paddingDelScaffold)) {
                when (pantallaActual) {
                    PantallaActual.HOME -> {
                        PantallaHome(onNavegarAVenta = { pantallaActual = PantallaActual.NUEVA_VENTA })
                    }
                    PantallaActual.INVENTARIO -> {
                        PantallaInventario(
                            productos = listaProductos,
                            onNuevoProducto = {
                                productoAEditar = null
                                pantallaActual = PantallaActual.FORMULARIO_PRODUCTO
                            },
                            onEditarProducto = { producto ->
                                productoAEditar = producto
                                pantallaActual = PantallaActual.FORMULARIO_PRODUCTO
                            }
                        )
                    }
                    PantallaActual.HISTORIAL_VENTAS -> {
                        PantallaHistorialVentas(
                            ventas = listaVentas,
                            onEliminarVenta = { ventaAEliminar ->
                                ejecutar(PantallaActual.HISTORIAL_VENTAS) { PersistenciaLocal.eliminarVenta(ventaAEliminar.id) }
                            }
                        )
                    }
                    PantallaActual.FORMULARIO_PRODUCTO -> {
                        PantallaFormulario(
                            productoAEditar = productoAEditar,
                            onGuardar = { producto ->
                                ejecutar(PantallaActual.INVENTARIO) {
                                    val ruta = PersistenciaLocal.prepararImagen(producto.rutaImagen)
                                    PersistenciaLocal.guardarProducto(producto.copy(rutaImagen = ruta))
                                }
                            },
                            onEliminar = {
                                val id = productoAEditar?.id
                                if (id != null) ejecutar(PantallaActual.INVENTARIO) { PersistenciaLocal.eliminarProducto(id) }
                            },
                            onVolver = { pantallaActual = PantallaActual.INVENTARIO }
                        )
                    }
                    PantallaActual.NUEVA_VENTA -> {
                        PantallaNuevaVenta(
                            productosDisponibles = listaProductos.filter { it.stock > 0 },
                            onVentaRealizada = { nuevaVenta ->
                                ejecutar(PantallaActual.HISTORIAL_VENTAS) { PersistenciaLocal.registrarVenta(nuevaVenta) }
                            },
                            onVolver = { pantallaActual = PantallaActual.HOME }
                        )
                    }
                    PantallaActual.RESPALDO -> {
                        PantallaRespaldo(onOcupado = { archivosOcupados = it }, onActualizado = { refrescar(); cargado = true }, onVolver = { pantallaActual = PantallaActual.HOME })
                    }
                    // 4. AGREGAMOS EL ENRUTADOR PARA ABRIR LA PANTALLA
                    PantallaActual.DASHBOARD -> {
                        PantallaDashboard(ventas = listaVentas.toList())
                    }
                }
            }
        }
    }
}
