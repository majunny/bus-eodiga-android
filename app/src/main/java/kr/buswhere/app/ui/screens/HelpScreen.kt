package kr.buswhere.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.buswhere.app.ui.components.FullWidthButton

/** 서비스 사용 방법과 대회 시연 모드의 범위를 설명합니다. */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("도움말", style = MaterialTheme.typography.headlineMedium)
        Text("BUS어디가는 고령자와 교통약자를 위한 수요응답형 버스 호출 서비스입니다.")
        HelpStep("1", "탑승 정류장 선택", "GPS 주변 정류장, 정류장명 검색 또는 지도에서 실제 울산 정류장을 선택합니다.")
        HelpStep("2", "이동 지원 선택", "고령자·휠체어·시각·청각 지원과 동반 인원을 선택합니다.")
        HelpStep("3", "목적지와 경로 확인", "목적지를 선택하면 OpenStreetMap 도로 경로와 거리를 확인할 수 있습니다.")
        HelpStep("4", "버스 호출", "Firebase 인증 후 Render API가 호출을 Firestore에 안전하게 저장합니다.")
        HelpStep("5", "배차 상태 확인", "차량이 배정되면 차량 번호, 도착 시간과 승차 위치를 안내합니다.")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("서울 대회 시연 모드", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            Text("대회장이 서비스 지역 밖이므로 위치만 울산역으로 설정합니다. 정류장 3,616개, OSM 경로, Firebase 인증, Render 호출과 Firestore 저장은 모두 실제 시스템을 사용합니다.")
        }
        FullWidthButton("이전 화면으로", onBack)
    }
}

@Composable
private fun HelpStep(number: String, title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("$number. $title", style = MaterialTheme.typography.titleLarge)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
