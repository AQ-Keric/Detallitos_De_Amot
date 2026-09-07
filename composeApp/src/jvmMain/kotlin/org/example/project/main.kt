package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    org.example.project.dominio.PersistenciaLocal.motor = org.example.project.dominio.MotorArchivo(
        java.io.File(System.getenv("XDG_DATA_HOME") ?: (System.getProperty("user.home") + "/.local/share"), "detallitos-de-amor")
    )
    application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Detallitos de Amor",
    ) {
        App()
    }
}
}
