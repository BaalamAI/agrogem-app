package com.agrogem.app.data.pest.domain

sealed interface PestResult {
    data class Success(
        val diagnosis: AnalysisDiagnosis,
        val evidence: PestAnalysisEvidence? = null,
    ) : PestResult
    data class Failure(val reason: PestFailure) : PestResult
}

data class PestAnalysisEvidence(
    val topMatchName: String,
    val similarity: Float,
    val weightedScore: Float,
    val confidenceLabel: String,
    val alternatives: List<PestAlternativeEvidence> = emptyList(),
    val votes: Map<String, Float> = emptyMap(),
)

data class PestAlternativeEvidence(
    val pestName: String,
    val similarity: Float,
    val imageId: String? = null,
)

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
