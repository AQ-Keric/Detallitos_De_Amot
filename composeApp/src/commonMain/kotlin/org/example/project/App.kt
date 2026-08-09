package org.example.project

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart // <-- NUEVO ÍCONO
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    HOME, INVENTARIO, HISTORIAL_VENTAS, NUEVA_VENTA, FORMULARIO_PRODUCTO, DASHBOARD
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var pantallaActual by remember { mutableStateOf(PantallaActual.HOME) }
        val listaProductos = remember { mutableStateListOf<Producto>() }
        val listaVentas = remember { mutableStateListOf<Venta>() }
        var productoAEditar by remember { mutableStateOf<Producto?>(null) }

        LaunchedEffect(Unit) {
            listaProductos.addAll(PersistenciaLocal.obtenerProductos())
            listaVentas.addAll(PersistenciaLocal.obtenerVentas())
        }

        BackHandler(enabled = pantallaActual != PantallaActual.HOME) {
            if (pantallaActual == PantallaActual.FORMULARIO_PRODUCTO) {
                pantallaActual = PantallaActual.INVENTARIO
            } else {
                pantallaActual = PantallaActual.HOME
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
                if (pantallaActual in listOf(PantallaActual.HOME, PantallaActual.INVENTARIO, PantallaActual.HISTORIAL_VENTAS, PantallaActual.DASHBOARD)) {
                    BottomNavigation(
                        backgroundColor = BlancoPuro,
                        contentColor = GrisCarbon,
                        elevation = 8.dp
                    ) {
                        BottomNavigationItem(
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("Inicio") },
                            selected = pantallaActual == PantallaActual.HOME,
                            onClick = { pantallaActual = PantallaActual.HOME },
                            selectedContentColor = GrisCarbon,
                            unselectedContentColor = GrisInactivo
                        )
                        BottomNavigationItem(
                            icon = { Icon(Icons.Default.Inventory2, null) },
                            label = { Text("Inventario") },
                            selected = pantallaActual == PantallaActual.INVENTARIO,
                            onClick = { pantallaActual = PantallaActual.INVENTARIO },
                            selectedContentColor = GrisCarbon,
                            unselectedContentColor = GrisInactivo
                        )
                        BottomNavigationItem(
                            icon = { Icon(Icons.Default.History, null) },
                            label = { Text("Historial") },
                            selected = pantallaActual == PantallaActual.HISTORIAL_VENTAS,
                            onClick = { pantallaActual = PantallaActual.HISTORIAL_VENTAS },
                            selectedContentColor = GrisCarbon,
                            unselectedContentColor = GrisInactivo
                        )
                        // 3. AGREGAMOS EL BOTÓN FÍSICO A LA BARRA
                        BottomNavigationItem(
                            icon = { Icon(Icons.Default.BarChart, null) },
                            label = { Text("Dashboard") }, // <--- ¡AQUÍ ESTÁ EL CAMBIO!
                            selected = pantallaActual == PantallaActual.DASHBOARD,
                            onClick = { pantallaActual = PantallaActual.DASHBOARD },
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
                                PersistenciaLocal.eliminarVenta(ventaAEliminar)
                                listaVentas.remove(ventaAEliminar)
                                listaProductos.clear()
                                listaProductos.addAll(PersistenciaLocal.obtenerProductos())
                            }
                        )
                    }
                    PantallaActual.FORMULARIO_PRODUCTO -> {
                        PantallaFormulario(
                            productoAEditar = productoAEditar,
                            onGuardar = { producto ->
                                PersistenciaLocal.guardarProducto(producto)
                                listaProductos.clear()
                                listaProductos.addAll(PersistenciaLocal.obtenerProductos())
                                pantallaActual = PantallaActual.INVENTARIO
                            },
                            onEliminar = {
                                if (productoAEditar != null) {
                                    PersistenciaLocal.eliminarProducto(productoAEditar!!.id)
                                    listaProductos.clear()
                                    listaProductos.addAll(PersistenciaLocal.obtenerProductos())
                                }
                                pantallaActual = PantallaActual.INVENTARIO
                            },
                            onVolver = { pantallaActual = PantallaActual.INVENTARIO }
                        )
                    }
                    PantallaActual.NUEVA_VENTA -> {
                        PantallaNuevaVenta(
                            productosDisponibles = listaProductos.filter { it.stock > 0 },
                            onVentaRealizada = { nuevaVenta ->
                                PersistenciaLocal.registrarVenta(nuevaVenta)
                                listaVentas.clear()
                                listaVentas.addAll(PersistenciaLocal.obtenerVentas())
                                listaProductos.clear()
                                listaProductos.addAll(PersistenciaLocal.obtenerProductos())
                                pantallaActual = PantallaActual.HISTORIAL_VENTAS
                            },
                            onVolver = { pantallaActual = PantallaActual.HOME }
                        )
                    }
                    // 4. AGREGAMOS EL ENRUTADOR PARA ABRIR LA PANTALLA
                    PantallaActual.DASHBOARD -> {
                        PantallaDashboard(ventas = listaVentas)
                    }
                }
            }
        }
    }
}