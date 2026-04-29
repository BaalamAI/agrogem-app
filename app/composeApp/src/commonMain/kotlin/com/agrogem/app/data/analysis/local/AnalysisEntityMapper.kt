package com.agrogem.app.data.analysis.local

import com.agrogem.app.data.analysis.domain.StoredAnalysis
import com.agrogem.app.data.local.db.Analysis
import com.agrogem.app.data.pest.domain.AnalysisDiagnosis
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AnalysisEntityMapper(
    private val json: Json = Json,
) {
    fun toStoredAnalysis(entity: Analysis): StoredAnalysis = StoredAnalysis(
        analysisId = entity.analysis_id,
        imageUri = entity.image_uri,
        diagnosis = AnalysisDiagnosis(
            pestName = entity.pest_name,
            confidence = entity.confidence.toFloat(),
            severity = entity.severity,
            affectedArea = entity.affected_area,
            cause = entity.cause,
            diagnosisText = entity.diagnosis_text,
            treatmentSteps = json.decodeFromString<List<String>>(entity.treatment_steps_json),
            isConfidenceReliable = entity.is_confidence_reliable != 0L,
        ),
        createdAtEpochMillis = entity.created_at_epoch_millis,
    )

    fun toParams(model: StoredAnalysis): AnalysisInsertParams = AnalysisInsertParams(
        analysisId = model.analysisId,
        imageUri = model.imageUri,
        pestName = model.diagnosis.pestName,
        confidence = model.diagnosis.confidence.toDouble(),
        severity = model.diagnosis.severity,
        affectedArea = model.diagnosis.affectedArea,
        cause = model.diagnosis.cause,
        diagnosisText = model.diagnosis.diagnosisText,
        treatmentStepsJson = json.encodeToString(model.diagnosis.treatmentSteps),
        isConfidenceReliable = if (model.diagnosis.isConfidenceReliable) 1L else 0L,
        createdAtEpochMillis = model.createdAtEpochMillis,
    )
}

data class AnalysisInsertParams(
    val analysisId: String,
    val imageUri: String,
    val pestName: String,
    val confidence: Double,
    val severity: String,
    val affectedArea: String,
    val cause: String,
    val diagnosisText: String,
    val treatmentStepsJson: String,
    val isConfidenceReliable: Long,
    val createdAtEpochMillis: Long,
)
