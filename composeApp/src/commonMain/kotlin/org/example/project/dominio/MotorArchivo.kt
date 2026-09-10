package org.example.project.dominio

import java.io.File
import java.io.FileOutputStream

/** Una instantánea de productos + ventas + meta; conserva la última versión válida. */
open class MotorArchivo(val directorio: File) : MotorPersistencia {
    private val archivo get() = File(directorio, "datos.xml")
    private val anterior get() = File(directorio, "datos.anterior.xml")
    private var aviso: String? = null
    protected open fun estadoInicial(): EstadoDatos = EstadoDatos()

    @Synchronized override fun cargarEstado(): EstadoDatos {
        if (!archivo.exists() && !anterior.exists()) {
            return estadoInicial().also { guardarEstado(it) }
        }
        try {
            return leer(archivo)
        } catch (e: Exception) {
            if (!anterior.exists()) throw IllegalStateException("No se pudieron leer tus datos. Conserva los archivos y restaura un respaldo.", e)
            val recuperado = leer(anterior)
            // Recuperar primero el principal, sin sobrescribir el respaldo válido.
            val temporal = File(directorio, "recuperacion.tmp")
            escribir(temporal, CodecEstado.codificar(recuperado))
            check(temporal.renameTo(archivo)) { "No se pudo recuperar el archivo principal" }
            aviso = "Se recuperó la última copia válida. Revisa las operaciones más recientes."
            return recuperado
        }
    }

    private fun leer(f: File): EstadoDatos {
        require(f.length() <= 32L * 1024 * 1024) { "Archivo de datos demasiado grande" }
        return CodecEstado.decodificar(f.readBytes())
    }
    @Synchronized override fun guardarEstado(estado: EstadoDatos) {
        val bytes = CodecEstado.codificar(estado)
        check(directorio.isDirectory || directorio.mkdirs()) { "No se pudo crear el almacenamiento" }
        val temporal = File(directorio, "datos.tmp")
        escribir(temporal, bytes)
        // Si el principal está dañado, nunca borrar el respaldo anterior.
        if (archivo.exists()) {
            leer(archivo)
            val copia = File(directorio, "anterior.tmp")
            escribir(copia, archivo.readBytes())
            check(copia.renameTo(anterior)) { "No se pudo guardar la copia anterior" }
        }
        check(temporal.renameTo(archivo)) { "No se pudo confirmar el guardado" }
    }
    @Synchronized override fun restaurarEstado(estado: EstadoDatos) {
        estado.validar()
        val resguardo = File(directorio, "antes-de-restaurar-${nuevoId()}")
        check(resguardo.mkdirs()) { "No se pudo conservar la copia anterior" }
        listOf(archivo, anterior).filter { it.exists() }.forEach { origen ->
            escribir(File(resguardo, origen.name), origen.readBytes())
        }
        val temporal = File(directorio, "restaurar.tmp")
        escribir(temporal, CodecEstado.codificar(estado))
        check(temporal.renameTo(archivo)) { "No se pudo confirmar la restauración" }
        // El nuevo principal ya es válido; el resguardo contiene la situación anterior completa.
    }
    private fun escribir(f: File, bytes: ByteArray) {
        FileOutputStream(f).use { it.write(bytes); it.fd.sync() }
    }
    override fun avisoRecuperacion(): String? = aviso.also { aviso = null }
    override fun cargarProductos() = cargarEstado().productos
    override fun cargarVentas() = cargarEstado().ventas
    override fun cargarMeta() = cargarEstado().meta
    override fun guardarProductos(productos: List<Producto>) = guardarEstado(cargarEstado().copy(productos = productos))
    override fun guardarVentas(ventas: List<Venta>) = guardarEstado(cargarEstado().copy(ventas = ventas))
    override fun guardarMeta(meta: Int) = guardarEstado(cargarEstado().copy(meta = meta))
    override fun importarImagen(uri: String): String {
        val origen = File(uri).canonicalFile
        require(origen.isFile) { "No se pudo leer la imagen" }
        if (origen.parentFile == File(directorio, "imagenes").canonicalFile) return origen.path
        require(origen.length() <= 20L * 1024 * 1024) { "La imagen supera los 20 MB" }
        val carpeta = File(directorio, "imagenes").apply { mkdirs() }
        return File(carpeta, "${nuevoId()}.img").also { origen.copyTo(it) }.absolutePath
    }
}
