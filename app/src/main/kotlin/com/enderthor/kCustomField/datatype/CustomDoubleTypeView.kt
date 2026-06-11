package com.enderthor.kCustomField.datatype

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import io.hammerhead.karooext.models.StreamState
import java.text.DateFormat
import java.util.Calendar
import kotlin.math.roundToInt

// ColorProviders compartidos para evitar instanciar uno por cada recomposición Glance.
// Glance reevalúa parámetros de @Composable en cada update; sin estas constantes los
// 12+ usos de TextDayNight crearían un objeto cada vez.
internal val TextDayNight: ColorProvider = ColorProvider(day = Color.Black, night = Color.White)
internal val TextNightDay: ColorProvider = ColorProvider(day = Color.White, night = Color.Black)
internal val PlaceholderAllBlack: ColorProvider = ColorProvider(day = Color.Black, night = Color.Black)
internal val OverlayBgDay: Color = Color(1f, 1f, 1f, 0.4f)
internal val OverlayBgNight: Color = Color(0f, 0f, 0f, 0.4f)


fun formatTimeRemaining(timeMs: Double): String {
    val totalMinutes = (timeMs / 60000).toInt() // Convertir ms a minutos
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    // Sin Locale.US/String.format: evita alocar Formatter en cada tick.
    // Locale-safe por construcción (no produce comas decimales en locales europeos).
    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    } else {
        minutes.toString()
    }
}

// Formatter cacheado: DateFormat.getTimeInstance aloca en cada render y esto se llama
// en cada tick de los campos Civil Dawn/Dusk. DateFormat no es thread-safe, pero la
// composición Glance siempre corre en Main (withContext(Dispatchers.Main) en las bases).
private val timeOfDayFormat: DateFormat by lazy { DateFormat.getTimeInstance(DateFormat.SHORT) }

private fun epochToTimeOfDay(epochMillis: Double): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = epochMillis.toLong()
    return timeOfDayFormat.format(calendar.time)
}


fun formatNumber(number: Double, isInt: Boolean, isTime: Boolean = false, isCivil: Boolean = false, isClimb: Boolean = false, thousandsSuffix: Char = 'k'): String = buildString {

    // La hidratación (sufijo 'L') NUNCA usa el formato de distancia del climb: en el campo
    // central del climb debe mostrar "3.5L" igual que los paneles laterales, no "3.50"
    // sin unidad (ni "-3500" crudo, que la rama climb no compacta para negativos).
    if (isClimb && thousandsSuffix != 'L') {
        if (isCivil) {
            val timeStr = epochToTimeOfDay(number)
            append(timeStr.split(':').firstOrNull() ?: "")
        } else if (isTime) {
            val timeStr = formatTimeRemaining(number)
            append(timeStr.split(':').firstOrNull() ?: "")
        } else {

            val numValue = number.roundToInt()
            when {
                numValue < 1000 -> {
                    append(numValue.toString())
                }

                numValue < 10000 -> {
                    val intPart = numValue / 1000
                    val decPart = (numValue % 1000) / 10
                    // Sin String.format: bug latente con locales europeos (coma decimal).
                    append(intPart)
                    append('.')
                    if (decPart < 10) append('0')
                    append(decPart)
                }

                else -> {
                    // Para distancias >= 10 km, usar formato más compacto: "10.5" en lugar de "10.50"
                    val intPart = numValue / 1000
                    val decPart = (numValue % 1000) / 100
                    append(intPart)
                    append('.')
                    append(decPart)
                }
            }
        }
    } else {
        if (isCivil) {
            append(epochToTimeOfDay(number))
        } else if (isTime) {
            append(formatTimeRemaining(number))
        } else {
            if (isInt) {
                val raw = number.roundToInt()
                // Long: abs(Int.MIN_VALUE) y absV+50 desbordan Int con ±Infinity de un
                // stream corrupto (roundToInt satura a Int.MAX/MIN) y saldría basura con
                // el signo cambiado.
                val absV = kotlin.math.abs(raw.toLong())
                if (thousandsSuffix == 'L' && absV >= 1000) {
                    // Hidratación: a partir de 1000 ml SIEMPRE en litros ("3.5L"), no solo
                    // cuando no cabe — 3500 ml son 3,5 litros. Redondeo a décimas de litro.
                    // Clamp a 999.9L: con datos basura la forma degradada ("999L") sigue
                    // cabiendo en cualquier presupuesto sin perder la unidad.
                    val sign = if (raw < 0) "-" else ""
                    val tenths = ((absV + 50) / 100).coerceAtMost(9999L)
                    append(fitNumberToChars("$sign${tenths / 10}.${tenths % 10}L", 5))
                } else {
                    // fitNumberToChars: un entero de 6+ dígitos (o negativo de 5) pasa a
                    // "123k" en vez de perder dígitos por la derecha
                    append(fitNumberToChars(raw.toString(), 5, thousandsSuffix))
                }
            }
            // trimEnd('.'): take(5) puede dejar un punto colgando ("1234." en vez de "1234")
            else append(((number * 10.0).roundToInt() / 10.0).toString().take(5).trimEnd('.'))
        }

    }
}


// Decide si un campo se muestra como entero o con 1 decimal.
// Regla base (igual que antes): es entero salvo velocidad / slopeZones / IF.
// power y climb fuerzan entero. Si el usuario activa "distancia con decimales"
// (ajuste global), los campos de distancia (convert=="distance") pasan a decimal.
fun isIntField(kaction: KarooAction, isPower: Boolean, isClimb: Boolean, distanceWithDecimals: Boolean): Boolean {
    val base = !(kaction.convert == "speed" || kaction.zone == "slopeZones" || kaction.label == "IF")
    val adjusted = if (distanceWithDecimals && kaction.convert == "distance") false else base
    return adjusted || isPower || isClimb
}

// Forma compacta ya generada por fitNumberToChars: "12k", "-12.3k", "12.5L"
private val COMPACT_K = Regex("^-?\\d+(\\.\\d)?[kL]$")

// Sufijo de compactación de miles según el campo: la hidratación de KSafe va en ml,
// así que sus miles son litros ("12.5L" lee mejor que "12.5k").
fun thousandsSuffixFor(action: KarooAction): Char =
    if (action == KarooAction.HYDRATION_DEFICIT) 'L' else 'k'

// KGhost marca cada gap como medido o estimado (GPS perdido / fuera de ruta) en el campo
// "estimated" del DataPoint. No podemos colorear solo el número, pero sí anteponer un "~"
// para que sea evidente que el valor es una estimación, no una medición firme.
fun estimateMarker(action: KarooAction, state: StreamState?): String =
    if ((action == KarooAction.GHOST_GAP_TIME || action == KarooAction.GHOST_GAP_DIST) &&
        (state as? StreamState.Streaming)?.dataPoint?.values?.get("estimated") == 1.0
    ) "~" else ""

// Ajusta un valor a maxChars SIN truncar dígitos: recortar "12345" a "1234" o "-1500" a
// "-150" es un error 10x silencioso. Enteros que no caben pasan a notación compacta de
// miles ("12.3k", y si tampoco cabe "12k"; la hidratación usa sufijo 'L' = litros).
// Valores extremos reales: déficit de hidratación KSafe >9999 ml, kcal acumuladas en
// rides muy largos. Strings no enteros (etiquetas FA "Close", tiempos "01:23")
// mantienen el recorte clásico.
fun fitNumberToChars(value: String, maxChars: Int, thousandsSuffix: Char = 'k'): String {
    // Marcador de estimación de KGhost ("~125"): se conserva y se compactan los dígitos
    // en el espacio restante, nunca se recorta para hacerle sitio al "~".
    if (value.startsWith('~')) {
        return "~" + fitNumberToChars(value.drop(1), (maxChars - 1).coerceAtLeast(1), thousandsSuffix)
    }
    if (value.length <= maxChars) return value
    val negative = value.startsWith('-')
    val body = if (negative) value.drop(1) else value
    val sign = if (negative) "-" else ""

    if (body.isNotEmpty() && body.all { it.isDigit() }) {
        val n = body.toLongOrNull() ?: return value.take(maxChars)
        val thousands = n / 1000
        if (thousands == 0L) return value.take(maxChars)
        val withDecimal = "$sign$thousands.${(n % 1000) / 100}$thousandsSuffix"
        if (withDecimal.length <= maxChars) return withDecimal
        val plain = "$sign$thousands$thousandsSuffix"
        return if (plain.length <= maxChars) plain else plain.take(maxChars)
    }
    // Ya compactado en un paso anterior ("12.3k"/"12.5L"): degradar quitando el decimal
    // y conservando el sufijo original, nunca recortando por la derecha (take daría
    // "12.3", que parece otro número).
    if (COMPACT_K.matches(value)) {
        val plain = "$sign${body.substringBefore('.')}${value.last()}"
        if (plain.length <= maxChars) return plain
    }
    return value.take(maxChars)
}

