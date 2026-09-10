package org.example.project.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberControladorImagen(onImagenSeleccionada: (String) -> Unit): ControladorImagen {

    val seleccionar by rememberUpdatedState(onImagenSeleccionada)
    fun abrirSelector() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Seleccionar Imagen"
        fileChooser.fileFilter = FileNameExtensionFilter("Imágenes", "jpg", "png", "jpeg")
        val resultado = fileChooser.showOpenDialog(null)
        if (resultado == JFileChooser.APPROVE_OPTION) {
            seleccionar(fileChooser.selectedFile.absolutePath)
        }
    }

    return remember {
        object : ControladorImagen {
            override fun lanzarGaleria() = abrirSelector()
            override fun lanzarCamara() = abrirSelector()
        }
    }
}

@Composable
actual fun recordarImagenDesdeRuta(ruta: String?, maxDimension: Int): ImageBitmap? {
    if (ruta.isNullOrEmpty()) return null
    var bitmap by remember(ruta) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(ruta) {
        try {
            val file = File(ruta)
            if (file.exists()) {
                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { file.readBytes() }
                bitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
    return bitmap
}