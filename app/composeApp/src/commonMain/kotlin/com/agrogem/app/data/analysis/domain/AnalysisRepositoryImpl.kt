package com.agrogem.app.data.analysis.domain

import com.agrogem.app.data.analysis.local.AnalysisLocalDataSource

class AnalysisRepositoryImpl(
    private val localDataSource: AnalysisLocalDataSource,
) : AnalysisRepository {
    override suspend fun save(analysis: StoredAnalysis) {
        localDataSource.upsert(analysis)
    }

    override suspend fun getById(analysisId: String): StoredAnalysis? =
        localDataSource.getById(analysisId)

    override suspend fun listRecent(limit: Long): List<StoredAnalysis> =
        localDataSource.listRecent(limit)
}
