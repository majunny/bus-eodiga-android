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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.buswhere.app.model.DemoPlaces
import kr.buswhere.app.model.MobilitySupport
import kr.buswhere.app.model.Place
import kr.buswhere.app.model.RideRequest
import kr.buswhere.app.ui.components.ChoiceCard
import kr.buswhere.app.ui.components.FullWidthButton

/** 앱 첫 화면으로 자주 가는 장소와 새 호출 진입점을 제공합니다. */
@Composable
fun HomeScreen(
    onStartBooking: () -> Unit,
    onQuickDestination: (Place) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("어디로 가시나요?", style = MaterialTheme.typography.headlineMedium)
        Text(
            "안전하고 편안하게 모시겠습니다.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(4.dp))
        Text("★  자주 가는 장소", style = MaterialTheme.typography.titleLarge)
        DemoPlaces.destinations.take(3).forEachIndexed { index, place ->
            ChoiceCard(
                title = place.name,
                subtitle = place.address,
                symbol = listOf("＋", "♿", "▣")[index],
                onClick = { onQuickDestination(place) },
            )
        }
        FullWidthButton("새로운 목적지 선택", onStartBooking)
    }
}

/** GPS·최근 정류장·지도 선택 중 탑승 위치 결정 방식을 선택합니다. */
@Composable
fun PickupScreen(
    selected: Place?,
    gpsMessage: String?,
    isDemoMode: Boolean,
    isModiMode: Boolean,
    onUseGps: () -> Unit,
    onUseDemoMode: () -> Unit,
    onUseModiMode: () -> Unit,
    onSelectDemoStop: (Place) -> Unit,
    onOpenRecent: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("어디서 타시나요?", style = MaterialTheme.typography.headlineMedium)
        ChoiceCard(
            title = "현재 위치 주변 정류장",
            subtitle = gpsMessage ?: "GPS로 가장 가까운 실제 정류장을 찾습니다",
            symbol = "◎",
            selected = selected?.category == "BUS_STOP" && gpsMessage != null && !isDemoMode && !isModiMode,
            onClick = onUseGps,
        )
        ChoiceCard(
            title = "울산 시연 모드",
            subtitle = "실제 정류장 3곳에서 서로 다른 출발지를 선택합니다",
            symbol = "▶",
            selected = isDemoMode && !isModiMode,
            onClick = onUseDemoMode,
        )
        ChoiceCard(
            title = "MODI 모형 연동",
            subtitle = "6개 정류장 선택을 Render를 통해 실제 모형 버스로 전송합니다",
            symbol = "▣",
            selected = isModiMode,
            onClick = onUseModiMode,
        )
        if (isDemoMode || isModiMode) {
            Text(
                if (isModiMode) "모형 승차 정류장 선택" else "시연 출발 정류장 선택",
                style = MaterialTheme.typography.titleLarge,
            )
            val selectableStops = if (isModiMode) DemoPlaces.modiModelStops else DemoPlaces.demoPickupStops
            selectableStops.forEachIndexed { index, stop ->
                ChoiceCard(
                    title = stop.name,
                    subtitle = stop.address,
                    symbol = "${index + 1}",
                    selected = selected?.id == stop.id,
                    onClick = { onSelectDemoStop(stop) },
                )
            }
        }
        ChoiceCard(
            title = "정류장 이름 검색",
            subtitle = "울산 정류장 3,616개에서 검색합니다",
            symbol = "▣",
            selected = selected?.category == "BUS_STOP" && gpsMessage == null && !isDemoMode && !isModiMode,
            onClick = onOpenRecent,
        )
        selected?.let {
            Text(
                "현재 선택: ${it.name}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/** 교통약자 지원 유형과 동반 인원을 선택합니다. */
@Composable
fun AssistanceScreen(
    selected: MobilitySupport,
    companionCount: Int,
    onSupportChange: (MobilitySupport) -> Unit,
    onCompanionChange: (Int) -> Unit,
) {
    val choices = listOf(
        Triple(MobilitySupport.STANDARD, "일반 탑승", "별도 지원이 필요하지 않습니다"),
        Triple(MobilitySupport.SENIOR, "고령자 탑승 지원", "천천히 탑승할 수 있게 기다립니다"),
        Triple(MobilitySupport.WHEELCHAIR, "휠체어 이용", "슬로프와 휠체어석이 필요합니다"),
        Triple(MobilitySupport.VISUAL, "시각 안내 필요", "음성과 직접 안내를 지원합니다"),
        Triple(MobilitySupport.HEARING, "청각 안내 필요", "문자 알림과 시각 표시를 제공합니다"),
    )
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("도움이 필요하신가요?", style = MaterialTheme.typography.headlineMedium)
        Text("탑승 상황에 맞는 지원 서비스를 선택해 주세요.", style = MaterialTheme.typography.bodyMedium)
        choices.forEach { (type, title, subtitle) ->
            ChoiceCard(
                title = title,
                subtitle = subtitle,
                symbol = if (type == MobilitySupport.WHEELCHAIR) "♿" else "●",
                selected = selected == type,
                onClick = { onSupportChange(type) },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(18.dp),
        ) {
            Text("동반 인원 선택", style = MaterialTheme.typography.titleLarge)
            Text("본인을 제외한 추가 인원입니다.", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { onCompanionChange((companionCount - 1).coerceAtLeast(0)) }) { Text("−") }
                Text("${companionCount}명", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { onCompanionChange((companionCount + 1).coerceAtMost(5)) }) { Text("＋") }
            }
        }
    }
}

/** 병원·복지관·시장 등 목적지를 선택합니다. */
@Composable
fun DestinationScreen(
    selected: Place?,
    pickup: Place?,
    isModiMode: Boolean,
    query: String,
    searchResults: List<Place>,
    isLoading: Boolean,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onSelect: (Place) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("도착지를 선택하세요", style = MaterialTheme.typography.headlineMedium)
        if (isModiMode) {
            Text(
                "모형의 6개 정류장 중 승차 지점을 제외한 목적지를 선택해 주세요.",
                style = MaterialTheme.typography.bodyLarge,
            )
            DemoPlaces.modiModelStops.filter { it.id != pickup?.id }.forEachIndexed { index, place ->
                ChoiceCard(
                    title = place.name,
                    subtitle = "정류장 ID ${place.id} · 모형 경로 ${index + 1}",
                    symbol = "${index + 1}",
                    selected = selected?.id == place.id,
                    onClick = { onSelect(place) },
                )
            }
            return@Column
        }
        Text("울산의 장소를 이름으로 검색하거나 추천 목적지를 눌러주세요.", style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("도착지 검색") },
            placeholder = { Text("예: 울산대학교, 울산대공원") },
            singleLine = true,
        )
        when {
            isLoading -> Text("장소를 검색하고 있습니다…", color = MaterialTheme.colorScheme.primary)
            errorMessage != null -> Text(errorMessage, color = MaterialTheme.colorScheme.error)
            query.trim().length >= 2 && searchResults.isEmpty() ->
                Text("울산에서 검색 결과를 찾지 못했습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (searchResults.isNotEmpty()) {
            Text("검색 결과", style = MaterialTheme.typography.titleLarge)
            searchResults.forEach { place ->
                ChoiceCard(
                    title = place.name,
                    subtitle = place.address,
                    symbol = "⌕",
                    selected = selected?.id == place.id,
                    onClick = { onSelect(place) },
                )
            }
        }
        Text("추천 목적지", style = MaterialTheme.typography.titleLarge)
        DemoPlaces.destinations.forEach { place ->
            ChoiceCard(
                title = place.name,
                subtitle = place.address,
                symbol = when (place.category) {
                    "HOSPITAL" -> "＋"
                    "WELFARE" -> "♿"
                    "MARKET" -> "▦"
                    else -> "▤"
                },
                selected = selected?.id == place.id,
                onClick = { onSelect(place) },
            )
        }
    }
}

/** 전송 전 호출 내용을 최종 확인합니다. */
@Composable
fun ConfirmationScreen(
    request: RideRequest,
    routeStatus: String,
    isDemoMode: Boolean,
    isModiMode: Boolean,
    isSubmitting: Boolean,
    onPreviewRoute: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("호출 내용을 확인하세요", style = MaterialTheme.typography.headlineMedium)
        Text("입력하신 정보가 맞는지 확인해 주세요.", style = MaterialTheme.typography.bodyLarge)
        if (isModiMode) {
            Text(
                "MODI 연동: 모형 버스는 동부아파트입구에서 출발해 선택한 승차·하차 정류장을 순서대로 운행합니다.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else if (isDemoMode) {
            Text(
                "시연 모드: 현재 위치만 울산역 기준이며 정류장·OSM 경로·호출 API는 실제 데이터를 사용합니다.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SummaryRow("출발지", request.pickup?.name.orEmpty())
            SummaryRow("목적지", request.destination?.name.orEmpty())
            SummaryRow("탑승 인원", "성인 ${request.companionCount + 1}명")
            SummaryRow("이동 지원", supportLabel(request.support))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("예상 대기 시간", style = MaterialTheme.typography.bodyLarge)
            Text("약 8분", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineLarge)
        }
        if (isModiMode) {
            Text(
                "모형 지도 경로 준비 완료 · 배차 후 차량 코드와 같은 0~5번 지도에서 실시간 위치를 표시합니다.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            FullWidthButton("OSM 도로 경로 확인", onPreviewRoute)
            Text(
                routeStatus,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        FullWidthButton(
            text = if (isSubmitting) "호출을 등록하고 있습니다…" else "⚡  버스 호출하기",
            onClick = onSubmit,
            enabled = !isSubmitting,
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
    }
}

private fun supportLabel(support: MobilitySupport): String = when (support) {
    MobilitySupport.STANDARD -> "일반 탑승"
    MobilitySupport.SENIOR -> "고령자 탑승 지원"
    MobilitySupport.WHEELCHAIR -> "휠체어 이용"
    MobilitySupport.VISUAL -> "시각 안내"
    MobilitySupport.HEARING -> "청각 안내"
}
