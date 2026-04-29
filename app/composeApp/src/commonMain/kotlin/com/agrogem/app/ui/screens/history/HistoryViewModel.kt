package com.agrogem.app.ui.screens.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.data.analysis.domain.AnalysisRepository
import com.agrogem.app.data.analysis.domain.StoredAnalysis
import com.agrogem.app.ui.components.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class HistoryViewModel(
    private val analysisRepository: AnalysisRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val analyses = analysisRepository.listRecent(limit = 100)
            _uiState.update {
                it.copy(entries = analyses.map(::toHistoryEntry))
            }
        }
    }

    private fun toHistoryEntry(analysis: StoredAnalysis): PersistedHistoryEntry {
        return PersistedHistoryEntry(
            analysisId = analysis.analysisId,
            imageUri = analysis.imageUri,
            crop = analysis.diagnosis.pestName,
            meta = formatHistoryMeta(analysis.createdAtEpochMillis),
            status = analysis.diagnosis.severity.uppercase(),
            severity = analysis.diagnosis.severity.toSeverity(),
        )
    }

    private fun formatHistoryMeta(createdAtEpochMillis: Long): String {
        val zone = TimeZone.currentSystemDefault()
        val createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis).toLocalDateTime(zone)
        val time = "${createdAt.hour.twoDigits()}:${createdAt.minute.twoDigits()}"
        return "${createdAt.dayOfMonth.twoDigits()} ${createdAt.month.spanishShort()}, $time"
    }
}

@Immutable
data class HistoryUiState(
    val entries: List<PersistedHistoryEntry> = emptyList(),
)

@Immutable
data class PersistedHistoryEntry(
    val analysisId: String,
    val imageUri: String,
    val crop: String,
    val meta: String,
    val status: String,
    val severity: Severity,
)

private fun String.toSeverity(): Severity {
    val value = lowercase()
    return when {
        value.contains("cr") || value.contains("alta") -> Severity.Critica
        value.contains("aten") || value.contains("media") || value.contains("moder") -> Severity.Atencion
        else -> Severity.Optimo
    }
}

private fun Int.twoDigits(): String = if (this < 10) "0$this" else "$this"

private fun kotlinx.datetime.Month.spanishShort(): String = when (this) {
    kotlinx.datetime.Month.JANUARY -> "ene"
    kotlinx.datetime.Month.FEBRUARY -> "feb"
    kotlinx.datetime.Month.MARCH -> "mar"
    kotlinx.datetime.Month.APRIL -> "abr"
    kotlinx.datetime.Month.MAY -> "may"
    kotlinx.datetime.Month.JUNE -> "jun"
    kotlinx.datetime.Month.JULY -> "jul"
    kotlinx.datetime.Month.AUGUST -> "ago"
    kotlinx.datetime.Month.SEPTEMBER -> "sep"
    kotlinx.datetime.Month.OCTOBER -> "oct"
    kotlinx.datetime.Month.NOVEMBER -> "nov"
    kotlinx.datetime.Month.DECEMBER -> "dic"
}
