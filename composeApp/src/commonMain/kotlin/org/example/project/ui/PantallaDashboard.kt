package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.dominio.CalculadoraFinanciera
import org.example.project.dominio.ModoTiempo
import org.example.project.dominio.FiltroVentas
import org.example.project.dominio.PersistenciaLocal // <-- Import agregado para leer y guardar la meta
import org.example.project.dominio.Venta
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDashboard(ventas: List<Venta>) {
    // ESTADOS
    var modoActual by remember { mutableStateOf(ModoTiempo.TOTAL) }
    var fechaReferencia by remember { mutableStateOf(LocalDate.now()) }
    var mostrarCalendario by remember { mutableStateOf(false) }

    // ESTADOS PARA LA META EDITABLE (Conectado a la base de datos)
    var metaDelPeriodo by remember { mutableStateOf(PersistenciaLocal.obtenerMeta()) } // <-- Lee el valor guardado
    var mostrarDialogoMeta by remember { mutableStateOf(false) }
    var inputNuevaMeta by remember { mutableStateOf("") }

    // COLORES SERIOS
    val GrisCarbon = Color(0xFF444444)
    val BlancoPuro = Color(0xFFFFFFFF)
    val GrisFondo = Color(0xFFF5F5F5)
    val ColorGanancia = Color(0xFF2E7D32)
    val ColorCosto = Color(0xFFC62828)
    val ColorMeta = Color(0xFFF57C00) // Naranja oscuro, más sobrio

    // FILTRO
    val ventasFiltradas = remember(ventas, modoActual, fechaReferencia) {
        FiltroVentas.aplicarFiltro(ventas, modoActual, fechaReferencia)
    }

    // CÁLCULOS
    val ingresos = CalculadoraFinanciera.calcularIngresosTotales(ventasFiltradas)
    val ganancia = CalculadoraFinanciera.calcularGananciaNeta(ventasFiltradas)
    val costos = CalculadoraFinanciera.calcularCostosTotales(ventasFiltradas)
    val metodosPago = CalculadoraFinanciera.ingresosPorMetodoPago(ventasFiltradas)
    val topVentas = CalculadoraFinanciera.obtenerTopVentas(ventasFiltradas)
    val promedios = CalculadoraFinanciera.calcularPromedios(ventasFiltradas)

    // POPUP PARA EDITAR LA META
    if (mostrarDialogoMeta) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoMeta = false },
            title = { Text("Ajustar Meta de Ventas", fontWeight = FontWeight.Bold, color = GrisCarbon, fontSize = 18.sp) },
            text = {
                OutlinedTextField(
                    value = inputNuevaMeta,
                    onValueChange = { nuevoValor ->
                        // Filtro para asegurar que solo se ingresen números
                        if (nuevoValor.all { it.isDigit() }) {
                            inputNuevaMeta = nuevoValor
                        }
                    },
                    label = { Text("Nuevo monto objetivo") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrisCarbon,
                        focusedLabelColor = GrisCarbon
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val montoParseado = inputNuevaMeta.toIntOrNull()
                    if (montoParseado != null && montoParseado > 0) {
                        metaDelPeriodo = montoParseado
                        // ¡MAGIA AQUÍ! Guardamos físicamente para que no se borre al cerrar la app
                        PersistenciaLocal.guardarMeta(montoParseado)
                    }
                    mostrarDialogoMeta = false
                }) { Text("Guardar", color = GrisCarbon, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoMeta = false }) { Text("Cancelar", color = Color.Gray) }
            }
        )
    }

    // POPUP DEL CALENDARIO NATIVO
    if (mostrarCalendario) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaReferencia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        fechaReferencia = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    mostrarCalendario = false
                }) { Text("Aceptar", color = GrisCarbon) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar", color = Color.Gray) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(GrisFondo).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECCIÓN 1: SELECTOR DE MODO ---
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BotonModo("Día", modoActual == ModoTiempo.DIA) { modoActual = ModoTiempo.DIA; fechaReferencia = LocalDate.now() }
                BotonModo("Semana", modoActual == ModoTiempo.SEMANA) { modoActual = ModoTiempo.SEMANA; fechaReferencia = LocalDate.now() }
                BotonModo("Mes", modoActual == ModoTiempo.MES) { modoActual = ModoTiempo.MES; fechaReferencia = LocalDate.now() }
                BotonModo("Total", modoActual == ModoTiempo.TOTAL) { modoActual = ModoTiempo.TOTAL }
            }
        }

        // --- SECCIÓN 2: NAVEGADOR EXACTO ---
        if (modoActual != ModoTiempo.TOTAL) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(BlancoPuro, RoundedCornerShape(8.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            fechaReferencia = when(modoActual) {
                                ModoTiempo.DIA -> fechaReferencia.minusDays(1)
                                ModoTiempo.SEMANA -> fechaReferencia.minusWeeks(1)
                                ModoTiempo.MES -> fechaReferencia.minusMonths(1)
                                ModoTiempo.TOTAL -> fechaReferencia
                            }
                        }) { Icon(Icons.Default.ChevronLeft, "Anterior", tint = GrisCarbon) }

                        val textoMostrar = when (modoActual) {
                            // ¡AQUÍ ESTÁ LA MAGIA! Le agregamos "EEEE, " al principio del patrón
                            ModoTiempo.DIA -> fechaReferencia.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale("es", "CL")))
                            ModoTiempo.SEMANA -> {
                                val inicio = fechaReferencia.with(DayOfWeek.MONDAY).format(DateTimeFormatter.ofPattern("dd MMM"))
                                val fin = fechaReferencia.with(DayOfWeek.SUNDAY).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                                "$inicio - $fin"
                            }
                            ModoTiempo.MES -> fechaReferencia.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "CL")))
                            ModoTiempo.TOTAL -> ""
                        }.uppercase()

                        Text(
                            text = textoMostrar,
                            fontWeight = FontWeight.Bold,
                            color = GrisCarbon,
                            modifier = Modifier
                                .clickable { mostrarCalendario = true }
                                .padding(8.dp)
                        )

                        IconButton(onClick = {
                            fechaReferencia = when(modoActual) {
                                ModoTiempo.DIA -> fechaReferencia.plusDays(1)
                                ModoTiempo.SEMANA -> fechaReferencia.plusWeeks(1)
                                ModoTiempo.MES -> fechaReferencia.plusMonths(1)
                                ModoTiempo.TOTAL -> fechaReferencia
                            }
                        }) { Icon(Icons.Default.ChevronRight, "Siguiente", tint = GrisCarbon) }
                    }
                    Text(
                        text = "Toca la fecha para selección exacta",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // --- SECCIÓN NUEVA: META DE VENTAS ---
        item {
            TarjetaMeta(
                ingresos = ingresos,
                meta = metaDelPeriodo,
                colorBarra = ColorMeta,
                colorFondo = BlancoPuro,
                onEditarClick = {
                    inputNuevaMeta = metaDelPeriodo.toString()
                    mostrarDialogoMeta = true
                }
            )
        }

        // --- SECCIÓN 3: MÉTRICAS ---
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TarjetaMetrica("INGRESOS", "$${ingresos.formatoPesos()}", BlancoPuro, GrisCarbon, Modifier.weight(1f))
                TarjetaMetrica("GANANCIA NETA", "$${ganancia.formatoPesos()}", BlancoPuro, ColorGanancia, Modifier.weight(1f))
            }
        }

        item {
            TarjetaMetrica("COSTOS DE PRODUCCIÓN", "$${costos.formatoPesos()}", BlancoPuro, ColorCosto, Modifier.fillMaxWidth())
        }

        // --- SECCIÓN 4: CUADRATURA ---
        item {
            Text("CUADRATURA DE CAJA", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GrisCarbon)
            Spacer(modifier = Modifier.height(8.dp))
            if (metodosPago.isEmpty()) {
                Text("Sin registros.", color = Color.Gray, fontSize = 14.sp)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    metodosPago.forEach { pago ->
                        TarjetaMetrica(pago.first.uppercase(), "$${pago.second.formatoPesos()}", BlancoPuro, GrisCarbon, Modifier.weight(1f))
                    }
                }
            }
        }

        // --- SECCIÓN 5: TOP VENTAS ---
        item {
            Text("MÁS VENDIDOS (UNIDADES)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GrisCarbon)
            Spacer(modifier = Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = BlancoPuro), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (topVentas.isEmpty()) {
                        Text("Sin datos en este periodo.", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        val maxVentas = topVentas.maxOf { it.second }.toFloat()
                        topVentas.forEach { par ->
                            GraficoBarraSerio(par.first, par.second.toString(), par.second / maxVentas, GrisCarbon)
                        }
                    }
                }
            }

        }

        // --- SECCIÓN 6: PROMEDIOS ---
        item {
            TarjetaPromedios(promedios = promedios, colorFondo = BlancoPuro, colorTexto = GrisCarbon)
        }
    }
}

