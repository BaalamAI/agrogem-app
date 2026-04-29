package com.agrogem.app.data.analysis

import com.agrogem.app.data.analysis.domain.AnalysisRepository
import com.agrogem.app.data.analysis.domain.AnalysisRepositoryImpl
import com.agrogem.app.data.analysis.local.AnalysisLocalDataSource
import com.agrogem.app.data.local.db.LocalDatabaseProvider

fun createAnalysisRepository(): AnalysisRepository = AnalysisRepositoryImpl(
    localDataSource = AnalysisLocalDataSource(LocalDatabaseProvider.database),
)
