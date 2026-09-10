package org.example.project.dominio

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class RespaldoPreparado(val estado: EstadoDatos, private val carpeta: File) {
    private var incorporado = false
    @Synchronized fun descartar() { if (!incorporado) carpeta.deleteRecursively() }
    @Synchronized fun restaurar() {
        // Imágenes únicas ya verificadas; los archivos anteriores se conservan para recuperación.
        PersistenciaLocal.importarEstado(estado)
        incorporado = true
    }
}

object Respaldo {
    private const val MAX_IMAGEN = 20L * 1024 * 1024
    private const val MAX_TOTAL = 200L * 1024 * 1024

    fun exportar(estado: EstadoDatos, salida: OutputStream) {
        estado.validar()
        val rutas = (estado.productos.mapNotNull { it.rutaImagen } + estado.ventas.mapNotNull { it.rutaImagen }).distinct()
        require(rutas.size <= 10_000) { "Demasiadas imágenes" }
        val nombres = rutas.mapIndexed { i, ruta -> ruta to "imagenes/$i.img" }.toMap()
        // Fallar explícitamente si falta una foto: nunca declarar completo un respaldo incompleto.
        val archivos = rutas.map { ruta -> File(ruta).also {
            require(it.isFile && it.length() in 1..MAX_IMAGEN) { "Falta una imagen o supera los 20 MB: ${it.name}" }
        } }
        val portable = estado.copy(
            productos = estado.productos.map { it.copy(rutaImagen = it.rutaImagen?.let(nombres::getValue)) },
            ventas = estado.ventas.map { it.copy(rutaImagen = it.rutaImagen?.let(nombres::getValue)) }
        )
        val datos = CodecEstado.codificar(portable)
        require(archivos.sumOf { it.length() } + datos.size <= MAX_TOTAL) { "El respaldo supera los 200 MB" }
        ZipOutputStream(salida).use { zip ->
            zip.putNextEntry(ZipEntry("datos.xml")); zip.write(datos); zip.closeEntry()
            rutas.zip(archivos).forEach { (ruta, archivo) ->
                zip.putNextEntry(ZipEntry(nombres.getValue(ruta)))
                archivo.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
        }
    }

    /** Extrae a una carpeta única. No cambia datos hasta que el usuario confirme la vista previa. */
    fun preparar(entrada: InputStream, directorio: File): RespaldoPreparado {
        val carpeta = File(directorio, "restauracion-${nuevoId()}")
        check(carpeta.mkdirs()) { "No se pudo preparar el respaldo" }
        try {
            val nombres = mutableSetOf<String>()
            var total = 0L
            ZipInputStream(entrada).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val nombre = entry.name
                    require(nombre == "datos.xml" || Regex("imagenes/[0-9]+\\.img").matches(nombre)) { "El respaldo contiene una ruta no válida" }
                    require(nombres.add(nombre) && nombres.size <= 10_001 && !entry.isDirectory) { "Entradas repetidas o excesivas" }
                    val destino = File(carpeta, nombre)
                    destino.parentFile?.mkdirs()
                    val limite = if (nombre == "datos.xml") 32L * 1024 * 1024 else MAX_IMAGEN
                    var tamano = 0L
                    destino.outputStream().use { out ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val n = zip.read(buffer)
                            if (n < 0) break
                            tamano += n; total += n
                            require(tamano <= limite && total <= MAX_TOTAL) { "El respaldo supera el tamaño permitido" }
                            out.write(buffer, 0, n)
                        }
                    }
                    zip.closeEntry()
                }
            }
            require("datos.xml" in nombres) { "Faltan los datos del respaldo" }
            val estado = CodecEstado.decodificar(File(carpeta, "datos.xml").readBytes())
            fun resolver(ruta: String?): String? {
                if (ruta == null) return null
                require(Regex("imagenes/[0-9]+\\.img").matches(ruta) && ruta in nombres) { "El respaldo está incompleto: falta una imagen" }
                return File(carpeta, ruta).also { require(it.length() > 0) { "Imagen vacía" } }.absolutePath
            }
            val local = estado.copy(productos = estado.productos.map { it.copy(rutaImagen = resolver(it.rutaImagen)) },
                ventas = estado.ventas.map { it.copy(rutaImagen = resolver(it.rutaImagen)) })
            return RespaldoPreparado(local, carpeta)
        } catch (e: Exception) {
            carpeta.deleteRecursively()
            throw e
        }
    }

    fun csv(ventas: List<Venta>): String {
        fun celda(v: Any?): String {
            var texto = v?.toString().orEmpty()
            // Evita que nombres elegidos por el usuario se interpreten como fórmulas.
            if (v is String && texto.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) texto = "'" + texto
            return "\"" + texto.replace("\"", "\"\"") + "\""
        }
        val filas = ventas.sortedByDescending { it.fechaEpochMillis }.map {
            listOf(it.id, it.productoId, it.productoNombre, it.fecha, it.cantidad, it.precioUnitario,
                it.costoUnitario, it.total, it.ganancia, it.metodoPago, it.fechaTextoLegada).joinToString(";", transform = ::celda)
        }
        return "\uFEFF" + (listOf("ID;Producto ID;Producto;Fecha;Cantidad;Precio unitario;Costo unitario;Total CLP;Ganancia CLP;Método de pago;Fecha original legada") + filas).joinToString("\r\n") + "\r\n"
    }
}
