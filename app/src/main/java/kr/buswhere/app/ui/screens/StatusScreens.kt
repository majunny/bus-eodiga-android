package kr.buswhere.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.buswhere.app.model.RideRequest
import kr.buswhere.app.model.GeoPointDto
import kr.buswhere.app.model.Place
import kr.buswhere.app.model.VehicleAssignment
import kr.buswhere.app.ui.components.FullWidthButton
import kr.buswhere.app.ui.components.MovingBusRoutePanel
import kr.buswhere.app.ui.components.MapRouteStop
import kr.buswhere.app.ui.components.MapRouteStopType
import kr.buswhere.app.ui.components.StatusPanel

/** 호출 후 차량을 찾는 동안 표시하는 화면입니다. */
@Composable
fun MatchingScreen(
    isCancelling: Boolean,
    isRequestingAssignment: Boolean,
    realtimeMessage: String,
    matchedPassengerCount: Int,
    demoGroupSize: Int,
    onCancel: () -> Unit,
    onRequestDemoAssignment: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        StatusPanel(
            symbol = "▣",
            title = "버스를 찾고 있습니다",
            description = "여러 승객의 출발지와 목적지를 묶어 한 대의 차량 경로를 계산합니다.",
        )
        Text(
            "평균 배차 시간  약 5분",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            realtimeMessage,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        FullWidthButton(
            when {
                isRequestingAssignment -> "다인 DRT 대기열 참여 중…"
                matchedPassengerCount > 0 -> "다른 승객 대기 중 ($matchedPassengerCount/$demoGroupSize)"
                else -> "다인 DRT 대기열 다시 참여"
            },
            onRequestDemoAssignment,
            enabled = matchedPassengerCount == 0 && !isRequestingAssignment && !isCancelling,
        )
        FullWidthButton(
            text = if (isCancelling) "요청을 취소하고 있습니다…" else "요청 취소",
            onClick = onCancel,
            danger = true,
            enabled = !isCancelling,
        )
    }
}

/** 배정 차량 번호, 도착 시간 및 승차 위치를 안내합니다. */
@Composable
fun AssignedScreen(
    assignment: VehicleAssignment,
    request: RideRequest,
    vehicleStart: GeoPointDto,
    routeCoordinates: List<GeoPointDto>,
    sharedRouteStops: List<Place>,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("▣", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineLarge)
        Text("버스가 배정되었습니다", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium)
        Text("예정 시간에 맞춰 정류장에서 기다려주세요.", style = MaterialTheme.typography.bodyLarge)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("차량 번호", style = MaterialTheme.typography.bodyMedium)
            Text(assignment.plateNumber, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineLarge)
        }
        InfoCard("◷", "${assignment.etaMinutes}분 뒤 도착", "현재 남은 정류장: ${assignment.remainingStops}개")
        InfoCard("⌖", "내 승차 위치", request.pickup?.name ?: assignment.boardingGuide)
        MovingBusRoutePanel(
            title = if (sharedRouteStops.isNotEmpty()) "다인 공동 DRT 운행이 시작됐습니다" else "버스가 승차 정류장으로 출발했습니다",
            startLabel = "차고지",
            endLabel = sharedRouteStops.lastOrNull()?.name ?: request.pickup?.name.orEmpty(),
            startLocation = vehicleStart,
            endLocation = sharedRouteStops.lastOrNull()?.location ?: request.pickup?.location ?: vehicleStart,
            routeCoordinates = routeCoordinates,
            routeStops = sharedRouteStops.toMapRouteStops(),
            durationMillis = 12_000,
        )
    }
}

/** 탑승 후 목적지까지의 진행 정보를 표시합니다. */
@Composable
fun OnBoardScreen(
    request: RideRequest,
    routeCoordinates: List<GeoPointDto>,
    sharedRouteStops: List<Place>,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("▣  목적지로 이동 중입니다", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(24.dp),
        ) {
            Text("도착 목적지", style = MaterialTheme.typography.bodyLarge)
            Text(request.destination?.name.orEmpty(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text("현재 울산역 방면 주행 중", style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                .padding(22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("도착 예정 시간", color = MaterialTheme.colorScheme.onPrimary)
                Text("오후 2:45", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("12분 남음", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                Text("3.2km 남음", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        MovingBusRoutePanel(
            title = "목적지까지 운행 중입니다",
            startLabel = request.pickup?.name.orEmpty(),
            endLabel = request.destination?.name.orEmpty(),
            startLocation = request.pickup?.location ?: GeoPointDto(35.5514, 129.1387),
            endLocation = request.destination?.location ?: GeoPointDto(35.5202, 129.4284),
            routeCoordinates = routeCoordinates,
            routeStops = sharedRouteStops.toMapRouteStops(),
            durationMillis = 15_000,
        )
        FullWidthButton("긴급 도움 요청", {}, danger = true)
        Text(
            "사고나 긴급 상황 발생 시에만 눌러주세요.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun List<Place>.toMapRouteStops(): List<MapRouteStop> {
    if (isEmpty()) return emptyList()
    val pickupCount = size / 2
    return mapIndexed { index, place ->
        val isPickup = index < pickupCount
        MapRouteStop(
            label = place.name,
            location = place.location,
            type = if (isPickup) MapRouteStopType.PICKUP else MapRouteStopType.DROPOFF,
            order = if (isPickup) index + 1 else index - pickupCount + 1,
        )
    }
}

/** 운행 완료와 보호자 알림 전송 결과를 표시합니다. */
@Composable
fun CompletedScreen(onHome: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StatusPanel("✓", "목적지에 도착했습니다", "보호자에게 도착 알림을 전송했습니다.")
        Text("오늘 이동 서비스는 어떠셨나요?", style = MaterialTheme.typography.titleLarge)
        listOf("☹  별로예요", "●  보통이에요", "☺  좋아요").forEach { label ->
            FullWidthButton(label, {})
        }
        FullWidthButton("홈으로 이동", onHome)
    }
}

/** 네트워크·권한·차량 부족 등 공통 예외 화면입니다. */
@Composable
fun ProblemScreen(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    onHome: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        StatusPanel("!", title, description, danger = true)
        FullWidthButton(actionLabel, onAction)
        FullWidthButton("홈으로 돌아가기", onHome)
    }
}

@Composable
private fun InfoCard(symbol: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(symbol, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
