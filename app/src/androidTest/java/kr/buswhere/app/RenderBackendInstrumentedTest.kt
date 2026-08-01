package kr.buswhere.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import kr.buswhere.app.data.BusStopClient
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
}
