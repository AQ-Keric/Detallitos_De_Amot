package org.example.project.dominio

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object MetasVentas {
    fun clave(modo: ModoTiempo, fecha: LocalDate): String = when (modo) {
        ModoTiempo.DIA -> "dia:$fecha"
        ModoTiempo.SEMANA -> "semana:${fecha.with(DayOfWeek.MONDAY)}"
        ModoTiempo.MES -> "mes:${YearMonth.from(fecha)}"
        ModoTiempo.TOTAL -> "total"
    }
    fun validarClave(clave: String) {
        val fecha = clave.substringAfter(':', "")
        when (clave.substringBefore(':')) {
            "dia" -> require(clave == "dia:${LocalDate.parse(fecha)}")
            "semana" -> require(clave == "semana:${LocalDate.parse(fecha).with(DayOfWeek.MONDAY)}")
            "mes" -> require(clave == "mes:${YearMonth.parse(fecha)}")
            else -> error("Período de meta inválido")
        }
    }
    fun progreso(ingresos: Long, meta: Int): ProgresoMeta {
        require(meta > 0 && ingresos >= 0)
        return ProgresoMeta(ingresos.toDouble() / meta * 100.0,
            (meta.toLong() - ingresos).coerceAtLeast(0), (ingresos - meta).coerceAtLeast(0))
    }
}
data class ProgresoMeta(val porcentaje: Double, val faltante: Long, val excedente: Long) {
    val barra: Float get() = (porcentaje / 100.0).coerceIn(0.0, 1.0).toFloat()
}
