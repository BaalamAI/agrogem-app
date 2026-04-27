package com.agrogem.app.data.geolocation.domain

import com.agrogem.app.data.geolocation.ResolvedLocationStore
import com.agrogem.app.data.shared.domain.LatLng
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolvedLocationStoreTest {

    @Test
    fun `observe returns null when empty`() = runTest {
        val store = ResolvedLocationStore()
        store.clear()
        val result = store.observe().first()
        assertNull(result)
    }

    @Test
    fun `write then observe returns same location`() = runTest {
        val store = ResolvedLocationStore()
        val expected = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = 230.5,
        )
        store.write(expected)
        val result = store.observe().first()
        assertEquals(expected, result)
    }

    @Test
    fun `clear resets to null`() = runTest {
        val store = ResolvedLocationStore()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        store.write(location)
        store.clear()
        val result = store.observe().first()
        assertNull(result)
    }
}
