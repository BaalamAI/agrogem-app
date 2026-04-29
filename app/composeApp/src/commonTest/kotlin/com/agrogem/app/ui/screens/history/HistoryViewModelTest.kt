package com.agrogem.app.ui.screens.history

import com.agrogem.app.data.analysis.domain.AnalysisRepository
import com.agrogem.app.data.analysis.domain.StoredAnalysis
import com.agrogem.app.data.pest.domain.AnalysisDiagnosis
import com.agrogem.app.ui.components.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh loads persisted analyses into ui state`() = runTest(testDispatcher) {
        val repository = FakeAnalysisRepository(
            items = listOf(
                storedAnalysis(
                    id = "analysis_1",
                    pestName = "Mildiu",
                    severity = "Alta",
                ),
            ),
        )
        val viewModel = HistoryViewModel(repository)

        advanceUntilIdle()

        val entry = viewModel.uiState.value.entries.single()
        assertEquals("analysis_1", entry.analysisId)
        assertEquals("Mildiu", entry.crop)
        assertEquals("ALTA", entry.status)
        assertEquals(Severity.Critica, entry.severity)
        assertNotEquals("1000", entry.meta)
    }

    private class FakeAnalysisRepository(
        private val items: List<StoredAnalysis>,
    ) : AnalysisRepository {
        override suspend fun save(analysis: StoredAnalysis) = Unit
        override suspend fun getById(analysisId: String): StoredAnalysis? = items.firstOrNull { it.analysisId == analysisId }
        override suspend fun listRecent(limit: Long): List<StoredAnalysis> = items.take(limit.toInt())
    }
}

private fun storedAnalysis(
    id: String,
    pestName: String,
    severity: String,
): StoredAnalysis = StoredAnalysis(
    analysisId = id,
    imageUri = "content://$id.jpg",
    diagnosis = AnalysisDiagnosis(
        pestName = pestName,
        confidence = 0.92f,
        severity = severity,
        affectedArea = "Hojas",
        cause = "Hongo",
        diagnosisText = "Diagnóstico",
        treatmentSteps = listOf("Paso 1"),
    ),
    createdAtEpochMillis = 1000L,
)
