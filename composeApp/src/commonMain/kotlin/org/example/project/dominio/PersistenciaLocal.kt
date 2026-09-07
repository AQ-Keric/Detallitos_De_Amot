package org.example.project.dominio

interface MotorPersistencia {
    fun guardarProductos(productos: List<Producto>)
    fun cargarProductos(): List<Producto>
    fun guardarVentas(ventas: List<Venta>)
    fun cargarVentas(): List<Venta>
    fun importarImagen(uri: String): String
    fun guardarMeta(meta: Int)
    fun cargarMeta(): Int
    fun cargarEstado(): EstadoDatos = EstadoDatos(cargarProductos(), cargarVentas(), cargarMeta())
    // Los motores reales sobrescriben esto con una única escritura atómica.
    fun guardarEstado(estado: EstadoDatos) {
        guardarProductos(estado.productos); guardarVentas(estado.ventas); guardarMeta(estado.meta)
    }
    fun restaurarEstado(estado: EstadoDatos) = guardarEstado(estado)
    fun avisoRecuperacion(): String? = null
}

object PersistenciaLocal {
    private var cache: EstadoDatos? = null
    var motor: MotorPersistencia? = null
        set(value) { field = value; cache = null }

    @Synchronized fun estado(): EstadoDatos = cache ?: requireNotNull(motor) { "Almacenamiento no disponible" }
        .cargarEstado().also { it.validar(); cache = it }

    @Synchronized private fun guardar(nuevo: EstadoDatos) {
        nuevo.validar()
        requireNotNull(motor) { "Almacenamiento no disponible" }.guardarEstado(nuevo)
        cache = nuevo // Solo publicar los cambios después de confirmar la escritura.
    }
    @Synchronized fun obtenerMeta(clave: String): Int? {
        val e = estado()
        return when {
            clave == "total" -> e.meta.takeIf { e.metaGlobalActiva }
            clave in e.periodosSinMeta -> null
            else -> e.metasPorPeriodo[clave] ?: e.metasRepetidas[clave.substringBefore(':')]
        }
    }
    @Synchronized fun guardarMetaRepetida(clave: String, monto: Int, todos: Boolean = false) {
        MetasVentas.validarClave(clave)
        val tipo = clave.substringBefore(':')
        require(tipo in setOf("dia", "semana", "mes") && monto > 0)
        val e = estado()
        val tipos = if (todos) setOf("dia", "semana", "mes") else setOf(tipo)
        guardar(e.copy(metasRepetidas = e.metasRepetidas + tipos.associateWith { monto },
            metasPorPeriodo = e.metasPorPeriodo - clave, periodosSinMeta = e.periodosSinMeta - clave))
    }
    @Synchronized fun eliminarMetaRepetida(tipo: String) {
        require(tipo in setOf("dia", "semana", "mes"))
        guardar(estado().copy(metasRepetidas = estado().metasRepetidas - tipo))
    }
    @Synchronized fun guardarMeta(clave: String, meta: Int) {
        require(meta > 0) { "La meta debe ser mayor que cero" }
        if (clave == "total") guardarMeta(meta)
        else {
            MetasVentas.validarClave(clave)
            guardar(estado().copy(metasPorPeriodo = estado().metasPorPeriodo + (clave to meta), periodosSinMeta = estado().periodosSinMeta - clave))
        }
    }
    fun obtenerMeta(): Int = estado().meta
    @Synchronized fun guardarMeta(meta: Int) {
        val e = estado()
        guardar(e.copy(meta = meta, metaGlobalActiva = true,
            metaGlobalDesde = if (e.metaGlobalActiva) e.metaGlobalDesde else System.currentTimeMillis()))
    }
    @Synchronized fun eliminarMeta(clave: String) {
        val e = estado()
        if (clave == "total") guardar(e.copy(metaGlobalActiva = false, metaGlobalDesde = 0L))
        else {
            MetasVentas.validarClave(clave)
            guardar(e.copy(metasPorPeriodo = e.metasPorPeriodo - clave, periodosSinMeta = e.periodosSinMeta + clave))
        }
    }
    @Synchronized fun reiniciarMetaGlobal(monto: Int, desde: Long = System.currentTimeMillis()) {
        require(monto > 0 && desde > 0L) { "Monto o inicio de meta inválido" }
        guardar(estado().copy(meta = monto, metaGlobalActiva = true, metaGlobalDesde = desde))
    }
    fun obtenerProductos(): List<Producto> = estado().productos.toList()
    fun obtenerVentas(): List<Venta> = estado().ventas.toList()

    @Synchronized fun guardarProducto(producto: Producto) {
        val e = estado()
        val productos = e.productos.toMutableList()
        val i = productos.indexOfFirst { it.id == producto.id }
        if (i < 0) productos.add(producto) else productos[i] = producto
        guardar(e.copy(productos = productos))
    }
    @Synchronized fun eliminarProducto(id: String) = guardar(estado().copy(productos = estado().productos.filterNot { it.id == id }))

    @Synchronized fun registrarVenta(venta: Venta) {
        val e = estado()
        require(e.ventas.none { it.id == venta.id }) { "La venta ya existe" }
        require(venta.cantidad > 0) { "Ingresa una cantidad mayor que cero" }
        val producto = e.productos.find { it.id == venta.productoId } ?: error("El producto ya no existe")
        require(venta.cantidad <= producto.stock) { "Stock insuficiente" }
        // El snapshot lo toma el repositorio para no depender de una pantalla desactualizada.
        val historica = venta.copy(productoNombre = producto.nombre, precioUnitario = producto.precioVenta,
            costoUnitario = producto.costoProduccion, rutaImagen = producto.rutaImagen)
        guardar(e.copy(ventas = listOf(historica) + e.ventas,
            productos = e.productos.map { if (it.id == producto.id) it.copy(stock = it.stock - venta.cantidad) else it }))
    }

    @Synchronized fun eliminarVenta(ventaId: String) {
        val e = estado()
        val venta = e.ventas.find { it.id == ventaId } ?: return
        // Una relación legada ambigua nunca se resuelve de nuevo por nombre.
        val producto = e.productos.find { venta.productoId != null && it.id == venta.productoId }
        val productos = e.productos.toMutableList()
        if (producto != null) {
            val stock = producto.stock.toLong() + venta.cantidad
            require(stock <= Int.MAX_VALUE) { "El stock excede el máximo permitido" }
            productos[productos.indexOf(producto)] = producto.copy(stock = stock.toInt())
        } else {
            productos.add(Producto(venta.productoId ?: nuevoId(), venta.productoNombre,
                venta.precioUnitario, venta.costoUnitario, venta.cantidad, venta.rutaImagen))
        }
        guardar(e.copy(productos = productos, ventas = e.ventas.filterNot { it.id == ventaId }))
    }

    fun prepararImagen(ruta: String?): String? = ruta?.let { requireNotNull(motor).importarImagen(it) }
    @Synchronized fun importarEstado(estado: EstadoDatos) {
        val nuevo = estado.copy(productos = estado.productos.toList(), ventas = estado.ventas.toList())
        nuevo.validar()
        requireNotNull(motor).restaurarEstado(nuevo)
        cache = nuevo
    }
    fun exportarDatosParaRescate(): String = CodecEstado.codificar(estado()).toString(Charsets.UTF_8)
}
