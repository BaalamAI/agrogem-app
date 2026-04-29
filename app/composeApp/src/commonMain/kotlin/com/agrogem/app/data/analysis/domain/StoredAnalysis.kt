package com.agrogem.app.data.analysis.domain

import com.agrogem.app.data.pest.domain.AnalysisDiagnosis

data class StoredAnalysis(
    val analysisId: String,
    val imageUri: String,
    val diagnosis: AnalysisDiagnosis,
    val createdAtEpochMillis: Long,
)

interface AnalysisRepository {
    suspend fun save(analysis: StoredAnalysis)
    suspend fun getById(analysisId: String): StoredAnalysis?
    suspend fun listRecent(limit: Long): List<StoredAnalysis>
}
