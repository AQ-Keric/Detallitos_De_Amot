package org.example.project.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface // <--- ESTA ES LA NATIVA (NO DA ERROR)
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream

// ==========================================
// 1. CARGADOR DE IMÁGENES (OPTIMIZADO)
// ==========================================
@Composable
actual fun recordarImagenDesdeRuta(ruta: String?): ImageBitmap? {
    if (ruta.isNullOrEmpty()) return null
    val context = LocalContext.current

    return remember(ruta) {
        try {
            // PASO 1: Cargar versión pequeña (Max 500px) para no saturar la RAM
            val bitmapOriginal = cargarImagenReducida(context, ruta, 500)

            // PASO 2: Enderezar la foto si viene rotada
            if (bitmapOriginal != null) {
                val bitmapRotado = rotarBitmapSiEsNecesario(context, bitmapOriginal, ruta)
                bitmapRotado.asImageBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// --- FUNCIÓN AUXILIAR: REDUCIR TAMAÑO ---
fun cargarImagenReducida(context: Context, ruta: String, maxAncho: Int): Bitmap? {
    return try {
        // 1. Medir dimensiones sin cargar en memoria
        val opciones = BitmapFactory.Options()
        opciones.inJustDecodeBounds = true

        if (ruta.startsWith("content://")) {
            val input = context.contentResolver.openInputStream(Uri.parse(ruta))
            BitmapFactory.decodeStream(input, null, opciones)
            input?.close()
        } else {
            BitmapFactory.decodeFile(ruta, opciones)
        }

        // 2. Calcular factor de reducción
        var escala = 1
        while (opciones.outWidth / escala / 2 >= maxAncho &&
            opciones.outHeight / escala / 2 >= maxAncho) {
            escala *= 2
        }

        // 3. Cargar imagen final reducida
        val opcionesFinales = BitmapFactory.Options()
        opcionesFinales.inSampleSize = escala

        if (ruta.startsWith("content://")) {
            val input = context.contentResolver.openInputStream(Uri.parse(ruta))
            val bitmap = BitmapFactory.decodeStream(input, null, opcionesFinales)
            input?.close()
            bitmap
        } else {
            BitmapFactory.decodeFile(ruta, opcionesFinales)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// --- FUNCIÓN AUXILIAR: CORREGIR ROTACIÓN ---
fun rotarBitmapSiEsNecesario(context: Context, bitmap: Bitmap, ruta: String): Bitmap {
    try {
        var input: InputStream? = null
        val exif: ExifInterface // Usamos la clase nativa importada arriba

        if (ruta.startsWith("content://")) {
            input = context.contentResolver.openInputStream(Uri.parse(ruta))
            if (input == null) return bitmap
            exif = ExifInterface(input)
        } else {
            exif = ExifInterface(ruta)
        }

        val orientacion = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        input?.close()

        val rotacionEnGrados = when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        return if (rotacionEnGrados != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotacionEnGrados.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        return bitmap
    }
}

// ==========================================
// 2. CONTROLADOR DE CÁMARA Y GALERÍA
// ==========================================
@Composable
actual fun rememberControladorImagen(onImagenSeleccionada: (String) -> Unit): ControladorImagen {
    val context = LocalContext.current
    var uriTemporal by remember { mutableStateOf<Uri?>(null) }

    // 1. RESPUESTA DE LA CÁMARA
    val launcherCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito && uriTemporal != null) {
            onImagenSeleccionada(uriTemporal.toString())
        }
    }

    // 2. RESPUESTA DEL PERMISO
    val launcherPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            // Si nos dio permiso, intentamos abrir la cámara de inmediato
            try {
                val archivoTemp = File.createTempFile("foto_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
                val authority = "${context.packageName}.fileprovider"
                uriTemporal = FileProvider.getUriForFile(context, authority, archivoTemp)
                launcherCamara.launch(uriTemporal!!)
            } catch (e: Exception) {
                Toast.makeText(context, "Error iniciando cámara: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Debes dar permiso para usar la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. RESPUESTA DE LA GALERÍA
    val launcherGaleria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onImagenSeleccionada(uri.toString())
        }
    }

    return remember {
        object : ControladorImagen {
            override fun lanzarCamara() {
                // A. Verificar permiso
                val permisoCamara = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)

                if (permisoCamara != PackageManager.PERMISSION_GRANTED) {
                    launcherPermiso.launch(Manifest.permission.CAMERA)
                    return
                }

                // B. Lanzar Cámara (con protección try-catch)
                try {
                    val archivoTemp = File.createTempFile("foto_${System.currentTimeMillis()}", ".jpg", context.cacheDir)

                    // IMPORTANTE: Esto debe coincidir con lo que pusiste en AndroidManifest.xml
                    val authority = "${context.packageName}.fileprovider"

                    uriTemporal = FileProvider.getUriForFile(context, authority, archivoTemp)
                    launcherCamara.launch(uriTemporal!!)

                } catch (e: IllegalArgumentException) {
                    // Este error sale si el "authority" no coincide con el Manifest
                    Toast.makeText(context, "Error: Revisa el FileProvider en el Manifest", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }

            override fun lanzarGaleria() {
                launcherGaleria.launch("image/*")
            }
        }
    }
}