package org.example.project.dominio

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Properties

data class EstadoDatos(
    val productos: List<Producto> = emptyList(),
    val ventas: List<Venta> = emptyList(),
    val meta: Int = 200_000,
    val metasPorPeriodo: Map<String, Int> = emptyMap(),
    val metaGlobalActiva: Boolean = true,
    val metaGlobalDesde: Long = 0L
) {
    fun validar() {
        require(metaGlobalDesde >= 0L) { "Inicio de meta inválido" }
        require(metasPorPeriodo.size <= 100_000) { "Demasiadas metas" }
        metasPorPeriodo.forEach { (clave, monto) -> MetasVentas.validarClave(clave); require(monto > 0) { "Meta inválida" } }
        require(meta > 0) { "La meta debe ser mayor que cero" }
        require(productos.size <= 100_000 && ventas.size <= 100_000) { "Demasiados registros" }
        require(productos.map { it.id }.distinct().size == productos.size) { "Hay productos con IDs repetidos" }
        require(ventas.map { it.id }.distinct().size == ventas.size) { "Hay ventas con IDs repetidos" }
        productos.forEach {
            require(it.umbralStockBajo == null || it.umbralStockBajo >= 0) { "Umbral de stock inválido" }
            require(it.id.isNotBlank() && it.nombre.isNotBlank()) { "Producto sin ID o nombre" }
            require(it.precioVenta >= 0 && it.costoProduccion >= 0 && it.stock >= 0) { "Producto con valores negativos" }
        }
        ventas.forEach {
            require(it.id.isNotBlank() && it.productoNombre.isNotBlank()) { "Venta sin ID o nombre" }
            require(it.cantidad > 0 && it.total >= 0 && it.precioUnitario >= 0 && it.costoUnitario >= 0) { "Venta con valores inválidos" }
            require(it.fechaEpochMillis >= 0 && it.metodoPago.isNotBlank()) { "Venta con fecha o pago inválido" }
            require(it.productoId == null || it.productoId.isNotBlank()) { "Referencia de producto inválida" }
        }
    }
}

/** Formato versionado y escapado: los nombres pueden contener separadores, saltos y emojis. */
object CodecEstado {
    fun codificar(estado: EstadoDatos): ByteArray {
        estado.validar()
        val p = Properties()
        fun put(k: String, v: Any?) { if (v != null) p.setProperty(k, v.toString()) }
        put("version", 4); put("app", "detallitos-de-amor"); put("meta", estado.meta)
        put("metaGlobalActiva", estado.metaGlobalActiva); put("metaGlobalDesde", estado.metaGlobalDesde)
        estado.metasPorPeriodo.forEach { (clave, monto) -> put("metaPeriodo.$clave", monto) }
        put("productos", estado.productos.size); put("ventas", estado.ventas.size)
        estado.productos.forEachIndexed { i, v ->
            val k = "p.$i."
            put(k+"id", v.id); put(k+"nombre", v.nombre); put(k+"precio", v.precioVenta)
            put(k+"costo", v.costoProduccion); put(k+"stock", v.stock); put(k+"umbralStockBajo", v.umbralStockBajo ?: "desactivado"); put(k+"imagen", v.rutaImagen)
        }
        estado.ventas.forEachIndexed { i, v ->
            val k = "v.$i."
            put(k+"id", v.id); put(k+"productoId", v.productoId); put(k+"nombre", v.productoNombre)
            put(k+"cantidad", v.cantidad); put(k+"total", v.total); put(k+"precio", v.precioUnitario)
            put(k+"costo", v.costoUnitario); put(k+"pago", v.metodoPago); put(k+"imagen", v.rutaImagen)
            put(k+"fecha", v.fechaEpochMillis); put(k+"fechaLegada", v.fechaTextoLegada)
        }
        return ByteArrayOutputStream().also { p.storeToXML(it, "Respaldo Detallitos de Amor", "UTF-8") }.toByteArray()
    }

    fun decodificar(bytes: ByteArray): EstadoDatos {
        require(bytes.size <= 32 * 1024 * 1024) { "Archivo de datos demasiado grande" }
        val p = Properties().apply { loadFromXML(ByteArrayInputStream(bytes)) }
        fun str(k: String) = requireNotNull(p.getProperty(k)) { "Falta el campo $k" }
        fun num(k: String) = str(k).toInt()
        require(str("app") == "detallitos-de-amor" && num("version") in 1..4) { "Formato de respaldo no compatible" }
        val np = num("productos"); val nv = num("ventas")
        require(np in 0..100_000 && nv in 0..100_000) { "Cantidad de registros inválida" }
        return EstadoDatos(
            List(np) { i ->
                val k = "p.$i."
                Producto(str(k+"id"), str(k+"nombre"), num(k+"precio"), num(k+"costo"), num(k+"stock"), p.getProperty(k+"imagen"),
                    if (num("version") < 4) 3 else str(k+"umbralStockBajo").let { if (it == "desactivado") null else it.toInt() })
            },
            List(nv) { i ->
                val k = "v.$i."
                Venta(str(k+"id"), p.getProperty(k+"productoId"), str(k+"nombre"), num(k+"cantidad"),
                    num(k+"total"), num(k+"precio"), num(k+"costo"), str(k+"pago"), p.getProperty(k+"imagen"),
                    str(k+"fecha").toLong(), p.getProperty(k+"fechaLegada"))
            }, num("meta"),
            if (num("version") >= 2) p.stringPropertyNames().filter { it.startsWith("metaPeriodo.") }
                .associate { it.removePrefix("metaPeriodo.") to num(it) } else emptyMap(),
            if (num("version") >= 3) str("metaGlobalActiva").toBooleanStrict() else true,
            if (num("version") >= 3) str("metaGlobalDesde").toLong() else 0L
        ).also { it.validar() }
    }
}
