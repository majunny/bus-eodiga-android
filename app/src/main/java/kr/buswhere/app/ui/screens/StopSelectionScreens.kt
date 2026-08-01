package kr.buswhere.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import kr.buswhere.app.BuildConfig
import kr.buswhere.app.model.GeoPointDto
import kr.buswhere.app.model.Place
import kr.buswhere.app.ui.components.ChoiceCard
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.MarkerOptions

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

/** MapLibre OSM 지도에서 울산 정류장 마커를 선택합니다. */
@Composable
fun StopMapScreen(
    stops: List<Place>,
    selected: Place?,
    isLoading: Boolean,
    errorMessage: String?,
    onSelect: (Place) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("지도에서 정류장 선택", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Text("지도 위 정류장 표시를 눌러주세요.", style = MaterialTheme.typography.bodyLarge)
        }
        if (isLoading) item { Text("실제 정류장 위치를 불러오고 있습니다…") }
        errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        selected?.let { stop ->
            item {
                Text(
                    "선택됨: ${stop.name}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        item { UlsanStopMap(stops = stops, selected = selected, onSelect = onSelect) }
        item {
            Text("지도 또는 아래 정류장 이름을 눌러 선택할 수 있습니다.", style = MaterialTheme.typography.bodyMedium)
        }
        items(stops, key = { it.id }) { stop ->
            ChoiceCard(
                title = stop.name,
                subtitle = stop.address,
                symbol = "▣",
                selected = selected?.id == stop.id,
                onClick = { onSelect(stop) },
            )
        }
        item {
            Text(
                "지도 데이터 © OpenStreetMap contributors",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** 목록에 없는 목적지 이름과 지도 좌표를 직접 지정합니다. */
@Composable
fun DestinationMapScreen(
    selected: Place?,
    destinationName: String,
    onNameChange: (String) -> Unit,
    onSelect: (Place) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("새로운 목적지 선택", style = MaterialTheme.typography.headlineMedium)
        Text("장소 이름을 입력하고 지도에서 위치를 눌러주세요.", style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = destinationName,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("목적지 이름") },
            placeholder = { Text("예: 친구 집, 주민센터") },
            singleLine = true,
        )
        selected?.let {
            Text(
                "선택 좌표: %.5f, %.5f".format(it.location.latitude, it.location.longitude),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        UlsanLocationMap(selected = selected) { coordinate ->
            onSelect(
                Place(
                    id = "custom-%.5f-%.5f".format(coordinate.latitude, coordinate.longitude),
                    name = destinationName.ifBlank { "지도에서 선택한 장소" },
                    address = "위도 %.5f, 경도 %.5f".format(coordinate.latitude, coordinate.longitude),
                    location = coordinate,
                    category = "CUSTOM_DESTINATION",
                ),
            )
        }
        Text("지도 데이터 © OpenStreetMap contributors", style = MaterialTheme.typography.bodyMedium)
    }
}

/** MapLibre MapView의 생명주기와 정류장 마커를 Compose에 연결합니다. */
@Composable
private fun UlsanStopMap(stops: List<Place>, selected: Place?, onSelect: (Place) -> Unit) {
    val context = LocalContext.current
    val currentStops = rememberUpdatedState(stops)
    val currentOnSelect = rememberUpdatedState(onSelect)
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(35.5396, 129.3114))
                    .zoom(10.5)
                    .build()
                map.setStyle(mapStyle()) {
                    currentStops.value.forEach { stop ->
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(stop.location.latitude, stop.location.longitude))
                                .title(stop.name)
                                .snippet(stop.address),
                        )
                    }
                }
                map.setOnMarkerClickListener { marker ->
                    currentStops.value.minByOrNull { stop ->
                        val latDelta = stop.location.latitude - marker.position.latitude
                        val lonDelta = stop.location.longitude - marker.position.longitude
                        latDelta * latDelta + lonDelta * lonDelta
                    }?.let(currentOnSelect.value)
                    true
                }
                map.addOnMapClickListener { point ->
                    currentStops.value.nearestTo(GeoPointDto(point.latitude, point.longitude))
                        ?.let(currentOnSelect.value)
                    true
                }
            }
            onStart()
            onResume()
        }
    }

    DisposableEffect(mapView) {
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp),
        update = { view ->
            view.getMapAsync { map ->
                map.clear()
                stops.forEach { stop ->
                    map.addMarker(
                        MarkerOptions()
                            .position(LatLng(stop.location.latitude, stop.location.longitude))
                            .title(stop.name)
                            .snippet(stop.address),
                    )
                }
                selected?.let { stop ->
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(stop.location.latitude, stop.location.longitude))
                        .zoom(13.5)
                        .build()
                }
            }
        },
    )
}

private fun List<Place>.nearestTo(location: GeoPointDto): Place? = minByOrNull { stop ->
    val latitudeDelta = stop.location.latitude - location.latitude
    val longitudeDelta = stop.location.longitude - location.longitude
    latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta
}

/** 지도에서 임의 좌표를 선택하는 MapLibre 지도입니다. */
@Composable
private fun UlsanLocationMap(selected: Place?, onMapClick: (GeoPointDto) -> Unit) {
    val context = LocalContext.current
    val currentOnMapClick = rememberUpdatedState(onMapClick)
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(35.5396, 129.3114))
                    .zoom(10.5)
                    .build()
                map.setStyle(mapStyle())
                map.addOnMapClickListener { point ->
                    currentOnMapClick.value(GeoPointDto(point.latitude, point.longitude))
                    true
                }
            }
            onStart()
            onResume()
        }
    }

    DisposableEffect(mapView) {
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp),
        update = { view ->
            selected?.let { place ->
                view.getMapAsync { map ->
                    map.clear()
                    map.addMarker(
                        MarkerOptions()
                            .position(LatLng(place.location.latitude, place.location.longitude))
                            .title(place.name),
                    )
                }
            }
        },
    )
}

/** Render 스타일 URL이 없으면 OSM 표준 래스터 타일을 직접 표시합니다. */
private fun mapStyle(): Style.Builder {
    return if (BuildConfig.MAP_STYLE_URL.isNotBlank()) {
        Style.Builder().fromUri(BuildConfig.MAP_STYLE_URL)
    } else {
        Style.Builder().fromJson(OSM_RASTER_STYLE)
    }
}

private const val OSM_RASTER_STYLE = """
{
  "version": 8,
  "name": "BUS어디가 OSM",
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "maxzoom": 19,
      "attribution": "© OpenStreetMap contributors"
    }
  },
  "layers": [
    {"id": "osm", "type": "raster", "source": "osm"}
  ]
}
"""
