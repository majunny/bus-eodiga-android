package kr.buswhere.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.UUID
import kr.buswhere.app.data.LocationService
import kr.buswhere.app.data.OsmRouteClient
import kr.buswhere.app.data.RideRequestClient
import kr.buswhere.app.data.toRideStatus
import kr.buswhere.app.model.GeoPointDto
import kr.buswhere.app.model.Place
import kr.buswhere.app.model.RideRequest
import kr.buswhere.app.model.RideStatus
import kr.buswhere.app.model.VehicleAssignment
import kr.buswhere.app.ui.components.BottomNavigationBar
import kr.buswhere.app.ui.components.BusHeader
import kr.buswhere.app.ui.screens.AssignedScreen
import kr.buswhere.app.ui.screens.AssistanceScreen
import kr.buswhere.app.ui.screens.CompletedScreen
import kr.buswhere.app.ui.screens.ConfirmationScreen
import kr.buswhere.app.ui.screens.DestinationScreen
import kr.buswhere.app.ui.screens.DestinationMapScreen
import kr.buswhere.app.ui.screens.HomeScreen
import kr.buswhere.app.ui.screens.MatchingScreen
import kr.buswhere.app.ui.screens.OnBoardScreen
import kr.buswhere.app.ui.screens.PickupScreen
import kr.buswhere.app.ui.screens.ProblemScreen
import kr.buswhere.app.ui.screens.RecentStopsScreen
import kr.buswhere.app.ui.screens.StopMapScreen
import kotlinx.coroutines.launch

/** BUS어디가 MVP에서 제공하는 화면 종류입니다. */
private enum class BusScreen {
    HOME,
    PICKUP,
    RECENT_STOPS,
    STOP_MAP,
    ASSISTANCE,
    DESTINATION,
    DESTINATION_MAP,
    CONFIRMATION,
    MATCHING,
    ASSIGNED,
    ON_BOARD,
    COMPLETED,
    PROBLEM,
}