// =======================================================
// COMPONENTES REUTILIZABLES
// =======================================================

@Composable
fun TarjetaMeta(ingresos: Int, meta: Int, colorBarra: Color, colorFondo: Color, onEditarClick: () -> Unit) {
    val progreso = (ingresos.toFloat() / meta.toFloat()).coerceIn(0f, 1f)
    val porcentajeTexto = (progreso * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "OBJETIVO DE VENTAS", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Meta",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onEditarClick() }
                    )
                }
                Text(text = "$porcentajeTexto%", fontSize = 12.sp, color = colorBarra, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colorBarra,
                trackColor = Color(0xFFE0E0E0),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Actual: $${ingresos.formatoPesos()}", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                Text(text = "Meta: $${meta.formatoPesos()}", fontSize = 12.sp, color = Color.Gray)
            }

            // Mensaje serio y corporativo al cumplir el objetivo
            if (ingresos >= meta) {
                Text(
                    text = "Objetivo del periodo alcanzado.",
                    fontSize = 12.sp,
                    color = colorBarra,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun BotonModo(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    val colorActivo = Color(0xFF444444)
    val colorInactivo = Color.Transparent

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (seleccionado) colorActivo else colorInactivo)
            .border(1.dp, colorActivo, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = texto,
            color = if (seleccionado) Color.White else colorActivo,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun TarjetaMetrica(titulo: String, valor: String, colorFondo: Color, colorTexto: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = colorFondo), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = titulo, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = valor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colorTexto)
        }
    }
}

@Composable
fun GraficoBarraSerio(nombre: String, valor: String, proporcion: Float, colorBarra: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = nombre, fontSize = 13.sp, color = Color.DarkGray)
            Text(text = valor, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorBarra)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0E0E0))) {
            Box(modifier = Modifier.fillMaxWidth(proporcion).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colorBarra))
        }
    }
}


fun Int.formatoPesos(): String {
    val formato = java.text.NumberFormat.getNumberInstance(java.util.Locale("es", "CL"))
    return formato.format(this)
}

@Composable
fun TarjetaPromedios(promedios: Map<String, Int>, colorFondo: Color, colorTexto: Color) {
    if (promedios.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "RENDIMIENTO PROMEDIO", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ColumnaPromedio("Por Venta", promedios["Por Cliente"], colorTexto)
                ColumnaPromedio("Diario", promedios["Diario"], colorTexto)
                ColumnaPromedio("Semanal", promedios["Semanal"], colorTexto)
                ColumnaPromedio("Mensual", promedios["Mensual"], colorTexto)
            }
        }
    }
}

@Composable
fun ColumnaPromedio(titulo: String, valor: Int?, colorTexto: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = titulo, fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$${valor?.formatoPesos() ?: "0"}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorTexto)
    }
}