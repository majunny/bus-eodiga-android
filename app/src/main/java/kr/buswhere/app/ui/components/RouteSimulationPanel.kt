package kr.buswhere.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.roundToInt

private data class MapPoint(val x: Float, val y: Float)

private val demoRoute = listOf(
    MapPoint(0.10f, 0.77f),
    MapPoint(0.25f, 0.64f),
    MapPoint(0.39f, 0.67f),
    MapPoint(0.52f, 0.45f),
    MapPoint(0.70f, 0.40f),
    MapPoint(0.88f, 0.18f),
)

/** 네이버 지도 버스 현황처럼 도로 위 차량 위치를 보여주는 오프라인 시연 지도입니다. */
@Composable
fun MovingBusRoutePanel(
    title: String,
    startLabel: String,
    endLabel: String,
    durationMillis: Int = 12_000,
) {
    val progress = remember(title, startLabel, endLabel) { Animatable(0f) }
    val primary = MaterialTheme.colorScheme.primary

    LaunchedEffect(progress) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis, easing = LinearEasing))
    }

    val percent = (progress.value * 100).roundToInt()
    val remainingSeconds = ceil((durationMillis / 1000.0) * (1.0 - progress.value)).toInt()
    val busPoint = pointAlongRoute(progress.value)
    val nextStop = when {
        progress.value >= 1f -> "도착 완료"
        progress.value < 0.48f -> "다음 정류장 · 태화강역"
        else -> "다음 정류장 · $endLabel"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAF7), RoundedCornerShape(18.dp)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, color = primary, style = MaterialTheme.typography.titleLarge)
            Text(
                if (progress.value >= 1f) "버스가 정류장에 도착했습니다"
                else "$nextStop · ${remainingSeconds}초 후 도착",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .background(Color(0xFFEFF3EA)),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val roadColor = Color.White
                val roadEdge = Color(0xFFDDE3D8)
                val minorRoads = listOf(
                    Pair(Offset(0f, size.height * .23f), Offset(size.width, size.height * .48f)),
                    Pair(Offset(size.width * .15f, 0f), Offset(size.width * .34f, size.height)),
                    Pair(Offset(size.width * .62f, 0f), Offset(size.width * .72f, size.height)),
                    Pair(Offset(0f, size.height * .83f), Offset(size.width, size.height * .64f)),
                )
                minorRoads.forEach { road ->
                    drawLine(roadEdge, road.first, road.second, 20.dp.toPx(), StrokeCap.Round)
                    drawLine(roadColor, road.first, road.second, 14.dp.toPx(), StrokeCap.Round)
                }

                drawRect(Color(0xFFDCEBDD), Offset(size.width * .03f, size.height * .08f), androidx.compose.ui.geometry.Size(size.width * .24f, size.height * .18f))
                drawRect(Color(0xFFE5E7DF), Offset(size.width * .72f, size.height * .63f), androidx.compose.ui.geometry.Size(size.width * .23f, size.height * .22f))

                val routePath = Path().apply {
                    val first = demoRoute.first()
                    moveTo(first.x * size.width, first.y * size.height)
                    demoRoute.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
                }
                drawPath(routePath, Color.White, style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
                drawPath(routePath, primary, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))

                listOf(demoRoute.first(), demoRoute[3], demoRoute.last()).forEachIndexed { index, point ->
                    val center = Offset(point.x * size.width, point.y * size.height)
                    drawCircle(Color.White, 10.dp.toPx(), center)
                    drawCircle(if (index == 1) Color(0xFF667085) else primary, 6.dp.toPx(), center)
                }
            }

            MapLabel("태화강역", 0.42f, 0.49f)
            MapLabel(endLabel, 0.54f, 0.08f)
            MapLabel(startLabel, 0.03f, 0.82f)

            Box(
                modifier = Modifier
                    .offset(
                        x = (maxWidth - 46.dp) * busPoint.x,
                        y = (270.dp - 46.dp) * busPoint.y,
                    )
                    .size(46.dp)
                    .background(primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🚌", fontSize = 25.sp)
            }

            Text(
                "시연 지도",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color.White.copy(alpha = .92f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                color = Color(0xFF475467),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("운행 진행률", style = MaterialTheme.typography.bodyMedium)
                Text("$percent%", color = primary, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { progress.value }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(startLabel, style = MaterialTheme.typography.bodySmall)
                Text(endLabel, textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.MapLabel(text: String, x: Float, y: Float) {
    Text(
        text,
        modifier = Modifier
            .offset(x = maxWidth * x, y = 270.dp * y)
            .background(Color.White.copy(alpha = .92f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        color = Color(0xFF344054),
        fontSize = 10.sp,
        maxLines = 1,
    )
}

private fun pointAlongRoute(progress: Float): MapPoint {
    if (progress >= 1f) return demoRoute.last()
    val scaled = progress.coerceIn(0f, 1f) * (demoRoute.size - 1)
    val segment = scaled.toInt().coerceAtMost(demoRoute.size - 2)
    val local = scaled - segment
    val from = demoRoute[segment]
    val to = demoRoute[segment + 1]
    return MapPoint(
        x = from.x + (to.x - from.x) * local,
        y = from.y + (to.y - from.y) * local,
    )
}