/** Stitch 디자인을 연결한 앱 루트이자 임시 시연 상태 관리자입니다. */
@Composable
fun Bus어디가App() {
    val context = LocalContext.current
    val locationService = remember { LocationService(context.applicationContext) }
    val osmRouteClient = remember { OsmRouteClient() }
    val rideRequestClient = remember { RideRequestClient() }
    val coroutineScope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(BusScreen.HOME) }
    var request by remember { mutableStateOf(RideRequest()) }
    var gpsMessage by remember { mutableStateOf<String?>(null) }
    var customDestinationName by remember { mutableStateOf("") }
    var routeStatus by remember { mutableStateOf("아직 OSM 경로를 확인하지 않았습니다.") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var idempotencyKey by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var problemTitle by remember { mutableStateOf("인터넷 연결을 확인하세요") }
    var problemDescription by remember { mutableStateOf("네트워크 연결이 원활하지 않습니다. 잠시 후 다시 시도해 주세요.") }
    var problemActionLabel by remember { mutableStateOf("다시 시도하기") }
    var problemReturnScreen by remember { mutableStateOf(BusScreen.HOME) }

    val assignment = remember {
        VehicleAssignment(
            vehicleId = "demo-bus-01",
            plateNumber = "울산 70자 1234",
            etaMinutes = 5,
            remainingStops = 3,
            boardingGuide = "울산역 1번 출구 앞",
        )
    }

    fun goHome() {
        request = RideRequest()
        routeStatus = "아직 OSM 경로를 확인하지 않았습니다."
        isSubmitting = false
        isCancelling = false
        idempotencyKey = UUID.randomUUID().toString()
        screen = BusScreen.HOME
    }

    fun showNetworkProblem(title: String, error: Throwable, returnScreen: BusScreen) {
        problemTitle = title
        problemDescription = error.message ?: "서버 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
        problemActionLabel = "다시 시도하기"
        problemReturnScreen = returnScreen
        screen = BusScreen.PROBLEM
    }

    fun submitRideRequest() {
        if (isSubmitting) return
        isSubmitting = true
        coroutineScope.launch {
            try {
                val record = rideRequestClient.create(request, idempotencyKey)
                request = request.copy(
                    requestId = record.requestId,
                    userId = record.userId,
                    status = record.toRideStatus(),
                    assignedVehicleId = record.assignedVehicleId,
                    createdAtEpochMillis = System.currentTimeMillis(),
                )
                screen = BusScreen.MATCHING
            } catch (error: Exception) {
                showNetworkProblem("버스 호출을 등록하지 못했습니다", error, BusScreen.CONFIRMATION)
            } finally {
                isSubmitting = false
            }
        }
    }

    fun cancelRideRequest() {
        if (isCancelling || request.requestId.isBlank()) return
        isCancelling = true
        coroutineScope.launch {
            try {
                val record = rideRequestClient.cancel(request.requestId)
                request = request.copy(status = record.toRideStatus())
                problemTitle = "호출이 취소되었습니다"
                problemDescription = "배차 대기 요청을 서버에서 안전하게 취소했습니다."
                problemActionLabel = "새 호출 시작"
                problemReturnScreen = BusScreen.HOME
                screen = BusScreen.PROBLEM
            } catch (error: Exception) {
                showNetworkProblem("호출을 취소하지 못했습니다", error, BusScreen.MATCHING)
            } finally {
                isCancelling = false
            }
        }
    }

    fun useCurrentLocation() {
        gpsMessage = "현재 위치를 확인하고 있습니다…"
        locationService.getCurrentLocation { result ->
            result.onSuccess { location ->
                if (location.isInsideUlsan()) {
                    request = request.copy(
                        pickup = Place(
                            id = "gps-current-location",
                            name = "현재 위치",
                            address = "위도 %.5f, 경도 %.5f".format(location.latitude, location.longitude),
                            location = location,
                            category = "GPS",
                        ),
                    )
                    gpsMessage = "현재 위치를 확인했습니다."
                } else {
                    request = request.copy(pickup = null)
                    gpsMessage = "GPS 정상 작동: 현재는 울산 서비스 지역 밖입니다. 최근 정류장이나 지도를 선택해 주세요."
                }
            }.onFailure { error ->
                gpsMessage = error.message ?: "현재 위치를 확인하지 못했습니다."
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) {
            useCurrentLocation()
        } else {
            gpsMessage = "위치 권한이 거부되었습니다. 최근 정류장이나 지도 선택을 이용해 주세요."
        }
    }

    fun requestCurrentLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            useCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BusHeader(onHome = ::goHome) },
        bottomBar = {
            when (screen) {
                BusScreen.PICKUP -> BottomNavigationBar(
                    onBack = { screen = BusScreen.HOME },
                    onNext = if (request.pickup != null) ({ screen = BusScreen.ASSISTANCE }) else null,
                )
                BusScreen.RECENT_STOPS -> BottomNavigationBar(
                    onBack = { screen = BusScreen.PICKUP },
                    onNext = if (request.pickup != null) ({ screen = BusScreen.ASSISTANCE }) else null,
                )
                BusScreen.STOP_MAP -> BottomNavigationBar(
                    onBack = { screen = BusScreen.PICKUP },
                    onNext = if (request.pickup != null) ({ screen = BusScreen.ASSISTANCE }) else null,
                )
                BusScreen.ASSISTANCE -> BottomNavigationBar(
                    onBack = { screen = BusScreen.PICKUP },
                    onNext = { screen = BusScreen.DESTINATION },
                )
                BusScreen.DESTINATION -> BottomNavigationBar(
                    onBack = { screen = BusScreen.ASSISTANCE },
                    onNext = if (request.destination != null) ({ screen = BusScreen.CONFIRMATION }) else null,
                )
                BusScreen.DESTINATION_MAP -> BottomNavigationBar(
                    onBack = { screen = BusScreen.DESTINATION },
                    onNext = if (request.destination?.category == "CUSTOM_DESTINATION") {
                        ({ screen = BusScreen.CONFIRMATION })
                    } else {
                        null
                    },
                )
                BusScreen.CONFIRMATION -> BottomNavigationBar(
                    onBack = { screen = BusScreen.DESTINATION },
                    onNext = null,
                )
                BusScreen.ASSIGNED -> BottomNavigationBar(
                    onBack = null,
                    onNext = {
                        request = request.copy(status = RideStatus.ON_BOARD)
                        screen = BusScreen.ON_BOARD
                    },
                    nextLabel = "탑승 시연",
                )
                BusScreen.ON_BOARD -> BottomNavigationBar(
                    onBack = null,
                    onNext = {
                        request = request.copy(status = RideStatus.COMPLETED)
                        screen = BusScreen.COMPLETED
                    },
                    nextLabel = "도착 시연",
                )
                else -> Unit
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(
                    if (screen == BusScreen.STOP_MAP || screen == BusScreen.DESTINATION_MAP) Modifier
                    else Modifier.verticalScroll(rememberScrollState()),
                ),
        ) {
            when (screen) {
                BusScreen.HOME -> HomeScreen(
                    onStartBooking = { screen = BusScreen.PICKUP },
                    onQuickDestination = { place ->
                        request = request.copy(destination = place)
                        screen = BusScreen.PICKUP
                    },
                )
                BusScreen.PICKUP -> PickupScreen(
                    selected = request.pickup,
                    gpsMessage = gpsMessage,
                    onUseGps = ::requestCurrentLocation,
                    onOpenRecent = { screen = BusScreen.RECENT_STOPS },
                    onOpenMap = { screen = BusScreen.STOP_MAP },
                )
                BusScreen.RECENT_STOPS -> RecentStopsScreen(
                    selected = request.pickup,
                    onSelect = { request = request.copy(pickup = it) },
                )
                BusScreen.STOP_MAP -> StopMapScreen(
                    selected = request.pickup,
                    onSelect = { request = request.copy(pickup = it) },
                )
                BusScreen.ASSISTANCE -> AssistanceScreen(
                    selected = request.support,
                    companionCount = request.companionCount,
                    onSupportChange = { request = request.copy(support = it) },
                    onCompanionChange = { request = request.copy(companionCount = it) },
                )
                BusScreen.DESTINATION -> DestinationScreen(
                    selected = request.destination,
                    onSelect = { request = request.copy(destination = it) },
                    onOpenMap = { screen = BusScreen.DESTINATION_MAP },
                )
                BusScreen.DESTINATION_MAP -> DestinationMapScreen(
                    selected = request.destination?.takeIf { it.category == "CUSTOM_DESTINATION" },
                    destinationName = customDestinationName,
                    onNameChange = { name ->
                        customDestinationName = name
                        request.destination?.takeIf { it.category == "CUSTOM_DESTINATION" }?.let { current ->
                            request = request.copy(destination = current.copy(name = name.ifBlank { "지도에서 선택한 장소" }))
                        }
                    },
                    onSelect = { request = request.copy(destination = it) },
                )
                BusScreen.CONFIRMATION -> ConfirmationScreen(
                    request = request,
                    routeStatus = routeStatus,
                    isSubmitting = isSubmitting,
                    onPreviewRoute = {
                        routeStatus = "OSM 서버에서 도로 경로를 계산하고 있습니다…"
                        coroutineScope.launch {
                            routeStatus = try {
                                val result = osmRouteClient.preview(request)
                                "OSM 연결 성공 · 도로 거리 %.2f km".format(result.distance_m / 1000.0)
                            } catch (error: Exception) {
                                "OSM 서버 연결 실패: ${error.message ?: "서버 주소를 확인해 주세요."}"
                            }
                        }
                    },
                    onSubmit = ::submitRideRequest,
                )
                BusScreen.MATCHING -> MatchingScreen(
                    isCancelling = isCancelling,
                    onCancel = ::cancelRideRequest,
                    onDemoAssigned = {
                        request = request.copy(
                            status = RideStatus.ASSIGNED,
                            assignedVehicleId = assignment.vehicleId,
                        )
                        screen = BusScreen.ASSIGNED
                    },
                )
                BusScreen.ASSIGNED -> AssignedScreen(assignment)
                BusScreen.ON_BOARD -> OnBoardScreen(request)
                BusScreen.COMPLETED -> CompletedScreen(::goHome)
                BusScreen.PROBLEM -> ProblemScreen(
                    title = problemTitle,
                    description = problemDescription,
                    actionLabel = problemActionLabel,
                    onAction = {
                        if (problemReturnScreen == BusScreen.HOME) goHome()
                        else screen = problemReturnScreen
                    },
                    onHome = ::goHome,
                )
            }
        }
    }
}

/** 울산광역시를 감싸는 단순 경계로 시연 위치의 서비스 가능 여부를 판단합니다. */
private fun GeoPointDto.isInsideUlsan(): Boolean {
    return latitude in 35.30..35.80 && longitude in 129.00..129.50
}
