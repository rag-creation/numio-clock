package com.rr.numio.clock.ui.clock

import com.rr.numio.clock.data.WorldCity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rr.numio.clock.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.border
import kotlin.math.cos
import kotlin.math.sin

// ── Dial styles ───────────────────────────────────────────────────────────────
enum class DialStyle(val label: String, val desc: String) {
    CLASSIC     ("Classic",       "Full dial with numbers"),
    CLASSIC_PRE ("Classic Pro",   "Premium amber dot markers"),
    SKELETON    ("Skeleton",      "Minimal markers only"),
}

// ── Ink trail state ───────────────────────────────────────────────────────────
private val inkTrailData = mutableListOf<Triple<Double, Double, Double>>()
private var inkLastSec = -1

// ── Flip state ────────────────────────────────────────────────────────────────
private var flipHPrev = "12"; private var flipHCur = "12"
private var flipMPrev = "00"; private var flipMCur = "00"
private var flipHProg = 1f;   private var flipMProg = 1f
private var flipLastMs = System.currentTimeMillis()

// ── Morph state ───────────────────────────────────────────────────────────────
private var morphCurrentH = -1; private var morphPrevH = 0
private var morphProgress = 1f; private var morphLastMs = 0L

val cityPickerList = listOf(
    WorldCity("New York",       "UTC −4",    -4),
    WorldCity("Los Angeles",    "UTC −7",    -7),
    WorldCity("Chicago",        "UTC −5",    -5),
    WorldCity("Toronto",        "UTC −4",    -4),
    WorldCity("São Paulo",      "UTC −3",    -3),
    WorldCity("London",         "UTC +1",     1),
    WorldCity("Paris",          "UTC +2",     2),
    WorldCity("Berlin",         "UTC +2",     2),
    WorldCity("Dubai",          "UTC +4",     4),
    WorldCity("Moscow",         "UTC +3",     3),
    WorldCity("Istanbul",       "UTC +3",     3),
    WorldCity("Riyadh",         "UTC +3",     3),
    WorldCity("Karachi",        "UTC +5",     5),
    WorldCity("Mumbai",         "UTC +5:30",  5),
    WorldCity("Dhaka",          "UTC +6",     6),
    WorldCity("Bangkok",        "UTC +7",     7),
    WorldCity("Singapore",      "UTC +8",     8),
    WorldCity("Hong Kong",      "UTC +8",     8),
    WorldCity("Beijing",        "UTC +8",     8),
    WorldCity("Tokyo",          "UTC +9",     9),
    WorldCity("Seoul",          "UTC +9",     9),
    WorldCity("Sydney",         "UTC +10",   10),
    WorldCity("Auckland",       "UTC +12",   12),
    WorldCity("Johannesburg",   "UTC +2",     2),
    WorldCity("Cairo",          "UTC +2",     2),
    WorldCity("Nairobi",        "UTC +3",     3),
    WorldCity("Kochi",          "UTC +5:30",  5),
    WorldCity("Pathanamthitta", "UTC +5:30",  5),
)

