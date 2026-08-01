package kr.buswhere.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.buswhere.app.model.Place
import kr.buswhere.app.ui.components.ChoiceCard

/** 실제 울산 정류장을 이름으로 검색해 큰 목록으로 표시합니다. */
@Composable
fun RecentStopsScreen(
    stops: List<Place>,
    selected: Place?,
    query: String,
    isLoading: Boolean,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onSelect: (Place) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("정류장 이름 검색", style = MaterialTheme.typography.headlineMedium)
        Text("두 글자 이상 입력해 실제 울산 정류장을 찾아보세요.", style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("정류장명") },
            placeholder = { Text("예: 태화강역, 울산역") },
            singleLine = true,
        )
        if (isLoading) Text("정류장을 검색하고 있습니다…")
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!isLoading && query.trim().length >= 2 && stops.isEmpty() && errorMessage == null) {
            Text("검색 결과가 없습니다.")
        }
        stops.forEach { stop ->
            ChoiceCard(
                title = stop.name,
                subtitle = stop.address,
                symbol = "▣",
                selected = selected?.id == stop.id,
                onClick = { onSelect(stop) },
            )
        }
    }
}
