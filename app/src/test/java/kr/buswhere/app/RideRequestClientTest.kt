package kr.buswhere.app

import kr.buswhere.app.data.RideRequestRecordDto
import kr.buswhere.app.data.toCreateDto
import kr.buswhere.app.data.toRideStatus
import kr.buswhere.app.model.DemoPlaces
import kr.buswhere.app.model.MobilitySupport
import kr.buswhere.app.model.RideRequest
import kr.buswhere.app.model.RideStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RideRequestClientTest {
    @Test
    fun rideRequestMapsToBackendContract() {
        val dto = RideRequest(
            pickup = DemoPlaces.ulsanStation,
            destination = DemoPlaces.destinations.first(),
            support = MobilitySupport.SENIOR,
            companionCount = 2,
        ).toCreateDto()

        assertEquals("ulsan-station", dto.pickup.placeId)
        assertEquals("uh-hospital", dto.destination.placeId)
        assertEquals(3, dto.passengerCount)
        assertEquals("SENIOR", dto.mobilitySupport)
    }

    @Test
    fun backendWaitingStatusMapsToMatchingScreen() {
        val record = RideRequestRecordDto(
            requestId = "request-1",
            userId = "user-1",
            status = "WAITING",
            assignedVehicleId = null,
            createdAt = "2026-08-01T00:00:00Z",
            updatedAt = "2026-08-01T00:00:00Z",
        )

        assertEquals(RideStatus.MATCHING, record.toRideStatus())
    }
}
