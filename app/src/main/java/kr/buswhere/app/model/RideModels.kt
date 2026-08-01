package kr.buswhere.app.model

/** 앱과 백엔드가 공통으로 사용하는 위도·경도 값입니다. */
data class GeoPointDto(
    val latitude: Double,
    val longitude: Double,
)

/** 승객이 선택한 이동 지원 유형입니다. */
enum class MobilitySupport {
    STANDARD,
    SENIOR,
    WHEELCHAIR,
    VISUAL,
    HEARING,
}

/** 호출 처리 상태입니다. Firebase 문서의 status 필드와 동일한 값을 사용합니다. */
enum class RideStatus {
    DRAFT,
    REQUESTED,
    MATCHING,
    ASSIGNED,
    ARRIVING,
    ON_BOARD,
    COMPLETED,
    CANCELLED,
    FAILED,
}

/** 정류장이나 목적지를 나타내는 앱 도메인 모델입니다. */
data class Place(
    val id: String,
    val name: String,
    val address: String,
    val location: GeoPointDto,
    val category: String,
)

/** Firebase와 향후 Python 배차 서버에 전달할 승차 호출입니다. */
data class RideRequest(
    val requestId: String = "",
    val userId: String = "demo-user",
    val pickup: Place? = null,
    val destination: Place? = null,
    val support: MobilitySupport = MobilitySupport.STANDARD,
    val companionCount: Int = 0,
    val status: RideStatus = RideStatus.DRAFT,
    val assignedVehicleId: String? = null,
    val demoTripId: String? = null,
    val matchedPassengerCount: Int = 0,
    val createdAtEpochMillis: Long = 0L,
)

/** 배정된 차량과 예상 도착 정보를 나타냅니다. */
data class VehicleAssignment(
    val vehicleId: String,
    val plateNumber: String,
    val etaMinutes: Int,
    val remainingStops: Int,
    val boardingGuide: String,
)

/** 울산 시연에서 사용하는 예제 장소입니다. */
object DemoPlaces {
    val ulsanStation = Place(
        id = "ulsan-station",
        name = "울산역",
        address = "울산광역시 울주군 삼남읍 울산역로 177",
        location = GeoPointDto(35.5514, 129.1387),
        category = "BUS_STOP",
    )

    val destinations = listOf(
        Place("uh-hospital", "울산대학교병원", "울산광역시 동구 방어진순환도로 877", GeoPointDto(35.5202, 129.4284), "HOSPITAL"),
        Place("namgu-center", "남구복지관", "울산광역시 남구 여천로12번길 50", GeoPointDto(35.5269, 129.3374), "WELFARE"),
        Place("sinjeong-market", "신정시장", "울산광역시 남구 월평로47번길 7", GeoPointDto(35.5371, 129.3112), "MARKET"),
        Place("ulsan-cityhall", "울산시청 행정복지센터", "울산광역시 남구 중앙로 201", GeoPointDto(35.5396, 129.3114), "PUBLIC_OFFICE"),
    )

    /** 서울 시연에서도 직접 선택할 수 있는 울산 예제 정류장 목록입니다. */
    val stops = listOf(
        ulsanStation,
        Place("cityhall-stop", "시청 앞 정류장", "울산광역시 남구 중앙로 201", GeoPointDto(35.5396, 129.3114), "BUS_STOP"),
        Place("taehwagang-stop", "태화강역 정류장", "울산광역시 남구 산업로 654", GeoPointDto(35.5386, 129.3537), "BUS_STOP"),
        Place("hospital-stop", "울산대학교병원 정류장", "울산광역시 동구 방어진순환도로 877", GeoPointDto(35.5202, 129.4284), "BUS_STOP"),
        Place("sinjeong-market-stop", "신정시장 앞 정류장", "울산광역시 남구 월평로47번길", GeoPointDto(35.5371, 129.3112), "BUS_STOP"),
    )

    /** 울산 정류소 CSV에서 선정한 대회용 다중 승객 출발지입니다. */
    val demoPickupStops = listOf(
        Place("15415", "울산역(종점)", "울산광역시 울주군", GeoPointDto(35.55119682, 129.1388802), "BUS_STOP"),
        Place("12318", "태화강역(종점)", "울산광역시 남구", GeoPointDto(35.53843654, 129.3528277), "BUS_STOP"),
        Place("31109", "시청앞", "울산광역시 남구", GeoPointDto(35.53915699, 129.3123405), "BUS_STOP"),
    )

    /** 초기 버전의 최근 이용 정류장 예제입니다. */
    val recentStops = listOf(ulsanStation, stops[1], stops[2])

    /** 현재 위치와 좌표상 가장 가까운 예제 정류장을 반환합니다. */
    fun nearestStop(location: GeoPointDto): Place = stops.minByOrNull { stop ->
        val latitudeDelta = stop.location.latitude - location.latitude
        val longitudeDelta = stop.location.longitude - location.longitude
        latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta
    } ?: ulsanStation
}
