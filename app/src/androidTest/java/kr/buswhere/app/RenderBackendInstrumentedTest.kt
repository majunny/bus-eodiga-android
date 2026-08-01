package kr.buswhere.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kr.buswhere.app.data.BusStopClient
import kr.buswhere.app.data.FirebaseSession
import kr.buswhere.app.data.PlaceSearchClient
import kr.buswhere.app.data.RideRequestClient
import kr.buswhere.app.model.DemoPlaces
import kr.buswhere.app.model.MobilitySupport
import kr.buswhere.app.model.RideRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 실제 기기에서 Firebase Authentication과 Render API의 생성·조회·취소를 검증합니다. */
@RunWith(AndroidJUnit4::class)
class RenderBackendInstrumentedTest {
    @Test
    fun searchRealUlsanDestinationPlaces() = runBlocking {
        val results = PlaceSearchClient().search("울산대학교", limit = 5)

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.location.latitude in 35.30..35.80 })
        assertTrue(results.all { it.location.longitude in 129.00..129.50 })
        assertTrue(results.any { "울산대학교" in it.name })
    }

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
    fun threeAnonymousUsersShareOneDrtTrip() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val secondApp = FirebaseApp.getApps(context).firstOrNull { it.name == "drt-second-phone" }
            ?: FirebaseApp.initializeApp(
                context,
                requireNotNull(FirebaseOptions.fromResource(context)),
                "drt-second-phone",
            )
        val thirdApp = FirebaseApp.getApps(context).firstOrNull { it.name == "drt-third-phone" }
            ?: FirebaseApp.initializeApp(
                context,
                requireNotNull(FirebaseOptions.fromResource(context)),
                "drt-third-phone",
            )
        val firstClient = RideRequestClient(session = FirebaseSession(FirebaseAuth.getInstance()))
        val secondClient = RideRequestClient(session = FirebaseSession(FirebaseAuth.getInstance(secondApp)))
        val thirdClient = RideRequestClient(session = FirebaseSession(FirebaseAuth.getInstance(thirdApp)))
        val firstRequest = RideRequest(
            pickup = DemoPlaces.demoPickupStops[0],
            destination = DemoPlaces.destinations[0],
        )
        val secondRequest = RideRequest(
            pickup = DemoPlaces.demoPickupStops[1],
            destination = DemoPlaces.destinations[1],
        )
        val thirdRequest = RideRequest(
            pickup = DemoPlaces.demoPickupStops[2],
            destination = DemoPlaces.destinations[2],
        )

        val firstCreated = firstClient.create(firstRequest, "drt-phone-one-${UUID.randomUUID()}")
        val secondCreated = secondClient.create(secondRequest, "drt-phone-two-${UUID.randomUUID()}")
        val thirdCreated = thirdClient.create(thirdRequest, "drt-phone-three-${UUID.randomUUID()}")
        assertEquals(3, setOf(firstCreated.userId, secondCreated.userId, thirdCreated.userId).size)

        val firstWaiting = firstClient.assignDemo(firstCreated.requestId)
        assertEquals("WAITING", firstWaiting.status)
        assertEquals(1, firstWaiting.matchedPassengerCount)

        val secondWaiting = secondClient.assignDemo(secondCreated.requestId)
        assertEquals("WAITING", secondWaiting.status)
        assertEquals(2, secondWaiting.matchedPassengerCount)

        val thirdAssigned = thirdClient.assignDemo(thirdCreated.requestId)
        val firstAssigned = firstClient.get(firstCreated.requestId)
        val secondAssigned = secondClient.get(secondCreated.requestId)
        assertEquals("ASSIGNED", thirdAssigned.status)
        assertEquals("ASSIGNED", firstAssigned.status)
        assertEquals("ASSIGNED", secondAssigned.status)
        assertEquals(3, thirdAssigned.matchedPassengerCount)
        assertEquals(firstAssigned.demoTripId, thirdAssigned.demoTripId)
        assertEquals(firstAssigned.assignedVehicleId, thirdAssigned.assignedVehicleId)
        assertEquals(6, thirdAssigned.demoRouteStops.size)

        var finalRecords = listOf(firstAssigned, secondAssigned, thirdAssigned)
        repeat(60) {
            if (finalRecords.all { it.status == "COMPLETED" }) return@repeat
            delay(1_000)
            finalRecords = listOf(
                firstClient.get(firstCreated.requestId),
                secondClient.get(secondCreated.requestId),
                thirdClient.get(thirdCreated.requestId),
            )
        }
        assertTrue(finalRecords.all { it.status == "COMPLETED" })
        assertTrue(finalRecords.all { it.demoTripPhase == "COMPLETED" })
        assertTrue(finalRecords.all { it.demoCurrentStopIndex == 5 })
    }
}
