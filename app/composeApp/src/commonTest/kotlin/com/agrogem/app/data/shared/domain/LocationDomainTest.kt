package com.agrogem.app.data.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationDomainTest {

    @Test
    fun `LatLng holds exact latitude and longitude`() {
        val latLng = LatLng(latitude = 14.6349, longitude = -90.5069)
        assertEquals(14.6349, latLng.latitude)
        assertEquals(-90.5069, latLng.longitude)
    }

    @Test
    fun `LocationInfo with null elevation defaults to null`() {
        val info = LocationInfo(
            name = "Guatemala City",
            latLng = LatLng(14.6349, -90.5069),
            elevationMeters = null
        )
        assertNull(info.elevationMeters)
    }

    @Test
    fun `LocationInfo holds elevation when provided`() {
        val info = LocationInfo(
            name = "Guatemala City",
            latLng = LatLng(14.6349, -90.5069),
            elevationMeters = 1500.0
        )
        assertEquals(1500.0, info.elevationMeters)
    }

    @Test
    fun `LocationResult Success holds location info`() {
        val info = LocationInfo(
            name = "Zacapa",
            latLng = LatLng(14.9726, -89.5301),
            elevationMeters = null
        )
        val result: LocationResult = LocationResult.Success(info)
        assertTrue(result is LocationResult.Success)
        assertEquals("Zacapa", result.info.name)
    }

    @Test
    fun `LocationResult NotFound is a valid variant`() {
        val result: LocationResult = LocationResult.NotFound
        assertTrue(result is LocationResult.NotFound)
    }

    @Test
    fun `LocationResult Failure holds throwable`() {
        val cause = Exception("network failure")
        val result: LocationResult = LocationResult.Failure(cause)
        assertTrue(result is LocationResult.Failure)
        assertEquals("network failure", result.cause.message)
    }
}
