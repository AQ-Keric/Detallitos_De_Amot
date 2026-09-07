package org.example.project.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.dominio.*
import javax.swing.JFileChooser
import javax.swing.JOptionPane

@Composable
actual fun ManejarVolver(enabled: Boolean, onBack: () -> Unit) { /* El escritorio usa los botones de navegación. */ }

@Composable
actual fun rememberControladorArchivos(onPreparado: (RespaldoPreparado) -> Unit, onTerminado: (String?) -> Unit): ControladorArchivos {
    val scope = rememberCoroutineScope()
    val preparado by rememberUpdatedState(onPreparado)
    val terminado by rememberUpdatedState(onTerminado)
    return remember {
        object : ControladorArchivos {
            private fun guardar(nombre: String, escribir: (java.io.OutputStream) -> Unit) {
                val selector = JFileChooser().apply { selectedFile = java.io.File(nombre) }
                if (selector.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) { terminado(null); return }
                val archivo = selector.selectedFile
                if (archivo.exists() && JOptionPane.showConfirmDialog(null, "¿Reemplazar ${archivo.name}?", "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { terminado(null); return }
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { archivo.outputStream().use(escribir) }
                        terminado("Archivo guardado correctamente.")
                    } catch (e: Exception) { terminado("No se pudo guardar: ${e.message}") }
                }
            }
            override fun exportarRespaldo(estado: EstadoDatos) = guardar("Detallitos.zip") { Respaldo.exportar(estado, it) }
            override fun exportarVentas(ventas: List<Venta>) = guardar("Ventas.csv") { it.write(Respaldo.csv(ventas).toByteArray(Charsets.UTF_8)) }
            override fun importarRespaldo() {
                val selector = JFileChooser()
                if (selector.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) { terminado(null); return }
                scope.launch {
                    try {
                        val resultado = withContext(Dispatchers.IO) {
                            selector.selectedFile.inputStream().use { Respaldo.preparar(it, (PersistenciaLocal.motor as MotorArchivo).directorio) }
                        }
                        preparado(resultado)
                    } catch (e: Exception) { terminado("No se pudo leer el respaldo: ${e.message}") }
                }
            }
        }
    }
}
