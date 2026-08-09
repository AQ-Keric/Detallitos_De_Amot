package org.example.project.dominio

interface MotorPersistencia {
    fun guardarProductos(productos: List<Producto>)
    fun cargarProductos(): List<Producto>
    fun guardarVentas(ventas: List<Venta>)
    fun cargarVentas(): List<Venta>
    fun importarImagen(uri: String): String

    // --- NUEVAS FUNCIONES PARA LA META ---
    fun guardarMeta(meta: Int)
    fun cargarMeta(): Int
}

object PersistenciaLocal {
    var motor: MotorPersistencia? = null

    private val productosCache = mutableListOf<Producto>()
    private val ventasCache = mutableListOf<Venta>()
    private var metaCache: Int? = null // Cache para la meta

    // --- META DE VENTAS ---
    fun obtenerMeta(): Int {
        if (metaCache == null) {
            // Si no hay nada guardado, carga del motor. Si el motor no tiene nada, da 200000 por defecto.
            metaCache = motor?.cargarMeta() ?: 200000
        }
        return metaCache!!
    }

    fun guardarMeta(meta: Int) {
        metaCache = meta
        motor?.guardarMeta(meta)
    }

    // --- PRODUCTOS ---
    fun obtenerProductos(): List<Producto> {
        if (productosCache.isEmpty()) {
            val guardados = motor?.cargarProductos() ?: emptyList()
            productosCache.addAll(guardados)
        }
        return productosCache
    }

    fun guardarProducto(producto: Producto) {
        val index = productosCache.indexOfFirst { it.id == producto.id }
        if (index != -1) {
            productosCache[index] = producto
        } else {
            productosCache.add(producto)
        }
        motor?.guardarProductos(productosCache)
    }

    fun eliminarProducto(id: String) {
        productosCache.removeAll { it.id == id }
        motor?.guardarProductos(productosCache)
    }

    // --- VENTAS ---
    fun obtenerVentas(): List<Venta> {
        if (ventasCache.isEmpty()) {
            val guardadas = motor?.cargarVentas() ?: emptyList()
            ventasCache.addAll(guardadas)
        }
        return ventasCache
    }

    fun registrarVenta(venta: Venta) {
        ventasCache.add(0, venta)
        motor?.guardarVentas(ventasCache)

        val prod = productosCache.find { it.nombre == venta.productoNombre }
        if (prod != null) {
            val nuevoStock = prod.stock - venta.cantidad
            if (nuevoStock >= 0) {
                guardarProducto(prod.copy(stock = nuevoStock))
            }
        }
    }

    fun eliminarVenta(venta: Venta) {
        ventasCache.remove(venta)
        motor?.guardarVentas(ventasCache)

        val prod = productosCache.find { it.nombre == venta.productoNombre }
        if (prod != null) {
            guardarProducto(prod.copy(stock = prod.stock + venta.cantidad))
        } else {
            val revivido = Producto(
                nombre = venta.productoNombre,
                precioVenta = venta.precioUnitario,
                costoProduccion = venta.costoUnitario,
                stock = venta.cantidad,
                rutaImagen = venta.rutaImagen
            )
            guardarProducto(revivido)
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

        // Aquí generamos un JSON o XML con todo el contenido
        // y lo guardamos en android.os.Environment.getExternalStorageDirectory()
        // o en context.getExternalFilesDir(...) para sacarlo por ADB.
        return "Productos: ${productos.size}, Ventas: ${ventas.size}, Meta: $meta"
    }
}
