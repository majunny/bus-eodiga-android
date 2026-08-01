package kr.buswhere.app.data

import com.google.gson.annotations.SerializedName
import kr.buswhere.app.BuildConfig
import kr.buswhere.app.model.GeoPointDto
import kr.buswhere.app.model.Place
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class BusStopDto(
    @SerializedName("stop_id") val stopId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val district: String,
    @SerializedName("distance_m") val distanceM: Double?,
)

private interface BusStopApi {
    @GET("v1/bus-stops")
    suspend fun search(
        @Query("query") query: String,
        @Query("limit") limit: Int,
    ): List<BusStopDto>

    @GET("v1/bus-stops/nearby")
    suspend fun nearby(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius_m") radiusM: Double,
        @Query("limit") limit: Int,
    ): List<BusStopDto>
}

/** Render에서 실제 울산 정류장 이름·주변 위치를 조회합니다. */
class BusStopClient(baseUrl: String = BuildConfig.API_BASE_URL) {
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BusStopApi::class.java)

    suspend fun search(query: String, limit: Int = 30): List<Place> =
        api.search(query, limit).map(BusStopDto::toPlace)

    suspend fun nearby(
        latitude: Double,
        longitude: Double,
        radiusM: Double = 2_000.0,
        limit: Int = 50,
    ): List<Place> = api.nearby(latitude, longitude, radiusM, limit).map(BusStopDto::toPlace)
}

fun BusStopDto.toPlace(): Place {
    val distanceLabel = distanceM?.let { " · %.0fm".format(it) }.orEmpty()
    return Place(
        id = stopId,
        name = name,
        address = "$district$distanceLabel",
        location = GeoPointDto(latitude, longitude),
        category = "BUS_STOP",
    )
}

private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"
