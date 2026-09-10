package org.example.project.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
import androidx.compose.material.icons.filled.MoreVert
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
fun PantallaDashboard(ventas: List<Venta>) {
    var verDetalles by remember { mutableStateOf(false) }
    var verAyuda by remember { mutableStateOf(false) }
    if (verAyuda) AlertDialog(onDismissRequest = { verAyuda = false }, title = { Text("Tus números") },
        text = { Text("Ingresos: total vendido. Ganancia: ingresos menos el costo de los productos; no incluye otros gastos. Los promedios usan períodos con ventas. Las ventas sin fecha se incluyen solo en Total.") },
        confirmButton = { TextButton(onClick = { verAyuda = false }) { Text("Entendido") } })
    // ESTADOS
    var modoActual by remember { mutableStateOf(ModoTiempo.TOTAL) }
    var fechaReferencia by remember { mutableStateOf(LocalDate.now()) }
    var mostrarCalendario by remember { mutableStateOf(false) }

    // ESTADOS PARA LA META EDITABLE (Conectado a la base de datos)
    val claveMeta = MetasVentas.clave(modoActual, fechaReferencia)
    var metaDelPeriodo by remember(claveMeta) { mutableStateOf(PersistenciaLocal.obtenerMeta(claveMeta)) }
    var inicioMetaGlobal by remember { mutableStateOf(PersistenciaLocal.estado().metaGlobalDesde) }
    val etiquetaMeta = when (modoActual) {
        ModoTiempo.DIA -> "Meta del día $fechaReferencia"
        ModoTiempo.SEMANA -> "Meta de la semana del ${fechaReferencia.with(DayOfWeek.MONDAY)}"
        ModoTiempo.MES -> "Meta de ${fechaReferencia.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CL")))}"
        ModoTiempo.TOTAL -> "Meta global"
    }
    val scope = rememberCoroutineScope()
    var guardandoMeta by remember { mutableStateOf(false) }
    ManejarVolver(enabled = guardandoMeta) {}
    var reiniciandoMeta by remember { mutableStateOf(false) }
    var confirmarEliminarMeta by remember { mutableStateOf(false) }
    var mostrarDialogoMeta by remember { mutableStateOf(false) }
    var alcanceMeta by remember { mutableStateOf(0) }
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

    val ingresosMeta = if (modoActual == ModoTiempo.TOTAL) MetasVentas.ingresosGlobal(ventas, inicioMetaGlobal) else ingresos
    val descripcionMeta = if (modoActual != ModoTiempo.TOTAL) "Ingresos del período seleccionado."
        else if (inicioMetaGlobal == 0L) "Ingresos acumulados de todo el historial."
        else "Ingresos desde " + Instant.ofEpochMilli(inicioMetaGlobal).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    if (confirmarEliminarMeta) {
        AlertDialog(onDismissRequest = { if (!guardandoMeta) confirmarEliminarMeta = false },
            title = { Text("¿Eliminar esta meta?") },
            text = { Text("Se quitará únicamente la meta. Tus ventas, imágenes e inventario se conservarán. Después podrás definir otra.") },
            confirmButton = { TextButton(enabled = !guardandoMeta, onClick = {
                val clave = claveMeta
                guardandoMeta = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { PersistenciaLocal.eliminarMeta(clave) }
                        metaDelPeriodo = null
                        inicioMetaGlobal = PersistenciaLocal.estado().metaGlobalDesde
                        confirmarEliminarMeta = false
                    } catch (e: Exception) { errorMeta = e.message ?: "No se pudo eliminar la meta" }
                    finally { guardandoMeta = false }
                }
            }) { Text(if (guardandoMeta) "Eliminando…" else "Eliminar") } },
            dismissButton = { TextButton(enabled = !guardandoMeta, onClick = { confirmarEliminarMeta = false }) { Text("Cancelar") } }
        )
    }
    // POPUP PARA EDITAR LA META
    if (mostrarDialogoMeta) {
        AlertDialog(
            onDismissRequest = { if (!guardandoMeta) mostrarDialogoMeta = false },
            title = { Text(if (reiniciandoMeta) "Reiniciar meta global" else etiquetaMeta, fontWeight = FontWeight.Bold, color = GrisCarbon, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(when {
                    reiniciandoMeta -> "El avance empezará desde cero con las ventas posteriores a la confirmación. Puedes mantener o cambiar el monto. No se borrará ninguna venta."
                    modoActual == ModoTiempo.TOTAL && metaDelPeriodo == null -> "Esta nueva meta contará las ventas desde ahora."
                    metaDelPeriodo != null -> "Cambiar el monto conserva el avance actual."
                    else -> "La meta usará todas las ventas del período seleccionado."
                }, fontSize = 13.sp)
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
                if (modoActual != ModoTiempo.TOTAL) {
                    val repeticion = when (modoActual) {
                        ModoTiempo.DIA -> "Todos los días"
                        ModoTiempo.SEMANA -> "Todas las semanas"
                        else -> "Todos los meses"
                    }
                    listOf("Solo este período", repeticion, "Días, semanas y meses").forEachIndexed { indice, texto ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = alcanceMeta == indice, enabled = !guardandoMeta, onClick = { alcanceMeta = indice })
                            TextButton(enabled = !guardandoMeta, onClick = { alcanceMeta = indice }) { Text(texto) }
                        }
                    }
                    if (alcanceMeta > 0) Text("Se repite el monto; cada período tiene su propio avance. Se conservan las metas personalizadas de otros períodos.", fontSize = 12.sp)
                    val tipo = claveMeta.substringBefore(':')
                    if (PersistenciaLocal.estado().metasRepetidas.containsKey(tipo)) {
                        TextButton(enabled = !guardandoMeta, onClick = {
                            guardandoMeta = true
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { PersistenciaLocal.eliminarMetaRepetida(tipo) }
                                    metaDelPeriodo = PersistenciaLocal.obtenerMeta(claveMeta)
                                    mostrarDialogoMeta = false
                                } catch (e: Exception) { errorMeta = e.message }
                                finally { guardandoMeta = false }
                            }
                        }) { Text("Dejar de repetir en este modo") }
                    }
                }
                }
            },
            confirmButton = {
                TextButton(enabled = !guardandoMeta && (inputNuevaMeta.toIntOrNull() ?: 0) > 0, onClick = {
                    val monto = inputNuevaMeta.toIntOrNull() ?: return@TextButton
                    val clave = claveMeta
                    guardandoMeta = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                if (reiniciandoMeta) PersistenciaLocal.reiniciarMetaGlobal(monto)
                                else if (alcanceMeta > 0 && clave != "total") PersistenciaLocal.guardarMetaRepetida(clave, monto, alcanceMeta == 2)
                                else PersistenciaLocal.guardarMeta(clave, monto)
                            }
                            metaDelPeriodo = monto
                            inicioMetaGlobal = PersistenciaLocal.estado().metaGlobalDesde
                            mostrarDialogoMeta = false
                        } catch (e: Exception) { errorMeta = e.message ?: "No se pudo guardar la meta" }
                        finally { guardandoMeta = false }
                    }
                }) { Text(if (guardandoMeta) "Guardando…" else if (reiniciandoMeta) "Reiniciar" else "Guardar", color = GrisCarbon, fontWeight = FontWeight.Bold) }
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
                        text = "Toca la fecha para cambiarla",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (ventasFiltradas.isEmpty()) item { Text("Sin ventas en este período", color = Color.Gray) }
        // --- SECCIÓN NUEVA: META DE VENTAS ---
        item {
            TarjetaMeta(
                ingresos = ingresosMeta,
                meta = metaDelPeriodo,
                descripcion = descripcionMeta,
                onEliminarClick = { confirmarEliminarMeta = true },
                onReiniciarClick = if (modoActual == ModoTiempo.TOTAL) ({
                    reiniciandoMeta = true
                    inputNuevaMeta = metaDelPeriodo?.toString().orEmpty()
                    mostrarDialogoMeta = true
                }) else null,
                titulo = etiquetaMeta,
                colorBarra = ColorMeta,
                colorFondo = BlancoPuro,
                onEditarClick = {
                    reiniciandoMeta = false
                    alcanceMeta = 0
                    inputNuevaMeta = metaDelPeriodo?.toString().orEmpty()
                    mostrarDialogoMeta = true
                }
            )
        }

        // --- SECCIÓN 3: MÉTRICAS ---
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TarjetaMetrica("Ingresos", "$${ingresos.formatoPesos()}", BlancoPuro, GrisCarbon, Modifier.weight(1f))
                TarjetaMetrica("Ganancia estimada", "$${ganancia.formatoPesos()}", BlancoPuro, ColorGanancia, Modifier.weight(1f))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { verDetalles = !verDetalles }) { Text(if (verDetalles) "Menos detalles" else "Ver detalles") }
                TextButton(onClick = { verAyuda = true }) { Text("Ayuda") }
            }
        }
        if (verDetalles) {
        item {
            TarjetaMetrica("Costo de productos", "$${costos.formatoPesos()}", BlancoPuro, ColorCosto, Modifier.fillMaxWidth())
        }

        // --- SECCIÓN 4: CUADRATURA ---
        item {
            Text("Por medio de pago", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GrisCarbon)
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

        }

        // --- SECCIÓN 5: TOP VENTAS ---
        item {
            Text("Más vendidos", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GrisCarbon)
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
        if (verDetalles) item {
            TarjetaPromedios(promedios = promedios, colorFondo = BlancoPuro, colorTexto = GrisCarbon)
        }
    }
}

// =======================================================
// COMPONENTES REUTILIZABLES
// =======================================================

@Composable
fun TarjetaMeta(ingresos: Long, meta: Int?, titulo: String, descripcion: String, colorBarra: Color, colorFondo: Color, onEditarClick: () -> Unit, onEliminarClick: () -> Unit, onReiniciarClick: (() -> Unit)?) {
    var menu by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colorFondo)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, modifier = Modifier.weight(1f), color = Color.DarkGray, fontWeight = FontWeight.Bold)
                if (meta != null) Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Opciones de meta") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Cambiar monto") }, onClick = { menu = false; onEditarClick() })
                        onReiniciarClick?.let { accion -> DropdownMenuItem(text = { Text("Reiniciar desde cero") }, onClick = { menu = false; accion() }) }
                        DropdownMenuItem(text = { Text("Eliminar meta") }, onClick = { menu = false; onEliminarClick() })
                    }
                }
            }
            Text(descripcion, color = Color.Gray, fontSize = 12.sp)
            if (meta == null) {
                Text("Sin meta", color = Color.Gray)
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
            Text(text = "Promedios", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
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