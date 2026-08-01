package kr.buswhere.app.data

import com.google.gson.annotations.SerializedName
import kr.buswhere.app.BuildConfig
import kr.buswhere.app.model.GeoPointDto
import kr.buswhere.app.model.Place
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class PlaceSearchDto(
    @SerializedName("place_id") val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
)

private interface PlaceSearchApi {
    @GET("v1/places/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("limit") limit: Int,
    ): List<PlaceSearchDto>
}

/** Render를 통해 울산 영역의 실제 OSM 장소를 이름으로 조회합니다. */
class PlaceSearchClient(baseUrl: String = BuildConfig.API_BASE_URL) {
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlashForPlaceSearch())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PlaceSearchApi::class.java)

    suspend fun search(query: String, limit: Int = 10): List<Place> =
        api.search(query, limit).map { place ->
            Place(
                id = place.placeId,
                name = place.name,
                address = place.address,
                location = GeoPointDto(place.latitude, place.longitude),
                category = place.category,
            )
        }
}

private fun String.ensureTrailingSlashForPlaceSearch(): String = if (endsWith('/')) this else "$this/"
