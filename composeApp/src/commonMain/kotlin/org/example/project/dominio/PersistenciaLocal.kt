package org.example.project.dominio

interface MotorPersistencia {
    fun guardarProductos(productos: List<Producto>)
    fun cargarProductos(): List<Producto>
    fun guardarVentas(ventas: List<Venta>)
    fun cargarVentas(): List<Venta>
    fun importarImagen(uri: String): String
    fun guardarMeta(meta: Int)
    fun cargarMeta(): Int
}

object PersistenciaLocal {
    private val productosCache = mutableListOf<Producto>()
    private val ventasCache = mutableListOf<Venta>()
    private var productosCargados = false
    private var ventasCargadas = false
    private var metaCache: Int? = null

    var motor: MotorPersistencia? = null
        set(value) {
            field = value
            productosCache.clear()
            ventasCache.clear()
            productosCargados = false
            ventasCargadas = false
            metaCache = null
        }

    fun obtenerMeta(): Int {
        if (metaCache == null) {
            metaCache = motor?.cargarMeta() ?: 200000
        }
        return metaCache!!
    }

    fun guardarMeta(meta: Int) {
        metaCache = meta
        motor?.guardarMeta(meta)
    }

    fun obtenerProductos(): List<Producto> {
        asegurarProductosCargados()
        return productosCache
    }

    fun guardarProducto(producto: Producto) {
        asegurarProductosCargados()
        val index = productosCache.indexOfFirst { it.id == producto.id }
        if (index != -1) {
            productosCache[index] = producto
        } else {
            productosCache.add(producto)
        }
        motor?.guardarProductos(productosCache)
    }

    fun eliminarProducto(id: String) {
        asegurarProductosCargados()
        productosCache.removeAll { it.id == id }
        motor?.guardarProductos(productosCache)
    }

    fun obtenerVentas(): List<Venta> {
        asegurarVentasCargadas()
        return ventasCache
    }

    fun registrarVenta(venta: Venta) {
        asegurarProductosCargados()
        asegurarVentasCargadas()
        require(ventasCache.none { it.id == venta.id }) { "La venta ya existe" }

        val producto = venta.productoId?.let { id -> productosCache.find { it.id == id } }
            ?: error("No se encontró el producto asociado a la venta")
        val nuevoStock = producto.stock - venta.cantidad
        require(nuevoStock >= 0) { "Stock insuficiente" }

        ventasCache.add(0, venta)
        motor?.guardarVentas(ventasCache)
        guardarProducto(producto.copy(stock = nuevoStock))
    }

    fun eliminarVenta(ventaId: String) {
        asegurarProductosCargados()
        asegurarVentasCargadas()
        val venta = ventasCache.find { it.id == ventaId } ?: return

        ventasCache.removeAll { it.id == ventaId }
        motor?.guardarVentas(ventasCache)

        val producto = buscarProductoDeVenta(venta)
        if (producto != null) {
            guardarProducto(producto.copy(stock = producto.stock + venta.cantidad))
        } else {
            guardarProducto(
                Producto(
                    id = venta.productoId ?: nuevoId(),
                    nombre = venta.productoNombre,
                    precioVenta = venta.precioUnitario,
                    costoProduccion = venta.costoUnitario,
                    stock = venta.cantidad,
                    rutaImagen = venta.rutaImagen
                )
            )
        }
    }

    fun prepararImagen(ruta: String?): String? {
        if (ruta == null) return null
        return motor?.importarImagen(ruta) ?: ruta
    }

    fun exportarDatosParaRescate(): String {
        val productos = obtenerProductos()
        val ventas = obtenerVentas()
        val meta = obtenerMeta()
        return "Productos: ${productos.size}, Ventas: ${ventas.size}, Meta: $meta"
    }

    private fun asegurarProductosCargados() {
        if (!productosCargados) {
            productosCache.clear()
            productosCache.addAll(motor?.cargarProductos() ?: emptyList())
            productosCargados = true
        }
    }

    private fun asegurarVentasCargadas() {
        if (!ventasCargadas) {
            ventasCache.clear()
            ventasCache.addAll(motor?.cargarVentas() ?: emptyList())
            ventasCargadas = true
        }
    }

    private fun buscarProductoDeVenta(venta: Venta): Producto? {
        venta.productoId?.let { id ->
            productosCache.find { it.id == id }?.let { return it }
        }

        if (venta.productoId == null) {
            val coincidencias = productosCache.filter {
                it.nombre.trim().equals(venta.productoNombre.trim(), ignoreCase = true)
            }
            if (coincidencias.size == 1) return coincidencias.first()
        }
        return null
    }
}
