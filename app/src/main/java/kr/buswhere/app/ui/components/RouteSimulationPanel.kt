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
import kr.buswhere.app.model.GeoPointDto
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

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
    val startMarker = remember { Marker(mapView) }
    val endMarker = remember { Marker(mapView) }

    LaunchedEffect(points) {
        routeLine.setPoints(points)
        startMarker.position = points.first()
        startMarker.setTitle(startLabel)
        endMarker.position = points.last()
        endMarker.setTitle(endLabel)
        busMarker.position = points.first()
        mapView.overlays.clear()
        mapView.overlays.add(routeLine)
        mapView.overlays.add(startMarker)
        mapView.overlays.add(endMarker)
        mapView.overlays.add(busMarker)
        mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 90)
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
