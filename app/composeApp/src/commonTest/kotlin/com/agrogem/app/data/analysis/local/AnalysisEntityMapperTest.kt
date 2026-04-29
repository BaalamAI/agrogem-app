package com.agrogem.app.data.analysis.local

import com.agrogem.app.data.analysis.domain.StoredAnalysis
import com.agrogem.app.data.local.db.Analysis
import com.agrogem.app.data.pest.domain.AnalysisDiagnosis
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalysisEntityMapperTest {

    private val mapper = AnalysisEntityMapper()

    @Test
    fun `toParams serializes diagnosis fields`() {
        val stored = sampleStoredAnalysis()

        val params = mapper.toParams(stored)

        assertEquals("analysis_123", params.analysisId)
        assertEquals("content://leaf.jpg", params.imageUri)
        assertEquals("Mildiu", params.pestName)
        assertTrue(abs(params.confidence - 0.89) < 0.0001)
        assertEquals(1L, params.isConfidenceReliable)
        assertTrue(params.treatmentStepsJson.contains("Aplicar fungicida"))
    }

    @Test
    fun `toStoredAnalysis maps sql entity to domain model`() {
        val entity = Analysis(
            analysis_id = "analysis_123",
            image_uri = "content://leaf.jpg",
            pest_name = "Mildiu",
            confidence = 0.89,
            severity = "Alta",
            affected_area = "Hojas",
            cause = "Hongo",
            diagnosis_text = "Infección foliar",
            treatment_steps_json = "[\"Aplicar fungicida\",\"Monitorear 48h\"]",
            is_confidence_reliable = 1L,
            created_at_epoch_millis = 1714300000000L,
        )

        val stored = mapper.toStoredAnalysis(entity)

        assertEquals("analysis_123", stored.analysisId)
        assertEquals("content://leaf.jpg", stored.imageUri)
        assertEquals("Mildiu", stored.diagnosis.pestName)
        assertEquals(listOf("Aplicar fungicida", "Monitorear 48h"), stored.diagnosis.treatmentSteps)
        assertTrue(stored.diagnosis.isConfidenceReliable)
    }

    private fun sampleStoredAnalysis(): StoredAnalysis = StoredAnalysis(
        analysisId = "analysis_123",
        imageUri = "content://leaf.jpg",
        diagnosis = AnalysisDiagnosis(
            pestName = "Mildiu",
            confidence = 0.89f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección foliar",
            treatmentSteps = listOf("Aplicar fungicida", "Monitorear 48h"),
            isConfidenceReliable = true,
        ),
        createdAtEpochMillis = 1714300000000,
    )
}
