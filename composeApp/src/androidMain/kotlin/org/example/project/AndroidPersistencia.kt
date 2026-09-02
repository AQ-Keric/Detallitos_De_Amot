package org.example.project

import android.content.Context
import android.net.Uri
import org.example.project.dominio.CodecPersistencia
import org.example.project.dominio.MotorPersistencia
import org.example.project.dominio.Producto
import org.example.project.dominio.Venta
import java.io.File
import java.io.FileOutputStream

class AndroidPersistencia(private val context: Context) : MotorPersistencia {
    private val prefs = context.getSharedPreferences("DetallitosDB", Context.MODE_PRIVATE)

    override fun importarImagen(uri: String): String {
        if (!uri.startsWith("content://")) return uri

        return try {
            val nombreArchivo = "img_${System.currentTimeMillis()}.jpg"
            val archivoDestino = File(context.filesDir, nombreArchivo)
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { entrada ->
                FileOutputStream(archivoDestino).use { salida ->
                    entrada.copyTo(salida)
                }
            } ?: return uri
            archivoDestino.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            uri
        }
    }

    override fun guardarProductos(productos: List<Producto>) {
        prefs.edit()
            .putString(CLAVE_PRODUCTOS, CodecPersistencia.codificarProductos(productos))
            .apply()
    }

    override fun cargarProductos(): List<Producto> {
        val texto = prefs.getString(CLAVE_PRODUCTOS, "").orEmpty()
        val resultado = CodecPersistencia.decodificarProductos(texto)
        if (resultado.requiereReescritura) {
            prefs.edit()
                .putString(CLAVE_PRODUCTOS, CodecPersistencia.codificarProductos(resultado.productos))
                .commit()
        }
        return resultado.productos
    }

    override fun guardarVentas(ventas: List<Venta>) {
        prefs.edit()
            .putString(CLAVE_VENTAS, CodecPersistencia.codificarVentas(ventas))
            .putInt(CLAVE_VERSION_VENTAS, VERSION_VENTAS_ACTUAL)
            .apply()
    }

    override fun cargarVentas(): List<Venta> {
        val texto = prefs.getString(CLAVE_VENTAS, "").orEmpty()
        val productos = cargarProductos()
        val resultado = CodecPersistencia.decodificarVentas(texto, productos)

        if (resultado.conteniaFormatoLegado && resultado.migracionCompleta) {
            prefs.edit()
                .putString(CLAVE_VENTAS, CodecPersistencia.codificarVentas(resultado.ventas))
                .putInt(CLAVE_VERSION_VENTAS, VERSION_VENTAS_ACTUAL)
                .commit()
        }
        return resultado.ventas
    }

    override fun guardarMeta(meta: Int) {
        prefs.edit().putInt("META_VENTAS", meta).apply()
    }

    override fun cargarMeta(): Int = prefs.getInt("META_VENTAS", 200000)

    private companion object {
        const val CLAVE_PRODUCTOS = "PRODUCTOS"
        const val CLAVE_VENTAS = "VENTAS"
        const val CLAVE_VERSION_VENTAS = "VERSION_VENTAS"
        const val VERSION_VENTAS_ACTUAL = 2
    }
}
