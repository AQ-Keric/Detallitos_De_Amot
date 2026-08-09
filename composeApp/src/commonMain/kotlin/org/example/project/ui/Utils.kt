package org.example.project.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

// 1. Para cargar imágenes (lo que ya tenías)
@Composable
expect fun recordarImagenDesdeRuta(ruta: String?): ImageBitmap?

// 2. Para capturar imágenes (LO NUEVO QUE FALTABA)
interface ControladorImagen {
    fun lanzarCamara()
    fun lanzarGaleria()
}

@Composable
expect fun rememberControladorImagen(onImagenSeleccionada: (String) -> Unit): ControladorImagen