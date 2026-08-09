package org.example.project

import android.content.Context
import android.net.Uri
import org.example.project.dominio.MotorPersistencia
import org.example.project.dominio.Producto
import org.example.project.dominio.Venta
import java.io.File
import java.io.FileOutputStream

class AndroidPersistencia(private val context: Context) : MotorPersistencia {

    private val prefs = context.getSharedPreferences("DetallitosDB", Context.MODE_PRIVATE)

    private val SEPARADOR_ITEM = "||item||"
    private val SEPARADOR_CAMPO = "<~campo~>"

    // --- FUNCIÓN CLAVE: COPIAR IMAGEN A CARPETA SEGURA ---
    override fun importarImagen(uri: String): String {

        if (!uri.startsWith("content://")) return uri

        return try {

            val inputStream = context.contentResolver.openInputStream(Uri.parse(uri)) ?: return uri


            val nombreArchivo = "img_${System.currentTimeMillis()}.jpg"
            val archivoDestino = File(context.filesDir, nombreArchivo)


            val outputStream = FileOutputStream(archivoDestino)
            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()


            archivoDestino.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            uri
        }
    }

    override fun guardarProductos(productos: List<Producto>) {
        val textoGigante = productos.joinToString(SEPARADOR_ITEM) { p ->
            "${p.id}$SEPARADOR_CAMPO${p.nombre}$SEPARADOR_CAMPO${p.precioVenta}$SEPARADOR_CAMPO${p.costoProduccion}$SEPARADOR_CAMPO${p.stock}$SEPARADOR_CAMPO${p.rutaImagen ?: "null"}"
        }
        prefs.edit().putString("PRODUCTOS", textoGigante).apply()
    }

    override fun cargarProductos(): List<Producto> {
        val texto = prefs.getString("PRODUCTOS", "") ?: ""
        if (texto.isEmpty()) return emptyList()

        return texto.split(SEPARADOR_ITEM).mapNotNull { linea ->
            try {
                val partes = linea.split(SEPARADOR_CAMPO)
                if (partes.size >= 6) {
                    Producto(
                        id = partes[0],
                        nombre = partes[1],
                        precioVenta = partes[2].toInt(),
                        costoProduccion = partes[3].toInt(),
                        stock = partes[4].toInt(),
                        rutaImagen = if (partes[5] == "null") null else partes[5]
                    )
                } else null
            } catch (e: Exception) { null }
        }
    }

    override fun guardarVentas(ventas: List<Venta>) {
        val textoGigante = ventas.joinToString(SEPARADOR_ITEM) { v ->
            "${v.productoNombre}$SEPARADOR_CAMPO${v.cantidad}$SEPARADOR_CAMPO${v.total}$SEPARADOR_CAMPO${v.precioUnitario}$SEPARADOR_CAMPO${v.costoUnitario}$SEPARADOR_CAMPO${v.metodoPago}$SEPARADOR_CAMPO${v.rutaImagen ?: "null"}$SEPARADOR_CAMPO${v.fecha}"
        }
        prefs.edit().putString("VENTAS", textoGigante).apply()
    }

    override fun cargarVentas(): List<Venta> {
        val texto = prefs.getString("VENTAS", "") ?: ""
        if (texto.isEmpty()) return emptyList()

        return texto.split(SEPARADOR_ITEM).mapNotNull { linea ->
            try {
                val partes = linea.split(SEPARADOR_CAMPO)
                if (partes.size >= 8) {
                    Venta(
                        productoNombre = partes[0],
                        cantidad = partes[1].toInt(),
                        total = partes[2].toInt(),
                        precioUnitario = partes[3].toInt(),
                        costoUnitario = partes[4].toInt(),
                        metodoPago = partes[5],
                        rutaImagen = if (partes[6] == "null") null else partes[6],
                        fecha = partes[7]
                    )
                } else null
            } catch (e: Exception) { null }
        }
    }

    override fun guardarMeta(meta: Int) {
        prefs.edit().putInt("META_VENTAS", meta).apply()
    }

    override fun cargarMeta(): Int {
        // Busca la meta guardada. Si es la primera vez que abre la app, devuelve 200000.
        return prefs.getInt("META_VENTAS", 200000)
    }
}

