package kr.buswhere.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class RideStatusUpdate(
    val requestId: String,
    val userId: String,
    val status: String,
    val assignedVehicleId: String?,
    val demoTripId: String?,
    val matchedPassengerCount: Int,
    val demoRouteStops: List<RideRouteStopUpdate>,
)

data class RideRouteStopUpdate(
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/** 로그인 사용자의 Firestore 호출 문서를 실시간 감시합니다. */
class RideRequestObserver(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun observe(requestId: String, onResult: (Result<RideStatusUpdate>) -> Unit): ListenerRegistration =
        firestore.collection("ride_requests").document(requestId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(Result.failure(error))
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
            val status = snapshot.getString("status")
            val userId = snapshot.getString("user_id")
            if (status == null || userId == null) {
                onResult(Result.failure(IllegalStateException("호출 상태 문서 형식이 올바르지 않습니다.")))
                return@addSnapshotListener
            }
            onResult(Result.success(RideStatusUpdate(
                requestId = snapshot.getString("request_id") ?: snapshot.id,
                userId = userId,
                status = status,
                assignedVehicleId = snapshot.getString("assigned_vehicle_id"),
                demoTripId = snapshot.getString("demo_trip_id"),
                matchedPassengerCount = snapshot.getLong("matched_passenger_count")?.toInt() ?: 0,
                demoRouteStops = snapshot.demoRouteStops(),
            )))
        }
}

private fun com.google.firebase.firestore.DocumentSnapshot.demoRouteStops(): List<RideRouteStopUpdate> {
    val stops = get("demo_route_stops") as? List<*> ?: return emptyList()
    return stops.mapNotNull { rawStop ->
        val stop = rawStop as? Map<*, *> ?: return@mapNotNull null
        val location = stop["location"] as? Map<*, *> ?: return@mapNotNull null
        val latitude = (location["latitude"] as? Number)?.toDouble() ?: return@mapNotNull null
        val longitude = (location["longitude"] as? Number)?.toDouble() ?: return@mapNotNull null
        RideRouteStopUpdate(
            placeId = stop["place_id"] as? String ?: return@mapNotNull null,
            name = stop["name"] as? String ?: return@mapNotNull null,
            latitude = latitude,
            longitude = longitude,
        )
    }
}
