package org.example.project.dominio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Venta(
    val productoNombre: String,
    val cantidad: Int,
    val total: Int,
    val precioUnitario: Int,
    val costoUnitario: Int,
    val metodoPago: String,
    val rutaImagen: String?,
    val fecha: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
)