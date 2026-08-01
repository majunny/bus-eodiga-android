package kr.buswhere.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kr.buswhere.app.data.BusStopClient
import kr.buswhere.app.data.FirebaseSession
import kr.buswhere.app.data.RideRequestClient
import kr.buswhere.app.model.DemoPlaces
import kr.buswhere.app.model.MobilitySupport
import kr.buswhere.app.model.RideRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 실제 기기에서 Firebase Authentication과 Render API의 생성·조회·취소를 검증합니다. */
@RunWith(AndroidJUnit4::class)
class RenderBackendInstrumentedTest {
    @Test
    fun searchAndFindNearbyRealBusStops() = runBlocking {
        val client = BusStopClient()
        val searchResults = client.search("태화강역", limit = 10)
        assertTrue(searchResults.isNotEmpty())
        assertTrue(searchResults.all { "태화강역" in it.name })

        val nearby = client.nearby(35.53937, 129.35194, radiusM = 1_000.0, limit = 10)
        assertTrue(nearby.isNotEmpty())
        assertEquals("태화강역", nearby.first().name)
    }

    @Test
    fun createReadAndCancelRideRequest() = runBlocking {
        val client = RideRequestClient()
        val request = RideRequest(
            pickup = DemoPlaces.ulsanStation,
            destination = DemoPlaces.destinations.first(),
            support = MobilitySupport.SENIOR,
        )

        val created = client.create(request, "android-test-${UUID.randomUUID()}")
        assertEquals("WAITING", created.status)

        try {
            val fetched = client.get(created.requestId)
            assertEquals(created.requestId, fetched.requestId)
            assertEquals(created.userId, fetched.userId)
        } finally {
            val cancelled = client.cancel(created.requestId)
            assertEquals("CANCELLED", cancelled.status)
            assertNotNull(cancelled.updatedAt)
        }
    }

    @Test
    fun twoAnonymousUsersShareOneDrtTrip() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val secondApp = FirebaseApp.getApps(context).firstOrNull { it.name == "drt-second-phone" }
            ?: FirebaseApp.initializeApp(
                context,
                requireNotNull(FirebaseOptions.fromResource(context)),
                "drt-second-phone",
            )
        val firstClient = RideRequestClient(session = FirebaseSession(FirebaseAuth.getInstance()))
        val secondClient = RideRequestClient(session = FirebaseSession(FirebaseAuth.getInstance(secondApp)))
        val firstRequest = RideRequest(
            pickup = DemoPlaces.demoPickupStops[0],
            destination = DemoPlaces.destinations[0],
        )
        val secondRequest = RideRequest(
            pickup = DemoPlaces.demoPickupStops[1],
            destination = DemoPlaces.destinations[1],
        )

        val firstCreated = firstClient.create(firstRequest, "drt-phone-one-${UUID.randomUUID()}")
        val secondCreated = secondClient.create(secondRequest, "drt-phone-two-${UUID.randomUUID()}")
        assertTrue(firstCreated.userId != secondCreated.userId)

        val firstWaiting = firstClient.assignDemo(firstCreated.requestId)
        assertEquals("WAITING", firstWaiting.status)
        assertEquals(1, firstWaiting.matchedPassengerCount)

        val secondAssigned = secondClient.assignDemo(secondCreated.requestId)
        val firstAssigned = firstClient.get(firstCreated.requestId)
        assertEquals("ASSIGNED", secondAssigned.status)
        assertEquals("ASSIGNED", firstAssigned.status)
        assertEquals(2, secondAssigned.matchedPassengerCount)
        assertEquals(firstAssigned.demoTripId, secondAssigned.demoTripId)
        assertEquals(firstAssigned.assignedVehicleId, secondAssigned.assignedVehicleId)
        assertEquals(4, secondAssigned.demoRouteStops.size)
    }
}
