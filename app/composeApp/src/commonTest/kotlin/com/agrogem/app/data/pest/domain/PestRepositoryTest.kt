package com.agrogem.app.data.pest.domain

import com.agrogem.app.data.ImageResult
import com.agrogem.app.data.network.ApiError
import com.agrogem.app.data.pest.api.PestApi
import com.agrogem.app.data.toByteArray
import com.agrogem.app.data.pest.api.PestIdentifyResponse
import com.agrogem.app.data.pest.api.PestUploadUrlResponse
import com.agrogem.app.data.pest.api.TopMatch
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PestRepositoryTest {

    @Test
    fun `identify orchestrates 3 steps and maps top match to DiagnosisResult`() = runTest {
        val fakeApi = FakePestApi(
            uploadUrl = PestUploadUrlResponse(
                objectPath = "pests/user-123/image.jpg",
                signedUrl = "https://storage.example.com/upload",
                contentType = "image/jpeg",
                expiresInSeconds = 300,
            ),
            identify = PestIdentifyResponse(
                topMatch = TopMatch(
                    pestName = "Spodoptera_litura",
                    similarity = 0.87,
                    confidence = "high",
                ),
                votes = mapOf("Spodoptera_litura" to 3),
            ),
        )
        val repo: PestRepository = PestRepositoryImpl(api = fakeApi)

        val result = repo.identify(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Success>(result)
        assertEquals("Spodoptera litura", result.diagnosis.pestName)
        assertEquals(0.87f, result.diagnosis.confidence)
        assertEquals("Alta", result.diagnosis.severity)
        assertEquals("Plagas detectadas", result.diagnosis.affectedArea)
        assertEquals("Spodoptera litura", result.diagnosis.cause)
        assertTrue(result.diagnosis.diagnosisText.isNotEmpty())
        assertTrue(result.diagnosis.treatmentSteps.isNotEmpty())
    }

    @Test
    fun `identify returns NoMatchFound when top match is null`() = runTest {
        val fakeApi = FakePestApi(
            uploadUrl = PestUploadUrlResponse(
                objectPath = "pests/user-123/image.jpg",
                signedUrl = "https://storage.example.com/upload",
                contentType = "image/jpeg",
                expiresInSeconds = 300,
            ),
            identify = PestIdentifyResponse(topMatch = null, votes = emptyMap()),
        )
        val repo: PestRepository = PestRepositoryImpl(api = fakeApi)

        val result = repo.identify(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Failure>(result)
        assertIs<PestFailure.NoMatchFound>(result.reason)
    }

    @Test
    fun `identify returns MissingImageBytes when bytes are null`() = runTest {
        val fakeApi = FakePestApi()
        val repo: PestRepository = PestRepositoryImpl(api = fakeApi)

        val result = repo.identify(ImageResult(uri = "content://test.jpg", bytes = null))

        assertIs<PestResult.Failure>(result)
        assertIs<PestFailure.MissingImageBytes>(result.reason)
    }

    @Test
    fun `identify returns Network on ApiError NetworkError`() = runTest {
        val fakeApi = FakePestApi(uploadError = ApiError.NetworkError(Exception("timeout")))
        val repo: PestRepository = PestRepositoryImpl(api = fakeApi)

        val result = repo.identify(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Failure>(result)
        assertIs<PestFailure.Network>(result.reason)
    }

    @Test
    fun `identify returns Server on ApiError ServerError`() = runTest {
        val fakeApi = FakePestApi(identifyError = ApiError.ServerError)
        val repo: PestRepository = PestRepositoryImpl(api = fakeApi)

        val result = repo.identify(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Failure>(result)
        assertIs<PestFailure.Server>(result.reason)
    }

    @Test
    fun `ImageResult toByteArray returns bytes field`() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val image = ImageResult(uri = "content://test.jpg", bytes = bytes)
        assertEquals(bytes, image.toByteArray())
    }

    @Test
    fun `ImageResult toByteArray returns null when bytes are null`() = runTest {
        val image = ImageResult(uri = "content://test.jpg", bytes = null)
        assertEquals(null, image.toByteArray())
    }

    @Test
    fun `identify returns ExpiredUrl on 403 upload response`() = runTest {
        val fakeApi = FakePestApi(uploadError = ApiError.Unauthorized)
        val repo: PestRepository = PestRepositoryImpl(api = fakeApi)

        val result = repo.identify(ImageResult(uri = "content://test.jpg", bytes = byteArrayOf(1, 2, 3)))

        assertIs<PestResult.Failure>(result)
        assertIs<PestFailure.ExpiredUrl>(result.reason)
    }

    private class FakePestApi(
        val uploadUrl: PestUploadUrlResponse = PestUploadUrlResponse(
            objectPath = "",
            signedUrl = "",
            contentType = "",
            expiresInSeconds = 0,
        ),
        val identify: PestIdentifyResponse = PestIdentifyResponse(),
        val uploadError: Throwable? = null,
        val identifyError: Throwable? = null,
    ) : PestApi {
        var lastUploadedBytes: ByteArray? = null
        var lastObjectPath: String? = null

        override suspend fun getUploadUrl(): PestUploadUrlResponse = uploadUrl

        override suspend fun uploadImage(signedUrl: String, imageBytes: ByteArray) {
            if (uploadError != null) throw uploadError
            lastUploadedBytes = imageBytes
        }

        override suspend fun identify(objectPath: String): PestIdentifyResponse {
            if (identifyError != null) throw identifyError
            lastObjectPath = objectPath
            return identify
        }
    }
}
