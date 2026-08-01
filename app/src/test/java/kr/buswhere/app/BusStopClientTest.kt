package kr.buswhere.app

import kr.buswhere.app.data.BusStopDto
import kr.buswhere.app.data.toPlace
import org.junit.Assert.assertEquals
import org.junit.Test

class BusStopClientTest {
    @Test
    fun backendStopMapsToPickupPlace() {
        val place = BusStopDto(
            stopId = "12345",
            name = "태화강역",
            latitude = 35.53937,
            longitude = 129.35194,
            district = "울산광역시 남구",
            distanceM = 128.4,
        ).toPlace()

        assertEquals("12345", place.id)
        assertEquals("태화강역", place.name)
        assertEquals("BUS_STOP", place.category)
        assertEquals("울산광역시 남구 · 128m", place.address)
    }
}
