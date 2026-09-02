package org.example.project.dominio

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val SEPARADOR_ITEM = "||item||"
private const val SEPARADOR_CAMPO = "<~campo~>"
private const val VERSION_VENTA = "v2"
private const val VALOR_NULO = "null"

data class ResultadoProductos(
    val productos: List<Producto>,
    val requiereReescritura: Boolean
)

data class ResultadoVentas(
    val ventas: List<Venta>,
    val migracionCompleta: Boolean,
    val conteniaFormatoLegado: Boolean
)

object CodecPersistencia {
    fun codificarProductos(productos: List<Producto>): String =
        productos.joinToString(SEPARADOR_ITEM) { producto ->
            listOf(
                producto.id,
                producto.nombre,
                producto.precioVenta,
                producto.costoProduccion,
                producto.stock,
                producto.rutaImagen ?: VALOR_NULO
            ).joinToString(SEPARADOR_CAMPO)
        }

    fun decodificarProductos(texto: String): ResultadoProductos {
        if (texto.isEmpty()) return ResultadoProductos(emptyList(), false)

        val idsUsados = mutableSetOf<String>()
        var requiereReescritura = false
        val productos = texto.split(SEPARADOR_ITEM).mapNotNull { registro ->
            try {
                val partes = registro.split(SEPARADOR_CAMPO)
                if (partes.size < 6) return@mapNotNull null

                val idOriginal = partes[0]
                val idSeguro = if (idOriginal.isNotBlank() && idsUsados.add(idOriginal)) {
                    idOriginal
                } else {
                    requiereReescritura = true
                    nuevoId().also(idsUsados::add)
                }

                Producto(
                    id = idSeguro,
                    nombre = partes[1],
                    precioVenta = partes[2].toInt(),
                    costoProduccion = partes[3].toInt(),
                    stock = partes[4].toInt(),
                    rutaImagen = partes[5].valorNullable()
                )
            } catch (_: Exception) {
                null
            }
        }
        return ResultadoProductos(productos, requiereReescritura)
    }

    fun codificarVentas(ventas: List<Venta>): String =
        ventas.joinToString(SEPARADOR_ITEM) { venta ->
            listOf(
                VERSION_VENTA,
                venta.id,
                venta.productoId ?: VALOR_NULO,
                venta.productoNombre,
                venta.cantidad,
                venta.total,
                venta.precioUnitario,
                venta.costoUnitario,
                venta.metodoPago,
                venta.rutaImagen ?: VALOR_NULO,
                venta.fechaEpochMillis,
                venta.fechaTextoLegada ?: VALOR_NULO
            ).joinToString(SEPARADOR_CAMPO)
        }

    fun decodificarVentas(
        texto: String,
        productos: List<Producto>,
        ahoraEpochMillis: Long = System.currentTimeMillis()
    ): ResultadoVentas {
        if (texto.isEmpty()) return ResultadoVentas(emptyList(), true, false)

        val registros = texto.split(SEPARADOR_ITEM)
        var conteniaFormatoLegado = false
        val ventas = registros.mapNotNull { registro ->
            val partes = registro.split(SEPARADOR_CAMPO)
            if (partes.firstOrNull() == VERSION_VENTA) {
                decodificarVentaActual(partes)
            } else {
                conteniaFormatoLegado = true
                decodificarVentaLegada(partes, productos, ahoraEpochMillis)
            }
        }

        return ResultadoVentas(
            ventas = ventas,
            migracionCompleta = ventas.size == registros.size,
            conteniaFormatoLegado = conteniaFormatoLegado
        )
    }

    private fun decodificarVentaActual(partes: List<String>): Venta? {
        if (partes.size < 12) return null
        return try {
            Venta(
                id = partes[1],
                productoId = partes[2].valorNullable(),
                productoNombre = partes[3],
                cantidad = partes[4].toInt(),
                total = partes[5].toInt(),
                precioUnitario = partes[6].toInt(),
                costoUnitario = partes[7].toInt(),
                metodoPago = partes[8],
                rutaImagen = partes[9].valorNullable(),
                fechaEpochMillis = partes[10].toLong(),
                fechaTextoLegada = partes[11].valorNullable()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun decodificarVentaLegada(
        partes: List<String>,
        productos: List<Producto>,
        ahoraEpochMillis: Long
    ): Venta? {
        if (partes.size < 8) return null
        return try {
            val nombre = partes[0]
            Venta(
                id = nuevoId(),
                productoId = resolverProductoId(nombre, productos),
                productoNombre = nombre,
                cantidad = partes[1].toInt(),
                total = partes[2].toInt(),
                precioUnitario = partes[3].toInt(),
                costoUnitario = partes[4].toInt(),
                metodoPago = partes[5],
                rutaImagen = partes[6].valorNullable(),
                fechaEpochMillis = inferirFechaLegada(partes[7], ahoraEpochMillis),
                fechaTextoLegada = partes[7]
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resolverProductoId(nombre: String, productos: List<Producto>): String? {
        val coincidencias = productos.filter {
            it.nombre.trim().equals(nombre.trim(), ignoreCase = true)
        }
        return coincidencias.singleOrNull()?.id
    }

    private fun inferirFechaLegada(texto: String, ahoraEpochMillis: Long): Long {
        return try {
            val zona = ZoneId.systemDefault()
            val ahora = Instant.ofEpochMilli(ahoraEpochMillis).atZone(zona)
            val formato = DateTimeFormatter.ofPattern("dd/MM HH:mm yyyy")
            var fecha = LocalDateTime.parse("$texto ${ahora.year}", formato).atZone(zona)
            if (fecha.isAfter(ahora.plusDays(1))) fecha = fecha.minusYears(1)
            fecha.toInstant().toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    private fun String.valorNullable(): String? = if (this == VALOR_NULO) null else this
}