fun trimNumberTo3Chars(x: String): String {
    val hasDecimal = x.contains('.')
    // Los valores compactados ("12.3k"/"12.5L") no se recortan: quitarles la cola cambia el número.
    if (!hasDecimal || x.endsWith('k') || x.endsWith('L')) return x

    val limit = if (x.startsWith('-')) 4 else 3
    val cut = x.take(limit)

    return cut.trimEnd('.')
}


@Composable
private fun VerticalDivider(isTopField: Boolean, fieldSize: FieldSize, isdivider: Boolean,isClimbField: Boolean = false) {
    val height = when {
        isClimbField -> 1.dp
        isTopField -> 10.dp
        fieldSize == FieldSize.LARGE -> 28.dp
        else -> 14.dp
    }
    Box(modifier = GlanceModifier.fillMaxWidth().height(height)) {
        Row(modifier = GlanceModifier.fillMaxSize()) {
            Column(modifier = GlanceModifier.defaultWeight()) {}
            if (isdivider) Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(TextDayNight))
            Column(modifier = GlanceModifier.defaultWeight()) {}
        }
    }
}

@Composable
private fun IconRow(
    icon: Int,
    colorFilter: ColorFilter,
    layout: FieldPosition,
    modifier: GlanceModifier = GlanceModifier.fillMaxWidth()
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = when (layout) {
            FieldPosition.CENTER -> Alignment.CenterHorizontally
            FieldPosition.RIGHT -> Alignment.End
            FieldPosition.LEFT -> Alignment.Start
        }
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = colorFilter
        )
    }
}


@Composable
private fun NumberRow(
    number: String,
    zoneColor: ColorProvider,
    layout: FieldPosition,
    fieldSize: FieldSize,
    onlyOne: Boolean,
    isheadwind: Boolean = false,
    iszone: Boolean = false,
    textColor: ColorProvider
) {
    val padding = if (fieldSize == FieldSize.LARGE) 8.dp else 2.dp
    val fontSize = when {
        onlyOne -> 42.sp
        number.length > 3 -> 32.sp
        else -> 38.sp
    }

    Row(
        modifier = GlanceModifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(bottom = padding),
        verticalAlignment = if (isheadwind) Alignment.CenterVertically else Alignment.Bottom,
        horizontalAlignment = when (layout) {
            FieldPosition.CENTER -> Alignment.CenterHorizontally
            FieldPosition.RIGHT -> Alignment.End
            FieldPosition.LEFT -> Alignment.Start
        }
    ) {
        Text(
            text = number,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace,
                color = if (iszone) textColor else TextDayNight
            )
        )
        Spacer(
            modifier = GlanceModifier
                .fillMaxHeight()
                .width(2.dp)
                .background(zoneColor)
        )
    }
}



@Composable
private fun OneIconRow(
    icon: Int,
    iconColor: ColorProvider,
    text: String,
    iszone: Boolean,
    fieldSize: FieldSize,
    isClimb: Boolean = false
) {
    val isSmall = fieldSize == FieldSize.SMALL
    val rowHeight = if (isSmall) 31.dp else 37.dp
    val iconSize = if (isSmall) 16.dp else 20.dp
    val fontSize = if (isSmall) 15.sp else 18.sp
    val topPadding = if (isSmall) (-2).dp else (-1).dp

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(rowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = if (isClimb) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Column(
            horizontalAlignment = if (isClimb) Alignment.CenterHorizontally else Alignment.Start,
            modifier = GlanceModifier
                .height(if (isSmall) 20.dp else 24.dp)
                .width(24.dp)
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = null,
                modifier = GlanceModifier.size(iconSize).padding(top = topPadding),
                colorFilter = ColorFilter.tint(iconColor)

            )
        }
        if (!isClimb) {
            Column(
                modifier = GlanceModifier
                    .height(if (isSmall) 32.dp else 36.dp)
                    .fillMaxWidth()
                    .padding(end = 3.dp),
                horizontalAlignment = Alignment.End,
                verticalAlignment = Alignment.CenterVertically
            ) {

                val displayText = text.takeIf { it.length <= 10 } ?: text.split(" ", limit = 2)
                    .let { parts -> if (parts.size > 1) "${parts[0]}\n${parts[1]}" else text }

                val adjustedFontSize =
                    if ((displayText.count { it == '\n' } + 1) == 2) (fontSize.value * 0.85).sp else fontSize

                Text(
                    text = displayText,
                    maxLines = 2,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = adjustedFontSize,
                        fontFamily = FontFamily.Monospace,
                        color = if (iszone) iconColor else TextDayNight,
                        textAlign = TextAlign.End
                    )
                )
            }
        }
    }
}

@Composable
private fun OneNumberRow(
    number: String,
    layout: FieldPosition,
    fieldSize: FieldSize,
    textSize: Int,
    iszone: Boolean,
    textColor: ColorProvider,
    ispower: Boolean,
    secondValue: String,
    isClimb: Boolean = false
) {
    val padding = if (fieldSize == FieldSize.LARGE) 7.dp else 2.dp
    val displayNumber =
        if (!ispower) {
        number
    } else {
        "${number.take(3)}-${secondValue.take(3)}"
    }

    // Ajustar el tamaño de fuente para el campo de subida cuando tiene 4+ caracteres
    // (4 dígitos, negativos "-1500" o etiquetas FA "Close")
    val adjustedTextSize = if (isClimb && displayNumber.length >= 4) {
        // Reducir el tamaño de fuente 25% para que 4 caracteres ocupen el mismo ancho que 3
        (textSize * 0.8).roundToInt()
    } else {
        textSize
    }

    Row(
        modifier = GlanceModifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(bottom = padding, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = if (isClimb) Alignment.CenterHorizontally
        else when (layout) {
            FieldPosition.CENTER -> Alignment.CenterHorizontally
            FieldPosition.RIGHT -> Alignment.End
            FieldPosition.LEFT -> Alignment.Start
        }
    ) {
        Text(
            text = displayNumber,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = adjustedTextSize.sp,
                fontFamily = FontFamily.Monospace,
                color = if (iszone) textColor else TextDayNight
            ),
            modifier = GlanceModifier.padding(top = -padding)
        )
    }
}


