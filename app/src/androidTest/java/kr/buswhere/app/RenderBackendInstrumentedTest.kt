package kr.buswhere.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import kr.buswhere.app.data.RideRequestClient
import kr.buswhere.app.model.DemoPlaces
import kr.buswhere.app.model.MobilitySupport
import kr.buswhere.app.model.RideRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** 실제 기기에서 Firebase Authentication과 Render API의 생성·조회·취소를 검증합니다. */
@RunWith(AndroidJUnit4::class)
class RenderBackendInstrumentedTest {
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
