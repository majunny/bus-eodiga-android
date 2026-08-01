package kr.buswhere.app.data

import com.google.gson.annotations.SerializedName
import java.util.UUID
import kr.buswhere.app.BuildConfig
import kr.buswhere.app.model.RideRequest
import kr.buswhere.app.model.RideStatus
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class RideCoordinateDto(
    val latitude: Double,
    val longitude: Double,
)

data class RidePlaceDto(
    @SerializedName("place_id") val placeId: String,
    val name: String,
    val location: RideCoordinateDto,
)

data class RideRequestCreateDto(
    val source: String = "ANDROID_APP",
    val pickup: RidePlaceDto,
    val destination: RidePlaceDto,
    @SerializedName("passenger_count") val passengerCount: Int,
    @SerializedName("mobility_support") val mobilitySupport: String,
    @SerializedName("guardian_notification_enabled") val guardianNotificationEnabled: Boolean = false,
)

data class RideRequestRecordDto(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("user_id") val userId: String,
    val status: String,
    @SerializedName("assigned_vehicle_id") val assignedVehicleId: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
)

private interface RideRequestApi {
    @POST("v1/ride-requests")
    suspend fun create(
        @Header("Authorization") authorization: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: RideRequestCreateDto,
    ): RideRequestRecordDto

    @GET("v1/ride-requests/{requestId}")
    suspend fun get(
        @Path("requestId") requestId: String,
        @Header("Authorization") authorization: String,
    ): RideRequestRecordDto

    @POST("v1/ride-requests/{requestId}/cancel")
    suspend fun cancel(
        @Path("requestId") requestId: String,
        @Header("Authorization") authorization: String,
    ): RideRequestRecordDto

    @POST("v1/ride-requests/{requestId}/demo-assign")
    suspend fun assignDemo(
        @Path("requestId") requestId: String,
        @Header("Authorization") authorization: String,
    ): RideRequestRecordDto
}

/** Firebase 인증 토큰을 포함해 Render 호출 API와 통신합니다. */
class RideRequestClient(
    baseUrl: String = BuildConfig.API_BASE_URL,
    private val session: FirebaseSession = FirebaseSession(),
) {
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RideRequestApi::class.java)

    suspend fun create(request: RideRequest, idempotencyKey: String = UUID.randomUUID().toString()): RideRequestRecordDto {
        val token = session.awaitIdToken()
        return api.create("Bearer $token", idempotencyKey, request.toCreateDto())
    }

    suspend fun get(requestId: String): RideRequestRecordDto {
        val token = session.awaitIdToken()
        return api.get(requestId, "Bearer $token")
    }

    suspend fun cancel(requestId: String): RideRequestRecordDto {
        val token = session.awaitIdToken()
        return api.cancel(requestId, "Bearer $token")
    }

    suspend fun assignDemo(requestId: String): RideRequestRecordDto {
        val token = session.awaitIdToken()
        return api.assignDemo(requestId, "Bearer $token")
    }
}

fun RideRequest.toCreateDto(): RideRequestCreateDto {
    val selectedPickup = requireNotNull(pickup) { "출발지를 먼저 선택해 주세요." }
    val selectedDestination = requireNotNull(destination) { "목적지를 먼저 선택해 주세요." }
    return RideRequestCreateDto(
        pickup = selectedPickup.toDto(),
        destination = selectedDestination.toDto(),
        passengerCount = companionCount + 1,
        mobilitySupport = support.name,
    )
}

fun RideRequestRecordDto.toRideStatus(): RideStatus = when (status) {
    "WAITING" -> RideStatus.MATCHING
    "ASSIGNED" -> RideStatus.ASSIGNED
    "PICKED_UP" -> RideStatus.ON_BOARD
    "COMPLETED" -> RideStatus.COMPLETED
    "CANCELLED" -> RideStatus.CANCELLED
    else -> RideStatus.FAILED
}

fun RideStatusUpdate.toRideStatus(): RideStatus = when (status) {
    "WAITING" -> RideStatus.MATCHING
    "ASSIGNED" -> RideStatus.ASSIGNED
    "PICKED_UP" -> RideStatus.ON_BOARD
    "COMPLETED" -> RideStatus.COMPLETED
    "CANCELLED" -> RideStatus.CANCELLED
    else -> RideStatus.FAILED
}

private fun kr.buswhere.app.model.Place.toDto(): RidePlaceDto = RidePlaceDto(
    placeId = id,
    name = name,
    location = RideCoordinateDto(location.latitude, location.longitude),
)

private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"
