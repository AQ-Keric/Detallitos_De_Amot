package org.example.project.dominio

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA_VENTA = DateTimeFormatter.ofPattern("dd/MM HH:mm")

data class Venta(
    val id: String,
    val productoId: String?,
    val productoNombre: String,
    val cantidad: Int,
    val total: Int,
    val precioUnitario: Int,
    val costoUnitario: Int,
    val metodoPago: String,
    val rutaImagen: String?,
    val fechaEpochMillis: Long,
    val fechaTextoLegada: String? = null
) {
    val fecha: String
        get() = fechaTextoLegada ?: FORMATO_FECHA_VENTA.format(
            Instant.ofEpochMilli(fechaEpochMillis).atZone(ZoneId.systemDefault())
        )

    val ganancia: Int
        get() = total - (costoUnitario * cantidad)
}
