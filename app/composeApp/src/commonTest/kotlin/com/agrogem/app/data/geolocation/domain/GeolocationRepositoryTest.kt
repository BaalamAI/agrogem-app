package com.agrogem.app.data.geolocation.domain

import com.agrogem.app.data.geolocation.api.ElevationResponse
import com.agrogem.app.data.geolocation.api.GeocodeHit
import com.agrogem.app.data.geolocation.api.GeolocationApi
import com.agrogem.app.data.geolocation.api.ReverseGeocodeResponse
import com.agrogem.app.data.shared.domain.LatLng
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeolocationRepositoryTest {

    @Test
    fun `reverseGeocode maps null displayName to default`() = runTest {
        val fakeApi = FakeGeolocationApi(
            reverseResponse = ReverseGeocodeResponse(
                displayName = null,
                lat = 14.9726,
                lng = -89.5301,
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            )
        )
        val store = com.agrogem.app.data.geolocation.ResolvedLocationStore()
        val repo = GeolocationRepositoryImpl(api = fakeApi, store = store)

        val result = repo.reverseGeocode(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val location = result.getOrNull()
        assertEquals("Ubicación desconocida", location?.display?.primary)
    }

    @Test
    fun `reverseGeocode composes LocationDisplay from municipality state country`() = runTest {
        val fakeApi = FakeGeolocationApi(
            reverseResponse = ReverseGeocodeResponse(
                displayName = "Zacapa, Guatemala",
                lat = 14.9726,
                lng = -89.5301,
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            )
        )
        val store = com.agrogem.app.data.geolocation.ResolvedLocationStore()
        val repo = GeolocationRepositoryImpl(api = fakeApi, store = store)

        val result = repo.reverseGeocode(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val location = result.getOrNull()
        assertEquals("Zacapa, Guatemala", location?.display?.primary)
        assertEquals("Zacapa", location?.display?.municipality)
        assertEquals("Zacapa Department", location?.display?.state)
        assertEquals("Guatemala", location?.display?.country)
    }

    @Test
    fun `reverseGeocode preserves null elevation`() = runTest {
        val fakeApi = FakeGeolocationApi(
            reverseResponse = ReverseGeocodeResponse(
                displayName = "Zacapa",
                lat = 14.9726,
                lng = -89.5301,
                municipality = null,
                state = null,
                country = null,
            )
        )
        val store = com.agrogem.app.data.geolocation.ResolvedLocationStore()
        val repo = GeolocationRepositoryImpl(api = fakeApi, store = store)

        val result = repo.reverseGeocode(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val location = result.getOrNull()
        assertNull(location?.elevationMeters)
    }

    @Test
    fun `saveResolvedLocation then observe returns it`() = runTest {
        val fakeApi = FakeGeolocationApi()
        val store = com.agrogem.app.data.geolocation.ResolvedLocationStore()
        val repo = GeolocationRepositoryImpl(api = fakeApi, store = store)

        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = 230.5,
        )
        repo.saveResolvedLocation(location)

        val observed = repo.observeResolvedLocation().first()
        assertEquals(location, observed)
    }

    @Test
    fun `observeResolvedLocation returns null when store empty`() = runTest {
        val fakeApi = FakeGeolocationApi()
        val store = com.agrogem.app.data.geolocation.ResolvedLocationStore()
        store.clear()
        val repo = GeolocationRepositoryImpl(api = fakeApi, store = store)

        val observed = repo.observeResolvedLocation().first()
        assertNull(observed)
    }

    @Test
    fun `reverseGeocode sets elevationMeters when elevation succeeds`() = runTest {
        val fakeApi = FakeGeolocationApi(
            reverseResponse = ReverseGeocodeResponse(
                displayName = "Zacapa",
                lat = 14.9726,
                lng = -89.5301,
                municipality = null,
                state = null,
                country = null,
            ),
            elevationResponse = ElevationResponse(elevationMeters = 230.5),
        )
        val store = com.agrogem.app.data.geolocation.ResolvedLocationStore()
        val repo = GeolocationRepositoryImpl(api = fakeApi, store = store)

        val result = repo.reverseGeocode(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val location = result.getOrNull()
        assertEquals(230.5, location?.elevationMeters)
    }

    @Test
    fun `reverseGeocode returns success with null elevation when elevation fails`() = runTest {
        val fakeApi = FakeGeolocationApi(
            reverseResponse = ReverseGeocodeResponse(
                displayName = "Zacapa",
                lat = 14.9726,
                lng = -89.5301,
                municipality = null,
                state = null,
                country = null,
            ),
            shouldThrowOnElevation = true,
        )
        val store = com.agrogem.app.data.geolocation.ResolvedLocationStore()
        val repo = GeolocationRepositoryImpl(api = fakeApi, store = store)

        val result = repo.reverseGeocode(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val location = result.getOrNull()
        assertNull(location?.elevationMeters)
    }

    private class FakeGeolocationApi(
        private val geocodeHit: GeocodeHit? = null,
        private val reverseResponse: ReverseGeocodeResponse = ReverseGeocodeResponse(
            displayName = null,
            lat = 0.0,
            lng = 0.0,
        ),
        private val elevationResponse: ElevationResponse = ElevationResponse(elevationMeters = null),
        private val shouldThrowOnElevation: Boolean = false,
    ) : GeolocationApi {
        override suspend fun geocode(query: String): GeocodeHit? = geocodeHit
        override suspend fun reverseGeocode(lat: Double, lng: Double): ReverseGeocodeResponse = reverseResponse
        override suspend fun elevation(lat: Double, lng: Double): ElevationResponse {
            if (shouldThrowOnElevation) throw RuntimeException("Elevation failed")
            return elevationResponse
        }
    }
}
