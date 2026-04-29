package com.agrogem.app.data.analysis.local

import com.agrogem.app.data.analysis.domain.StoredAnalysis
import com.agrogem.app.data.local.db.AgroGemDatabase

class AnalysisLocalDataSource(
    private val database: AgroGemDatabase,
    private val mapper: AnalysisEntityMapper = AnalysisEntityMapper(),
) {
    fun upsert(analysis: StoredAnalysis) {
        val params = mapper.toParams(analysis)
        database.analysisQueries.upsertAnalysis(
            analysis_id = params.analysisId,
            image_uri = params.imageUri,
            pest_name = params.pestName,
            confidence = params.confidence,
            severity = params.severity,
            affected_area = params.affectedArea,
            cause = params.cause,
            diagnosis_text = params.diagnosisText,
            treatment_steps_json = params.treatmentStepsJson,
            is_confidence_reliable = params.isConfidenceReliable,
            created_at_epoch_millis = params.createdAtEpochMillis,
        )
    }

    fun getById(analysisId: String): StoredAnalysis? =
        database.analysisQueries.selectAnalysisById(analysisId)
            .executeAsOneOrNull()
            ?.let(mapper::toStoredAnalysis)

    fun listRecent(limit: Long): List<StoredAnalysis> =
        database.analysisQueries.selectRecentAnalyses(limit)
            .executeAsList()
            .map(mapper::toStoredAnalysis)
}
