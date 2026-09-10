package org.example.project.dominio

object CalculadoraFinanciera {

    //Función 1: Suma total de todas las ventas.
    fun calcularIngresosTotales(ventas: List<Venta>): Long{
        return ventas.sumOf { it.total.toLong() }
    }

    //Función 2: Costos Totales.
    fun calcularCostosTotales(ventas: List<Venta>):Long{
        return ventas.sumOf {it.costoUnitario.toLong() * it.cantidad}
    }

    //Función 3: Ganancia neta.
    fun calcularGananciaNeta(ventas: List<Venta>): Long{
        return calcularIngresosTotales(ventas) - calcularCostosTotales(ventas)
    }

    //Función 4: Productos más vendidos.
    fun obtenerTopVentas(ventas: List<Venta>, limite: Int = 3): List<Pair<String,Long>>{
        val ventasAgrupadas = ventas.groupBy(::claveProducto)

        val totalesPorProducto = ventasAgrupadas.map {(_, listaVentasDelGrupo) ->
            val sumaCantidades = listaVentasDelGrupo.sumOf {it.cantidad.toLong()}
            val nombre = listaVentasDelGrupo.maxByOrNull { it.fechaEpochMillis }?.productoNombre.orEmpty()
            Pair(nombre,sumaCantidades)
        }
        val ordenadosDeMayorAMenor = totalesPorProducto.sortedByDescending{ it.second }

        val topFinal = ordenadosDeMayorAMenor.take(limite)

        return topFinal
    }

    //Función 5: Producto que da más ganancia.
    fun obtenerTopProductoGanancia(ventas: List<Venta>, limite: Int = 3): List<Pair<String, Long>>{
        val ventasAgrupadas = ventas.groupBy(::claveProducto)
        val ventasTotalesPorProducto = ventasAgrupadas.map {(_, listaVentasDelGrupo)->
            val nombre = listaVentasDelGrupo.maxByOrNull { it.fechaEpochMillis }?.productoNombre.orEmpty()
            val sumatotal = listaVentasDelGrupo.sumOf { it.ganancia }
            Pair(nombre,sumatotal)
        }
        val ordenadosDeMayorAMenor = ventasTotalesPorProducto.sortedByDescending{ it.second }
        val topFinal = ordenadosDeMayorAMenor.take(limite)
        return topFinal
    }

    //Función 6: Ingresos según cómo pagaron (Efectivo vs Transferencia)
    fun ingresosPorMetodoPago(ventas: List<Venta>): List<Pair<String, Long>> {
        val ventasAgrupadas = ventas.groupBy { it.metodoPago }
        return ventasAgrupadas.map { (metodo, lista) ->
            val totalPorMetodo = lista.sumOf { it.total.toLong() }
            Pair(metodo, totalPorMetodo)
        }
    }

    // Función 7: Rendimiento y Promedios (Corregida con Matemática Real)
    fun calcularPromedios(ventas: List<Venta>): Map<String, Long> {
        if (ventas.isEmpty()) return emptyMap()

        val ingresosTotales = calcularIngresosTotales(ventas)
        val ticketPromedio = ingresosTotales / ventas.size

        val fechasReales = ventas.mapNotNull { venta ->
            if (venta.fechaEpochMillis <= 0L) null
            else java.time.Instant.ofEpochMilli(venta.fechaEpochMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }

        if (fechasReales.isEmpty()) {
            return mapOf("Por Venta" to ticketPromedio, "Diario" to 0L, "Semanal" to 0L, "Mensual" to 0L)
        }

        val diasDistintos = fechasReales.distinct().size

        val semanasDistintas = fechasReales.map { it.with(java.time.DayOfWeek.MONDAY) }.distinct().size

        val mesesDistintos = fechasReales.map { java.time.YearMonth.from(it) }.distinct().size

        val ingresosConFecha = ventas.filter { it.fechaEpochMillis > 0L }.sumOf { it.total.toLong() }
        val promedioDiario = ingresosConFecha / diasDistintos
        val promedioSemanal = ingresosConFecha / semanasDistintas
        val promedioMensual = ingresosConFecha / mesesDistintos

        return mapOf(
            "Por Venta" to ticketPromedio,
            "Diario" to promedioDiario,
            "Semanal" to promedioSemanal,
            "Mensual" to promedioMensual
        )
    }

    private fun claveProducto(venta: Venta): String =
        venta.productoId ?: "legacy:${venta.productoNombre.trim().lowercase()}"
}
