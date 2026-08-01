package kr.buswhere.app

import kr.buswhere.app.model.DemoPlaces
import kr.buswhere.app.model.GeoPointDto
import org.junit.Assert.assertEquals
import org.junit.Test

/** 정류장 선택 도메인 로직 테스트입니다. */
class StopSelectionTest {
    @Test
    fun nearestStopReturnsUlsanStationNearStationCoordinate() {
        val stop = DemoPlaces.nearestStop(GeoPointDto(35.5515, 129.1388))
        assertEquals("ulsan-station", stop.id)
    }

    @Test
    fun modiModelUsesSixStopsAndFixedDongbuVehicleStart() {
        assertEquals(
            listOf("31208", "31205", "40404", "40411", "40410", "64201"),
            DemoPlaces.modiModelStops.map { it.id },
        )
        assertEquals(6, DemoPlaces.modiModelStops.size)
        assertEquals("동부아파트입구", DemoPlaces.modiVehicleStart.name)
        assertEquals(35.52742029, DemoPlaces.modiVehicleStart.location.latitude, 0.0)
        assertEquals(129.3225519, DemoPlaces.modiVehicleStart.location.longitude, 0.0)
    }
}
