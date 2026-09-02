package org.example.project

import org.example.project.dominio.CalculadoraFinanciera
import org.example.project.dominio.CodecPersistencia
import org.example.project.dominio.MotorPersistencia
import org.example.project.dominio.PersistenciaLocal
import org.example.project.dominio.Producto
import org.example.project.dominio.Venta
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Etapa1ModelosTest {
    @AfterTest
    fun limpiarMotor() {
        PersistenciaLocal.motor = null
    }

    @Test
    fun unaVentaModificaStockPorIdAunqueLosNombresSeanIguales() {
        val primero = producto("producto-1", stock = 5)
        val segundo = producto("producto-2", stock = 8)
        val motor = MotorMemoria(listOf(primero, segundo))
        PersistenciaLocal.motor = motor

        val venta = venta("venta-1", segundo, cantidad = 2)
        PersistenciaLocal.registrarVenta(venta)

        assertEquals(5, PersistenciaLocal.obtenerProductos().first { it.id == primero.id }.stock)
        assertEquals(6, PersistenciaLocal.obtenerProductos().first { it.id == segundo.id }.stock)

        PersistenciaLocal.eliminarVenta(venta.id)
        assertEquals(8, PersistenciaLocal.obtenerProductos().first { it.id == segundo.id }.stock)
    }

    @Test
    fun unaVentaConservaSuSnapshotCuandoElProductoCambia() {
        val original = producto("producto-1", nombre = "Ramo", precio = 10_000, costo = 4_000)
        val venta = venta("venta-1", original)
        val editado = original.copy(nombre = "Ramo premium", precioVenta = 15_000, costoProduccion = 6_000)

        assertEquals("Ramo", venta.productoNombre)
        assertEquals(10_000, venta.precioUnitario)
        assertEquals(4_000, venta.costoUnitario)
        assertNotEquals(editado.nombre, venta.productoNombre)
    }

    @Test
    fun migraVentaLegadaYLaRelacionaSoloConNombreUnico() {
        val producto = producto("producto-1", nombre = "Ramo")
        val texto = "Ramo<~campo~>2<~campo~>20000<~campo~>10000<~campo~>4000<~campo~>Efectivo<~campo~>null<~campo~>31/08 10:30"
        val ahora = LocalDateTime.of(2026, 9, 2, 12, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val resultado = CodecPersistencia.decodificarVentas(texto, listOf(producto), ahora)
        val migrada = resultado.ventas.single()

        assertTrue(resultado.migracionCompleta)
        assertTrue(resultado.conteniaFormatoLegado)
        assertEquals(producto.id, migrada.productoId)
        assertEquals("31/08 10:30", migrada.fechaTextoLegada)
        assertTrue(migrada.fechaEpochMillis > 0L)
        assertNotNull(migrada.id)
    }

    @Test
    fun unaVentaLegadaAmbiguaNoInventaProductoId() {
        val productos = listOf(
            producto("producto-1", nombre = "Ramo"),
            producto("producto-2", nombre = "ramo")
        )
        val texto = "Ramo<~campo~>1<~campo~>10000<~campo~>10000<~campo~>4000<~campo~>Efectivo<~campo~>null<~campo~>01/09 12:00"

        val migrada = CodecPersistencia.decodificarVentas(texto, productos).ventas.single()

        assertNull(migrada.productoId)
    }

    @Test
    fun formatoActualPuedeGuardarseYLeerseSinPerderDatos() {
        val producto = producto("producto-1")
        val original = venta("venta-1", producto)

        val texto = CodecPersistencia.codificarVentas(listOf(original))
        val resultado = CodecPersistencia.decodificarVentas(texto, listOf(producto))

        assertEquals(listOf(original), resultado.ventas)
        assertTrue(resultado.migracionCompleta)
        assertTrue(!resultado.conteniaFormatoLegado)
    }

    @Test
    fun dashboardAgrupaPorProductoIdYUsaElNombreMasReciente() {
        val producto = producto("producto-1", nombre = "Ramo")
        val antigua = venta("venta-1", producto, cantidad = 1, fecha = 1_000L)
        val reciente = venta(
            id = "venta-2",
            producto = producto.copy(nombre = "Ramo premium"),
            cantidad = 2,
            fecha = 2_000L
        )

        assertEquals(listOf("Ramo premium" to 3), CalculadoraFinanciera.obtenerTopVentas(listOf(antigua, reciente)))
    }

    private fun producto(
        id: String,
        nombre: String = "Producto repetido",
        precio: Int = 10_000,
        costo: Int = 4_000,
        stock: Int = 10
    ) = Producto(id, nombre, precio, costo, stock, null)

    private fun venta(
        id: String,
        producto: Producto,
        cantidad: Int = 1,
        fecha: Long = 1_000L
    ) = Venta(
        id = id,
        productoId = producto.id,
        productoNombre = producto.nombre,
        cantidad = cantidad,
        total = producto.precioVenta * cantidad,
        precioUnitario = producto.precioVenta,
        costoUnitario = producto.costoProduccion,
        metodoPago = "Efectivo",
        rutaImagen = producto.rutaImagen,
        fechaEpochMillis = fecha
    )
}

private class MotorMemoria(productosIniciales: List<Producto>) : MotorPersistencia {
    private var productos = productosIniciales.toList()
    private var ventas = emptyList<Venta>()
    private var meta = 200_000

    override fun guardarProductos(productos: List<Producto>) {
        this.productos = productos.toList()
    }

    override fun cargarProductos(): List<Producto> = productos

    override fun guardarVentas(ventas: List<Venta>) {
        this.ventas = ventas.toList()
    }

    override fun cargarVentas(): List<Venta> = ventas

    override fun importarImagen(uri: String): String = uri

    override fun guardarMeta(meta: Int) {
        this.meta = meta
    }

    override fun cargarMeta(): Int = meta
}
