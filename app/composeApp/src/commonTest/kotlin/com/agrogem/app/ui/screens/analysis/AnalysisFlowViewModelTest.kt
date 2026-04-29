package com.agrogem.app.ui.screens.analysis

import com.agrogem.app.data.ImageResult
import com.agrogem.app.data.analysis.domain.AnalysisRepository
import com.agrogem.app.data.analysis.domain.StoredAnalysis
import com.agrogem.app.data.pest.domain.PestFailure
import com.agrogem.app.data.pest.domain.PestResult
import com.agrogem.app.data.pest.domain.PlantAnalysisRepository
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisFlowViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeRepo(result: PestResult = PestResult.Success(mockDiagnosisResult())): FakePlantAnalysisRepository =
        FakePlantAnalysisRepository(result)

    private fun fakeAnalysisRepo(): FakeAnalysisRepository = FakeAnalysisRepository()

    @Test
    fun `loadFromHistory bypasses network and shows results`() = runTest(testDispatcher) {
        val viewModel = AnalysisFlowViewModel(plantAnalysisRepository = fakeRepo(), analysisRepository = fakeAnalysisRepo())

        viewModel.loadFromHistory(imageUri = "content://history.jpg")

        assertIs<AnalysisPhase.Results>(viewModel.phase.value)
        assertEquals("content://history.jpg", viewModel.capturedImage.value?.uri)
        assertTrue(viewModel.steps.value.all { it.done })
    }

    @Test
    fun `startAnalysis transitions through steps to results`() = runTest(testDispatcher) {
        val repo = fakeRepo(PestResult.Success(mockDiagnosisResult(pestName = "Roya")))
        val analysisRepository = fakeAnalysisRepo()
        val viewModel = AnalysisFlowViewModel(plantAnalysisRepository = repo, analysisRepository = analysisRepository)
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.setCapturedImage(image)
        viewModel.startAnalysis(image)

        assertIs<AnalysisPhase.Analyzing>(viewModel.phase.value)
        assertEquals(3, viewModel.steps.value.size)

        advanceUntilIdle()

        assertIs<AnalysisPhase.Results>(viewModel.phase.value)
        assertEquals("Roya", viewModel.diagnosisResult.pestName)
        assertEquals("content://test.jpg", viewModel.capturedImage.value?.uri)
        assertNotNull(viewModel.analysisId.value)
        assertTrue(viewModel.analysisId.value!!.startsWith("analysis_"))
        assertEquals(1, analysisRepository.saved.size)
    }

    @Test
    fun `startAnalysis transitions steps in order`() = runTest(testDispatcher) {
        val repo = fakeRepo(PestResult.Success(mockDiagnosisResult()))
        val viewModel = AnalysisFlowViewModel(plantAnalysisRepository = repo, analysisRepository = fakeAnalysisRepo())
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.startAnalysis(image)

        val stepsBefore = viewModel.steps.value
        assertEquals("Subiendo imagen...", stepsBefore[0].title)
        assertEquals("Identificando plaga...", stepsBefore[1].title)
        assertEquals("Procesando resultados...", stepsBefore[2].title)
        assertTrue(stepsBefore.none { it.done })

        advanceUntilIdle()

        val stepsAfter = viewModel.steps.value
        assertTrue(stepsAfter.all { it.done })
    }

    @Test
    fun `startAnalysis with network error transitions to error then retry to results`() = runTest(testDispatcher) {
        val failRepo = fakeRepo(PestResult.Failure(PestFailure.Network(Exception("timeout"))))
        val successRepo = fakeRepo(PestResult.Success(mockDiagnosisResult(pestName = "Mildiu")))

        val viewModel = AnalysisFlowViewModel(plantAnalysisRepository = failRepo, analysisRepository = fakeAnalysisRepo())
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.setCapturedImage(image)
        viewModel.startAnalysis(image)
        advanceUntilIdle()

        val errorPhase = viewModel.phase.value
        assertIs<AnalysisPhase.Error>(errorPhase)
        assertTrue(errorPhase.retryable)

        // Retry with success repo (new ViewModel simulates retry with working repository)
        val retryViewModel = AnalysisFlowViewModel(plantAnalysisRepository = successRepo, analysisRepository = fakeAnalysisRepo())
        retryViewModel.setCapturedImage(image)
        retryViewModel.startAnalysis(image)
        advanceUntilIdle()

        assertIs<AnalysisPhase.Results>(retryViewModel.phase.value)
        assertEquals("Mildiu", retryViewModel.diagnosisResult.pestName)
    }

    @Test
    fun `startAnalysis with no match error shows non-retryable error`() = runTest(testDispatcher) {
        val repo = fakeRepo(PestResult.Failure(PestFailure.NoMatchFound))
        val viewModel = AnalysisFlowViewModel(plantAnalysisRepository = repo, analysisRepository = fakeAnalysisRepo())
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.startAnalysis(image)
        advanceUntilIdle()

        val errorPhase = viewModel.phase.value
        assertIs<AnalysisPhase.Error>(errorPhase)
        assertEquals(false, errorPhase.retryable)
    }

    @Test
    fun `cancelAnalysis resets state including analysisId`() = runTest(testDispatcher) {
        val repo = fakeRepo(PestResult.Success(mockDiagnosisResult()))
        val viewModel = AnalysisFlowViewModel(plantAnalysisRepository = repo, analysisRepository = fakeAnalysisRepo())
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.setCapturedImage(image)
        viewModel.startAnalysis(image)
        viewModel.cancelAnalysis()

        assertIs<AnalysisPhase.Analyzing>(viewModel.phase.value)
        assertNull(viewModel.capturedImage.value)
        assertTrue(viewModel.steps.value.none { it.done })
        assertNull(viewModel.analysisId.value)
    }

    @Test
    fun `clearAll resets state including analysisId`() = runTest(testDispatcher) {
        val repo = fakeRepo(PestResult.Success(mockDiagnosisResult()))
        val viewModel = AnalysisFlowViewModel(plantAnalysisRepository = repo, analysisRepository = fakeAnalysisRepo())
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.setCapturedImage(image)
        viewModel.startAnalysis(image)
        advanceUntilIdle()
        assertIs<AnalysisPhase.Results>(viewModel.phase.value)
        assertNotNull(viewModel.analysisId.value)

        viewModel.clearAll()

        assertIs<AnalysisPhase.Analyzing>(viewModel.phase.value)
        assertNull(viewModel.capturedImage.value)
        assertTrue(viewModel.steps.value.none { it.done })
        assertNull(viewModel.analysisId.value)
    }

    @Test
    fun `loadFromHistory leaves analysisId null`() = runTest(testDispatcher) {
        val viewModel = AnalysisFlowViewModel(plantAnalysisRepository = fakeRepo(), analysisRepository = fakeAnalysisRepo())

        viewModel.loadFromHistory(imageUri = "content://history.jpg")

        assertIs<AnalysisPhase.Results>(viewModel.phase.value)
        assertNull(viewModel.analysisId.value)
    }

    @Test
    fun `loadFromHistory clears stale analysis context when no hydration payload provided`() = runTest(testDispatcher) {
        val staleDiagnosis = mockDiagnosisResult(pestName = "Stale")
        val repo = fakeRepo(PestResult.Success(staleDiagnosis))
        val viewModel = AnalysisFlowViewModel(plantAnalysisRepository = repo, analysisRepository = fakeAnalysisRepo())
        val image = ImageResult(uri = "content://fresh.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.startAnalysis(image)
        advanceUntilIdle()
        assertEquals("Stale", viewModel.diagnosisResult.pestName)
        assertNotNull(viewModel.analysisId.value)

        viewModel.loadFromHistory(imageUri = "content://history.jpg")

        assertNull(viewModel.analysisId.value)
        assertEquals("Esperando análisis", viewModel.diagnosisResult.pestName)
    }

    @Test
    fun `loadFromHistory hydrates analysis context when payload is provided`() = runTest(testDispatcher) {
        val viewModel = AnalysisFlowViewModel(plantAnalysisRepository = fakeRepo(), analysisRepository = fakeAnalysisRepo())
        val diagnosis = mockDiagnosisResult(pestName = "Mildiu")

        viewModel.loadFromHistory(
            imageUri = "content://history.jpg",
            analysisId = "analysis_hist_123",
            diagnosis = diagnosis,
        )

        assertEquals("analysis_hist_123", viewModel.analysisId.value)
        assertEquals("Mildiu", viewModel.diagnosisResult.pestName)
    }

    @Test
    fun `loadFromPersistedAnalysis hydrates from repository data`() = runTest(testDispatcher) {
        val analysisRepository = fakeAnalysisRepo().apply {
            byId["analysis_hist_001"] = StoredAnalysis(
                analysisId = "analysis_hist_001",
                imageUri = "content://persisted.jpg",
                diagnosis = mockDiagnosisResult(pestName = "Tizón"),
                createdAtEpochMillis = 123L,
            )
        }
        val viewModel = AnalysisFlowViewModel(plantAnalysisRepository = fakeRepo(), analysisRepository = analysisRepository)

        viewModel.loadFromPersistedAnalysis("analysis_hist_001")
        advanceUntilIdle()

        assertEquals("analysis_hist_001", viewModel.analysisId.value)
        assertEquals("content://persisted.jpg", viewModel.capturedImage.value?.uri)
        assertEquals("Tizón", viewModel.diagnosisResult.pestName)
        assertIs<AnalysisPhase.Results>(viewModel.phase.value)
    }

    // ========== Fakes ==========

    private class FakePlantAnalysisRepository(var result: PestResult) : PlantAnalysisRepository {
        override suspend fun analyze(image: ImageResult): PestResult = result
    }

    private class FakeAnalysisRepository : AnalysisRepository {
        val saved = mutableListOf<StoredAnalysis>()
        val byId = mutableMapOf<String, StoredAnalysis>()

        override suspend fun save(analysis: StoredAnalysis) {
            saved += analysis
        }

        override suspend fun getById(analysisId: String): StoredAnalysis? = byId[analysisId]
        override suspend fun listRecent(limit: Long): List<StoredAnalysis> = emptyList()
    }
}

private fun mockDiagnosisResult(
    pestName: String = "Plaga detectada",
    confidence: Float = 0.95f,
    severity: String = "Alta",
): DiagnosisResult = DiagnosisResult(
    pestName = pestName,
    confidence = confidence,
    severity = severity,
    affectedArea = "Hojas",
    cause = "Hongo",
    diagnosisText = "Infección detectada en el cultivo.",
    treatmentSteps = listOf("Aplicar tratamiento A", "Monitorear cada 48h"),
)
