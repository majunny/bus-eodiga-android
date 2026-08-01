package kr.buswhere.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kr.buswhere.app.model.GeoPointDto
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

data class MapRouteStop(
    val label: String,
    val location: GeoPointDto,
    val type: MapRouteStopType,
    val order: Int,
    val isActive: Boolean = false,
    val isCompleted: Boolean = false,
)

enum class MapRouteStopType { PICKUP, DROPOFF }

/** 실제 OpenStreetMap 위에서 도로 경로와 움직이는 버스 위치를 표시합니다. */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun MovingBusRoutePanel(
    title: String,
    startLabel: String,
    endLabel: String,
    startLocation: GeoPointDto,
    endLocation: GeoPointDto,
    routeCoordinates: List<GeoPointDto> = emptyList(),
    routeStops: List<MapRouteStop> = emptyList(),
    durationMillis: Int = 12_000,
) {
    val context = LocalContext.current
    val points = remember(startLocation, endLocation, routeCoordinates) {
        routeCoordinates.ifEmpty { listOf(startLocation, endLocation) }.map {
            GeoPoint(it.latitude, it.longitude)
        }
    }
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 5.0
            maxZoomLevel = 19.0
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN,
                    MotionEvent.ACTION_MOVE,
                    -> view.parent?.requestDisallowInterceptTouchEvent(true)

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                    -> view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }
    val routeLine = remember { Polyline(mapView).apply { outlinePaint.color = Color.rgb(7, 103, 200); outlinePaint.strokeWidth = 14f } }
    val busMarker = remember {
        Marker(mapView).apply {
            icon = createBusMarker(context.resources.displayMetrics.density)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setTitle("BUS어디가 시연 차량")
        }
    }

    LaunchedEffect(points, routeStops) {
        val markerPositions = routeStops.mapIndexed { index, _ -> spreadMarkerPosition(routeStops, index) }
        routeLine.setPoints(points)
        busMarker.position = points.first()
        mapView.overlays.clear()
        mapView.overlays.add(routeLine)
        if (routeStops.isEmpty()) {
            mapView.overlays.add(createStopMarker(mapView, points.first(), "S", startLabel, Color.rgb(7, 103, 200)))
            mapView.overlays.add(createStopMarker(mapView, points.last(), "D", endLabel, Color.rgb(239, 108, 0)))
        } else {
            routeStops.forEachIndexed { index, stop ->
                val prefix = if (stop.type == MapRouteStopType.PICKUP) "P" else "D"
                val color = when {
                    stop.isActive -> Color.rgb(0, 150, 90)
                    stop.isCompleted -> Color.rgb(102, 112, 133)
                    stop.type == MapRouteStopType.PICKUP -> Color.rgb(7, 103, 200)
                    else -> Color.rgb(239, 108, 0)
                }
                val typeLabel = if (stop.type == MapRouteStopType.PICKUP) "승차" else "하차"
                mapView.overlays.add(
                    createStopMarker(
                        mapView = mapView,
                        position = markerPositions[index],
                        symbol = "$prefix${stop.order}",
                        title = "$typeLabel ${stop.order} · ${stop.label}",
                        color = color,
                    ),
                )
            }
        }
        mapView.overlays.add(busMarker)
        val visiblePoints = points + markerPositions
        mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(visiblePoints), true, 110)
        mapView.invalidate()

        val frames = (durationMillis / 50).coerceAtLeast(1)
        repeat(frames + 1) { frame ->
            val progress = frame.toFloat() / frames
            busMarker.position = pointAlongRoute(points, progress)
            mapView.invalidate()
            if (frame < frames) delay(50)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeColor.White, RoundedCornerShape(18.dp)),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            Text("실제 OSM 도로 지도 · 버스 위치 시뮬레이션", style = MaterialTheme.typography.bodyMedium)
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp),
            factory = { mapView },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(startLabel, style = MaterialTheme.typography.bodySmall)
            Text("→  $endLabel", style = MaterialTheme.typography.bodySmall)
        }
        if (routeStops.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("공동 DRT 운행 순서", style = MaterialTheme.typography.titleMedium)
                routeStops.forEach { stop ->
                    val type = if (stop.type == MapRouteStopType.PICKUP) "승차" else "하차"
                    val color = when {
                        stop.isActive -> ComposeColor(0xFF00965A)
                        stop.isCompleted -> ComposeColor(0xFF667085)
                        stop.type == MapRouteStopType.PICKUP -> MaterialTheme.colorScheme.primary
                        else -> ComposeColor(0xFFEF6C00)
                    }
                    val state = when {
                        stop.isActive -> " · 현재"
                        stop.isCompleted -> " · 완료"
                        else -> ""
                    }
                    Text(
                        "${if (stop.type == MapRouteStopType.PICKUP) "P" else "D"}${stop.order}  $type · ${stop.label}$state",
                        color = color,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun pointAlongRoute(points: List<GeoPoint>, progress: Float): GeoPoint {
    if (points.size == 1 || progress >= 1f) return points.last()
    val scaled = progress.coerceIn(0f, 1f) * (points.size - 1)
    val segment = scaled.toInt().coerceAtMost(points.size - 2)
    val local = scaled - segment
    val from = points[segment]
    val to = points[segment + 1]
    return GeoPoint(
        from.latitude + (to.latitude - from.latitude) * local,
        from.longitude + (to.longitude - from.longitude) * local,
    )
}

private fun createBusMarker(density: Float): BitmapDrawable {
    val size = (52 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.rgb(7, 103, 200)
    canvas.drawCircle(size / 2f, size / 2f, size * .46f, paint)
    paint.color = Color.WHITE
    paint.textSize = size * .48f
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("B", size / 2f, size * .67f, paint)
    return BitmapDrawable(null, bitmap)
}

private fun createStopMarker(
    mapView: MapView,
    position: GeoPoint,
    symbol: String,
    title: String,
    color: Int,
): Marker {
    val density = mapView.resources.displayMetrics.density
    val size = (42 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.WHITE
    }
    canvas.drawCircle(size / 2f, size / 2f, size * .49f, paint)
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size * .42f, paint)
    paint.color = Color.WHITE
    paint.textSize = size * .34f
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText(symbol, size / 2f, size * .63f, paint)
    return Marker(mapView).apply {
        this.position = position
        icon = BitmapDrawable(mapView.resources, bitmap)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        setTitle(title)
    }
}

/** 같은 정류장을 선택한 여러 승객의 P/D 마커가 서로 가려지지 않게 펼칩니다. */
private fun spreadMarkerPosition(stops: List<MapRouteStop>, index: Int): GeoPoint {
    val current = stops[index]
    val overlapping = stops.indices.filter { candidateIndex ->
        val candidate = stops[candidateIndex]
        abs(candidate.location.latitude - current.location.latitude) < 0.00005 &&
            abs(candidate.location.longitude - current.location.longitude) < 0.00005
    }
    if (overlapping.size == 1) {
        return GeoPoint(current.location.latitude, current.location.longitude)
    }
    val position = overlapping.indexOf(index)
    val angle = (2.0 * PI * position / overlapping.size) - PI / 2.0
    val radius = 0.00016
    val longitudeScale = cos(current.location.latitude * PI / 180.0).coerceAtLeast(0.2)
    return GeoPoint(
        current.location.latitude + radius * sin(angle),
        current.location.longitude + radius * cos(angle) / longitudeScale,
    )
}
