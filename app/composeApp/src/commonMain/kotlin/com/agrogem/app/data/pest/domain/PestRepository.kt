package com.agrogem.app.data.pest.domain

import com.agrogem.app.ui.screens.analysis.DiagnosisResult

sealed interface PestResult {
    data class Success(val diagnosis: DiagnosisResult) : PestResult
    data class Failure(val reason: PestFailure) : PestResult
}

sealed interface PestFailure {
    data class Network(val cause: Throwable) : PestFailure
    data object Server : PestFailure
    data object UploadFailed : PestFailure
    data object ExpiredUrl : PestFailure
    data object NoMatchFound : PestFailure
    data object UnsupportedPlatform : PestFailure
    data object MissingImageBytes : PestFailure
}

interface PestRepository {
    suspend fun identify(image: com.agrogem.app.data.ImageResult): PestResult
}
