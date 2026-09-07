package org.example.project

import org.example.project.dominio.*
import kotlin.test.*
import java.io.*
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PersistenciaRespaldoTest {
    private val temporal = Files.createTempDirectory("detallitos-test").toFile()
    private fun producto() = Producto("p", "Ramo 🌸 <~campo~> ||item||\n\"especial\"", 10000, 4000, 10, null)
    private fun venta(p: Producto = producto(), id: String = "v", fecha: Long = 1000) = Venta(id, p.id, p.nombre, 2, 18000, p.precioVenta, p.costoProduccion, "Efectivo", p.rutaImagen, fecha)
    @AfterTest fun limpiar() { PersistenciaLocal.motor = null; temporal.deleteRecursively() }

    @Test fun umbralIndividualSePuedeEditarYDesactivar() {
        assertTrue(producto().copy(stock = 5, umbralStockBajo = 5).stockBajo)
        assertFalse(producto().copy(stock = 6, umbralStockBajo = 5).stockBajo)
        assertFalse(producto().copy(stock = 0, umbralStockBajo = null).stockBajo)
        assertTrue(producto().copy(stock = 0, umbralStockBajo = 0).stockBajo)
        assertFails { EstadoDatos(listOf(producto().copy(umbralStockBajo = -1))).validar() }
        PersistenciaLocal.motor = MotorArchivo(temporal)
        PersistenciaLocal.guardarProducto(producto().copy(umbralStockBajo = 8))
        PersistenciaLocal.registrarVenta(venta())
        assertTrue(PersistenciaLocal.obtenerProductos().single().stockBajo)
        PersistenciaLocal.motor = MotorArchivo(temporal)
        assertEquals(8, PersistenciaLocal.obtenerProductos().single().umbralStockBajo)
        PersistenciaLocal.guardarProducto(PersistenciaLocal.obtenerProductos().single().copy(umbralStockBajo = null))
        assertFalse(PersistenciaLocal.obtenerProductos().single().stockBajo)
        val e = PersistenciaLocal.estado()
        val bytes = ByteArrayOutputStream().also { Respaldo.exportar(e, it) }.toByteArray()
        val listo = Respaldo.preparar(ByteArrayInputStream(bytes), temporal)
        assertNull(listo.estado.productos.single().umbralStockBajo)
        listo.descartar()
    }
    @Test fun respaldoV3RecibeUmbralCompatible() {
        val xml = CodecEstado.codificar(EstadoDatos(listOf(producto()))).toString(Charsets.UTF_8)
            .replace("<entry key=\"version\">4</entry>", "<entry key=\"version\">3</entry>")
            .replace("<entry key=\"p.0.umbralStockBajo\">3</entry>", "")
        assertEquals(3, CodecEstado.decodificar(xml.toByteArray()).productos.single().umbralStockBajo)
    }
    @Test fun reiniciarYEliminarMetaNoTocanVentasNiStock() {
        PersistenciaLocal.motor = MotorArchivo(temporal)
        PersistenciaLocal.guardarProducto(producto())
        PersistenciaLocal.registrarVenta(venta(fecha = 1000))
        val antes = PersistenciaLocal.estado()
        PersistenciaLocal.reiniciarMetaGlobal(50000, 2000)
        assertEquals(0L, MetasVentas.ingresosGlobal(PersistenciaLocal.obtenerVentas(), 2000))
        PersistenciaLocal.registrarVenta(venta(id = "nueva", fecha = 3000))
        assertEquals(18000L, MetasVentas.ingresosGlobal(PersistenciaLocal.obtenerVentas(), 2000))
        PersistenciaLocal.guardarMeta("total", 75000)
        assertEquals(2000L, PersistenciaLocal.estado().metaGlobalDesde)
        val e = PersistenciaLocal.estado()
        assertEquals(e, CodecEstado.decodificar(CodecEstado.codificar(e)))
        PersistenciaLocal.eliminarMeta("total")
        assertNull(PersistenciaLocal.obtenerMeta("total"))
        assertEquals(e.ventas, PersistenciaLocal.obtenerVentas())
        assertEquals(e.productos, PersistenciaLocal.obtenerProductos())
        PersistenciaLocal.motor = MotorArchivo(temporal)
        assertNull(PersistenciaLocal.obtenerMeta("total"))
        PersistenciaLocal.guardarMeta("total", 90000)
        assertTrue(PersistenciaLocal.estado().metaGlobalDesde > 3000)
        assertEquals(90000, PersistenciaLocal.obtenerMeta("total"))
        assertEquals(antes.ventas.size + 1, PersistenciaLocal.obtenerVentas().size)
    }
    @Test fun sePuedeQuitarMetaDeUnMesSinTocarLosDemas() {
        PersistenciaLocal.motor = MotorArchivo(temporal)
        PersistenciaLocal.guardarMeta("mes:2026-09", 10000)
        PersistenciaLocal.guardarMeta("mes:2026-10", 20000)
        PersistenciaLocal.eliminarMeta("mes:2026-09")
        assertNull(PersistenciaLocal.obtenerMeta("mes:2026-09"))
        assertEquals(20000, PersistenciaLocal.obtenerMeta("mes:2026-10"))
        assertEquals(200000, PersistenciaLocal.obtenerMeta("total"))
    }
    @Test fun metasIndependientesSeConservanAlReabrirYExportar() {
        PersistenciaLocal.motor = MotorArchivo(temporal)
        val septiembre = MetasVentas.clave(ModoTiempo.MES, LocalDate.of(2026, 9, 1))
        val octubre = MetasVentas.clave(ModoTiempo.MES, LocalDate.of(2026, 10, 1))
        assertNull(PersistenciaLocal.obtenerMeta(septiembre))
        PersistenciaLocal.guardarMeta(septiembre, 500000)
        PersistenciaLocal.guardarMeta(octubre, 600000)
        PersistenciaLocal.motor = MotorArchivo(temporal)
        assertEquals(500000, PersistenciaLocal.obtenerMeta(septiembre))
        assertEquals(600000, PersistenciaLocal.obtenerMeta(octubre))
        assertEquals(200000, PersistenciaLocal.obtenerMeta("total"))
        val e = PersistenciaLocal.estado()
        assertEquals(e, CodecEstado.decodificar(CodecEstado.codificar(e)))
        val bytes = ByteArrayOutputStream().also { Respaldo.exportar(e, it) }.toByteArray()
        val listo = Respaldo.preparar(ByteArrayInputStream(bytes), temporal)
        assertEquals(e.metasPorPeriodo, listo.estado.metasPorPeriodo)
        listo.descartar()
    }
    @Test fun semanaComparteMetaInclusoAlCambiarDeAnio() {
        assertEquals(MetasVentas.clave(ModoTiempo.SEMANA, LocalDate.of(2025,12,31)),
            MetasVentas.clave(ModoTiempo.SEMANA, LocalDate.of(2026,1,1)))
        assertNotEquals(MetasVentas.clave(ModoTiempo.MES, LocalDate.of(2025,1,1)),
            MetasVentas.clave(ModoTiempo.MES, LocalDate.of(2026,1,1)))
    }
    @Test fun metaMuestraExcedenteSinLimitarPorcentajeACien() {
        val avance = MetasVentas.progreso(150000, 100000)
        assertEquals(150.0, avance.porcentaje)
        assertEquals(1f, avance.barra)
        assertEquals(50000L, avance.excedente)
        assertEquals(0L, avance.faltante)
        assertEquals(100000L, MetasVentas.progreso(0,100000).faltante)
        assertFails { MetasVentas.progreso(100, 0) }
    }
    @Test fun respaldoAnteriorConservaMetaTotalSinInventarMetasPorPeriodo() {
        val xml = CodecEstado.codificar(EstadoDatos(meta = 75000)).toString(Charsets.UTF_8)
            .replace("<entry key=\"version\">4</entry>", "<entry key=\"version\">1</entry>")
        val e = CodecEstado.decodificar(xml.toByteArray())
        assertEquals(75000, e.meta)
        assertTrue(e.metasPorPeriodo.isEmpty())
    }
    @Test fun conservaSeparadoresUnicodeYValoresNulos() {
        val e = EstadoDatos(listOf(producto()), listOf(venta()), 50000)
        assertEquals(e, CodecEstado.decodificar(CodecEstado.codificar(e)))
    }
    @Test fun unaVentaYStockSeConservanAlReabrir() {
        PersistenciaLocal.motor = MotorArchivo(temporal)
        PersistenciaLocal.guardarProducto(producto())
        PersistenciaLocal.registrarVenta(venta())
        PersistenciaLocal.motor = MotorArchivo(temporal)
        assertEquals(8, PersistenciaLocal.obtenerProductos().single().stock)
        assertEquals(18000, PersistenciaLocal.obtenerVentas().single().total)
    }
    @Test fun unaEscrituraFallidaNoCambiaLaCache() {
        val motor = object : MotorArchivo(temporal) {
            var fallar = false
            override fun guardarEstado(estado: EstadoDatos) { if (fallar) error("Disco lleno") else super.guardarEstado(estado) }
        }
        PersistenciaLocal.motor = motor
        PersistenciaLocal.guardarProducto(producto()); motor.fallar = true
        assertFails { PersistenciaLocal.registrarVenta(venta()) }
        assertEquals(10, PersistenciaLocal.obtenerProductos().single().stock)
        assertTrue(PersistenciaLocal.obtenerVentas().isEmpty())
    }
    @Test fun recuperaUltimoEstadoValidoTrasCorrupcion() {
        val motor = MotorArchivo(temporal)
        motor.guardarEstado(EstadoDatos(listOf(producto())))
        motor.guardarEstado(EstadoDatos(listOf(producto().copy(stock = 8)), listOf(venta())))
        File(temporal, "datos.xml").writeText("dañado")
        assertEquals(10, motor.cargarEstado().productos.single().stock)
        assertNotNull(motor.avisoRecuperacion())
        assertEquals(10, MotorArchivo(temporal).cargarEstado().productos.single().stock)
    }
    @Test fun respaldoPortatilIncluyeImagenHistoricaYNoModificaAntesDeConfirmar() {
        val imagen = File(temporal, "foto.jpg").apply { writeBytes(byteArrayOf(1,2,3,4)) }
        val p = producto().copy(rutaImagen = imagen.path)
        val e = EstadoDatos(listOf(p), listOf(venta(p)), 70000)
        val bytes = ByteArrayOutputStream().also { Respaldo.exportar(e, it) }.toByteArray()
        imagen.delete()
        val motor = MotorArchivo(File(temporal, "destino"))
        PersistenciaLocal.motor = motor
        assertTrue(PersistenciaLocal.obtenerProductos().isEmpty())
        val listo = Respaldo.preparar(ByteArrayInputStream(bytes), motor.directorio)
        assertTrue(PersistenciaLocal.obtenerProductos().isEmpty())
        listo.restaurar()
        listo.descartar() // Salir de la pantalla no elimina imágenes ya incorporadas.
        PersistenciaLocal.motor = MotorArchivo(motor.directorio)
        val cargado = PersistenciaLocal.estado()
        assertEquals(70000, cargado.meta)
        assertContentEquals(byteArrayOf(1,2,3,4), File(cargado.ventas.single().rutaImagen!!).readBytes())
        assertEquals(cargado.productos.single().rutaImagen, cargado.ventas.single().rutaImagen)
    }
    @Test fun rechazaRutasQueEscapanDelZip() {
        val bytes = ByteArrayOutputStream().also { out -> ZipOutputStream(out).use {
            it.putNextEntry(ZipEntry("../escape")); it.write(byteArrayOf(1)); it.closeEntry()
        } }.toByteArray()
        assertFails { Respaldo.preparar(ByteArrayInputStream(bytes), temporal) }
        assertFalse(File(temporal.parentFile, "escape").exists())
    }
    @Test fun respaldoIncompletoNoSeExportaComoExitoso() {
        val e = EstadoDatos(listOf(producto().copy(rutaImagen = File(temporal, "no-existe").path)))
        assertFails { Respaldo.exportar(e, ByteArrayOutputStream()) }
    }
    @Test fun rechazaDuplicadosYVentaSinStock() {
        assertFails { EstadoDatos(listOf(producto(), producto())).validar() }
        PersistenciaLocal.motor = MotorArchivo(temporal)
        PersistenciaLocal.guardarProducto(producto().copy(stock = 1))
        assertFails { PersistenciaLocal.registrarVenta(venta()) }
        assertTrue(PersistenciaLocal.obtenerVentas().isEmpty())
        assertEquals(1, PersistenciaLocal.obtenerProductos().single().stock)
    }
    @Test fun anularVentaAmbiguaNoAfectaProductoConNombreIgual() {
        PersistenciaLocal.motor = MotorArchivo(temporal)
        PersistenciaLocal.importarEstado(EstadoDatos(listOf(producto()), listOf(venta().copy(productoId = null))))
        PersistenciaLocal.eliminarVenta("v")
        assertEquals(10, PersistenciaLocal.obtenerProductos().first { it.id == "p" }.stock)
        assertEquals(2, PersistenciaLocal.obtenerProductos().size)
        PersistenciaLocal.eliminarVenta("v")
        assertEquals(2, PersistenciaLocal.obtenerProductos().size)
    }
    @Test fun filtrosNoIncluyenFechasDesconocidasYNumerosNoDesbordanInt() {
        assertTrue(FiltroVentas.aplicarFiltro(listOf(venta(fecha = 0)), ModoTiempo.DIA, LocalDate.now()).isEmpty())
        val ventas = listOf(venta().copy(total = Int.MAX_VALUE), venta(id = "otra").copy(total = Int.MAX_VALUE))
        assertEquals(2L * Int.MAX_VALUE, CalculadoraFinanciera.calcularIngresosTotales(ventas))
    }
    @Test fun promediosSeparanAnios() {
        fun fecha(anio: Int) = LocalDate.of(anio, 1, 10).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ventas = listOf(venta(fecha = fecha(2025)), venta(id = "otra", fecha = fecha(2026)))
        assertEquals(18000L, CalculadoraFinanciera.calcularPromedios(ventas)["Mensual"])
    }
    @Test fun csvEscapaCeldasYNoEjecutaFormulas() {
        val csv = Respaldo.csv(listOf(venta().copy(productoNombre = "=SUM(A1);\"hola\"\n")))
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("\"'=SUM(A1);\"\"hola\"\"\n\""))
    }
    @Test fun restauracionExplicitaConservaArchivosDanados() {
        File(temporal, "datos.xml").writeText("dañado")
        PersistenciaLocal.motor = MotorArchivo(temporal)
        PersistenciaLocal.importarEstado(EstadoDatos(listOf(producto())))
        assertEquals(producto(), PersistenciaLocal.obtenerProductos().single())
        assertTrue(temporal.listFiles()!!.any { it.name.startsWith("antes-de-restaurar-") && File(it, "datos.xml").readText() == "dañado" })
    }
}