@Composable
private fun HorizontalScreenContent(number: String, icon: Int, colorFilter: ColorProvider, layout: FieldPosition, iszone: Boolean, fontSize: Int = 38, iconSize: Int = 20, maxLines: Int = Int.MAX_VALUE, iconGap: Int = 5) {
   val colorIcon= ColorFilter.tint(colorFilter)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = when (layout.name) {
            "CENTER" -> Alignment.CenterHorizontally
            "RIGHT" -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        if (layout.name == "LEFT") {
            Image(
                provider = ImageProvider(icon),
                contentDescription = "Icon",
                modifier = GlanceModifier.size(iconSize.dp),
                colorFilter = colorIcon
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
        }
        Text(
            text = number,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = fontSize.sp,
                fontFamily = FontFamily.Monospace,
                color = if (iszone) colorFilter else TextDayNight,
                textAlign = when (layout.name) {
                    "CENTER" -> TextAlign.Center
                    "RIGHT" -> TextAlign.End
                    else -> TextAlign.Start
                }
            ),
            maxLines = maxLines,
            modifier = GlanceModifier.defaultWeight()
        )
        if (layout.name != "LEFT") {
            Spacer(modifier = GlanceModifier.width(iconGap.dp).fillMaxHeight())
            Image(
                provider = ImageProvider(icon),
                contentDescription = "Icon",
                modifier = GlanceModifier.size(iconSize.dp),
                colorFilter = colorIcon
            )
        }
        Spacer(modifier = GlanceModifier.height(1.dp).background(TextDayNight))
    }
}

// Variante para el sextuple en slots grandes: el icono va en la esquina superior
// derecha (no en línea con el número), así el dígito dispone de TODO el ancho de la
// columna y cabe mucho más grande. El número se centra; maxLines=1 evita el salto.
@Composable
private fun SextupleBigCellContent(number: String, icon: Int, colorFilter: ColorProvider, iszone: Boolean, fontSize: Int, iconSize: Int) {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = number,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (iszone) colorFilter else TextDayNight,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
        Box(modifier = GlanceModifier.fillMaxSize().padding(end = 2.dp, top = 1.dp), contentAlignment = Alignment.TopEnd) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = "Icon",
                modifier = GlanceModifier.size(iconSize.dp),
                colorFilter = ColorFilter.tint(colorFilter)
            )
        }
    }
}


@Composable
private fun SingleHorizontalField(icon: Int, iconColor: ColorProvider, layout: FieldPosition, fieldSize: FieldSize, zoneColor: ColorProvider, number: String, isheadwind: Boolean, iszone: Boolean, thousandsSuffix: Char = 'k') {
    val height = when (fieldSize) {
        FieldSize.LARGE -> 12.dp
        FieldSize.SMALL -> 6.dp
        FieldSize.MEDIUM -> 9.dp
        FieldSize.EXTRA_LARGE ->15.dp
    }


    Spacer(modifier = GlanceModifier.height(height))
    IconRow(icon, ColorFilter.tint(iconColor), layout)
    Spacer(modifier = GlanceModifier.height(5.dp))
    // Presupuesto con signo ("-1500" necesita 5) y compactación honesta: un valor que
    // no cabe pasa a "12k"/"12L" en vez de perder dígitos por la derecha (error 10x).
    // El presupuesto se mide sobre el núcleo sin el marcador "~" de estimación (KGhost),
    // que fitNumberToChars conserva aparte; +1 al budget cuando está presente.
    val core = number.removePrefix("~")
    val maxChars = (if (core.startsWith('-')) 5 else 4) + (number.length - core.length)
    val fitted = fitNumberToChars(number, maxChars, thousandsSuffix)
    if (isheadwind && fieldSize == FieldSize.MEDIUM) NumberRow(fitted, zoneColor, layout, fieldSize, false,true,iszone,iconColor)
    else NumberRow(fitted, zoneColor, layout, fieldSize, false,false,iszone,iconColor)

}


@Composable
fun NotSupported(overlayText: String, fontSize: Int)
{
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(5.dp),
        contentAlignment = Alignment(
            vertical = Alignment.Vertical.CenterVertically,
            horizontal = Alignment.Horizontal.CenterHorizontally,
        ),
    ) {
        Text(
            overlayText,
            maxLines = 2,
            style = TextStyle(
                TextDayNight,
                fontSize = (0.8 * fontSize).sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = GlanceModifier.background(TextNightDay
            ).padding(1.dp)
        )
    }

}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun RollingFieldScreen(
    dNumber: Double,
    isInt: Boolean,
    action: KarooAction,
    iconColor: ColorProvider,
    zonecolor: ColorProvider,
    fieldsize: FieldSize,
    iskaroo3: Boolean,
    clayout: FieldPosition,
    windtext: String,
    winddiff: Int,
    baseBitmap: Bitmap,
    selector: Boolean,
    textSize: Int,
    iszone: Boolean,
    ispreview: Boolean,
    secondValue: Double,
    isClimb: Boolean = false,
    fieldState: StreamState? = null
) {

    val icon = action.icon
    val label = action.label
    val ispower = action.powerField
    val isTime = action.action == KarooAction.TIMETODEST.action
    val isCivil =
        action.action == KarooAction.CIVIL_DUSK.action || action.action == KarooAction.CIVIL_DAWN.action

    // Misma regla que en los campos dobles/séxtuples (checkRealZone): pedal balance solo
    // colorea fuera de la banda 85%-107% y nunca con valor 0 (sin datos).
    val isRealZone = if (checkRealZone(action, iszone, dNumber, secondValue)) iszone else false



    if (selector) {
        val newInt = if (isClimb) true else isInt

        // Campos FA: formatear el enum a etiqueta ("Open", "Lock"...) como en los dobles.
        val isFA = action.name.startsWith("FA_")
        val number = if (isFA && fieldState != null) formatFAValue(fieldState, action.name)
        else estimateMarker(action, fieldState) + formatNumber(dNumber, newInt, isTime, isCivil, isClimb, thousandsSuffixFor(action))
        val numberSecond = formatNumber(secondValue, isInt, isTime, isCivil, isClimb)


        Box(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = if (iskaroo3) GlanceModifier.fillMaxSize()
                    .cornerRadius(6.dp) else GlanceModifier.fillMaxSize()
            )
            {
                Column(modifier = GlanceModifier.defaultWeight().background(zonecolor)) {
                    when (fieldsize) {

                        FieldSize.SMALL -> Spacer(modifier = GlanceModifier.height(2.dp))
                        FieldSize.MEDIUM -> Spacer(modifier = GlanceModifier.height(4.dp))
                        else -> Spacer(modifier = GlanceModifier.height(1.dp))

                    }
                    if (fieldsize == FieldSize.LARGE || fieldsize == FieldSize.EXTRA_LARGE) NotSupported(
                        "Size Not Supported",
                        24
                    )
                    else {
                        OneIconRow(icon, iconColor, label.uppercase(), isRealZone, fieldsize, isClimb)
                        // El presupuesto se mide sobre el núcleo SIN el marcador "~" (que
                        // fitNumberToChars conserva aparte): si no, "~-125" se tomaría como
                        // positivo y se recortaría el último dígito.
                        val core = number.removePrefix("~")
                        val markerBudget = number.length - core.length
                        OneNumberRow(
                            // Presupuesto con signo y compactación honesta (ver fitNumberToChars)
                            if (isClimb && !isFA) fitNumberToChars(number, (if (core.startsWith('-')) 5 else 4) + markerBudget, thousandsSuffixFor(action))
                            else fitNumberToChars(number, 6 + markerBudget, thousandsSuffixFor(action)),
                            clayout,
                            fieldsize,
                            (textSize * (if (ispreview) 0.8 else if (isClimb) 1.1 else 1.0)).roundToInt(),
                            isRealZone,
                            iconColor,
                            ispower,
                            numberSecond.take(3),
                            isClimb
                        )
                    }
                }
            }
        }
    } else HeadwindDirection(baseBitmap, winddiff, textSize, windtext)
}

