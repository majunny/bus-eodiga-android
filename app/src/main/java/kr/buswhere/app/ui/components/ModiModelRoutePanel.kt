package kr.buswhere.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class ModiMapNode(
    val placeId: String,
    val number: Int,
    val label: String,
    val x: Float,
    val y: Float,
    val labelAbove: Boolean,
)

/** 친구의 차량 제어 좌표계에 맞춰 원본 모형 지도를 좌우 반전한 상대 배치입니다. */
private val modiMapNodes = listOf(
    ModiMapNode("31208", 0, "동부아파트", .18f, .80f, false),
    ModiMapNode("31205", 1, "수암시장", .42f, .80f, false),
    ModiMapNode("40404", 2, "공업탑", .83f, .21f, true),
    ModiMapNode("40411", 3, "달동현대아파트", .18f, .17f, true),
    // 실제 모형의 4번 표기를 사용하되 내부 정류장 ID는 기존 연동값을 유지합니다.
    ModiMapNode("40410", 4, "굿모닝병원", .44f, .21f, true),
    ModiMapNode("64201", 5, "롯데마트", .65f, .43f, false),
)

/** OSM 대신 물리 MODI 도로판과 같은 고정 배치에서 공동 운행을 표시합니다. */
@Composable
fun ModiModelRoutePanel(
    title: String,
    routeStops: List<MapRouteStop>,
    currentStopIndex: Int,
    tripPhase: String,
) {
    val nodeByPlaceId = remember { modiMapNodes.associateBy { it.placeId } }
    val busProgress = remember { Animatable(1f) }
    LaunchedEffect(currentStopIndex, tripPhase) {
        if (currentStopIndex >= 0 && tripPhase == "EN_ROUTE") {
            busProgress.snapTo(0f)
            busProgress.animateTo(1f, tween(durationMillis = 2_500))
        } else {
            busProgress.snapTo(1f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            Text("MODI+ 차량 제어 방향과 동일한 0~5번 주행 지도", style = MaterialTheme.typography.bodyMedium)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(410.dp)
                .padding(horizontal = 10.dp),
        ) {
            val left = 22.dp.toPx()
            val top = 20.dp.toPx()
            val width = size.width - left * 2
            val height = size.height - top * 2
            fun point(node: ModiMapNode) = Offset(left + node.x * width, top + node.y * height)

            drawRoundRect(
                color = Color(0xFF4B6077),
                topLeft = Offset(2f, 2f),
                size = androidx.compose.ui.geometry.Size(size.width - 4f, size.height - 4f),
                cornerRadius = CornerRadius(22.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx()),
            )

            // 모형판은 모든 정류장 조합을 직선 양방향으로 연결합니다.
            modiMapNodes.forEachIndexed { firstIndex, first ->
                modiMapNodes.drop(firstIndex + 1).forEach { second ->
                    drawLine(
                        color = Color(0xFFB5BFCC),
                        start = point(first),
                        end = point(second),
                        strokeWidth = 3.dp.toPx(),
                    )
                }
            }

            val routeNodes = buildList {
                add(nodeByPlaceId.getValue("31208"))
                routeStops.mapNotNullTo(this) { nodeByPlaceId[it.placeId] }
            }
            routeNodes.zipWithNext().forEach { (from, to) ->
                drawLine(
                    color = Color(0xFF0872CE),
                    start = point(from),
                    end = point(to),
                    strokeWidth = 5.dp.toPx(),
                )
            }

            val completedPlaceIds = routeStops.take((currentStopIndex + 1).coerceAtLeast(0)).map { it.placeId }.toSet()
            val activePlaceId = routeStops.getOrNull(currentStopIndex)?.placeId
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(20, 43, 69)
                textAlign = Paint.Align.CENTER
                textSize = 13.dp.toPx()
                typeface = Typeface.DEFAULT_BOLD
            }
            val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(20, 43, 69)
                textAlign = Paint.Align.CENTER
                textSize = 18.dp.toPx()
                typeface = Typeface.DEFAULT_BOLD
            }

            modiMapNodes.forEach { node ->
                val center = point(node)
                val radius = 22.dp.toPx()
                val fill = when {
                    node.placeId == activePlaceId -> Color(0xFF66D19E)
                    node.placeId in completedPlaceIds -> Color(0xFFD5DBE3)
                    else -> Color(0xFFFFBE24)
                }
                drawCircle(Color(0xFF102E53), radius = radius + 3.dp.toPx(), center = center)
                drawCircle(fill, radius = radius, center = center)
                drawContext.canvas.nativeCanvas.drawText(
                    node.number.toString(),
                    center.x,
                    center.y + 6.dp.toPx(),
                    numberPaint,
                )

                val labelWidth = labelPaint.measureText(node.label) + 20.dp.toPx()
                val labelCenterY = center.y + if (node.labelAbove) -42.dp.toPx() else 43.dp.toPx()
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(center.x - labelWidth / 2, labelCenterY - 15.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(labelWidth, 30.dp.toPx()),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
                drawRoundRect(
                    color = Color(0xFF4B6077),
                    topLeft = Offset(center.x - labelWidth / 2, labelCenterY - 15.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(labelWidth, 30.dp.toPx()),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx()),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    node.label,
                    center.x,
                    labelCenterY + 5.dp.toPx(),
                    labelPaint,
                )
            }

            val depot = nodeByPlaceId.getValue("31208")
            val targetIndex = currentStopIndex.coerceIn(0, (routeStops.size - 1).coerceAtLeast(0))
            val target = routeStops.getOrNull(targetIndex)?.let { nodeByPlaceId[it.placeId] } ?: depot
            val previous = if (targetIndex > 0) {
                routeStops.getOrNull(targetIndex - 1)?.let { nodeByPlaceId[it.placeId] } ?: depot
            } else {
                depot
            }
            val fromPoint = point(previous)
            val toPoint = point(target)
            val progress = if (currentStopIndex < 0) 0f else busProgress.value
            val busPoint = Offset(
                fromPoint.x + (toPoint.x - fromPoint.x) * progress,
                fromPoint.y + (toPoint.y - fromPoint.y) * progress,
            )
            drawCircle(Color.White, radius = 16.dp.toPx(), center = busPoint)
            drawCircle(Color(0xFF0872CE), radius = 14.dp.toPx(), center = busPoint)
            val busPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = 12.dp.toPx()
                typeface = Typeface.DEFAULT_BOLD
            }
            drawContext.canvas.nativeCanvas.drawText("BUS", busPoint.x, busPoint.y + 4.dp.toPx(), busPaint)
        }
        Text(
            "회색 선: 양방향 직통로  ·  파란 선: 현재 공동 배차 경로  ·  0번: 동부아파트 차고지",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            color = Color(0xFF5E6D7E),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}
