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
import org.example.project.dominio.MetasVentas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDashboard(ventas: List<Venta>, productos: List<org.example.project.dominio.Producto>) {
    // ESTADOS
    var modoActual by remember { mutableStateOf(ModoTiempo.TOTAL) }
    var fechaReferencia by remember { mutableStateOf(LocalDate.now()) }
    var mostrarCalendario by remember { mutableStateOf(false) }

    // ESTADOS PARA LA META EDITABLE (Conectado a la base de datos)
    val claveMeta = MetasVentas.clave(modoActual, fechaReferencia)
    var metaDelPeriodo by remember(claveMeta) { mutableStateOf(PersistenciaLocal.obtenerMeta(claveMeta)) }
    val etiquetaMeta = when (modoActual) {
        ModoTiempo.DIA -> "Meta del día $fechaReferencia"
        ModoTiempo.SEMANA -> "Meta de la semana del ${fechaReferencia.with(DayOfWeek.MONDAY)}"
        ModoTiempo.MES -> "Meta de ${fechaReferencia.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CL")))}"
        ModoTiempo.TOTAL -> "Meta total acumulada"
    }
    val scope = rememberCoroutineScope()
    var guardandoMeta by remember { mutableStateOf(false) }
    ManejarVolver(enabled = guardandoMeta) {}
    var mostrarDialogoMeta by remember { mutableStateOf(false) }
    var inputNuevaMeta by remember { mutableStateOf("") }
    var errorMeta by remember { mutableStateOf<String?>(null) }
    if (errorMeta != null) AlertDialog(onDismissRequest = { errorMeta = null }, title = { Text("No se pudo guardar") }, text = { Text(errorMeta.orEmpty()) }, confirmButton = { TextButton(onClick = { errorMeta = null }) { Text("Entendido") } })

    // COLORES SERIOS
    val GrisCarbon = Color(0xFF444444)
    val BlancoPuro = Color(0xFFFFFFFF)
    val GrisFondo = Color(0xFFF5F5F5)
    val ColorGanancia = Color(0xFF2E7D32)
    val ColorCosto = Color(0xFFC62828)
    val ColorMeta = Color(0xFFF57C00) // Naranja oscuro, más sobrio

    // FILTRO
    val ventasFiltradas = FiltroVentas.aplicarFiltro(ventas, modoActual, fechaReferencia)

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
            onDismissRequest = { if (!guardandoMeta) mostrarDialogoMeta = false },
            title = { Text(etiquetaMeta, fontWeight = FontWeight.Bold, color = GrisCarbon, fontSize = 18.sp) },
            text = {
                OutlinedTextField(
                    enabled = !guardandoMeta,
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
                TextButton(enabled = !guardandoMeta && (inputNuevaMeta.toIntOrNull() ?: 0) > 0, onClick = {
                    val monto = inputNuevaMeta.toIntOrNull() ?: return@TextButton
                    val clave = claveMeta
                    guardandoMeta = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { PersistenciaLocal.guardarMeta(clave, monto) }
                            metaDelPeriodo = monto
                            mostrarDialogoMeta = false
                        } catch (e: Exception) { errorMeta = e.message ?: "No se pudo guardar la meta" }
                        finally { guardandoMeta = false }
                    }
                }) { Text(if (guardandoMeta) "Guardando…" else "Guardar", color = GrisCarbon, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(enabled = !guardandoMeta, onClick = { mostrarDialogoMeta = false }) { Text("Cancelar", color = Color.Gray) }
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
                            ModoTiempo.DIA -> fechaReferencia.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.forLanguageTag("es-CL")))
                            ModoTiempo.SEMANA -> {
                                val inicio = fechaReferencia.with(DayOfWeek.MONDAY).format(DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es-CL")))
                                val fin = fechaReferencia.with(DayOfWeek.SUNDAY).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es-CL")))
                                "$inicio - $fin"
                            }
                            ModoTiempo.MES -> fechaReferencia.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CL")))
                            ModoTiempo.TOTAL -> ""
                        }.uppercase()

                        Text(
                            text = textoMostrar,
                            fontWeight = FontWeight.Bold,
                            color = GrisCarbon,
                            modifier = Modifier
                                .weight(1f).clickable { mostrarCalendario = true }
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

        item {
            Text("Inventario: ${productos.size} productos · ${productos.count { it.stock == 0 }} agotados · ${productos.count { it.stock in 1..3 }} con stock bajo", color = GrisCarbon)
            if (ventas.any { it.fechaEpochMillis <= 0L }) Text("Las ventas sin fecha válida se incluyen solamente en Total.", color = Color.Gray)
            if (ventasFiltradas.isEmpty()) Text("Aún no hay ventas en este período.", color = Color.Gray)
            Text("La ganancia descuenta el costo de producción registrado; no incluye otros gastos.", color = Color.Gray, fontSize = 12.sp)
        }
        // --- SECCIÓN NUEVA: META DE VENTAS ---
        item {
            TarjetaMeta(
                ingresos = ingresos,
                meta = metaDelPeriodo,
                titulo = etiquetaMeta,
                colorBarra = ColorMeta,
                colorFondo = BlancoPuro,
                onEditarClick = {
                    inputNuevaMeta = metaDelPeriodo?.toString().orEmpty()
                    mostrarDialogoMeta = true
                }
            )
        }

        // --- SECCIÓN 3: MÉTRICAS ---
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TarjetaMetrica("INGRESOS", "$${ingresos.formatoPesos()}", BlancoPuro, GrisCarbon, Modifier.weight(1f))
                TarjetaMetrica("GANANCIA ESTIMADA", "$${ganancia.formatoPesos()}", BlancoPuro, ColorGanancia, Modifier.weight(1f))
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
fun TarjetaMeta(ingresos: Long, meta: Int?, titulo: String, colorBarra: Color, colorFondo: Color, onEditarClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colorFondo)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, color = Color.DarkGray, fontWeight = FontWeight.Bold)
            Text("Se calcula con los ingresos por ventas del período seleccionado.", color = Color.Gray, fontSize = 12.sp)
            if (meta == null) {
                Text("No has definido una meta para este período.", color = Color.Gray)
                OutlinedButton(onClick = onEditarClick) { Text("Definir meta") }
            } else {
                val avance = MetasVentas.progreso(ingresos, meta)
                val porcentaje = java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CL")).apply {
                    maximumFractionDigits = 1
                }.format(avance.porcentaje)
                Text("$porcentaje% · $${ingresos.formatoPesos()} de $${meta.formatoPesos()}", color = colorBarra, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { avance.barra }, modifier = Modifier.fillMaxWidth(), color = colorBarra)
                Text(when {
                    avance.faltante > 0 -> "Faltan $${avance.faltante.formatoPesos()} para alcanzar tu meta."
                    avance.excedente > 0 -> "¡Meta superada por $${avance.excedente.formatoPesos()}!"
                    else -> "¡Meta alcanzada!"
                }, color = Color.DarkGray)
                TextButton(onClick = onEditarClick) { Text("Cambiar meta") }
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


fun Number.formatoPesos(): String {
    val formato = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("es-CL"))
    return formato.format(this)
}

@Composable
fun TarjetaPromedios(promedios: Map<String, Long>, colorFondo: Color, colorTexto: Color) {
    if (promedios.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "PROMEDIOS EN PERÍODOS CON VENTAS", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ColumnaPromedio("Por Venta", promedios["Por Venta"], colorTexto)
                ColumnaPromedio("Diario", promedios["Diario"], colorTexto)
                ColumnaPromedio("Semanal", promedios["Semanal"], colorTexto)
                ColumnaPromedio("Mensual", promedios["Mensual"], colorTexto)
            }
        }
    }
}

@Composable
fun ColumnaPromedio(titulo: String, valor: Long?, colorTexto: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = titulo, fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$${valor?.formatoPesos() ?: "0"}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorTexto)
    }
}