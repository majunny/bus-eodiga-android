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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import kr.buswhere.app.data.BusStopClient
import kr.buswhere.app.data.OsmRouteClient
import kr.buswhere.app.data.PlaceSearchClient
import kr.buswhere.app.data.RideRequestClient
import kr.buswhere.app.data.RideRequestObserver
import kr.buswhere.app.data.toRideStatus
import kr.buswhere.app.model.GeoPointDto
import kr.buswhere.app.model.DemoPlaces
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
import kr.buswhere.app.ui.screens.HomeScreen
import kr.buswhere.app.ui.screens.HelpScreen
import kr.buswhere.app.ui.screens.MatchingScreen
import kr.buswhere.app.ui.screens.OnBoardScreen
import kr.buswhere.app.ui.screens.PickupScreen
import kr.buswhere.app.ui.screens.ProblemScreen
import kr.buswhere.app.ui.screens.RecentStopsScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/** BUS어디가 MVP에서 제공하는 화면 종류입니다. */
private enum class BusScreen {
    HOME,
    PICKUP,
    RECENT_STOPS,
    ASSISTANCE,
    DESTINATION,
    CONFIRMATION,
    MATCHING,
    ASSIGNED,
    ON_BOARD,
    COMPLETED,
    PROBLEM,
    HELP,
}