fun checkRealZone(action: KarooAction, iszone: Boolean, dNumber: Double, secondValue: Double): Boolean {

    return if( (action.name =="AVERAGE_PEDAL_BALANCE" || action.name =="PEDAL_BALANCE") && iszone && action.powerField)
            !((dNumber > secondValue * 0.85 && dNumber < secondValue * 1.07) || (dNumber == 0.0))
        else true
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleScreenSelector(
    selector: Int, showH: Boolean, leftNumber: Double, rightNumber: Double, leftField: DoubleFieldType, rightField: DoubleFieldType,
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColorLeft: ColorProvider, zoneColorRight: ColorProvider, fieldSize: FieldSize,
    isKaroo3: Boolean, layout: FieldPosition, text: String, windDirection: Int, baseBitmap: Bitmap, isdivider:Boolean, leftNumberSecond:Double = 0.0, rightNumberSecond:Double = 0.0, isClimb: Boolean = false, isClimbField: Boolean = false,
    leftFieldState: StreamState? = null, rightFieldState: StreamState? = null, distanceWithDecimals: Boolean = false){


    val leftIcon= leftField.kaction.icon
    val rightIcon= rightField.kaction.icon
    val leftLabel= leftField.kaction.label
    val rightLabel= rightField.kaction.label

    val ispowerLeft= leftField.kaction.powerField
    val ispowerRight= rightField.kaction.powerField
    
    // Detectar si son campos de Flight Attendant
    val isLeftFA = leftField.kaction.name.startsWith("FA_")
    val isRightFA = rightField.kaction.name.startsWith("FA_")
    
    val isLeftInt= isIntField(leftField.kaction, ispowerLeft, isClimb, distanceWithDecimals)
    val isRightInt= isIntField(rightField.kaction, ispowerRight, isClimb, distanceWithDecimals)
    val leftCivil=leftField.kaction.action==KarooAction.CIVIL_DUSK.action ||  leftField.kaction.action==KarooAction.CIVIL_DAWN.action
    val rightCivil=rightField.kaction.action==KarooAction.CIVIL_DUSK.action ||  rightField.kaction.action==KarooAction.CIVIL_DAWN.action
    val leftTime=leftField.kaction.action==KarooAction.TIMETODEST.action
    val rightTime=rightField.kaction.action==KarooAction.TIMETODEST.action


    val iszoneLeft= if (checkRealZone(leftField.kaction,leftField.iszone,leftNumber,leftNumberSecond)) leftField.iszone else false
    val iszoneRight= if (checkRealZone(rightField.kaction,rightField.iszone,rightNumber,rightNumberSecond)) rightField.iszone else false

    // Formateo especial para campos FA; el marcador "~" de estimación (KGhost) se
    // antepone al valor real (devuelve "" salvo en gap estimado, así no afecta al resto).
    val newLeft = estimateMarker(leftField.kaction, leftFieldState) + when {
        isLeftFA && leftFieldState != null -> formatFAValue(leftFieldState, leftField.kaction.name)
        ispowerLeft -> (formatNumber(leftNumber, true) + "-" + formatNumber(
            leftNumberSecond,
            true
        ))
        !showH -> formatNumber(leftNumber, isLeftInt, leftTime, leftCivil, thousandsSuffix = thousandsSuffixFor(leftField.kaction))
        else -> when (selector) {
            0, 3 -> if (leftLabel == "IF") ((leftNumber * 10.0).roundToInt() / 10.0).toString()
                .take(3) else formatNumber(leftNumber, true, leftTime, leftCivil, thousandsSuffix = thousandsSuffixFor(leftField.kaction))
            else -> "0.0"
        }
    }


    val newRight = estimateMarker(rightField.kaction, rightFieldState) + when {
        isRightFA && rightFieldState != null -> formatFAValue(rightFieldState, rightField.kaction.name)
        ispowerRight -> (formatNumber(rightNumber, true) + "-" + formatNumber(
            rightNumberSecond,
            true
        ))
        !showH -> formatNumber(rightNumber, isRightInt, rightTime, rightCivil, thousandsSuffix = thousandsSuffixFor(rightField.kaction))
        else -> when (selector) {
            1, 3 -> if (rightLabel == "IF") ((rightNumber * 10.0).roundToInt() / 10.0).toString()
                .take(3) else formatNumber(rightNumber, true, rightTime, rightCivil, thousandsSuffix = thousandsSuffixFor(rightField.kaction))
            else -> "0.0"
        }
    }


    val icon1 = if (selector == 0 || selector == 3) leftIcon else 1
    val icon2 = if (selector == 1 || selector == 3) rightIcon else 1
    val iconColor1 = if (selector == 0 || selector == 3) iconColorLeft else PlaceholderAllBlack
    val iconColor2 = if (selector == 1 || selector == 3) iconColorRight else PlaceholderAllBlack
    val zoneColor1 = if (selector == 0 || selector == 3) zoneColorLeft else PlaceholderAllBlack
    val zoneColor2 = if (selector == 1 || selector == 3) zoneColorRight else PlaceholderAllBlack


  // Timber.w("newLeft: $newLeft  iconColorLeft: $iconColorLeft  zoneColorLeft: $zoneColorLeft  iszoneLeft: $iszoneLeft")
    if (!showH || ispowerLeft || ispowerRight) {
        when (fieldSize) {
            FieldSize.SMALL -> DoubleTypesVerticalScreenSmall(
                newLeft,
                newRight,
                leftIcon,
                rightIcon,
                iconColorLeft,
                iconColorRight,
                zoneColorLeft,
                zoneColorRight,
                isKaroo3,
                layout,
                iszoneLeft,
                iszoneRight,
                isdivider
            )

            FieldSize.MEDIUM, FieldSize.LARGE -> DoubleTypesVerticalScreenBig(
                newLeft,
                newRight,
                leftIcon,
                rightIcon,
                iconColorLeft,
                iconColorRight,
                zoneColorLeft,
                zoneColorRight,
                isKaroo3,
                layout,
                iszoneLeft,
                iszoneRight,
                isdivider
            )

            FieldSize.EXTRA_LARGE -> NotSupported("Size Not Supported", 24)
        }
    } else {
        DoubleTypesScreenHorizontal(
            newLeft,
            newRight,
            icon1,
            icon2,
            iconColor1,
            iconColor2,
            zoneColor1,
            zoneColor2,
            fieldSize,
            isKaroo3,
            layout,
            selector,
            text,
            windDirection,
            baseBitmap,
            iszoneLeft,
            iszoneRight,
            isdivider,
            isClimbField,
            thousandsSuffixFor(leftField.kaction),
            thousandsSuffixFor(rightField.kaction)
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun SextupleScreenSelector(
    selector: Int, showH: Boolean, firstNumber: Double, secondNumber: Double, thirdNumber: Double, fourthNumber: Double, fifthNumber: Double, sixthNumber: Double,
    firstField: DoubleFieldType, secondField: DoubleFieldType, thirdField: DoubleFieldType, fourthField: DoubleFieldType, fifthField: DoubleFieldType, sixthField: DoubleFieldType,
    iconColorFirst: ColorProvider, iconColorSecond: ColorProvider, iconColorThird: ColorProvider, iconColorFourth: ColorProvider, iconColorFifth: ColorProvider, iconColorSixth: ColorProvider,
    zoneColorFirst: ColorProvider, zoneColorSecond: ColorProvider, zoneColorThird: ColorProvider, zoneColorFourth: ColorProvider, zoneColorFifth: ColorProvider, zoneColorSixth: ColorProvider, fieldSize: FieldSize,
    isKaroo3: Boolean, layout: FieldPosition, text: String, windDirection: Int, baseBitmap: Bitmap, isdivider:Boolean,  firstNumberSecond:Double = 0.0, secondNumberSecond:Double = 0.0, thirdNumberSecond:Double = 0.0, fourthNumberSecond:Double = 0.0, fifthNumberSecond:Double = 0.0, sixthNumberSecond:Double = 0.0, isClimb: Boolean = false,isClimbField: Boolean = false,
    firstFieldState: StreamState? = null, secondFieldState: StreamState? = null, thirdFieldState: StreamState? = null, fourthFieldState: StreamState? = null, fifthFieldState: StreamState? = null, sixthFieldState: StreamState? = null){


    val firstIcon= firstField.kaction.icon
    val secondIcon= secondField.kaction.icon
    val thirdIcon= thirdField.kaction.icon
    val fourthIcon= fourthField.kaction.icon
    val fifthIcon= fifthField.kaction.icon
    val sixthIcon= sixthField.kaction.icon
    val firstLabel= firstField.kaction.label
    val secondLabel= secondField.kaction.label
    val thirdLabel= thirdField.kaction.label
    val fourthLabel= fourthField.kaction.label
    val fifthLabel= fifthField.kaction.label
    val sixthLabel= sixthField.kaction.label

    val ispowerFirst= firstField.kaction.powerField
    val ispowerSecond= secondField.kaction.powerField
    val ispowerThird= thirdField.kaction.powerField
    val ispowerFourth= fourthField.kaction.powerField
    val ispowerFifth= fifthField.kaction.powerField
    val ispowerSixth= sixthField.kaction.powerField
    val isFirstInt= (!(firstField.kaction.convert == "speed" || firstField.kaction.zone == "slopeZones" || firstField.kaction.label == "IF")) || (ispowerFirst) || (isClimb)
    val isSecondInt= !(secondField.kaction.convert == "speed" || secondField.kaction.zone == "slopeZones" || secondField.kaction.label == "IF")  || (ispowerSecond) || (isClimb)
    val isThirdInt= !(thirdField.kaction.convert == "speed" || thirdField.kaction.zone == "slopeZones" || thirdField.kaction.label == "IF")  || (ispowerThird) || (isClimb)
    val isFourthInt= !(fourthField.kaction.convert == "speed" || fourthField.kaction.zone == "slopeZones" || fourthField.kaction.label == "IF")  || (ispowerFourth) || (isClimb)
    val isFifthInt= !(fifthField.kaction.convert == "speed" || fifthField.kaction.zone == "slopeZones" || fifthField.kaction.label == "IF")  || (ispowerFifth) || (isClimb)
    val isSixthInt= !(sixthField.kaction.convert == "speed" || sixthField.kaction.zone == "slopeZones" || sixthField.kaction.label == "IF")  || (ispowerSixth) || (isClimb)
    val firstCivil=firstField.kaction.action==KarooAction.CIVIL_DUSK.action ||  firstField.kaction.action==KarooAction.CIVIL_DAWN.action
    val secondCivil=secondField.kaction.action==KarooAction.CIVIL_DUSK.action ||  secondField.kaction.action==KarooAction.CIVIL_DAWN.action
    val thirdCivil=thirdField.kaction.action==KarooAction.CIVIL_DUSK.action ||  thirdField.kaction.action==KarooAction.CIVIL_DAWN.action
    val fourthCivil=fourthField.kaction.action==KarooAction.CIVIL_DUSK.action ||  fourthField.kaction.action==KarooAction.CIVIL_DAWN.action
    val fifthCivil=fifthField.kaction.action==KarooAction.CIVIL_DUSK.action ||  fifthField.kaction.action==KarooAction.CIVIL_DAWN.action
    val sixthCivil=sixthField.kaction.action==KarooAction.CIVIL_DUSK.action ||  sixthField.kaction.action==KarooAction.CIVIL_DAWN.action
    val firstTime=firstField.kaction.action==KarooAction.TIMETODEST.action
    val secondTime=secondField.kaction.action==KarooAction.TIMETODEST.action
    val thirdTime=thirdField.kaction.action==KarooAction.TIMETODEST.action
    val fourthTime=fourthField.kaction.action==KarooAction.TIMETODEST.action
    val fifthTime=fifthField.kaction.action==KarooAction.TIMETODEST.action
    val sixthTime=sixthField.kaction.action==KarooAction.TIMETODEST.action


    val iszoneFirst= if (checkRealZone(firstField.kaction,firstField.iszone,firstNumber,firstNumberSecond)) firstField.iszone else false
    val iszoneSecond= if (checkRealZone(secondField.kaction,secondField.iszone,secondNumber,secondNumberSecond)) secondField.iszone else false
    val iszoneThird= if (checkRealZone(thirdField.kaction,thirdField.iszone,thirdNumber,thirdNumberSecond)) thirdField.iszone else false
    val iszoneFourth= if (checkRealZone(fourthField.kaction,fourthField.iszone,fourthNumber,fourthNumberSecond)) fourthField.iszone else false
    val iszoneFifth= if (checkRealZone(fifthField.kaction,fifthField.iszone,fifthNumber,fifthNumberSecond)) fifthField.iszone else false
    val iszoneSixth= if (checkRealZone(sixthField.kaction,sixthField.iszone,sixthNumber,sixthNumberSecond)) sixthField.iszone else false

    // Campos FA (Flight Attendant): formateo de los valores enum a etiquetas ("Open",
    // "Lock"...) igual que en DoubleScreenSelector; sin el StreamState mostraban el crudo.
    val newFirst = estimateMarker(firstField.kaction, firstFieldState) + when {
        firstField.kaction.name.startsWith("FA_") && firstFieldState != null ->
            formatFAValue(firstFieldState, firstField.kaction.name)
        ispowerFirst -> (formatNumber(firstNumber, true) + "-" + formatNumber(
            firstNumberSecond,
            true
        ))
        else -> formatNumber(firstNumber, isFirstInt, firstTime, firstCivil, thousandsSuffix = thousandsSuffixFor(firstField.kaction))
    }


    val newSecond = estimateMarker(secondField.kaction, secondFieldState) + when {
        secondField.kaction.name.startsWith("FA_") && secondFieldState != null ->
            formatFAValue(secondFieldState, secondField.kaction.name)
        ispowerSecond -> (formatNumber(secondNumber, true) + "-" + formatNumber(
            secondNumberSecond,
            true
        ))
        else -> formatNumber(secondNumber, isSecondInt, secondTime, secondCivil, thousandsSuffix = thousandsSuffixFor(secondField.kaction))
    }


    val newThird = estimateMarker(thirdField.kaction, thirdFieldState) + when {
        thirdField.kaction.name.startsWith("FA_") && thirdFieldState != null ->
            formatFAValue(thirdFieldState, thirdField.kaction.name)
        ispowerThird -> (formatNumber(thirdNumber, true) + "-" + formatNumber(
            thirdNumberSecond,
            true
        ))
        else -> formatNumber(thirdNumber, isThirdInt, thirdTime, thirdCivil, thousandsSuffix = thousandsSuffixFor(thirdField.kaction))
    }


    val newFourth = estimateMarker(fourthField.kaction, fourthFieldState) + when {
        fourthField.kaction.name.startsWith("FA_") && fourthFieldState != null ->
            formatFAValue(fourthFieldState, fourthField.kaction.name)
        ispowerFourth -> (formatNumber(fourthNumber, true) + "-" + formatNumber(
            fourthNumberSecond,
            true
        ))
        else -> formatNumber(fourthNumber, isFourthInt, fourthTime, fourthCivil, thousandsSuffix = thousandsSuffixFor(fourthField.kaction))
    }


    val newFifth = estimateMarker(fifthField.kaction, fifthFieldState) + when {
        fifthField.kaction.name.startsWith("FA_") && fifthFieldState != null ->
            formatFAValue(fifthFieldState, fifthField.kaction.name)
        ispowerFifth -> (formatNumber(fifthNumber, true) + "-" + formatNumber(
            fifthNumberSecond,
            true
        ))
        else -> formatNumber(fifthNumber, isFifthInt, fifthTime, fifthCivil, thousandsSuffix = thousandsSuffixFor(fifthField.kaction))
    }


    val newSixth = estimateMarker(sixthField.kaction, sixthFieldState) + when {
        sixthField.kaction.name.startsWith("FA_") && sixthFieldState != null ->
            formatFAValue(sixthFieldState, sixthField.kaction.name)
        ispowerSixth -> (formatNumber(sixthNumber, true) + "-" + formatNumber(
            sixthNumberSecond,
            true
        ))
        else -> formatNumber(sixthNumber, isSixthInt, sixthTime, sixthCivil, thousandsSuffix = thousandsSuffixFor(sixthField.kaction))
    }


    // Timber.w("newLeft: $newLeft  iconColorLeft: $iconColorLeft  zoneColorLeft: $zoneColorLeft  iszoneLeft: $iszoneLeft")
    when (fieldSize) {
        FieldSize.SMALL -> SextupleTypesVerticalScreenSmall(
            newFirst,
            newSecond,
            newThird,
            newFourth,
            newFifth,
            newSixth,
            firstIcon,
            secondIcon,
            thirdIcon,
            fourthIcon,
            fifthIcon,
            sixthIcon,
            iconColorFirst,
            iconColorSecond,
            iconColorThird,
            iconColorFourth,
            iconColorFifth,
            iconColorSixth,
            zoneColorFirst,
            zoneColorSecond,
            zoneColorThird,
            zoneColorFourth,
            zoneColorFifth,
            zoneColorSixth,
            isKaroo3,
            layout,
            iszoneFirst,
            iszoneSecond,
            iszoneThird,
            iszoneFourth,
            iszoneFifth,
            iszoneSixth,
            isdivider
        )

        FieldSize.MEDIUM, FieldSize.LARGE, FieldSize.EXTRA_LARGE -> SextupleTypesVerticalScreenBig(
            newFirst,
            newSecond,
            newThird,
            newFourth,
            newFifth,
            newSixth,
            firstIcon,
            secondIcon,
            thirdIcon,
            fourthIcon,
            fifthIcon,
            sixthIcon,
            iconColorFirst,
            iconColorSecond,
            iconColorThird,
            iconColorFourth,
            iconColorFifth,
            iconColorSixth,
            zoneColorFirst,
            zoneColorSecond,
            zoneColorThird,
            zoneColorFourth,
            zoneColorFifth,
            zoneColorSixth,
            isKaroo3,
            layout,
            iszoneFirst,
            iszoneSecond,
            iszoneThird,
            iszoneFourth,
            iszoneFifth,
            iszoneSixth,
            isdivider,
            fieldSize
        )
    }
}


fun getFieldTypeSelector(firstFieldState:String,secondFieldState:String) :Int
{

    return when {
        firstFieldState == "HEADWIND"  && secondFieldState=="HEADWIND" -> 2
        firstFieldState == "HEADWIND" -> 1
        secondFieldState =="HEADWIND" -> 0
        else -> 3
    }

}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
fun ClimbScreenSelector(
    firstValue: Double,
    secondValue: Double,
    thirdValue: Double,
    fourthValue: Double,
    climbValue: Double,
    firstField: DoubleFieldType,
    secondField: DoubleFieldType,
    thirdField: DoubleFieldType,
    fourthField: DoubleFieldType,
    climbField: DoubleFieldType,
    firstIconColor: ColorProvider,
    secondIconColor: ColorProvider,
    thirdIconColor: ColorProvider,
    fourthIconColor: ColorProvider,
    climbIconColor: ColorProvider,
    firstZoneColor: ColorProvider,
    secondZoneColor: ColorProvider,
    thirdZoneColor: ColorProvider,
    fourthZoneColor: ColorProvider,
    climbZoneColor: ColorProvider,
    fieldSize: Int,
    effectiveFieldSize: FieldSize = FieldSize.MEDIUM,
    isKaroo3: Boolean,
    layout: FieldPosition,
    windText: String,
    windDirection: Int,
    baseBitmap: Bitmap,
    isDivider: Boolean,
    firstValueRight: Double,
    secondValueRight: Double,
    thirdValueRight: Double,
    fourthValueRight: Double,
    climbValueRight: Double,
    isFirsthorizontal: Boolean,
    isSecondhorizontal: Boolean,
    isClimbEnabled: Boolean,
    distanceWithDecimals: Boolean = false,
    firstFieldState: StreamState? = null,
    secondFieldState: StreamState? = null,
    thirdFieldState: StreamState? = null,
    fourthFieldState: StreamState? = null,
    climbFieldState: StreamState? = null
) {
    if (fieldSize < 400) {
        NotSupported("Size Not Supported", 24)
        return
    }

    // Tamaño efectivo para sub-paneles: forzar máximo MEDIUM en climb
    // (los paneles laterales de 90dp no soportan LARGE/EXTRA_LARGE)
    val innerFieldSize = if (effectiveFieldSize.ordinal > FieldSize.MEDIUM.ordinal) FieldSize.MEDIUM else effectiveFieldSize
    val centerTextSize = if (innerFieldSize == FieldSize.SMALL) 30 else 40

    if (!isClimbEnabled)
    {

        Row(modifier = GlanceModifier.fillMaxSize()) {

            Column(modifier = GlanceModifier.fillMaxHeight().defaultWeight()) {
                DoubleScreenSelector(
                    selector = getFieldTypeSelector(firstField.kaction.name, secondField.kaction.name),
                    showH = isFirsthorizontal,
                    leftNumber = firstValue,
                    rightNumber = secondValue,
                    leftField = firstField,
                    rightField = secondField,
                    iconColorLeft = firstIconColor,
                    iconColorRight = secondIconColor,
                    zoneColorLeft = firstZoneColor,
                    zoneColorRight = secondZoneColor,
                    fieldSize = innerFieldSize,
                    isKaroo3 = isKaroo3,
                    layout = layout,
                    text = windText,
                    windDirection = windDirection,
                    baseBitmap = baseBitmap,
                    isdivider = isDivider,
                    leftNumberSecond = firstValueRight,
                    rightNumberSecond = secondValueRight,
                    isClimb = false,
                    isClimbField = true,
                    leftFieldState = firstFieldState,
                    rightFieldState = secondFieldState,
                    distanceWithDecimals = distanceWithDecimals
                )
            }


            if (isDivider) {
                Spacer(
                    modifier = GlanceModifier.fillMaxHeight()
                        .width(1.dp)
                        .background(TextDayNight)
                )
            }


            Column(modifier = GlanceModifier.fillMaxHeight().defaultWeight()) {
                DoubleScreenSelector(
                    selector = getFieldTypeSelector(thirdField.kaction.name, fourthField.kaction.name),
                    showH = isSecondhorizontal,
                    leftNumber = thirdValue,
                    rightNumber = fourthValue,
                    leftField = thirdField,
                    rightField = fourthField,
                    iconColorLeft = thirdIconColor,
                    iconColorRight = fourthIconColor,
                    zoneColorLeft = thirdZoneColor,
                    zoneColorRight = fourthZoneColor,
                    fieldSize = innerFieldSize,
                    isKaroo3 = isKaroo3,
                    layout = layout,
                    text = windText,
                    windDirection = windDirection,
                    baseBitmap = baseBitmap,
                    isdivider = isDivider,
                    leftNumberSecond = thirdValueRight,
                    rightNumberSecond = fourthValueRight,
                    isClimb = false,
                    isClimbField = true,
                    leftFieldState = thirdFieldState,
                    rightFieldState = fourthFieldState,
                    distanceWithDecimals = distanceWithDecimals
                )
            }
        }
    } else {

        Row(modifier = GlanceModifier.fillMaxSize()) {

            Column(modifier = GlanceModifier.fillMaxHeight().width(90.dp)) {
                DoubleScreenSelector(
                    selector = getFieldTypeSelector(firstField.kaction.name, secondField.kaction.name),
                    showH = isFirsthorizontal,
                    leftNumber = firstValue,
                    rightNumber = secondValue,
                    leftField = firstField,
                    rightField = secondField,
                    iconColorLeft = firstIconColor,
                    iconColorRight = secondIconColor,
                    zoneColorLeft = firstZoneColor,
                    zoneColorRight = secondZoneColor,
                    fieldSize = innerFieldSize,
                    isKaroo3 = isKaroo3,
                    layout = layout,
                    text = windText,
                    windDirection = windDirection,
                    baseBitmap = baseBitmap,
                    isdivider = isDivider,
                    leftNumberSecond = firstValueRight,
                    rightNumberSecond = secondValueRight,
                    isClimb = true,
                    isClimbField = true,
                    leftFieldState = firstFieldState,
                    rightFieldState = secondFieldState
                )
            }


            if (isDivider) {
                Spacer(
                    modifier = GlanceModifier.fillMaxHeight()
                        .width(1.dp)
                        .background(TextDayNight)
                )
            }


            Column(modifier = GlanceModifier.fillMaxHeight().defaultWeight()) {
                RollingFieldScreen(
                    dNumber = climbValue,
                    isInt = !(climbField.kaction.convert == "speed" || climbField.kaction.zone == "slopeZones" || climbField.kaction.label == "IF") || climbField.kaction.powerField,
                    action = climbField.kaction,
                    iconColor = climbIconColor,
                    zonecolor = climbZoneColor,
                    fieldsize = innerFieldSize,
                    iskaroo3 = isKaroo3,
                    clayout = FieldPosition.CENTER,
                    windtext = windText,
                    winddiff = windDirection,
                    baseBitmap = baseBitmap,
                    selector = true,
                    textSize = centerTextSize,
                    iszone = climbField.iszone,
                    ispreview = false,
                    secondValue = climbValueRight,
                    isClimb = true,
                    fieldState = climbFieldState,
                )
            }


            if (isDivider) {
                Spacer(
                    modifier = GlanceModifier.fillMaxHeight()
                        .width(1.dp)
                        .background(TextDayNight)
                )
            }


            Column(modifier = GlanceModifier.fillMaxHeight().width(90.dp)) {
                DoubleScreenSelector(
                    selector = getFieldTypeSelector(thirdField.kaction.name, fourthField.kaction.name),
                    showH = isSecondhorizontal,
                    leftNumber = thirdValue,
                    rightNumber = fourthValue,
                    leftField = thirdField,
                    rightField = fourthField,
                    iconColorLeft = thirdIconColor,
                    iconColorRight = fourthIconColor,
                    zoneColorLeft = thirdZoneColor,
                    zoneColorRight = fourthZoneColor,
                    fieldSize = innerFieldSize,
                    isKaroo3 = isKaroo3,
                    layout = layout,
                    text = windText,
                    windDirection = windDirection,
                    baseBitmap = baseBitmap,
                    isdivider = isDivider,
                    leftNumberSecond = thirdValueRight,
                    rightNumberSecond = fourthValueRight,
                    isClimb = true,
                    isClimbField = true,
                    leftFieldState = thirdFieldState,
                    rightFieldState = fourthFieldState
                )
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
private fun DoubleTypesScreenHorizontal(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, fieldSize: FieldSize,
    isKaroo3: Boolean, layout: FieldPosition, selector: Int, text: String, windDirection: Int, baseBitmap: Bitmap,iszoneLeft: Boolean,iszoneRight: Boolean,isdivider:Boolean,isClimbField: Boolean = false,
    leftThousandsSuffix: Char = 'k', rightThousandsSuffix: Char = 'k'
) {


    if (!isClimbField) VerticalDivider(true, fieldSize,isdivider)
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {

        Row(modifier = GlanceModifier.fillMaxSize().let { if (isKaroo3) it.cornerRadius(8.dp) else it }) {
            Column(modifier = GlanceModifier.defaultWeight().background(if (selector in 1..2) TextNightDay else zoneColor1)) {
                when (selector) {
                    1, 2 -> HeadwindDirectionDoubleType(baseBitmap, windDirection, 38, text)
                    0 -> SingleHorizontalField(leftIcon, iconColorLeft, layout, fieldSize, zoneColor1, leftNumber, true,iszoneLeft, leftThousandsSuffix)
                    else -> SingleHorizontalField(leftIcon, iconColorLeft, layout, fieldSize, zoneColor1, leftNumber, false,iszoneLeft, leftThousandsSuffix)
                }
            }
            if (isdivider) Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(TextDayNight))
            Column(modifier = GlanceModifier.defaultWeight().background(if (selector in listOf(0, 2)) TextNightDay else zoneColor2)) {
                when (selector) {
                    0, 2 -> HeadwindDirectionDoubleType(baseBitmap, windDirection, 38, text)
                    1 -> SingleHorizontalField(rightIcon, iconColorRight, layout, fieldSize, zoneColor2, rightNumber, true,iszoneRight, rightThousandsSuffix)
                    else -> SingleHorizontalField(rightIcon, iconColorRight, layout, fieldSize, zoneColor2, rightNumber, false,iszoneRight, rightThousandsSuffix)
                }
            }
        }
    }
    VerticalDivider(false, fieldSize,isdivider)
}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
private fun DoubleTypesVerticalScreenSmall(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, isKaroo3: Boolean, layout: FieldPosition, iszoneLeft: Boolean, iszoneRight: Boolean, isdivider: Boolean
) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        Column(modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(zoneColor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zoneColor1)) {
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor1)) {
                Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(zoneColor1))
                HorizontalScreenContent(leftNumber, leftIcon, iconColorLeft, layout,iszoneLeft)
            }
            if (isdivider) Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(TextDayNight))
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(zoneColor2))
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor2)) {
                HorizontalScreenContent(rightNumber, rightIcon, iconColorRight, layout,iszoneRight)
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
private fun DoubleTypesVerticalScreenBig(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, isKaroo3: Boolean, layout: FieldPosition,iszoneLeft: Boolean,iszoneRight: Boolean,isdivider: Boolean
) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        Column(modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(zoneColor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zoneColor1)) {
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor1)) {
                Spacer(modifier = GlanceModifier.fillMaxWidth().height(2.dp).background(zoneColor1))
                HorizontalScreenContent(leftNumber, leftIcon, iconColorLeft, layout,iszoneLeft)
            }
            if (isdivider) Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(TextDayNight))
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(7.dp).background(zoneColor2))
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor2)) {
                HorizontalScreenContent(rightNumber, rightIcon, iconColorRight, layout,iszoneRight)
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
private fun SextupleTypesVerticalScreenSmall(
    firstNumber: String, secondNumber: String, thirdNumber: String, fourthNumber: String, fifthNumber: String, sixthNumber: String,
    firstIcon: Int, secondIcon: Int, thirdIcon: Int, fourthIcon: Int, fifthIcon: Int, sixthIcon: Int,
    iconColorFirst: ColorProvider, iconColorSecond: ColorProvider, iconColorThird: ColorProvider, iconColorFourth: ColorProvider, iconColorFifth: ColorProvider, iconColorSixth: ColorProvider,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, zoneColor3: ColorProvider, zoneColor4: ColorProvider, zoneColor5: ColorProvider, zoneColor6: ColorProvider,
    isKaroo3: Boolean, layout: FieldPosition,
    iszoneFirst: Boolean, iszoneSecond: Boolean, iszoneThird: Boolean, iszoneFourth: Boolean, iszoneFifth: Boolean, iszoneSixth: Boolean,
    isdivider: Boolean
) {
    // Escalado de fuente por longitud del valor más largo (ya recortado a 3-4 chars),
    // igual que SextupleTypesVerticalScreenBig: aquí el icono va EN LÍNEA con el número
    // (HorizontalScreenContent), así que un valor de 4+ chars no cabe a 38sp. maxLines=1
    // evita el salto de línea. trimNumberTo3Chars solo recorta decimales, por eso un
    // entero de 4 dígitos (o un power "123-456") llega entero y necesita la fuente menor.
    val maxLen = listOf(firstNumber, secondNumber, thirdNumber, fourthNumber, fifthNumber, sixthNumber)
        .maxOf { trimNumberTo3Chars(it).length }
    val (cellFontSize, cellIconSize) = when {
        maxLen <= 3 -> 38 to 20
        maxLen == 4 -> 30 to 18
        else -> 24 to 16
    }
    // Hueco número↔icono solo para el sextuple pequeño (los dobles usan el default 5dp).
    // Súbelo/bájalo aquí en un único sitio para afinar la separación.
    val cellIconGap = 12
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        //cornerRadius(8.dp)
        Column(modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(TextNightDay).cornerRadius(0.dp) else GlanceModifier.fillMaxSize().background(TextNightDay)) {
            // row added
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor1)) {
                    //Spacer(
                    //    modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(zoneColor1)
                    //)
                    HorizontalScreenContent(trimNumberTo3Chars(firstNumber), firstIcon, iconColorFirst, layout,iszoneFirst, cellFontSize, cellIconSize, 1, cellIconGap)
                }
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor2)) {
                    //Spacer(
                    //    modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(zoneColor2)
                    //)
                    HorizontalScreenContent(trimNumberTo3Chars(secondNumber), secondIcon, iconColorSecond, layout,iszoneSecond, cellFontSize, cellIconSize, 1, cellIconGap)
                }
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor3)) {
                    //Spacer(
                    //    modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(zoneColor3)
                    //)
                    HorizontalScreenContent(trimNumberTo3Chars(thirdNumber), thirdIcon, iconColorThird, layout,iszoneThird, cellFontSize, cellIconSize, 1, cellIconGap)
                }
            }
            if (isdivider) Spacer(
                modifier = GlanceModifier.fillMaxWidth().height(1.dp)
                    .background(TextDayNight)
            ) else Spacer(
                modifier = GlanceModifier.fillMaxWidth().height(1.dp)
                    .background(TextNightDay)
            )

            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor4)) {
                    //Spacer(modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(zoneColor4))
                    HorizontalScreenContent(trimNumberTo3Chars(fourthNumber), fourthIcon, iconColorFourth, layout,iszoneFourth, cellFontSize, cellIconSize, 1, cellIconGap)
                }
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor5)) {
                    //Spacer(modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(zoneColor5))
                    HorizontalScreenContent(trimNumberTo3Chars(fifthNumber), fifthIcon, iconColorFifth, layout,iszoneFifth, cellFontSize, cellIconSize, 1, cellIconGap)
                }
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor6)) {
                    //Spacer(modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(zoneColor6))
                    HorizontalScreenContent(trimNumberTo3Chars(sixthNumber), sixthIcon, iconColorSixth, layout,iszoneSixth, cellFontSize, cellIconSize, 1, cellIconGap)
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
private fun SextupleTypesVerticalScreenBig(
    firstNumber: String, secondNumber: String, thirdNumber: String, fourthNumber: String, fifthNumber: String, sixthNumber: String,
    firstIcon: Int, secondIcon: Int, thirdIcon: Int, fourthIcon: Int, fifthIcon: Int, sixthIcon: Int,
    iconColorFirst: ColorProvider, iconColorSecond: ColorProvider, iconColorThird: ColorProvider, iconColorFourth: ColorProvider, iconColorFifth: ColorProvider, iconColorSixth: ColorProvider,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, zoneColor3: ColorProvider, zoneColor4: ColorProvider, zoneColor5: ColorProvider, zoneColor6: ColorProvider,
    isKaroo3: Boolean, layout: FieldPosition,
    iszoneFirst: Boolean, iszoneSecond: Boolean, iszoneThird: Boolean, iszoneFourth: Boolean, iszoneFifth: Boolean, iszoneSixth: Boolean,
    isdivider: Boolean, fieldSize: FieldSize = FieldSize.MEDIUM
) {
    // EXTRA_LARGE reutiliza este layout (es 100% weight-based), pero con fuente e
    // icono mayores para aprovechar el slot full-width; MEDIUM/LARGE mantienen 38sp/20dp.
    val isExtraLarge = fieldSize == FieldSize.EXTRA_LARGE
    // El icono va en la esquina superior derecha (SextupleBigCellContent), así que el
    // número dispone de TODO el ancho de la columna (1/3 del slot). Aun así escalamos la
    // fuente al nº de caracteres del valor más largo (ya recortado a 3-4) para que un
    // número de 3-4 dígitos no se recorte; maxLines=1 evita el salto de línea.
    val maxLen = listOf(firstNumber, secondNumber, thirdNumber, fourthNumber, fifthNumber, sixthNumber)
        .maxOf { trimNumberTo3Chars(it).length }
    val (contentFontSize, contentIconSize) = if (isExtraLarge) {
        when {
            maxLen <= 2 -> 58 to 22
            maxLen == 3 -> 48 to 20
            maxLen <= 5 -> 40 to 18   // 4-5 chars ("-1.2", "12.5L", "Close", "12500")
            else -> 32 to 16          // 6+ chars (power "123-456", "-99.9L")
        }
    } else {
        when {
            maxLen <= 3 -> 42 to 18
            maxLen <= 5 -> 34 to 16   // 4-5 chars: mismo tamaño que antes (un "12500" de
                                      // ascenso imperial no debe encoger las 6 celdas)
            else -> 28 to 14          // 6+ chars (power "123-456")
        }
    }
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        //cornerRadius(8.dp)
        Column(modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(TextNightDay).cornerRadius(0.dp) else GlanceModifier.fillMaxSize().background(TextNightDay)) {
            // row added
            //Spacer(modifier = GlanceModifier.fillMaxWidth().height(7.dp).background(zoneColor1))
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor1)) {
                    Spacer(modifier = GlanceModifier.fillMaxWidth().height(8.dp).background(zoneColor1))
                    SextupleBigCellContent(trimNumberTo3Chars(firstNumber), firstIcon, iconColorFirst, iszoneFirst, contentFontSize, contentIconSize)
                }
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor2)) {
                    Spacer(modifier = GlanceModifier.fillMaxWidth().height(8.dp).background(zoneColor2))
                    SextupleBigCellContent(trimNumberTo3Chars(secondNumber), secondIcon, iconColorSecond, iszoneSecond, contentFontSize, contentIconSize)
                }
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor3)) {
                    Spacer(modifier = GlanceModifier.fillMaxWidth().height(8.dp).background(zoneColor3))
                    SextupleBigCellContent(trimNumberTo3Chars(thirdNumber), thirdIcon, iconColorThird, iszoneThird, contentFontSize, contentIconSize)
                }
            }
            if (isdivider) Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(TextDayNight))
            else Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(TextNightDay))
            // row added
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor4)) {
                    Spacer(modifier = GlanceModifier.fillMaxWidth().height(7.dp).background(zoneColor4))
                    SextupleBigCellContent(trimNumberTo3Chars(fourthNumber), fourthIcon, iconColorFourth, iszoneFourth, contentFontSize, contentIconSize)
                }
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor5)) {
                    Spacer(modifier = GlanceModifier.fillMaxWidth().height(7.dp).background(zoneColor5))
                    SextupleBigCellContent(trimNumberTo3Chars(fifthNumber), fifthIcon, iconColorFifth, iszoneFifth, contentFontSize, contentIconSize)
                }
                Column(modifier = GlanceModifier.defaultWeight().background(zoneColor6)) {
                    Spacer(modifier = GlanceModifier.fillMaxWidth().height(7.dp).background(zoneColor6))
                    SextupleBigCellContent(trimNumberTo3Chars(sixthNumber), sixthIcon, iconColorSixth, iszoneSixth, contentFontSize, contentIconSize)
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
private fun HeadwindDirectionDoubleType(baseBitmap: Bitmap, bearing: Int, fontSize: Int, overlayText: String) {
    Spacer(modifier = GlanceModifier.fillMaxWidth().height(10.dp))
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = GlanceModifier.fillMaxSize(),
            provider = ImageProvider(getArrowBitmapByBearing(baseBitmap, bearing)),
            contentDescription = "Relative wind direction indicator",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(TextDayNight)
        )
        if (overlayText.isNotEmpty()) {
            Text(
                overlayText,
                style = TextStyle(TextDayNight, fontSize = (0.6 * fontSize).sp, fontFamily = FontFamily.Monospace),
                modifier = GlanceModifier.background(OverlayBgDay, OverlayBgNight).padding(1.dp)
            )
        }
    }
}
