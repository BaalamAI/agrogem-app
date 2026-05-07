package com.agrogem.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GemmaPreparationTest {

    @Test
    fun `ensureReady downloads and initializes when needed`() = runTest {
        val manager = FakeGemmaManager()
        val downloader = FakeGemmaModelDownloader(downloaded = false)
        val holder = GemmaPreparation(manager, downloader)

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
        val holder = GemmaPreparation(manager, downloader)

        val result = holder.ensureReady()

        assertTrue(!result)
        assertIs<GemmaPreparationStatus.Unavailable>(holder.status.value)
    }

    @Test
    fun `ensureReady exposes download progress`() = runTest {
        val manager = FakeGemmaManager()
        val downloader = FakeGemmaModelDownloader(downloaded = false, progress = 42, downloadReadyAfterChecks = 1)
        val holder = GemmaPreparation(manager, downloader)
        val progressValues = mutableListOf<Int?>()
        val progressJob = launch {
            holder.downloadProgress.collect { progressValues += it }
        }

        holder.ensureReady()
        progressJob.cancel()

        assertTrue(progressValues.contains(42))
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
            toolBundle: GemmaToolBundle?,
        ): String = ""

        override fun sendMessageStream(
            systemPrompt: String,
            userPrompt: String,
            images: List<String>,
            audioPath: String?,
            temperature: Float,
            toolBundle: GemmaToolBundle?,
        ): Flow<GemmaResponse> = flowOf()

        override fun startChatSession(
            systemPrompt: String,
            temperature: Float,
            toolBundle: GemmaToolBundle?,
        ): GemmaChatSession = object : GemmaChatSession {
            override fun sendMessage(text: String, images: List<String>): Flow<GemmaResponse> = flowOf()
            override fun close() {}
        }

        override fun close() {}
    }

    private class FakeGemmaModelDownloader(
        private var downloaded: Boolean,
        private val downloadShouldFail: Boolean = false,
        progress: Int? = null,
        private val downloadReadyAfterChecks: Int = 0,
    ) : GemmaModelDownloader {
        var downloadCalled = false
        private var downloadChecks = 0
        override val downloadProgress: Flow<Int?> = flowOf(progress)

        override suspend fun downloadModel(url: String): Result<String> {
            downloadCalled = true
            return if (downloadShouldFail) {
                Result.failure(IllegalStateException("download failed"))
            } else {
                if (downloadReadyAfterChecks == 0) {
                    downloaded = true
                }
                Result.success("/tmp/model.litertlm")
            }
        }

        override fun isModelDownloaded(): Boolean {
            if (!downloaded && downloadCalled && downloadReadyAfterChecks > 0) {
                downloadChecks++
                if (downloadChecks > downloadReadyAfterChecks) {
                    downloaded = true
                }
            }
            return downloaded
        }

        override fun getModelPath(): String = "/tmp/model.litertlm"
    }
}
