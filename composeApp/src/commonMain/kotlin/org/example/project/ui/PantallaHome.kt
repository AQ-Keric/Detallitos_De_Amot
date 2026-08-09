package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elbolsilloucm.composeapp.generated.resources.Res
import elbolsilloucm.composeapp.generated.resources.logodetallitosdeamor
import org.jetbrains.compose.resources.painterResource



@Composable
fun PantallaHome(
    onNavegarAVenta: () -> Unit
) {
    // --- PALETA ELEGANCIA ---
    val GrisCarbon = Color(0xFF444444)
    val BlancoPuro = Color(0xFFFFFFFF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlancoPuro) // Fondo limpio
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- LOGO (Con el recorte que hiciste) ---
        Box(
            modifier = Modifier
                .size(220.dp) // Grande para que luzca
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.logodetallitosdeamor),
                contentDescription = "Logo Detallitos de Amor",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp), // Margen mínimo
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- TÍTULOS ---
        Text(
            text = "Detallitos de Amor",
            fontSize = 30.sp,
            fontWeight = FontWeight.Light, // Letra fina y elegante
            color = GrisCarbon
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Línea decorativa
        Divider(color = GrisCarbon, thickness = 1.dp, modifier = Modifier.width(80.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SISTEMA DE GESTIÓN",
            fontSize = 14.sp,
            letterSpacing = 3.sp, // Letras separadas (estilo premium)
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(80.dp))

        // --- BOTÓN PRINCIPAL ---
        Button(
            onClick = onNavegarAVenta,
            modifier = Modifier.fillMaxWidth().height(65.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = GrisCarbon),
            shape = RoundedCornerShape(50.dp), // Redondo completo
            elevation = ButtonDefaults.elevation(6.dp)
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text("NUEVA VENTA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}