@Composable
fun ClockScreen(vm: ClockViewModel = viewModel()) {
    var time by remember { mutableStateOf(Calendar.getInstance()) }
    val accentColor = AppColor.accent.value
    var showCityPicker by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var dialStyle by remember { mutableStateOf(DialStyle.CLASSIC) }

    // Load saved dial style
    val savedDialStyle by com.rr.numio.clock.data.WorldCityStore
        .getDialStyle(context).collectAsState(initial = "CLASSIC")
    LaunchedEffect(savedDialStyle) {
        dialStyle = try { DialStyle.valueOf(savedDialStyle) } catch (e: Exception) { DialStyle.CLASSIC }
    }

    val selectedCities by vm.selectedCities.collectAsState()
    var activeMenuIndex by remember { mutableStateOf(-1) }

    // Dial picker state
    var isPickerActive by remember { mutableStateOf(false) }
    var pickerSelectedIdx by remember { mutableStateOf(0) }
    var pickerDragX by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) { time = Calendar.getInstance(); delay(16) }
    }

    val density = LocalDensity.current.density
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // Close picker if user scrolls the list
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && isPickerActive) {
            isPickerActive = false
            pickerDragX = 0f
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .pointerInput(activeMenuIndex) {
                detectTapGestures { if (activeMenuIndex != -1) activeMenuIndex = -1 }
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "CLOCK · NUMIO",
                    fontSize = 12.sp, fontWeight = FontWeight.W500,
                    color = accentColor, letterSpacing = 3.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))

                // ── Dial area: normal or picker mode ──────────────────────
                if (!isPickerActive) {
                    // Normal clock
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = {
                                    isPickerActive = true
                                    pickerSelectedIdx = dialStyle.ordinal
                                    pickerDragX = 0f
                                })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            when (dialStyle) {
                                DialStyle.CLASSIC     -> drawClassicDial(time, accentColor)
                                DialStyle.CLASSIC_PRE -> drawClassicPremiumDial(time, accentColor)
                                DialStyle.SKELETON    -> drawSkeletonDial(time, accentColor)
                            }
                        }
                    }
                    Text(
                        "hold to change style",
                        fontSize = 9.sp,
                        color = NumioTextMuted.copy(alpha = 0.35f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                // picker renders as overlay outside LazyColumn — see below

                Spacer(modifier = Modifier.height(14.dp))

                // Digital time display (hide for LCD/Flip/Smart — they show their own)
                val showDigitalBelow = true
                if (showDigitalBelow) {
                    val hour = time.get(Calendar.HOUR)
                    val minute = time.get(Calendar.MINUTE)
                    val amPm = if (time.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                    val h = if (hour == 0) 12 else hour
                    val m = minute.toString().padStart(2, '0')
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text("$h:$m", fontSize = 64.sp, fontWeight = FontWeight.W100,
                            color = NumioTextPrimary, letterSpacing = (-2).sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(amPm, fontSize = 24.sp, fontWeight = FontWeight.W400,
                            color = accentColor, modifier = Modifier.padding(top = 10.dp))
                    }
                    Canvas(modifier = Modifier.width(60.dp).height(2.dp)) {
                        drawRect(color = accentColor)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val dayNames = listOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
                val monthNames = listOf("January","February","March","April","May","June","July","August","September","October","November","December")
                Text(
                    "${dayNames[time.get(Calendar.DAY_OF_WEEK)-1]}, ${monthNames[time.get(Calendar.MONTH)]} ${time.get(Calendar.DAY_OF_MONTH)}".uppercase(),
                    fontSize = 11.sp, fontWeight = FontWeight.W400,
                    color = NumioTextMuted, letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            itemsIndexed(selectedCities) { index, city ->
                val isDragging = draggingIndex == index
                WorldClockRow(
                    city = city, accentColor = accentColor,
                    isMenuOpen = activeMenuIndex == index,
                    isDragging = isDragging,
                    dragOffsetY = if (isDragging) dragOffsetY else 0f,
                    onLongPress = { activeMenuIndex = if (activeMenuIndex == index) -1 else index },
                    onDelete = { vm.removeCity(city); activeMenuIndex = -1 },
                    onMoveUp = {
                        if (index > 0) {
                            val list = selectedCities.toMutableList()
                            val tmp = list[index]; list[index] = list[index-1]; list[index-1] = tmp
                            vm.reorderCities(list)
                        }
                        activeMenuIndex = -1
                    },
                    onMoveDown = {
                        if (index < selectedCities.lastIndex) {
                            val list = selectedCities.toMutableList()
                            val tmp = list[index]; list[index] = list[index+1]; list[index+1] = tmp
                            vm.reorderCities(list)
                        }
                        activeMenuIndex = -1
                    },
                    onDragStart = { draggingIndex = index; dragOffsetY = 0f; activeMenuIndex = -1 },
                    onDrag = { dy ->
                        dragOffsetY += dy
                        val rowH = 56f * density
                        val steps = (dragOffsetY / rowH).toInt()
                        val newIdx = (index + steps).coerceIn(0, selectedCities.lastIndex)
                        if (newIdx != index) {
                            val list = selectedCities.toMutableList()
                            val item = list.removeAt(index); list.add(newIdx, item)
                            vm.reorderCities(list); draggingIndex = newIdx
                            dragOffsetY -= steps * rowH
                        }
                    },
                    onDragEnd = { draggingIndex = null; dragOffsetY = 0f }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { showCityPicker = true; activeMenuIndex = -1 }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("+ Add city", fontSize = 13.sp,
                        color = accentColor.copy(alpha = 0.6f), letterSpacing = 0.5.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ── Dial picker overlay — Apple Watch style ──────────────────────────────
    if (isPickerActive) {
        val styles = DialStyle.values()
        val dialSize = 220.dp
        val peekSize = 110.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0000000))
                .pointerInput(Unit) {
                    // tap outside dials = close
                    detectTapGestures { isPickerActive = false; pickerDragX = 0f }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "DIAL STYLE",
                    fontSize = 10.sp, color = NumioTextMuted,
                    letterSpacing = 2.sp, fontWeight = FontWeight.W500
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Clipped row — only show peek amount on edges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dialSize),
                    contentAlignment = Alignment.Center
                ) {
                    // Previous peek (left edge)
                    val prevIdx = pickerSelectedIdx - 1
                    if (prevIdx >= 0) {
                        Box(
                            modifier = Modifier
                                .size(peekSize)
                                .align(Alignment.CenterStart)
                                .offset(x = (-peekSize * 0.3f) + (if (pickerDragX > 0) (pickerDragX * 0.003f).dp else 0.dp))
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(0xFF111111))
                                .clickable {
                                    pickerSelectedIdx = prevIdx
                                    pickerDragX = 0f
                                    dialStyle = styles[prevIdx]
                                    isPickerActive = false
                                    scope.launch {
                                        com.rr.numio.clock.data.WorldCityStore
                                            .saveDialStyle(context, styles[prevIdx].name)
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawDialByStyle(styles[prevIdx], time, accentColor)
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)))
                        }
                    }

                    // Next peek (right edge)
                    val nextIdx = pickerSelectedIdx + 1
                    if (nextIdx <= styles.lastIndex) {
                        Box(
                            modifier = Modifier
                                .size(peekSize)
                                .align(Alignment.CenterEnd)
                                .offset(x = (peekSize * 0.3f) + (if (pickerDragX < 0) (pickerDragX * 0.003f).dp else 0.dp))
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(0xFF111111))
                                .clickable {
                                    pickerSelectedIdx = nextIdx
                                    pickerDragX = 0f
                                    dialStyle = styles[nextIdx]
                                    isPickerActive = false
                                    scope.launch {
                                        com.rr.numio.clock.data.WorldCityStore
                                            .saveDialStyle(context, styles[nextIdx].name)
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawDialByStyle(styles[nextIdx], time, accentColor)
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)))
                        }
                    }

                    // Center dial — main
                    Box(
                        modifier = Modifier
                            .size(dialSize)
                            .offset(x = (pickerDragX * 0.004f).dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .border(2.dp, accentColor, androidx.compose.foundation.shape.CircleShape)
                            .pointerInput(pickerSelectedIdx) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitPointerEvent()
                                        val dc = down.changes.firstOrNull() ?: continue
                                        if (!dc.pressed) continue
                                        dc.consume()
                                        val startX = dc.position.x
                                        while (true) {
                                            val move = awaitPointerEvent()
                                            val mc = move.changes.firstOrNull() ?: break
                                            mc.consume()
                                            if (!mc.pressed) {
                                                val dx = pickerDragX
                                                when {
                                                    dx < -50f && pickerSelectedIdx < styles.lastIndex -> pickerSelectedIdx++
                                                    dx > 50f && pickerSelectedIdx > 0 -> pickerSelectedIdx--
                                                    abs(dx) < 10f -> {
                                                        // Tap — select and save
                                                        dialStyle = styles[pickerSelectedIdx]
                                                        isPickerActive = false
                                                        scope.launch {
                                                            com.rr.numio.clock.data.WorldCityStore
                                                                .saveDialStyle(context, styles[pickerSelectedIdx].name)
                                                        }
                                                    }
                                                }
                                                pickerDragX = 0f
                                                break
                                            }
                                            pickerDragX = mc.position.x - startX
                                        }
                                    }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawDialByStyle(styles[pickerSelectedIdx], time, accentColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    styles[pickerSelectedIdx].label.uppercase(),
                    fontSize = 14.sp, color = accentColor,
                    letterSpacing = 2.sp, fontWeight = FontWeight.W500
                )
                Text(
                    styles[pickerSelectedIdx].desc,
                    fontSize = 11.sp, color = NumioTextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dot indicators
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    styles.forEachIndexed { i, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (i == pickerSelectedIdx) 8.dp else 5.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (i == pickerSelectedIdx) accentColor
                                    else Color(0xFF333333)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "swipe · tap to select",
                    fontSize = 9.sp,
                    color = NumioTextMuted.copy(alpha = 0.35f),
                    letterSpacing = 1.sp
                )
            }
        }
    }

    // City picker dialog
    if (showCityPicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showCityPicker = false }) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF161616))
                    .padding(16.dp)
            ) {
                Text("Add city", fontSize = 16.sp, fontWeight = FontWeight.W500,
                    color = NumioTextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    val available = cityPickerList.filter { city -> selectedCities.none { it.name == city.name } }
                    itemsIndexed(available) { _, city ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { vm.addCity(city); showCityPicker = false }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(city.name, fontSize = 14.sp, color = NumioTextPrimary)
                            Text(city.offset, fontSize = 12.sp, color = NumioTextMuted)
                        }
                        Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                            drawRect(color = Color(0xFF1E1E1E))
                        }
                    }
                }
            }
        }
    }
}

