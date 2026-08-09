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

    fun abrirSelector() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Seleccionar Imagen"
        fileChooser.fileFilter = FileNameExtensionFilter("Imágenes", "jpg", "png", "jpeg")
        val resultado = fileChooser.showOpenDialog(null)
        if (resultado == JFileChooser.APPROVE_OPTION) {
            onImagenSeleccionada(fileChooser.selectedFile.absolutePath)
        }
    }

    return remember {
        ControladorImagen(
            lanzarGaleria = { abrirSelector() },
            lanzarCamara = { abrirSelector() } // En PC usamos el mismo selector por ahora
        )
    }
}

@Composable
actual fun recordarImagenDesdeRuta(ruta: String?): ImageBitmap? {
    if (ruta.isNullOrEmpty()) return null
    var bitmap by remember(ruta) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(ruta) {
        try {
            val file = File(ruta)
            if (file.exists()) {
                val bytes = file.readBytes()
                bitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
    return bitmap
}