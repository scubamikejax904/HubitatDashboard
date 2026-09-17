package com.tim.hubitatdash.ui.gpsmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tim.hubitatdash.data.model.GpsDataPoint
import com.tim.hubitatdash.viewmodel.GpsMapLayer
import com.tim.hubitatdash.viewmodel.GpsMapViewModel
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsMapScreen(
    onNavigateBack: () -> Unit,
    viewModel: GpsMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isMapFullscreen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.refreshIntervalSeconds, uiState.startDate, uiState.endDate) {
        if (uiState.refreshIntervalSeconds <= 0) return@LaunchedEffect
        while (true) {
            delay(uiState.refreshIntervalSeconds * 1000L)
            viewModel.fetchData()
        }
    }

    Scaffold(
        topBar = {
            if (!isMapFullscreen) {
            TopAppBar(
                title = { Text("GPS Map") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
            }
        }
    ) { paddingValues ->
        if (!uiState.configured) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("GPS map is not configured", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Set a GPS Map CSV URL in GPS Tracker settings.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isMapFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .zIndex(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField(
                        value = uiState.startDate,
                        onDateChange = viewModel::setStartDate,
                        label = "Start",
                        modifier = Modifier.weight(1f)
                    )
                    DateField(
                        value = uiState.endDate,
                        onDateChange = viewModel::setEndDate,
                        label = "End",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.fetchData() }, modifier = Modifier.weight(1f)) {
                        Text("Filter")
                    }
                    Button(onClick = { viewModel.fetchData() }, modifier = Modifier.weight(1f)) {
                        Text("Refresh")
                    }
                    Button(onClick = { viewModel.clearDates() }, modifier = Modifier.weight(1f)) {
                        Text("Clear")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LayerSelector(
                        layer = uiState.mapLayer,
                        onLayerChange = viewModel::setMapLayer,
                        modifier = Modifier.weight(1f)
                    )
                    AutoRefreshSelector(
                        selected = uiState.refreshIntervalSeconds,
                        onChange = viewModel::setRefreshInterval,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    buildStatusLine(uiState.data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                uiState.error?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF5C1A1A))
                            .padding(10.dp)
                    ) {
                        Text(msg, color = Color(0xFFFFD6D6), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .zIndex(0f)
            ) {
                GpsOsmMap(
                    data = uiState.data,
                    layer = uiState.mapLayer,
                    isFullscreen = isMapFullscreen,
                    onExitFullscreen = { isMapFullscreen = false },
                    modifier = Modifier.fillMaxSize(),
                    userAgent = context.packageName
                )
                if (uiState.loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                IconButton(
                    onClick = { isMapFullscreen = !isMapFullscreen },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .zIndex(2f)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMapFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = if (isMapFullscreen) "Exit fullscreen" else "Fullscreen"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    value: String,
    onDateChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val displayText = remember(value) {
        if (value.isBlank()) "" else value
    }
    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Pick $label date")
            }
        },
        modifier = modifier
    )
    if (showPicker) {
        val initial = runCatching { java.time.LocalDate.parse(value) }.getOrNull()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial?.atStartOfDay(java.time.ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        onDateChange(date.toString())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayerSelector(
    layer: GpsMapLayer,
    onLayerChange: (GpsMapLayer) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = layer.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Map layer") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GpsMapLayer.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onLayerChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoRefreshSelector(
    selected: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = if (selected == 0) "Auto: Off" else "Auto: ${selected}s",
            onValueChange = {},
            readOnly = true,
            label = { Text("Refresh") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { sec ->
                DropdownMenuItem(
                    text = { Text(if (sec == 0) "Auto: Off" else "${sec}s") },
                    onClick = {
                        onChange(sec)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GpsOsmMap(
    data: List<GpsDataPoint>,
    layer: GpsMapLayer,
    userAgent: String,
    isFullscreen: Boolean,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val updateKeyState = remember { mutableStateOf<String?>(null) }
    val fullscreenState = rememberUpdatedState(isFullscreen)
    val exitFullscreenState = rememberUpdatedState(onExitFullscreen)
    val mapView = remember {
        Configuration.getInstance().userAgentValue = userAgent
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(4.0)
            controller.setCenter(GeoPoint(39.8283, -98.5795))
            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean = false
                override fun onZoom(event: ZoomEvent?): Boolean = false
            })
            val tapDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (fullscreenState.value) {
                        exitFullscreenState.value.invoke()
                        return true
                    }
                    return false
                }
            })
            setOnTouchListener { _, event ->
                tapDetector.onTouchEvent(event)
                false
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = updateLoop@{ map ->
            val updateKey = "${layer.name}|${data.hashCode()}"
            if (updateKeyState.value == updateKey) return@updateLoop
            updateKeyState.value = updateKey
            map.setTileSource(
                when (layer) {
                    GpsMapLayer.STREET -> TileSourceFactory.MAPNIK
                    GpsMapLayer.SATELLITE -> googleTileSource("s")
                    GpsMapLayer.HYBRID -> googleTileSource("y")
                }
            )

            val sortedPoints = data.sortedBy { parseInstantOrNull(it.timestamp)?.toEpochMilli() ?: Long.MAX_VALUE }
            val latestPerDevice = sortedPoints.groupBy { it.device.orEmpty() }
                .mapValues { (_, points) -> points.maxByOrNull { parseInstantOrNull(it.timestamp)?.toEpochMilli() ?: Long.MIN_VALUE }?.timestamp }
            val deviceColors = buildDeviceColors(sortedPoints)

            map.overlays.clear()
            val geoPoints = mutableListOf<GeoPoint>()

            sortedPoints.forEach { point ->
                val device = point.device.orEmpty()
                val colors = deviceColors[device] ?: ColorPair(trail = 0xFF3B82F6.toInt(), current = 0xFFEF4444.toInt())
                val isCurrent = latestPerDevice[device] == point.timestamp
                val fillColor = if (isCurrent) colors.current else colors.trail
                val radius = if (isCurrent) 16 else 11

                val marker = Marker(map).apply {
                    position = GeoPoint(point.lat, point.long)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = BitmapDrawable(
                        context.resources,
                        makeMarkerBitmap(fillColor, radius)
                    )
                    title = buildString {
                        append(point.device ?: "Unknown")
                        if (isCurrent) append(" (current)")
                    }
                    snippet = "${formatPointTimestamp(point.timestamp)}\n${"%.6f".format(point.lat)}, ${"%.6f".format(point.long)}"
                }
                map.overlays.add(marker)
                geoPoints.add(marker.position)
            }

            if (geoPoints.size > 1) {
                val bounds = BoundingBox.fromGeoPointsSafe(geoPoints)
                map.zoomToBoundingBox(bounds, false, 80)
            } else if (geoPoints.size == 1) {
                map.controller.setCenter(geoPoints.first())
                map.controller.setZoom(14.0)
            }

            map.invalidate()
        }
    )
}

private data class ColorPair(val trail: Int, val current: Int)

private fun buildDeviceColors(points: List<GpsDataPoint>): Map<String, ColorPair> {
    val devices = points.map { it.device.orEmpty() }.distinct().sorted()
    val colorPairs = listOf(
        ColorPair(trail = 0xFF3B82F6.toInt(), current = 0xFFEF4444.toInt()),
        ColorPair(trail = 0xFF22C55E.toInt(), current = 0xFFFACC15.toInt())
    )
    return devices.mapIndexed { index, device ->
        device to colorPairs[index % colorPairs.size]
    }.toMap()
}

private fun makeMarkerBitmap(fillColor: Int, radius: Int): Bitmap {
    val size = radius * 2 + 8
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }

    canvas.drawCircle(center, center, radius + 2f, strokePaint)
    canvas.drawCircle(center, center, radius.toFloat(), fillPaint)
    return bitmap
}

private fun buildStatusLine(points: List<GpsDataPoint>): String {
    if (points.isEmpty()) return "0 data points"
    val start = points.first().timestamp
    val end = points.last().timestamp
    return "${points.size} data point${if (points.size == 1) "" else "s"} from ${formatPointTimestamp(start)} to ${formatPointTimestamp(end)}"
}

private fun parseInstantOrNull(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()

private fun formatPointTimestamp(value: String): String {
    val instant = parseInstantOrNull(value) ?: return value
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}

private fun googleTileSource(layerCode: String): OnlineTileSourceBase {
    return object : OnlineTileSourceBase(
        "Google-$layerCode",
        0,
        20,
        256,
        ".png",
        arrayOf("https://mt1.google.com/vt/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            val z = MapTileIndex.getZoom(pMapTileIndex)
            return "${getBaseUrl()}lyrs=$layerCode&x=$x&y=$y&z=$z"
        }
    }
}