/** Stitch 디자인을 연결한 앱 루트이자 임시 시연 상태 관리자입니다. */
@Composable
fun Bus어디가App() {
    val context = LocalContext.current
    val locationService = remember { LocationService(context.applicationContext) }
    val busStopClient = remember { BusStopClient() }
    val osmRouteClient = remember { OsmRouteClient() }
    val placeSearchClient = remember { PlaceSearchClient() }
    val rideRequestClient = remember { RideRequestClient() }
    val rideRequestObserver = remember { RideRequestObserver() }
    val coroutineScope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(BusScreen.HOME) }
    var previousScreen by remember { mutableStateOf(BusScreen.HOME) }
    var request by remember { mutableStateOf(RideRequest()) }
    var gpsMessage by remember { mutableStateOf<String?>(null) }
    var isDemoMode by remember { mutableStateOf(false) }
    var availableStops by remember { mutableStateOf<List<Place>>(emptyList()) }
    var stopQuery by remember { mutableStateOf("") }
    var stopsLoading by remember { mutableStateOf(false) }
    var stopsError by remember { mutableStateOf<String?>(null) }
    var destinationQuery by remember { mutableStateOf("") }
    var destinationResults by remember { mutableStateOf<List<Place>>(emptyList()) }
    var destinationLoading by remember { mutableStateOf(false) }
    var destinationError by remember { mutableStateOf<String?>(null) }
    var routeStatus by remember { mutableStateOf("아직 OSM 경로를 확인하지 않았습니다.") }
    var liveRouteCoordinates by remember { mutableStateOf<List<GeoPointDto>>(emptyList()) }
    var sharedRouteStops by remember { mutableStateOf<List<Place>>(emptyList()) }
    var loadedSharedRouteKey by remember { mutableStateOf<String?>(null) }
    var loadingSharedRouteKey by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var isRequestingAssignment by remember { mutableStateOf(false) }
    var realtimeMessage by remember { mutableStateOf("Firestore 실시간 배차 상태를 기다리고 있습니다.") }
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
    val demoVehicleStart = remember { GeoPointDto(35.5588, 129.1255) }

    fun goHome() {
        request = RideRequest()
        gpsMessage = null
        isDemoMode = false
        availableStops = emptyList()
        stopQuery = ""
        stopsError = null
        destinationQuery = ""
        destinationResults = emptyList()
        destinationLoading = false
        destinationError = null
        routeStatus = "아직 OSM 경로를 확인하지 않았습니다."
        liveRouteCoordinates = emptyList()
        sharedRouteStops = emptyList()
        loadedSharedRouteKey = null
        loadingSharedRouteKey = null
        isSubmitting = false
        isCancelling = false
        isRequestingAssignment = false
        realtimeMessage = "Firestore 실시간 배차 상태를 기다리고 있습니다."
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
                    demoTripId = record.demoTripId,
                    matchedPassengerCount = record.matchedPassengerCount,
                    demoGroupSize = record.demoGroupSize,
                    demoCurrentStopIndex = record.demoCurrentStopIndex,
                    demoTripPhase = record.demoTripPhase,
                    createdAtEpochMillis = System.currentTimeMillis(),
                )
                screen = BusScreen.MATCHING
                if (isDemoMode) {
                    isRequestingAssignment = true
                    realtimeMessage = "다인 DRT 대기열에 참여하고 있습니다…"
                    val pooled = rideRequestClient.assignDemo(record.requestId)
                    request = request.copy(
                        matchedPassengerCount = pooled.matchedPassengerCount,
                        demoTripId = pooled.demoTripId,
                        demoGroupSize = pooled.demoGroupSize,
                        demoCurrentStopIndex = pooled.demoCurrentStopIndex,
                        demoTripPhase = pooled.demoTripPhase,
                    )
                    realtimeMessage = if (pooled.matchedPassengerCount < pooled.demoGroupSize) {
                        "현재 탑승 인원 · ${pooled.matchedPassengerCount}/${pooled.demoGroupSize}명"
                    } else {
                        "탑승 인원 ${pooled.matchedPassengerCount}명 · 공동 배차 시작"
                    }
                    isRequestingAssignment = false
                }
            } catch (error: Exception) {
                showNetworkProblem("버스 호출을 등록하지 못했습니다", error, BusScreen.CONFIRMATION)
            } finally {
                isSubmitting = false
                isRequestingAssignment = false
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

    fun requestDemoAssignment() {
        if (isRequestingAssignment || request.requestId.isBlank()) return
        isRequestingAssignment = true
        realtimeMessage = "Render가 Firestore에 차량 배정을 기록하고 있습니다…"
        coroutineScope.launch {
            try {
                val pooled = rideRequestClient.assignDemo(request.requestId)
                request = request.copy(
                    matchedPassengerCount = pooled.matchedPassengerCount,
                    demoTripId = pooled.demoTripId,
                    demoGroupSize = pooled.demoGroupSize,
                    demoCurrentStopIndex = pooled.demoCurrentStopIndex,
                    demoTripPhase = pooled.demoTripPhase,
                )
                realtimeMessage = if (pooled.matchedPassengerCount < pooled.demoGroupSize) {
                    "현재 탑승 인원 · ${pooled.matchedPassengerCount}/${pooled.demoGroupSize}명"
                } else {
                    "탑승 인원 ${pooled.matchedPassengerCount}명 · 공동 배차 시작"
                }
            } catch (error: Exception) {
                showNetworkProblem("시연 차량을 배정하지 못했습니다", error, BusScreen.MATCHING)
            } finally {
                isRequestingAssignment = false
            }
        }
    }

    fun selectNearestStop(location: GeoPointDto, demoMode: Boolean) {
        isDemoMode = demoMode
        stopsLoading = true
        coroutineScope.launch {
            try {
                val nearby = busStopClient.nearby(location.latitude, location.longitude)
                availableStops = nearby
                val nearest = nearby.firstOrNull()
                request = request.copy(pickup = nearest)
                gpsMessage = nearest?.let {
                    val distance = it.address.substringAfterLast(" · ")
                    if (demoMode) "시연 위치: 울산역 · ${it.name} ($distance)"
                    else "가장 가까운 정류장: ${it.name} ($distance)"
                } ?: "현재 위치 반경 2km에 정류장이 없습니다."
                stopsError = null
            } catch (error: Exception) {
                gpsMessage = "주변 정류장을 불러오지 못했습니다."
                stopsError = error.message
            } finally {
                stopsLoading = false
            }
        }
    }

    fun useCurrentLocation() {
        isDemoMode = false
        gpsMessage = "현재 위치를 확인하고 있습니다…"
        locationService.getCurrentLocation { result ->
            result.onSuccess { location ->
                if (location.isInsideUlsan()) {
                    selectNearestStop(location, demoMode = false)
                } else {
                    request = request.copy(pickup = null)
                    gpsMessage = "GPS 정상 작동: 현재는 울산 서비스 지역 밖입니다. 울산 시연 모드를 이용해 주세요."
                }
            }.onFailure { error ->
                gpsMessage = error.message ?: "현재 위치를 확인하지 못했습니다."
            }
        }
    }

    fun useDemoLocation() {
        isDemoMode = true
        request = request.copy(pickup = null)
        gpsMessage = "아래 실제 정류장 3곳 중 출발지를 선택해 주세요."
    }

    fun loadSharedRouteIfNeeded(tripId: String?, stops: List<Place>) {
        if (tripId.isNullOrBlank() || stops.isEmpty()) return
        val routeKey = buildString {
            append(tripId)
            stops.forEach { stop ->
                append('|')
                append(stop.id)
                append(':')
                append(stop.location.latitude)
                append(':')
                append(stop.location.longitude)
            }
        }
        if (loadedSharedRouteKey == routeKey || loadingSharedRouteKey == routeKey) return

        loadingSharedRouteKey = routeKey
        liveRouteCoordinates = emptyList()
        coroutineScope.launch {
            runCatching { osmRouteClient.routeThrough(demoVehicleStart, stops) }
                .onSuccess { route ->
                    if (route.isNotEmpty() && loadingSharedRouteKey == routeKey) {
                        liveRouteCoordinates = route
                        loadedSharedRouteKey = routeKey
                    }
                }
            if (loadingSharedRouteKey == routeKey) loadingSharedRouteKey = null
        }
    }

    DisposableEffect(request.requestId) {
        if (request.requestId.isBlank()) {
            onDispose { }
        } else {
            val registration = rideRequestObserver.observe(request.requestId) { result ->
                result.onSuccess { update ->
                    val liveStatus = update.toRideStatus()
                    val updatedSharedStops = update.demoRouteStops.map { stop ->
                        Place(
                            id = stop.placeId,
                            name = stop.name,
                            address = "다인 공동 DRT 경유지",
                            location = GeoPointDto(stop.latitude, stop.longitude),
                            category = "DRT_STOP",
                        )
                    }
                    if (updatedSharedStops.isNotEmpty()) sharedRouteStops = updatedSharedStops
                    request = request.copy(
                        userId = update.userId,
                        status = liveStatus,
                        assignedVehicleId = update.assignedVehicleId,
                        demoTripId = update.demoTripId,
                        matchedPassengerCount = update.matchedPassengerCount,
                        demoGroupSize = update.demoGroupSize,
                        demoCurrentStopIndex = update.demoCurrentStopIndex,
                        demoTripPhase = update.demoTripPhase,
                    )
                    realtimeMessage = when {
                        liveStatus == RideStatus.MATCHING && update.matchedPassengerCount > 0 ->
                            "현재 탑승 인원 · ${update.matchedPassengerCount}/${update.demoGroupSize}명"
                        liveStatus == RideStatus.ASSIGNED ->
                            "탑승 인원 ${update.matchedPassengerCount}명 · 같은 차량으로 공동 배차"
                        else -> "Firestore 실시간 연결됨 · ${update.status}"
                    }
                    when (liveStatus) {
                        RideStatus.ASSIGNED, RideStatus.ARRIVING -> {
                            screen = BusScreen.ASSIGNED
                            if (updatedSharedStops.isNotEmpty()) {
                                loadSharedRouteIfNeeded(update.demoTripId, updatedSharedStops)
                            } else {
                                coroutineScope.launch {
                                    request.pickup?.let { pickup ->
                                        runCatching { osmRouteClient.route(demoVehicleStart, pickup) }
                                            .onSuccess { route ->
                                                liveRouteCoordinates = route.route_coords.mapNotNull { coordinate ->
                                                    if (coordinate.size >= 2) GeoPointDto(coordinate[0], coordinate[1]) else null
                                                }
                                        }
                                    }
                                }
                            }
                        }
                        RideStatus.ON_BOARD -> {
                            if (updatedSharedStops.isNotEmpty()) {
                                // 공동 운행은 탑승 후에도 같은 화면·경로를 유지해야 모든 기기의 지도가 일치합니다.
                                screen = BusScreen.ASSIGNED
                                loadSharedRouteIfNeeded(update.demoTripId, updatedSharedStops)
                            } else {
                                screen = BusScreen.ON_BOARD
                                coroutineScope.launch {
                                    runCatching { osmRouteClient.preview(request) }
                                        .onSuccess { route ->
                                            liveRouteCoordinates = route.route_coords.mapNotNull { coordinate ->
                                                if (coordinate.size >= 2) GeoPointDto(coordinate[0], coordinate[1]) else null
                                            }
                                        }
                                }
                            }
                        }
                        RideStatus.COMPLETED -> screen = BusScreen.COMPLETED
                        RideStatus.CANCELLED -> {
                            problemTitle = "호출이 취소되었습니다"
                            problemDescription = "Firestore에서 취소 상태를 실시간으로 확인했습니다."
                            problemActionLabel = "새 호출 시작"
                            problemReturnScreen = BusScreen.HOME
                            screen = BusScreen.PROBLEM
                        }
                        RideStatus.FAILED -> {
                            problemTitle = "배차를 완료하지 못했습니다"
                            problemDescription = "Firestore에서 배차 실패 상태를 수신했습니다."
                            problemActionLabel = "새 호출 시작"
                            problemReturnScreen = BusScreen.HOME
                            screen = BusScreen.PROBLEM
                        }
                        else -> Unit
                    }
                }.onFailure { error ->
                    realtimeMessage = "실시간 연결 실패: ${error.message ?: "Firestore 연결을 확인해 주세요."}"
                }
            }
            onDispose { registration.remove() }
        }
    }

    LaunchedEffect(screen, stopQuery) {
        if (screen != BusScreen.RECENT_STOPS) return@LaunchedEffect
        if (stopQuery.trim().length < 2) {
            availableStops = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        stopsLoading = true
        try {
            availableStops = busStopClient.search(stopQuery.trim())
            stopsError = null
        } catch (error: Exception) {
            stopsError = error.message ?: "정류장 검색에 실패했습니다."
        } finally {
            stopsLoading = false
        }
    }

    LaunchedEffect(screen, destinationQuery) {
        if (screen != BusScreen.DESTINATION) return@LaunchedEffect
        val normalizedQuery = destinationQuery.trim()
        if (normalizedQuery.length < 2) {
            destinationResults = emptyList()
            destinationLoading = false
            destinationError = null
            return@LaunchedEffect
        }
        delay(350)
        destinationLoading = true
        try {
            destinationResults = placeSearchClient.search(normalizedQuery)
            destinationError = null
        } catch (error: Exception) {
            destinationResults = emptyList()
            destinationError = error.message ?: "도착지 검색에 실패했습니다."
        } finally {
            destinationLoading = false
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
        topBar = {
            BusHeader(
                onHome = ::goHome,
                onHelp = {
                    if (screen != BusScreen.HELP) {
                        previousScreen = screen
                        screen = BusScreen.HELP
                    }
                },
            )
        },
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
                BusScreen.ASSISTANCE -> BottomNavigationBar(
                    onBack = { screen = BusScreen.PICKUP },
                    onNext = { screen = BusScreen.DESTINATION },
                )
                BusScreen.DESTINATION -> BottomNavigationBar(
                    onBack = { screen = BusScreen.ASSISTANCE },
                    onNext = if (request.destination != null) ({ screen = BusScreen.CONFIRMATION }) else null,
                )
                BusScreen.CONFIRMATION -> BottomNavigationBar(
                    onBack = { screen = BusScreen.DESTINATION },
                    onNext = null,
                )
                else -> Unit
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
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
                    isDemoMode = isDemoMode,
                    onUseGps = ::requestCurrentLocation,
                    onUseDemoMode = ::useDemoLocation,
                    onSelectDemoStop = { stop ->
                        isDemoMode = true
                        request = request.copy(pickup = stop)
                        gpsMessage = "시연 출발지: ${stop.name}"
                    },
                    onOpenRecent = {
                        stopQuery = ""
                        availableStops = emptyList()
                        stopsError = null
                        screen = BusScreen.RECENT_STOPS
                    },
                )
                BusScreen.RECENT_STOPS -> RecentStopsScreen(
                    stops = availableStops,
                    selected = request.pickup,
                    query = stopQuery,
                    isLoading = stopsLoading,
                    errorMessage = stopsError,
                    onQueryChange = { stopQuery = it },
                    onSelect = {
                        request = request.copy(pickup = it)
                        gpsMessage = null
                    },
                )
                BusScreen.ASSISTANCE -> AssistanceScreen(
                    selected = request.support,
                    companionCount = request.companionCount,
                    onSupportChange = { request = request.copy(support = it) },
                    onCompanionChange = { request = request.copy(companionCount = it) },
                )
                BusScreen.DESTINATION -> DestinationScreen(
                    selected = request.destination,
                    query = destinationQuery,
                    searchResults = destinationResults,
                    isLoading = destinationLoading,
                    errorMessage = destinationError,
                    onQueryChange = { destinationQuery = it },
                    onSelect = { request = request.copy(destination = it) },
                )
                BusScreen.CONFIRMATION -> ConfirmationScreen(
                    request = request,
                    routeStatus = routeStatus,
                    isDemoMode = isDemoMode,
                    isSubmitting = isSubmitting,
                    onPreviewRoute = {
                        routeStatus = "OSM 서버에서 도로 경로를 계산하고 있습니다…"
                        coroutineScope.launch {
                            routeStatus = try {
                                val result = osmRouteClient.preview(request)
                                liveRouteCoordinates = result.route_coords.mapNotNull { coordinate ->
                                    if (coordinate.size >= 2) GeoPointDto(coordinate[0], coordinate[1]) else null
                                }
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
                    isRequestingAssignment = isRequestingAssignment,
                    realtimeMessage = realtimeMessage,
                    matchedPassengerCount = request.matchedPassengerCount,
                    demoGroupSize = request.demoGroupSize,
                    onCancel = ::cancelRideRequest,
                    onRequestDemoAssignment = ::requestDemoAssignment,
                )
                BusScreen.ASSIGNED -> AssignedScreen(
                    assignment,
                    request,
                    demoVehicleStart,
                    liveRouteCoordinates,
                    sharedRouteStops,
                    request.demoCurrentStopIndex,
                    request.demoTripPhase,
                )
                BusScreen.ON_BOARD -> OnBoardScreen(
                    request,
                    liveRouteCoordinates,
                    sharedRouteStops,
                    request.demoCurrentStopIndex,
                    request.demoTripPhase,
                )
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
                BusScreen.HELP -> HelpScreen(onBack = { screen = previousScreen })
            }
        }
    }
}

/** 울산광역시를 감싸는 단순 경계로 시연 위치의 서비스 가능 여부를 판단합니다. */
private fun GeoPointDto.isInsideUlsan(): Boolean {
    return latitude in 35.30..35.80 && longitude in 129.00..129.50
}
