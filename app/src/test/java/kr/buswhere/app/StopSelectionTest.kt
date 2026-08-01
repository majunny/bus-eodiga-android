package kr.buswhere.app

import kr.buswhere.app.model.DemoPlaces
import kr.buswhere.app.model.GeoPointDto
import org.junit.Assert.assertEquals
import org.junit.Test

/** 정류장 선택 도메인 로직 테스트입니다. */
class StopSelectionTest {
    @Test
    fun requestedDemoStopsKeepExactIdsAndCoordinates() {
        val stops = DemoPlaces.demoPickupStops

        assertEquals(listOf("31208", "31205", "40404", "40411", "40410", "57172"), stops.map { it.id })
        assertEquals(35.52742029, stops.first().location.latitude, 0.0)
        assertEquals(129.3225519, stops.first().location.longitude, 0.0)
        assertEquals("부산광역시 · 버스운영과", stops.last().address)
        assertEquals(35.23916902, stops.last().location.latitude, 0.0)
        assertEquals(129.0927024, stops.last().location.longitude, 0.0)
    }

    @Test
    fun nearestStopReturnsUlsanStationNearStationCoordinate() {
        val stop = DemoPlaces.nearestStop(GeoPointDto(35.5515, 129.1388))
        assertEquals("ulsan-station", stop.id)
    }
}