// ── WorldClockRow ─────────────────────────────────────────────────────────────
@Composable
fun WorldClockRow(
    city: WorldCity, accentColor: Color,
    isMenuOpen: Boolean, isDragging: Boolean, dragOffsetY: Float,
    onLongPress: () -> Unit, onDelete: () -> Unit,
    onMoveUp: () -> Unit, onMoveDown: () -> Unit,
    onDragStart: () -> Unit, onDrag: (Float) -> Unit, onDragEnd: () -> Unit,
) {
    var time by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) { while (true) { time = Calendar.getInstance(); delay(60000) } }
    val utc = time.timeInMillis - time.timeZone.getOffset(time.timeInMillis)
    val there = Calendar.getInstance().apply { timeInMillis = utc + city.utcOffset * 3600000L }
    val h = there.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val m = there.get(Calendar.MINUTE).toString().padStart(2, '0')
    val ap = if (there.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

    Column(
        modifier = Modifier.fillMaxWidth()
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else 0f
                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                alpha = if (isDragging) 0.85f else 1f
            }
            .background(if (isDragging) Color(0xFF1A1A1A) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDrag = { change, amt -> change.consume(); onDrag(amt.y) },
                        onDragEnd = { onDragEnd() }, onDragCancel = { onDragEnd() }
                    )
                }
                .padding(horizontal = 24.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(city.name, fontSize = 13.sp, color = NumioTextPrimary)
                Text(city.offset, fontSize = 10.sp, color = NumioTextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isMenuOpen) {
                    Text("↑", fontSize = 14.sp, color = accentColor,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .clickable { onMoveUp() }.padding(horizontal = 10.dp, vertical = 4.dp))
                    Text("↓", fontSize = 14.sp, color = accentColor,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .clickable { onMoveDown() }.padding(horizontal = 10.dp, vertical = 4.dp))
                    Text("Delete", fontSize = 11.sp, color = NumioRed,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0x20E04040))
                            .clickable { onDelete() }.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Text("$h:$m $ap", fontSize = 18.sp, fontWeight = FontWeight.W300, color = NumioTextPrimary)
            }
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 24.dp)) {
            drawRect(color = Color(0xFF1E1E1E))
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────
private fun toRad(deg: Double) = (deg - 90) * Math.PI / 180.0

private fun DrawScope.drawHand(cx: Float, cy: Float, deg: Double, length: Float, width: Float, color: Color) {
    val a = toRad(deg)
    drawLine(color = color, start = Offset(cx, cy),
        end = Offset(cx + length * cos(a).toFloat(), cy + length * sin(a).toFloat()),
        strokeWidth = width, cap = StrokeCap.Round)
}

private fun DrawScope.drawSwordHand(cx: Float, cy: Float, deg: Double, length: Float, baseW: Float, tipW: Float, color: Color) {
    val a = toRad(deg)
    for (i in 0 until 20) {
        val t0 = i / 20f; val t1 = (i + 1) / 20f; val w = baseW * (1 - t0) + tipW * t0
        drawLine(color = color,
            start = Offset(cx + length * t0 * cos(a).toFloat(), cy + length * t0 * sin(a).toFloat()),
            end   = Offset(cx + length * t1 * cos(a).toFloat(), cy + length * t1 * sin(a).toFloat()),
            strokeWidth = w, cap = StrokeCap.Butt)
    }
}

private fun DrawScope.drawCommonHands(time: Calendar, accentColor: Color) {
    val cx = size.width / 2; val cy = size.height / 2; val radius = size.minDimension / 2f
    val hour = time.get(Calendar.HOUR); val min = time.get(Calendar.MINUTE)
    val sec = time.get(Calendar.SECOND); val ms = time.get(Calendar.MILLISECOND)
    val hDeg = (hour % 12) * 30.0 + min * 0.5
    val mDeg = min * 6.0 + sec * 0.1
    val sDeg = sec * 6.0 + ms * 0.006
    drawSwordHand(cx, cy, hDeg, radius * 0.48f, 10f, 3f, Color(0xFFF0F0F0))
    drawSwordHand(cx, cy, mDeg, radius * 0.68f, 7f,  2f, Color(0xFFDDDDDD))
    drawHand(cx, cy, sDeg, radius * 0.74f, 1.5f, accentColor)
    drawHand(cx, cy, sDeg + 180, radius * 0.25f, 1.5f, accentColor)
    val sA = toRad(sDeg)
    drawCircle(accentColor, 3.5f, Offset(cx + radius * 0.64f * cos(sA).toFloat(), cy + radius * 0.64f * sin(sA).toFloat()))
    drawCircle(Color(0xFF0A0A0A), 14f, Offset(cx, cy))
    drawCircle(Color(0xFF444444), 14f, Offset(cx, cy), style = Stroke(1.5f))
    drawCircle(accentColor, 10f, Offset(cx, cy), style = Stroke(2f))
    drawCircle(Color(0xFF0A0A0A), 7f, Offset(cx, cy))
    drawCircle(accentColor, 4f, Offset(cx, cy))
}

private fun nativePaint(block: android.graphics.Paint.() -> Unit) =
    android.graphics.Paint().apply { isAntiAlias = true; block() }

// ── Dial dispatcher helper ───────────────────────────────────────────────────
fun DrawScope.drawDialByStyle(style: DialStyle, time: Calendar, accentColor: Color) {
    when (style) {
        DialStyle.CLASSIC     -> drawClassicDial(time, accentColor)
        DialStyle.CLASSIC_PRE -> drawClassicPremiumDial(time, accentColor)
        DialStyle.SKELETON    -> drawSkeletonDial(time, accentColor)
    }
}

// ── 1. CLASSIC ───────────────────────────────────────────────────────────────
fun DrawScope.drawClassicDial(time: Calendar, accentColor: Color) {
    val cx = size.width / 2; val cy = size.height / 2; val r = size.minDimension / 2f
    drawCircle(Color(0xFF0A0A0A), r)
    drawCircle(Color(0xFF444444), r, style = Stroke(2.5f))
    drawCircle(Color(0xFF2A2A2A), r - 12f, style = Stroke(0.8f))
    for (i in 0 until 60) {
        val a = toRad(i * 6.0); val isH = i % 5 == 0; val isQ = i % 15 == 0
        val inner = when { isQ -> r - 28f; isH -> r - 22f; else -> r - 15f }
        drawLine(
            color = when { isQ -> Color(0xFF999999); isH -> Color(0xFF666666); else -> Color(0xFF333333) },
            start = Offset(cx + inner * cos(a).toFloat(), cy + inner * sin(a).toFloat()),
            end   = Offset(cx + (r - 4f) * cos(a).toFloat(), cy + (r - 4f) * sin(a).toFloat()),
            strokeWidth = when { isQ -> 3.5f; isH -> 2f; else -> 1f }, cap = StrokeCap.Round)
    }
    listOf(0, 90, 180, 270).forEach { deg ->
        val a = toRad(deg.toDouble()); val rd = r - 28f
        val x = cx + rd * cos(a).toFloat(); val y = cy + rd * sin(a).toFloat()
        drawCircle(accentColor.copy(alpha = 0.3f), 9f, Offset(x, y))
        drawCircle(accentColor, 5f, Offset(x, y))
    }
    val p = nativePaint { textAlign = android.graphics.Paint.Align.CENTER }
    listOf("12","1","2","3","4","5","6","7","8","9","10","11").forEachIndexed { i, label ->
        val a = toRad(i * 30.0); val isQ = i % 3 == 0; val rd = r - 50f
        p.color = if (isQ) 0xFF888888.toInt() else 0xFF444444.toInt()
        p.textSize = if (isQ) 34f else 26f
        drawContext.canvas.nativeCanvas.drawText(label, cx + rd * cos(a).toFloat(), cy + rd * sin(a).toFloat() + 12f, p)
    }
    drawCommonHands(time, accentColor)
}

// ── 2. CLASSIC PREMIUM ───────────────────────────────────────────────────────
fun DrawScope.drawClassicPremiumDial(time: Calendar, accentColor: Color) {
    val cx = size.width / 2; val cy = size.height / 2; val r = size.minDimension / 2f
    drawCircle(Color(0xFF080808), r)
    // Double ring border
    drawCircle(Color(0xFF333333), r, style = Stroke(3f))
    drawCircle(Color(0xFF1A1A1A), r - 8f, style = Stroke(0.5f))
    // Minute ticks
    for (i in 0 until 60) {
        val a = toRad(i * 6.0); val isH = i % 5 == 0; val isQ = i % 15 == 0
        val len = when { isQ -> 20f; isH -> 13f; else -> 7f }
        drawLine(
            color = when { isQ -> accentColor.copy(alpha = 0.9f); isH -> Color(0xFF666666); else -> Color(0xFF2A2A2A) },
            start = Offset(cx + (r - len - 3f) * cos(a).toFloat(), cy + (r - len - 3f) * sin(a).toFloat()),
            end   = Offset(cx + (r - 4f) * cos(a).toFloat(), cy + (r - 4f) * sin(a).toFloat()),
            strokeWidth = when { isQ -> 3f; isH -> 1.8f; else -> 0.8f }, cap = StrokeCap.Round)
    }
    // Quarter dots (amber filled)
    listOf(0, 90, 180, 270).forEach { deg ->
        val a = toRad(deg.toDouble()); val rd = r - 34f
        val x = cx + rd * cos(a).toFloat(); val y = cy + rd * sin(a).toFloat()
        drawCircle(accentColor.copy(alpha = 0.2f), 12f, Offset(x, y))
        drawCircle(accentColor, 6f, Offset(x, y))
    }
    // Large clean numbers at 12,3,6,9 only
    val p = nativePaint { textAlign = android.graphics.Paint.Align.CENTER; textSize = 40f; isFakeBoldText = true }
    listOf(Pair("12", 0), Pair("3", 90), Pair("6", 180), Pair("9", 270)).forEach { (label, deg) ->
        val a = toRad(deg.toDouble()); val rd = r - 56f
        p.color = 0xFFAAAAAA.toInt()
        drawContext.canvas.nativeCanvas.drawText(label, cx + rd * cos(a).toFloat(), cy + rd * sin(a).toFloat() + 14f, p)
    }
    // Sub-seconds arc (inner ring)
    val sec = time.get(Calendar.SECOND); val ms = time.get(Calendar.MILLISECOND)
    val sPct = (sec + ms / 1000f) / 60f
    drawArc(accentColor.copy(alpha = 0.08f), -90f, 360f, false,
        Offset(cx - (r - 22f), cy - (r - 22f)), Size((r - 22f) * 2, (r - 22f) * 2), style = Stroke(2f))
    drawArc(accentColor, -90f, sPct * 360f, false,
        Offset(cx - (r - 22f), cy - (r - 22f)), Size((r - 22f) * 2, (r - 22f) * 2), style = Stroke(2f, cap = StrokeCap.Round))
    drawCommonHands(time, accentColor)
}

// ── 3. SKELETON ──────────────────────────────────────────────────────────────
fun DrawScope.drawSkeletonDial(time: Calendar, accentColor: Color) {
    val cx = size.width / 2; val cy = size.height / 2; val r = size.minDimension / 2f
    drawCircle(Color(0xFF0D0D0D), r)
    drawCircle(Color(0xFF2A2A2A), r, style = Stroke(1f))
    for (i in 0 until 12) {
        val a = toRad(i * 30.0); val isQ = i % 3 == 0
        val inner = r - (if (isQ) 16f else 8f)
        drawLine(
            color = if (isQ) accentColor.copy(alpha = 0.8f) else Color(0xFF333333),
            start = Offset(cx + inner * cos(a).toFloat(), cy + inner * sin(a).toFloat()),
            end   = Offset(cx + (r - 2f) * cos(a).toFloat(), cy + (r - 2f) * sin(a).toFloat()),
            strokeWidth = if (isQ) 2.5f else 1.5f, cap = StrokeCap.Round)
    }
    drawCommonHands(time, accentColor)
}

// ── 4. INK TRAIL ─────────────────────────────────────────────────────────────
fun DrawScope.drawInkTrailDial(time: Calendar, accentColor: Color) {
    val cx = size.width / 2; val cy = size.height / 2; val r = size.minDimension / 2f
    drawCircle(Color(0xFF050505), r)
    val sec = time.get(Calendar.SECOND); val ms = time.get(Calendar.MILLISECOND)
    val sDeg = sec * 6.0 + ms * 0.006
    val mDeg = time.get(Calendar.MINUTE) * 6.0 + sec * 0.1
    val hDeg = (time.get(Calendar.HOUR) % 12) * 30.0 + time.get(Calendar.MINUTE) * 0.5
    if (sec != inkLastSec) {
        inkLastSec = sec
        inkTrailData.add(Triple(hDeg, mDeg, sDeg))
        if (inkTrailData.size > 25) inkTrailData.removeAt(0)
    }
    inkTrailData.forEachIndexed { i, tr ->
        val a = (i + 1f) / inkTrailData.size
        drawHand(cx, cy, tr.first,  r * 0.45f, 3f,   Color(android.graphics.Color.argb((a * 0.4f * 255).toInt(), 0xE0, 0xE0, 0xE0)))
        drawHand(cx, cy, tr.second, r * 0.65f, 2f,   Color(android.graphics.Color.argb((a * 0.5f * 255).toInt(), 0xCC, 0xCC, 0xCC)))
        drawHand(cx, cy, tr.third,  r * 0.72f, 1.5f, Color(android.graphics.Color.argb((a * 0.6f * 255).toInt(), 0xFF, 0xC1, 0x07)))
    }
    drawCommonHands(time, accentColor)
}

// ── 5. FLIP CARD ─────────────────────────────────────────────────────────────
fun DrawScope.drawFlipCardDial(time: Calendar, accentColor: Color) {
    val W = size.width; val H = size.height
    drawCircle(Color(0xFF080808), W / 2f)
    val curH = (time.get(Calendar.HOUR) % 12).let { if (it == 0) 12 else it }.toString().padStart(2, '0')
    val curM = time.get(Calendar.MINUTE).toString().padStart(2, '0')
    val now = System.currentTimeMillis()
    val dt = (now - flipLastMs).toFloat()
    flipLastMs = now
    if (curH != flipHCur) { flipHPrev = flipHCur; flipHCur = curH; flipHProg = 0f }
    if (curM != flipMCur) { flipMPrev = flipMCur; flipMCur = curM; flipMProg = 0f }
    flipHProg = (flipHProg + dt / 380f).coerceAtMost(1f)
    flipMProg = (flipMProg + dt / 380f).coerceAtMost(1f)

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val pad = 20f; val gap = 14f
        val cw = (W - pad * 2 - gap) / 2f; val ch = H * 0.55f
        val y = (H - ch) / 2f
        drawFlipCardCanvas(nc, pad, y, cw, ch, flipHPrev, flipHCur, flipHProg)
        drawFlipCardCanvas(nc, pad + cw + gap, y, cw, ch, flipMPrev, flipMCur, flipMProg)
        // Amber colon dots
        val dotX = W / 2f; val dotR = 7f
        val dotPaint = nativePaint { color = accentColor.toArgb() }
        nc.drawCircle(dotX, H / 2f - 20f, dotR, dotPaint)
        nc.drawCircle(dotX, H / 2f + 20f, dotR, dotPaint)
        // AM/PM
        val smallP = nativePaint { color = 0xFF555555.toInt(); textSize = 22f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.LEFT }
        nc.drawText(if (time.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM", pad + 6f, H - 12f, smallP)
        val secP = nativePaint { color = 0xFF444444.toInt(); textSize = 20f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.RIGHT }
        nc.drawText(":${time.get(Calendar.SECOND).toString().padStart(2,'0')}", W - pad - 6f, H - 12f, secP)
    }
}

private fun drawFlipCardCanvas(nc: android.graphics.Canvas, x: Float, y: Float, w: Float, h: Float, prev: String, cur: String, prog: Float) {
    val r = 10f
    val mid = y + h / 2f
    val fontSize = (h * 0.48f).coerceAtMost(w * 0.58f)

    // Shadow
    val shadowP = nativePaint { color = android.graphics.Color.argb(140,0,0,0) }
    nc.drawRoundRect(android.graphics.RectF(x+3f,y+5f,x+w+3f,y+h+5f), r, r, shadowP)

    // Bottom half background (static — shows current bottom)
    val botP = nativePaint { color = 0xFF141414.toInt() }
    nc.drawRoundRect(android.graphics.RectF(x, mid, x+w, y+h), r, r, botP)

    // Top half background (static — shows prev top)
    val topP = nativePaint { color = 0xFF1E1E1E.toInt() }
    nc.drawRoundRect(android.graphics.RectF(x, y, x+w, mid), r, r, topP)

    // Divider
    val divP = nativePaint { color = 0xFF000000.toInt() }
    nc.drawRect(x, mid-1.5f, x+w, mid+1.5f, divP)

    val textP = nativePaint {
        color = 0xFFE8E8E8.toInt()
        textSize = fontSize
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = android.graphics.Paint.Align.CENTER
    }
    // Text baseline: vertically centered in each half
    val topBaseline = mid - (fontSize * 0.08f)   // center of top half
    val botBaseline = y + h - (h/2f - fontSize * 0.42f) // center of bottom half

    // Static bottom: current number bottom half
    val s1 = nc.save()
    nc.clipRect(x, mid, x+w, y+h)
    nc.drawText(cur, x+w/2f, botBaseline, textP)
    nc.restoreToCount(s1)

    // Static top: previous number top half
    val s2 = nc.save()
    nc.clipRect(x, y, x+w, mid)
    nc.drawText(prev, x+w/2f, topBaseline, textP)
    nc.restoreToCount(s2)

    // Flipping panel: top half flips down revealing current top
    if (prog < 1f) {
        val angle = (prog * Math.PI).toFloat()
        val scaleY = cos(angle.toDouble()).toFloat()
        val s3 = nc.save()
        // Clip to top half only
        nc.clipRect(x, y, x+w, mid)
        // Scale around horizontal center line
        val matrix = android.graphics.Matrix()
        matrix.postScale(1f, abs(scaleY), x + w/2f, mid)
        nc.concat(matrix)
        // Panel background
        val panelP = nativePaint { color = if(scaleY >= 0) 0xFF242424.toInt() else 0xFF1A1A1A.toInt() }
        nc.drawRoundRect(android.graphics.RectF(x, y, x+w, mid), r, r, panelP)
        // Panel text: prev when going down (scaleY>0), cur when coming up (scaleY<0)
        val panelText = if (scaleY >= 0) prev else cur
        nc.drawText(panelText, x+w/2f, topBaseline, textP)
        // Gloss
        val glossP = nativePaint { color = android.graphics.Color.argb(8,255,255,255) }
        nc.drawRect(x, y, x+w, mid, glossP)
        nc.restoreToCount(s3)
    }
}

// ── 6. BOLD LCD ──────────────────────────────────────────────────────────────
fun DrawScope.drawBoldLCDDial(time: Calendar, accentColor: Color) {
    val W = size.width; val CX = W / 2f; val CY = size.height / 2f; val R = W / 2f
    drawCircle(Color(0xFF0A0A0A), R)
    // Brushed metal ring
    for (i in 0 until 360) {
        val a = Math.toRadians(i.toDouble())
        drawLine(
            color = if (i % 6 < 3) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
            start = Offset(CX + (R - 16f) * cos(a).toFloat(), CY + (R - 16f) * sin(a).toFloat()),
            end   = Offset(CX + (R - 2f)  * cos(a).toFloat(), CY + (R - 2f)  * sin(a).toFloat()),
            strokeWidth = 2f)
    }
    drawCircle(Color(0xFF111111), R - 18f)
    drawCircle(Color(0xFF0A0A0A), R - 18f, style = Stroke(2f))

    val hh = (time.get(Calendar.HOUR) % 12).let { if (it == 0) 12 else it }.toString().padStart(2, '0')
    val mm = time.get(Calendar.MINUTE).toString().padStart(2, '0')
    val ss = time.get(Calendar.SECOND).toString().padStart(2, '0')
    val ampm = if (time.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    val blink = time.get(Calendar.SECOND) % 2 == 0
    val d = time.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    val mo = (time.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val dayStr = listOf("SUN","MON","TUE","WED","THU","FRI","SAT")[time.get(Calendar.DAY_OF_WEEK) - 1]

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val dsegFont = android.graphics.Typeface.MONOSPACE

        // Ghost segments
        val ghostP = nativePaint { color = 0xFF1E1E1E.toInt(); textSize = R * 0.44f; typeface = dsegFont; textAlign = android.graphics.Paint.Align.CENTER }
        nc.drawText("88:88", CX, CY - R * 0.06f, ghostP)

        // Main time
        val mainP = nativePaint { color = 0xFFD4D4D4.toInt(); textSize = R * 0.44f; typeface = dsegFont; textAlign = android.graphics.Paint.Align.CENTER }
        nc.drawText("${hh}${if (blink) ":" else " "}${mm}", CX, CY - R * 0.06f, mainP)

        // Seconds
        val secP = nativePaint { color = 0xFF555555.toInt(); textSize = R * 0.15f; typeface = dsegFont; textAlign = android.graphics.Paint.Align.LEFT }
        nc.drawText(ss, CX + R * 0.42f, CY - R * 0.18f, secP)

        // AM/PM
        val ampP = nativePaint { color = 0xFF666666.toInt(); textSize = R * 0.11f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.LEFT }
        nc.drawText(ampm, CX - R * 0.62f, CY - R * 0.24f, ampP)

        // Date row
        val dateP = nativePaint { color = 0xFF666666.toInt(); textSize = R * 0.15f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.CENTER }
        nc.drawText("$d · $mo · $dayStr", CX, CY + R * 0.26f, dateP)

        // Numio logo
        val logoP = nativePaint { color = 0xFF252525.toInt(); textSize = R * 0.08f; typeface = android.graphics.Typeface.DEFAULT; textAlign = android.graphics.Paint.Align.CENTER }
        nc.drawText("N U M I O", CX, CY + R * 0.46f, logoP)
    }
    // Dividers
    drawRect(Color(0xFF252525), Offset(CX - R * 0.62f, CY + R * 0.09f), Size(R * 1.24f, 1f))
    drawRect(Color(0xFF1A1A1A), Offset(CX - R * 0.62f, CY + R * 0.34f), Size(R * 1.24f, 1f))
}

// ── 7. SMART FACE ────────────────────────────────────────────────────────────
fun DrawScope.drawSmartFaceDial(time: Calendar, accentColor: Color) {
    val W = size.width; val CX = W / 2f; val CY = size.height / 2f; val R = W / 2f

    drawCircle(Color(0xFF0C0C0C), R)

    // Subtle grid lines
    for (i in 0 until 16) {
        val a = Math.toRadians(i * 22.5)
        drawLine(Color(0xFF141414), Offset(CX + 45f * cos(a).toFloat(), CY + 45f * sin(a).toFloat()),
            Offset(CX + R * 0.88f * cos(a).toFloat(), CY + R * 0.88f * sin(a).toFloat()), 0.8f)
    }

    // Tick ring
    for (i in 0 until 60) {
        val a = toRad(i * 6.0); val isH = i % 5 == 0
        val r1 = R - (if (isH) 11f else 5f)
        drawLine(
            color = if (isH) Color(0xFF3A3A3A) else Color(0xFF1E1E1E),
            start = Offset(CX + r1 * cos(a).toFloat(), CY + r1 * sin(a).toFloat()),
            end   = Offset(CX + (R - 1f) * cos(a).toFloat(), CY + (R - 1f) * sin(a).toFloat()),
            strokeWidth = if (isH) 2f else 0.8f)
    }

    // Second arc
    val sp = (time.get(Calendar.SECOND) + time.get(Calendar.MILLISECOND) / 1000f) / 60f
    drawArc(accentColor.copy(alpha = 0.1f), -90f, 360f, false,
        Offset(CX - (R - 4f), CY - (R - 4f)), Size((R - 4f) * 2, (R - 4f) * 2), style = Stroke(4f))
    drawArc(accentColor, -90f, sp * 360f, false,
        Offset(CX - (R - 4f), CY - (R - 4f)), Size((R - 4f) * 2, (R - 4f) * 2), style = Stroke(4f, cap = StrokeCap.Round))

    // Mini analog top-right
    val mCX = CX + R * 0.44f; val mCY = CY - R * 0.38f; val mR = R * 0.22f
    drawCircle(Color(0xFF181818), mR, Offset(mCX, mCY))
    drawCircle(Color(0xFF2A2A2A), mR, Offset(mCX, mCY), style = Stroke(1.5f))
    for (i in 0 until 12) {
        val a = toRad(i * 30.0); val isQ = i % 3 == 0
        drawLine(
            color = if (isQ) accentColor.copy(alpha = 0.5f) else Color(0xFF2A2A2A),
            start = Offset(mCX + (mR - if (isQ) 8f else 4f) * cos(a).toFloat(), mCY + (mR - if (isQ) 8f else 4f) * sin(a).toFloat()),
            end   = Offset(mCX + (mR - 1f) * cos(a).toFloat(), mCY + (mR - 1f) * sin(a).toFloat()),
            strokeWidth = if (isQ) 1.5f else 0.8f)
    }
    val hDeg = (time.get(Calendar.HOUR) % 12) * 30.0 + time.get(Calendar.MINUTE) * 0.5
    val mDeg = time.get(Calendar.MINUTE) * 6.0 + time.get(Calendar.SECOND) * 0.1
    drawHand(mCX, mCY, hDeg, mR * 0.5f,  2.5f, Color(0xFFE0E0E0))
    drawHand(mCX, mCY, mDeg, mR * 0.72f, 1.5f, Color(0xFFBBBBBB))
    drawHand(mCX, mCY, time.get(Calendar.SECOND) * 6.0, mR * 0.82f, 1f, accentColor)
    drawCircle(accentColor, 3f, Offset(mCX, mCY))
    drawCircle(Color(0xFF0C0C0C), 1.5f, Offset(mCX, mCY))

    val hh  = time.get(Calendar.HOUR).toString().padStart(2, '0')
    val mm  = time.get(Calendar.MINUTE).toString().padStart(2, '0')
    val ss  = time.get(Calendar.SECOND).toString().padStart(2, '0')
    val mon = listOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")[time.get(Calendar.MONTH)]
    val dayLetters = listOf("Mo","Tu","We","Th","Fr","Sa","Su")
    val todayIdx = (time.get(Calendar.DAY_OF_WEEK) + 5) % 7

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas

        // Date top-left
        val dateP = nativePaint { color = 0xFF777777.toInt(); textSize = R * 0.13f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.LEFT }
        nc.drawText("$mon ${time.get(Calendar.DAY_OF_MONTH)}", CX - R * 0.82f, CY - R * 0.68f, dateP)

        // Day pills
        val pillY = CY - R * 0.49f
        dayLetters.forEachIndexed { i, dl ->
            val px = CX - R * 0.70f + i * R * 0.23f
            val isT = i == todayIdx
            if (isT) {
                val pillPaint = nativePaint { color = accentColor.toArgb() }
                nc.drawRoundRect(android.graphics.RectF(px - R * 0.085f, pillY - R * 0.085f, px + R * 0.085f, pillY + R * 0.085f), 5f, 5f, pillPaint)
            }
            val dpaint = nativePaint {
                color = if (isT) android.graphics.Color.BLACK else 0xFF444444.toInt()
                textSize = R * 0.09f; typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
            }
            nc.drawText(dl, px, pillY + R * 0.035f, dpaint)
        }

        // Big time
        val ghostP = nativePaint { color = 0xFF1A1A1A.toInt(); textSize = R * 0.43f; typeface = android.graphics.Typeface.MONOSPACE; textAlign = android.graphics.Paint.Align.CENTER }
        nc.drawText("88:88", CX - R * 0.06f, CY + R * 0.24f, ghostP)
        val timeP = nativePaint { color = 0xFFEEEEEE.toInt(); textSize = R * 0.43f; typeface = android.graphics.Typeface.MONOSPACE; textAlign = android.graphics.Paint.Align.CENTER }
        nc.drawText("$hh:$mm", CX - R * 0.06f, CY + R * 0.24f, timeP)

        // 24H badge
        val badgeP = nativePaint { color = 0xFF555555.toInt(); textSize = R * 0.09f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.LEFT }
        nc.drawText("24H", CX + R * 0.38f, CY + R * 0.08f, badgeP)
        val secP = nativePaint { color = 0xFF444444.toInt(); textSize = R * 0.14f; typeface = android.graphics.Typeface.MONOSPACE; textAlign = android.graphics.Paint.Align.LEFT }
        nc.drawText(ss, CX + R * 0.38f, CY + R * 0.26f, secP)

        // Bottom band
        val bandPaint = nativePaint { color = 0xFF141414.toInt() }
        nc.drawRoundRect(android.graphics.RectF(CX - R * 0.72f, CY + R * 0.5f, CX + R * 0.72f, CY + R * 0.8f), 8f, 8f, bandPaint)
        val logoP = nativePaint { color = 0xFF333333.toInt(); textSize = R * 0.08f; typeface = android.graphics.Typeface.DEFAULT; textAlign = android.graphics.Paint.Align.CENTER }
        nc.drawText("NUMIO CLOCK", CX, CY + R * 0.62f, logoP)
        val tzP = nativePaint { color = 0xFF555555.toInt(); textSize = R * 0.12f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.CENTER }
        val tzOff = -(java.util.TimeZone.getDefault().rawOffset / 3600000)
        nc.drawText("UTC ${if (tzOff >= 0) "+" else ""}$tzOff", CX, CY + R * 0.76f, tzP)
    }
}