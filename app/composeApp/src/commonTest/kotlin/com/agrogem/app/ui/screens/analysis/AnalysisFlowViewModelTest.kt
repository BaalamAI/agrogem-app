package com.agrogem.app.ui.screens.analysis

import com.agrogem.app.data.ImageResult
import com.agrogem.app.data.pest.domain.PestFailure
import com.agrogem.app.data.pest.domain.PestRepository
import com.agrogem.app.data.pest.domain.PestResult
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

    private fun fakeRepo(result: PestResult = PestResult.Success(mockDiagnosisResult())): FakePestRepository =
        FakePestRepository(result)

    // ========== Mock fallback (null repository) ==========

    @Test
    fun `null repository falls back to simulated analysis via startAnalysis`() = runTest(testDispatcher) {
        val viewModel = AnalysisFlowViewModel(pestRepository = null)
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.startAnalysis(image)
        advanceUntilIdle()

        assertIs<AnalysisPhase.Results>(viewModel.phase.value)
        assertEquals("Plaga detectada", viewModel.diagnosisResult.pestName)
    }

    @Test
    fun `loadFromHistory bypasses network and shows results`() = runTest(testDispatcher) {
        val viewModel = AnalysisFlowViewModel(pestRepository = fakeRepo())

        viewModel.loadFromHistory(imageUri = "content://history.jpg")

        assertIs<AnalysisPhase.Results>(viewModel.phase.value)
        assertEquals("content://history.jpg", viewModel.capturedImage.value?.uri)
        assertTrue(viewModel.steps.value.all { it.done })
    }

    // ========== Real repository path ==========

    @Test
    fun `startAnalysis with repository transitions through steps to results`() = runTest(testDispatcher) {
        val repo = fakeRepo(PestResult.Success(mockDiagnosisResult(pestName = "Roya")))
        val viewModel = AnalysisFlowViewModel(pestRepository = repo)
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.setCapturedImage(image)
        viewModel.startAnalysis(image)

        assertIs<AnalysisPhase.Analyzing>(viewModel.phase.value)
        assertEquals(3, viewModel.steps.value.size)

        advanceUntilIdle()

        assertIs<AnalysisPhase.Results>(viewModel.phase.value)
        assertEquals("Roya", viewModel.diagnosisResult.pestName)
        assertEquals("content://test.jpg", viewModel.capturedImage.value?.uri)
    }

    @Test
    fun `startAnalysis transitions steps in order`() = runTest(testDispatcher) {
        val repo = fakeRepo(PestResult.Success(mockDiagnosisResult()))
        val viewModel = AnalysisFlowViewModel(pestRepository = repo)
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
        val viewModel = AnalysisFlowViewModel(pestRepository = failRepo)
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.setCapturedImage(image)
        viewModel.startAnalysis(image)
        advanceUntilIdle()

        val errorPhase = viewModel.phase.value
        assertIs<AnalysisPhase.Error>(errorPhase)
        assertTrue(errorPhase.retryable)

        // Retry with success repo
        viewModel.setPestRepository(successRepo)
        viewModel.startAnalysis(image)
        advanceUntilIdle()

        assertIs<AnalysisPhase.Results>(viewModel.phase.value)
        assertEquals("Mildiu", viewModel.diagnosisResult.pestName)
    }

    @Test
    fun `startAnalysis with no match error shows non-retryable error`() = runTest(testDispatcher) {
        val repo = fakeRepo(PestResult.Failure(PestFailure.NoMatchFound))
        val viewModel = AnalysisFlowViewModel(pestRepository = repo)
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.startAnalysis(image)
        advanceUntilIdle()

        val errorPhase = viewModel.phase.value
        assertIs<AnalysisPhase.Error>(errorPhase)
        assertEquals(false, errorPhase.retryable)
    }

    @Test
    fun `cancelAnalysis resets state`() = runTest(testDispatcher) {
        val repo = fakeRepo(PestResult.Success(mockDiagnosisResult()))
        val viewModel = AnalysisFlowViewModel(pestRepository = repo)
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.setCapturedImage(image)
        viewModel.startAnalysis(image)
        viewModel.cancelAnalysis()

        assertIs<AnalysisPhase.Analyzing>(viewModel.phase.value)
        assertNull(viewModel.capturedImage.value)
        assertTrue(viewModel.steps.value.none { it.done })
    }

    @Test
    fun `clearAll resets state`() = runTest(testDispatcher) {
        val repo = fakeRepo(PestResult.Success(mockDiagnosisResult()))
        val viewModel = AnalysisFlowViewModel(pestRepository = repo)
        val image = ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3))

        viewModel.setCapturedImage(image)
        viewModel.startAnalysis(image)
        advanceUntilIdle()
        assertIs<AnalysisPhase.Results>(viewModel.phase.value)

        viewModel.clearAll()

        assertIs<AnalysisPhase.Analyzing>(viewModel.phase.value)
        assertNull(viewModel.capturedImage.value)
        assertTrue(viewModel.steps.value.none { it.done })
    }

    // ========== Fakes ==========

    private class FakePestRepository(var result: PestResult) : PestRepository {
        override suspend fun identify(image: ImageResult): PestResult = result
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
