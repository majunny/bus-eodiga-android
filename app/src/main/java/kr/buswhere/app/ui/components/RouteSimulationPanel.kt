package kr.buswhere.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.roundToInt

/** 지도 타일 없이 버스의 노선 이동을 애니메이션으로 보여주는 시연 패널입니다. */
@Composable
fun MovingBusRoutePanel(
    title: String,
    startLabel: String,
    endLabel: String,
    durationMillis: Int = 12_000,
) {
    val progress = remember(title, startLabel, endLabel) { Animatable(0f) }
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant

    LaunchedEffect(progress) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis, easing = LinearEasing))
    }

    val percent = (progress.value * 100).roundToInt()
    val remainingSeconds = ceil((durationMillis / 1000.0) * (1.0 - progress.value)).toInt()
    val status = when {
        progress.value >= 1f -> "도착 완료"
        progress.value < 0.08f -> "버스가 출발했습니다"
        else -> "버스가 이동 중입니다 · ${remainingSeconds}초 후 도착"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(primary.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, color = primary, style = MaterialTheme.typography.titleLarge)
        Text(status, style = MaterialTheme.typography.bodyLarge)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp),
            ) {
                val centerY = size.height / 2f
                val margin = 28.dp.toPx()
                drawLine(
                    color = track,
                    start = Offset(margin, centerY),
                    end = Offset(size.width - margin, centerY),
                    strokeWidth = 10.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = primary,
                    start = Offset(margin, centerY),
                    end = Offset(margin + (size.width - margin * 2) * progress.value, centerY),
                    strokeWidth = 10.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(primary, radius = 9.dp.toPx(), center = Offset(margin, centerY))
                drawCircle(primary, radius = 9.dp.toPx(), center = Offset(size.width - margin, centerY))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (maxWidth - 52.dp) * progress.value),
                contentAlignment = Alignment.Center,
            ) {
                Text("🚌", fontSize = 34.sp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(startLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(
                endLabel,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LinearProgressIndicator(
            progress = { progress.value },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("운행 진행률 $percent%", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
