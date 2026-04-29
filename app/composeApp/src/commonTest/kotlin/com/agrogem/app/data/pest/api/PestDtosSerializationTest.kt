package com.agrogem.app.data.pest.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PestDtosSerializationTest {

    @Test
    fun `PestIdentifyResponse decodes weighted score and votes as doubles`() {
        val json = Json { ignoreUnknownKeys = true }
        val payload = """
            {
              "top_match": {
                "pest_name": "Spodoptera_litura",
                "similarity": 0.87,
                "weighted_score": 0.91,
                "confidence": "high"
              },
              "votes": {
                "Spodoptera_litura": 0.91,
                "Helicoverpa_armigera": 0.42
              }
            }
        """.trimIndent()

        val result = json.decodeFromString<PestIdentifyResponse>(payload)

        assertEquals(0.91, result.topMatch?.weightedScore)
        assertEquals(mapOf("Spodoptera_litura" to 0.91, "Helicoverpa_armigera" to 0.42), result.votes)
    }
}
