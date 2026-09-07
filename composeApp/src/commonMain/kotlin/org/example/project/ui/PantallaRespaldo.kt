package org.example.project.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.dominio.*

@Composable
fun PantallaRespaldo(onOcupado: (Boolean) -> Unit, onActualizado: () -> Unit, onVolver: () -> Unit) {
    var ocupado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var preparado by remember { mutableStateOf<RespaldoPreparado?>(null) }
    val scope = rememberCoroutineScope()
    val actual = runCatching { PersistenciaLocal.estado() }.getOrNull()
    val controlador = rememberControladorArchivos(
        onPreparado = { preparado = it; ocupado = false },
        onTerminado = { mensaje = it; ocupado = false }
    )
    LaunchedEffect(ocupado) { onOcupado(ocupado) }
    DisposableEffect(Unit) { onDispose { preparado?.descartar(); onOcupado(false) } }
    ManejarVolver(enabled = true) { if (!ocupado) onVolver() }
    preparado?.let { respaldo ->
        AlertDialog(
            onDismissRequest = { if (!ocupado) { respaldo.descartar(); preparado = null } },
            title = { Text("¿Restaurar este respaldo?") },
            text = { Text("Contiene ${respaldo.estado.productos.size} productos y ${respaldo.estado.ventas.size} ventas. Reemplazará tus datos actuales, incluyendo imágenes y meta. Exporta primero un respaldo si quieres conservarlos aparte.") },
            confirmButton = { TextButton(enabled = !ocupado, onClick = {
                ocupado = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { respaldo.restaurar() }
                        preparado = null // Las imágenes ahora forman parte del almacenamiento.
                        onActualizado()
                        mensaje = "Respaldo restaurado correctamente."
                    } catch (e: Exception) { mensaje = "No se pudo restaurar: ${e.message}" }
                    finally { ocupado = false }
                }
            }) { Text("Restaurar") } },
            dismissButton = { TextButton(enabled = !ocupado, onClick = { respaldo.descartar(); preparado = null }) { Text("Cancelar") } }
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Respaldo", style = MaterialTheme.typography.h5)
        Text("${actual?.productos?.size ?: 0} productos · ${actual?.ventas?.size ?: 0} ventas")
        Text("Respaldo completo", style = MaterialTheme.typography.h6)
        Text("Productos, ventas, metas y fotos en un solo archivo.")
        Button(enabled = !ocupado && actual != null, modifier = Modifier.fillMaxWidth(), onClick = {
            ocupado = true
            try { controlador.exportarRespaldo(requireNotNull(actual)) } catch (e: Exception) { ocupado = false; mensaje = e.message }
        }) { Text("Guardar respaldo ZIP") }
        OutlinedButton(enabled = !ocupado, modifier = Modifier.fillMaxWidth(), onClick = {
            ocupado = true
            try { controlador.importarRespaldo() } catch (e: Exception) { ocupado = false; mensaje = e.message }
        }) { Text("Restaurar un respaldo") }
        Divider()
        Text("Ventas para planilla", style = MaterialTheme.typography.h6)
        Text("Tus ventas en CSV para abrir en una planilla.")
        OutlinedButton(enabled = !ocupado && actual != null, modifier = Modifier.fillMaxWidth(), onClick = {
            ocupado = true
            try { controlador.exportarVentas(requireNotNull(actual).ventas) } catch (e: Exception) { ocupado = false; mensaje = e.message }
        }) { Text("Exportar ventas CSV") }
        if (ocupado) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Preparando archivo…") }
        mensaje?.let { Text(it, color = Color(0xFF444444)) }
    }
}
