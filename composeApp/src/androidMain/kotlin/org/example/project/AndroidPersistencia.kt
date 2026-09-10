package org.example.project

import android.content.Context
import android.net.Uri
import org.example.project.dominio.*
import java.io.File
import java.io.FileOutputStream

class AndroidPersistencia(context: Context) : MotorArchivo(File(context.filesDir, "detallitos")) {
    private val app = context.applicationContext
    override fun estadoInicial(): EstadoDatos {
        val prefs = app.getSharedPreferences("DetallitosDB", Context.MODE_PRIVATE)
        val textoProductos = prefs.getString("PRODUCTOS", "").orEmpty()
        val resultado = CodecPersistencia.decodificarProductos(textoProductos)
        val cantidad = if (textoProductos.isEmpty()) 0 else textoProductos.split("||item||").size
        require(resultado.productos.size == cantidad) { "Hay productos antiguos que no se pueden leer. No se ha modificado el original." }
        val ventas = CodecPersistencia.decodificarVentas(prefs.getString("VENTAS", "").orEmpty(), resultado.productos)
        require(ventas.migracionCompleta) { "Hay ventas antiguas que no se pueden leer. No se ha modificado el original." }
        return EstadoDatos(resultado.productos, ventas.ventas, prefs.getInt("META_VENTAS", 200000)).also { it.validar() }
    }

    override fun importarImagen(uri: String): String {
        if (!uri.startsWith("content://")) return super.importarImagen(uri)
        val carpeta = File(directorio, "imagenes").apply { mkdirs() }
        val archivo = File(carpeta, "${nuevoId()}.img")
        try {
            val entrada = requireNotNull(app.contentResolver.openInputStream(Uri.parse(uri))) { "No se pudo abrir la foto" }
            entrada.use { input ->
                FileOutputStream(archivo).use { out ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        total += n
                        require(total <= 20L * 1024 * 1024) { "La imagen supera los 20 MB" }
                        out.write(buffer, 0, n)
                    }
                    require(total > 0) { "La imagen está vacía" }
                    out.fd.sync()
                }
            }
            return archivo.absolutePath
        } catch (e: Exception) {
            archivo.delete()
            throw IllegalStateException("No se pudo guardar la foto: ${e.message}", e)
        }
    }
}
