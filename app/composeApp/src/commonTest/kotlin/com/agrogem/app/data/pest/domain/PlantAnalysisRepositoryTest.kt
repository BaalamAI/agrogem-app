package com.agrogem.app.data.pest.domain

import com.agrogem.app.data.GemmaManager
import com.agrogem.app.data.GemmaModelDownloader
import com.agrogem.app.data.GemmaResponse
import com.agrogem.app.data.ImageResult
import com.agrogem.app.data.connectivity.ConnectivityMonitor
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PlantAnalysisRepositoryTest {

    @Test
    fun `analyze returns gemma result when gemma and backend both succeed`() = runTest {
        val gemma = FakeGemmaManager(response = validGemmaJson())
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(
            PestResult.Success(backendDiagnosis(pestName = "BackendPest", confidence = 0.6f))
        )
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("GemmaPest", result.diagnosis.pestName)
        assertEquals(0.92f, result.diagnosis.confidence)
        assertTrue(pestRepo.wasCalled)
    }

    @Test
    fun `analyze returns gemma result even when backend fails`() = runTest {
        val gemma = FakeGemmaManager(response = validGemmaJson())
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(PestResult.Failure(PestFailure.Network(Exception("timeout"))))
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("GemmaPest", result.diagnosis.pestName)
    }

    @Test
    fun `analyze falls back to backend when gemma fails`() = runTest {
        val gemma = FakeGemmaManager(throwOnSend = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(
            PestResult.Success(backendDiagnosis(pestName = "FallbackPest", confidence = 0.7f))
        )
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("FallbackPest", result.diagnosis.pestName)
        assertEquals(0.7f, result.diagnosis.confidence)
    }

    @Test
    fun `analyze returns backend failure when gemma fails and backend also fails`() = runTest {
        val gemma = FakeGemmaManager(throwOnSend = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(PestResult.Failure(PestFailure.Server))
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Failure>(result)
        assertIs<PestFailure.Server>(result.reason)
    }

    @Test
    fun `analyze falls back to backend when model is not downloaded`() = runTest {
        val gemma = FakeGemmaManager(response = validGemmaJson())
        val downloader = FakeGemmaModelDownloader(downloaded = false)
        val pestRepo = FakePestRepository(
            PestResult.Success(backendDiagnosis(pestName = "BackendOnly", confidence = 0.5f))
        )
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("BackendOnly", result.diagnosis.pestName)
    }

    @Test
    fun `analyze returns network error when model not downloaded and backend fails`() = runTest {
        val gemma = FakeGemmaManager(response = validGemmaJson())
        val downloader = FakeGemmaModelDownloader(downloaded = false)
        val pestRepo = FakePestRepository(PestResult.Failure(PestFailure.Network(Exception("offline"))))
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Failure>(result)
        assertIs<PestFailure.Network>(result.reason)
    }

    @Test
    fun `analyze parses gemma json wrapped in markdown backticks`() = runTest {
        val gemma = FakeGemmaManager(response = "```json\n${validGemmaJson()}\n```")
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(
            PestResult.Success(backendDiagnosis(pestName = "BackendPest", confidence = 0.6f))
        )
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("GemmaPest", result.diagnosis.pestName)
    }

    @Test
    fun `analyze parses gemma json with prose before and after markdown fences`() = runTest {
        val prose = "Claro, aquí está el análisis:\n\n```json\n${validGemmaJson()}\n```\n\nEspero que esto ayude."
        val gemma = FakeGemmaManager(response = prose)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(PestResult.Failure(PestFailure.Server))
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("GemmaPest", result.diagnosis.pestName)
    }

    @Test
    fun `analyze parses gemma json with prose inside markdown fences`() = runTest {
        val prose = "```json\nAquí tienes:\n${validGemmaJson()}\n```"
        val gemma = FakeGemmaManager(response = prose)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(PestResult.Failure(PestFailure.Server))
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("GemmaPest", result.diagnosis.pestName)
    }

    @Test
    fun `analyze parses gemma json with prose but no markdown fences`() = runTest {
        val prose = "Basado en la imagen, el diagnóstico es: ${validGemmaJson()} ¿Tenés alguna pregunta?"
        val gemma = FakeGemmaManager(response = prose)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(PestResult.Failure(PestFailure.Server))
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("GemmaPest", result.diagnosis.pestName)
    }

    @Test
    fun `analyze normalizes english severity variants`() = runTest {
        val gemma = FakeGemmaManager(response = gemmaJsonWith(severity = "high"))
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(PestResult.Failure(PestFailure.Server))
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("Alta", result.diagnosis.severity)
    }

    @Test
    fun `analyze normalizes mixed case severity`() = runTest {
        val gemma = FakeGemmaManager(response = gemmaJsonWith(severity = "SeVeRe"))
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(PestResult.Failure(PestFailure.Server))
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("Alta", result.diagnosis.severity)
    }

    @Test
    fun `analyze defaults unknown severity to Media`() = runTest {
        val gemma = FakeGemmaManager(response = gemmaJsonWith(severity = "desconocido"))
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(PestResult.Failure(PestFailure.Server))
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("Media", result.diagnosis.severity)
    }

    @Test
    fun `analyze falls back to backend when gemma returns invalid json`() = runTest {
        val gemma = FakeGemmaManager(response = "esto no es json")
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(
            PestResult.Success(backendDiagnosis(pestName = "FallbackPest", confidence = 0.8f))
        )
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("FallbackPest", result.diagnosis.pestName)
    }

    @Test
    fun `analyze falls back to backend when gemma returns json with blank pestName`() = runTest {
        val gemma = FakeGemmaManager(response = """
            {"pestName":"","confidence":0.5,"severity":"Media","affectedArea":"Hoja","cause":"","diagnosisText":"","treatmentSteps":[]}
        """.trimIndent())
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(
            PestResult.Success(backendDiagnosis(pestName = "FallbackPest", confidence = 0.8f))
        )
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("FallbackPest", result.diagnosis.pestName)
    }

    @Test
    fun `analyze coerces confidence outside zero_to_one range`() = runTest {
        val gemma = FakeGemmaManager(response = """
            {"pestName":"HighConf","confidence":1.5,"severity":"Alta","affectedArea":"Tallo","cause":"Hongo","diagnosisText":"Diag","treatmentSteps":["Paso 1"]}
        """.trimIndent())
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(PestResult.Failure(PestFailure.Server))
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals(1f, result.diagnosis.confidence)
    }

    @Test
    fun `analyze coerces negative confidence to 0`() = runTest {
        val gemma = FakeGemmaManager(response = """
            {"pestName":"NegConf","confidence":-0.3,"severity":"Baja","affectedArea":"Raíz","cause":"Bacteria","diagnosisText":"Diag","treatmentSteps":["Paso 1"]}
        """.trimIndent())
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(PestResult.Failure(PestFailure.Server))
        val connectivity = FakeConnectivityMonitor(online = true)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals(0f, result.diagnosis.confidence)
    }

    @Test
    fun `analyze skips backend when offline and returns gemma result`() = runTest {
        val gemma = FakeGemmaManager(response = validGemmaJson())
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(
            PestResult.Success(backendDiagnosis(pestName = "BackendPest", confidence = 0.6f))
        )
        val connectivity = FakeConnectivityMonitor(online = false)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("GemmaPest", result.diagnosis.pestName)
        assertFalse(pestRepo.wasCalled)
    }

    @Test
    fun `analyze returns network error when offline and gemma not ready`() = runTest {
        val gemma = FakeGemmaManager(response = validGemmaJson())
        val downloader = FakeGemmaModelDownloader(downloaded = false)
        val pestRepo = FakePestRepository(
            PestResult.Success(backendDiagnosis(pestName = "BackendPest", confidence = 0.6f))
        )
        val connectivity = FakeConnectivityMonitor(online = false)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Failure>(result)
        assertIs<PestFailure.Network>(result.reason)
        assertFalse(pestRepo.wasCalled)
    }

    @Test
    fun `analyze returns server error when offline and gemma fails`() = runTest {
        val gemma = FakeGemmaManager(throwOnSend = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val pestRepo = FakePestRepository(
            PestResult.Success(backendDiagnosis(pestName = "FallbackPest", confidence = 0.7f))
        )
        val connectivity = FakeConnectivityMonitor(online = false)
        val repo = PlantAnalysisRepositoryImpl(gemma, downloader, pestRepo, connectivity)

        val result = repo.analyze(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Failure>(result)
        assertIs<PestFailure.Server>(result.reason)
        assertFalse(pestRepo.wasCalled)
    }

    // ========== Fakes ==========

    private class FakeGemmaManager(
        private val response: String = "",
        private val throwOnSend: Boolean = false,
    ) : GemmaManager {
        override val isInitialized: Flow<Boolean> = flowOf(true)
        var lastSystemPrompt: String? = null

        override suspend fun initialize(modelPath: String) {}

        override suspend fun sendMessage(
            systemPrompt: String,
            userPrompt: String,
            images: List<String>,
            audioPath: String?,
            temperature: Float,
        ): String {
            if (throwOnSend) throw RuntimeException("Gemma error")
            lastSystemPrompt = systemPrompt
            return response
        }

        override fun sendMessageStream(
            systemPrompt: String,
            userPrompt: String,
            images: List<String>,
            audioPath: String?,
            temperature: Float,
        ): Flow<GemmaResponse> = flowOf()

        override fun close() {}
    }

    private class FakeGemmaModelDownloader(
        private val downloaded: Boolean = true,
    ) : GemmaModelDownloader {
        override suspend fun downloadModel(url: String): Result<String> = Result.success("")
        override fun isModelDownloaded(): Boolean = downloaded
        override fun getModelPath(): String = "/fake/model.path"
    }

    private class FakePestRepository(private val result: PestResult) : PestRepository {
        var wasCalled = false
        override suspend fun identify(image: ImageResult): PestResult {
            wasCalled = true
            return result
        }
    }

    private class FakeConnectivityMonitor(private val online: Boolean) : ConnectivityMonitor {
        override fun isOnline(): Boolean = online
    }

    private fun validGemmaJson(): String = """
        {
            "pestName": "GemmaPest",
            "confidence": 0.92,
            "severity": "Alta",
            "affectedArea": "Hojas",
            "cause": "Hongo",
            "diagnosisText": "Diagnóstico por Gemma.",
            "treatmentSteps": ["Paso 1", "Paso 2"]
        }
    """.trimIndent()

    private fun gemmaJsonWith(severity: String): String = """
        {
            "pestName": "GemmaPest",
            "confidence": 0.92,
            "severity": "$severity",
            "affectedArea": "Hojas",
            "cause": "Hongo",
            "diagnosisText": "Diagnóstico por Gemma.",
            "treatmentSteps": ["Paso 1", "Paso 2"]
        }
    """.trimIndent()

    private fun backendDiagnosis(pestName: String, confidence: Float): DiagnosisResult = DiagnosisResult(
        pestName = pestName,
        confidence = confidence,
        severity = "Media",
        affectedArea = "Tallo",
        cause = pestName,
        diagnosisText = "Diagnóstico backend.",
        treatmentSteps = listOf("Tratamiento A"),
    )
}
