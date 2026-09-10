package org.example.project.dominio

data class Producto(
    val id: String,
    val nombre: String,
    val precioVenta: Int,
    val costoProduccion: Int,
    val stock: Int,
    val rutaImagen: String?,
    val umbralStockBajo: Int? = 3
) {
    val stockBajo: Boolean get() = umbralStockBajo?.let { stock <= it } ?: false
}
