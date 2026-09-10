package org.example.project.ui

import androidx.compose.runtime.Composable
import org.example.project.dominio.*

interface ControladorArchivos {
    fun exportarRespaldo(estado: EstadoDatos)
    fun exportarVentas(ventas: List<Venta>)
    fun importarRespaldo()
}

@Composable
expect fun rememberControladorArchivos(
    onPreparado: (RespaldoPreparado) -> Unit,
    onTerminado: (String?) -> Unit
): ControladorArchivos

@Composable
expect fun ManejarVolver(enabled: Boolean, onBack: () -> Unit)
