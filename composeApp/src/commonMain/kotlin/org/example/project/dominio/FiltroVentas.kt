package org.example.project.dominio

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// 1. LOS MODOS DE BÚSQUEDA (¡Volvió la Semana!)
enum class ModoTiempo {
    DIA, SEMANA, MES, TOTAL
}

// 2. EL MOTOR DE FILTRADO EXACTO
object FiltroVentas {

    fun aplicarFiltro(ventas: List<Venta>, modo: ModoTiempo, fechaReferencia: LocalDate): List<Venta> {
        if (modo == ModoTiempo.TOTAL) return ventas

        // Calculamos los bordes de la semana de la fecha que elijas
        val inicioSemana = fechaReferencia.with(DayOfWeek.MONDAY)
        val finSemana = fechaReferencia.with(DayOfWeek.SUNDAY)

        return ventas.filter { venta ->
            if (venta.fechaEpochMillis <= 0L) {
                true
            } else {
                val fechaReal = Instant.ofEpochMilli(venta.fechaEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                when (modo) {
                    ModoTiempo.DIA -> fechaReal.isEqual(fechaReferencia)
                    ModoTiempo.SEMANA -> !fechaReal.isBefore(inicioSemana) && !fechaReal.isAfter(finSemana)
                    ModoTiempo.MES -> fechaReal.year == fechaReferencia.year && fechaReal.monthValue == fechaReferencia.monthValue
                    ModoTiempo.TOTAL -> true
                }
            }
        }
    }
}
