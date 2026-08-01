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
import kr.buswhere.app.model.VehicleAssignment
import kr.buswhere.app.ui.components.FullWidthButton
import kr.buswhere.app.ui.components.MovingBusRoutePanel
import kr.buswhere.app.ui.components.StatusPanel

/** 호출 후 차량을 찾는 동안 표시하는 화면입니다. */
@Composable
fun MatchingScreen(isCancelling: Boolean, onCancel: () -> Unit, onDemoAssigned: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        StatusPanel(
            symbol = "▣",
            title = "버스를 찾고 있습니다",
            description = "울산역 주변의 가장 빠른 BUS어디가 차량을 연결하고 있습니다.",
        )
        Text(
            "평균 배차 시간  약 5분",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        FullWidthButton("시연: 차량 배정 및 출발", onDemoAssigned)
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
fun AssignedScreen(assignment: VehicleAssignment, request: RideRequest) {
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
        InfoCard("⌖", "승차 위치", assignment.boardingGuide)
        MovingBusRoutePanel(
            title = "버스가 승차 정류장으로 출발했습니다",
            startLabel = "차고지",
            endLabel = request.pickup?.name.orEmpty(),
            durationMillis = 12_000,
        )
    }
}

/** 탑승 후 목적지까지의 진행 정보를 표시합니다. */
@Composable
fun OnBoardScreen(request: RideRequest) {
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
