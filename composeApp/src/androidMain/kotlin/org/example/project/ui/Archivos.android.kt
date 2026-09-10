package org.example.project.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.dominio.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
actual fun ManejarVolver(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)

@Composable
actual fun rememberControladorArchivos(onPreparado: (RespaldoPreparado) -> Unit, onTerminado: (String?) -> Unit): ControladorArchivos {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val preparado by rememberUpdatedState(onPreparado)
    val terminado by rememberUpdatedState(onTerminado)
    var pendiente by remember { mutableStateOf<EstadoDatos?>(null) }
    var ventas by remember { mutableStateOf<List<Venta>?>(null) }
    val zip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) terminado(null) else scope.launch {
            try {
                val datos = requireNotNull(pendiente) { "Vuelve a iniciar la exportación" }
                withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { Respaldo.exportar(datos, it) }
                }
                terminado("Respaldo guardado correctamente.")
            } catch (e: Exception) { terminado("No se pudo exportar. El archivo puede estar incompleto: ${e.message}") }
            finally { pendiente = null }
        }
    }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) terminado(null) else scope.launch {
            try {
                val datos = requireNotNull(ventas) { "Vuelve a iniciar la exportación" }
                withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(Respaldo.csv(datos).toByteArray(Charsets.UTF_8)) }
                }
                terminado("CSV guardado correctamente.")
            } catch (e: Exception) { terminado("No se pudo exportar: ${e.message}") }
            finally { ventas = null }
        }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) terminado(null) else scope.launch {
            try {
                val motor = PersistenciaLocal.motor as MotorArchivo
                val resultado = withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openInputStream(uri)).use { Respaldo.preparar(it, motor.directorio) }
                }
                preparado(resultado)
            } catch (e: Exception) { terminado("No se pudo abrir el respaldo: ${e.message}") }
        }
    }
    return remember {
        object : ControladorArchivos {
            private fun fecha() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            override fun exportarRespaldo(estado: EstadoDatos) { pendiente = estado; zip.launch("Detallitos_${fecha()}.zip") }
            override fun exportarVentas(datos: List<Venta>) { ventas = datos; csv.launch("Ventas_${fecha()}.csv") }
            override fun importarRespaldo() { importar.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) }
        }
    }
}
