package com.agrogem.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GemmaPreparationStateHolderTest {

    @Test
    fun `ensureReady downloads and initializes when needed`() = runTest {
        val manager = FakeGemmaManager()
        val downloader = FakeGemmaModelDownloader(downloaded = false)
        val holder = GemmaPreparationStateHolder(manager, downloader)

        val result = holder.ensureReady()

        assertTrue(result)
        assertTrue(downloader.downloadCalled)
        assertEquals(1, manager.initializeCallCount)
        assertIs<GemmaPreparationStatus.Ready>(holder.status.value)
    }

    @Test
    fun `ensureReady becomes unavailable when downloader cannot provide model`() = runTest {
        val manager = FakeGemmaManager()
        val downloader = FakeGemmaModelDownloader(downloaded = false, downloadShouldFail = true)
        val holder = GemmaPreparationStateHolder(manager, downloader)

        val result = holder.ensureReady()

        assertTrue(!result)
        assertIs<GemmaPreparationStatus.Unavailable>(holder.status.value)
    }

    private class FakeGemmaManager : GemmaManager {
        private val initialized = MutableStateFlow(false)
        override val isInitialized: Flow<Boolean> = initialized
        var initializeCallCount = 0

        override suspend fun initialize(modelPath: String) {
            initializeCallCount++
            initialized.value = true
        }

        override suspend fun sendMessage(
            systemPrompt: String,
            userPrompt: String,
            images: List<String>,
            audioPath: String?,
            temperature: Float,
        ): String = ""

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
        private var downloaded: Boolean,
        private val downloadShouldFail: Boolean = false,
    ) : GemmaModelDownloader {
        var downloadCalled = false

        override suspend fun downloadModel(url: String): Result<String> {
            downloadCalled = true
            return if (downloadShouldFail) {
                Result.failure(IllegalStateException("download failed"))
            } else {
                downloaded = true
                Result.success("/tmp/model.litertlm")
            }
        }

        override fun isModelDownloaded(): Boolean = downloaded

        override fun getModelPath(): String = "/tmp/model.litertlm"
    }
}
