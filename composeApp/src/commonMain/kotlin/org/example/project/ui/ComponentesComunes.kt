package org.example.project.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BarraBusqueda(
    texto: String,
    onTextoCambio: (String) -> Unit,
    placeholder: String = "Buscar..."
) {
    val GrisCarbon = Color(0xFF444444)

    TextField(
        value = texto,
        onValueChange = onTextoCambio,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        placeholder = { Text(placeholder, color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = GrisCarbon) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onTextoCambio("") }) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
            }
        },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = GrisCarbon,
            textColor = GrisCarbon
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

// ESTA ES LA ÚNICA DEFINICIÓN DE CHIPFILTRO QUE DEBE EXISTIR
@Composable
fun ChipFiltro(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    val colorFondo = if (seleccionado) Color(0xFF444444) else Color.White
    val colorTexto = if (seleccionado) Color.White else Color(0xFF444444)
    val borde = if (seleccionado) Color.Transparent else Color.LightGray

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = colorFondo),
        shape = RoundedCornerShape(50),
        elevation = ButtonDefaults.elevation(0.dp),
        border = BorderStroke(1.dp, borde),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(texto, color = colorTexto, fontSize = 12.sp)
    }
}