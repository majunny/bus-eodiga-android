package kr.buswhere.app.data

import kr.buswhere.app.BuildConfig
import kr.buswhere.app.model.RideRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/** 참고 OSMnx 서버가 받는 장소 좌표 형식입니다. */
data class RoutePlaceDto(val name: String, val lat: Double, val lon: Double)

/** 참고 서버의 `/api/find_nearest` 요청 형식입니다. */
data class OsmRouteRequestDto(
    val start_lat: Double,
    val start_lon: Double,
    val hospitals: List<RoutePlaceDto>,
    val network_type: String = "drive",
    val buffer_m: Int = 1200,
)

/** 참고 서버가 반환하는 도로 경로 결과입니다. */
data class OsmRouteResponseDto(
    val nearest_hospital: RoutePlaceDto,
    val distance_m: Double,
    val map_url: String,
    val route_coords: List<List<Double>>,
)

private interface OsmRouteApi {
    @POST("api/find_nearest")
    suspend fun findRoute(@Body request: OsmRouteRequestDto): OsmRouteResponseDto
}

/** 로컬 OSMnx 또는 Render 서버와 통신하는 경로 API 클라이언트입니다. */
class OsmRouteClient(baseUrl: String = BuildConfig.API_BASE_URL) {
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OsmRouteApi::class.java)

    /** 앱 호출 모델을 참고 서버 형식으로 바꿔 실제 도로 경로를 요청합니다. */
    suspend fun preview(request: RideRequest): OsmRouteResponseDto {
        val pickup = requireNotNull(request.pickup) { "출발지를 먼저 선택해 주세요." }
        val destination = requireNotNull(request.destination) { "목적지를 먼저 선택해 주세요." }
        return api.findRoute(
            OsmRouteRequestDto(
                start_lat = pickup.location.latitude,
                start_lon = pickup.location.longitude,
                hospitals = listOf(
                    RoutePlaceDto(
                        name = destination.name,
                        lat = destination.location.latitude,
                        lon = destination.location.longitude,
                    ),
                ),
            ),
        )
    }
}

private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"
