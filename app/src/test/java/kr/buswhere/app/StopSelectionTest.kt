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
}
