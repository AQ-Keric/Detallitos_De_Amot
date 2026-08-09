package org.example.project.dominio

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 1. LOS MODOS DE BÚSQUEDA (¡Volvió la Semana!)
enum class ModoTiempo {
    DIA, SEMANA, MES, TOTAL
}

// 2. EL MOTOR DE FILTRADO EXACTO
object FiltroVentas {

    fun aplicarFiltro(ventas: List<Venta>, modo: ModoTiempo, fechaReferencia: LocalDate): List<Venta> {
        if (modo == ModoTiempo.TOTAL) return ventas

        val formateador = DateTimeFormatter.ofPattern("dd/MM HH:mm yyyy")
        val anioActual = LocalDate.now().year

        // Calculamos los bordes de la semana de la fecha que elijas
        val inicioSemana = fechaReferencia.with(DayOfWeek.MONDAY)
        val finSemana = fechaReferencia.with(DayOfWeek.SUNDAY)

        return ventas.filter { venta ->
            try {
                // Truco para leer tu formato actual de la base de datos
                val textoFechaConAnio = "${venta.fecha} $anioActual"
                val fechaReal = LocalDateTime.parse(textoFechaConAnio, formateador).toLocalDate()

                // Filtramos según el modo seleccionado
                when (modo) {
                    ModoTiempo.DIA -> fechaReal.isEqual(fechaReferencia)
                    ModoTiempo.SEMANA -> !fechaReal.isBefore(inicioSemana) && !fechaReal.isAfter(finSemana)
                    ModoTiempo.MES -> fechaReal.year == fechaReferencia.year && fechaReal.monthValue == fechaReferencia.monthValue
                    ModoTiempo.TOTAL -> true
                }
            } catch (e: Exception) {
                true // Si falla el formato, mostramos la venta igual para no perder el dato
            }
        }
    }
}