package com.agrogem.app.ui.screens.analysis

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.data.ImageResult

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisFlowViewModel : ViewModel() {

    private val _capturedImage = MutableStateFlow<ImageResult?>(null)
    val capturedImage: StateFlow<ImageResult?> = _capturedImage.asStateFlow()

    private val _phase = MutableStateFlow<AnalysisPhase>(AnalysisPhase.Analyzing)
    val phase: StateFlow<AnalysisPhase> = _phase.asStateFlow()

    private val _steps = MutableStateFlow(defaultSteps())
    val steps: StateFlow<List<AnalysisStepUi>> = _steps.asStateFlow()

    private var analysisJob: Job? = null

    val diagnosisResult: DiagnosisResult = mockDiagnosisResult()

    fun setCapturedImage(image: ImageResult) {
        _capturedImage.value = image
    }

    fun startSimulatedAnalysis() {
        _phase.value = AnalysisPhase.Analyzing
        _steps.value = defaultSteps()
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            val stepCount = _steps.value.size
            for (i in 0 until stepCount) {
                delay(1500L)
                _steps.update { current ->
                    current.mapIndexed { index, step ->
                        if (index == i) step.copy(done = true) else step
                    }
                }
            }
            delay(500L)
            _phase.value = AnalysisPhase.Results
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        _phase.value = AnalysisPhase.Analyzing
        _steps.value = defaultSteps()
        _capturedImage.value = null
    }

    fun clearAll() {
        analysisJob?.cancel()
        _capturedImage.value = null
        _phase.value = AnalysisPhase.Analyzing
        _steps.value = defaultSteps()
    }

    /**
     * Load a previous analysis entry directly in the Results phase.
     * Used when navigating from the history screen.
     */
    fun loadFromHistory(imageUri: String) {
        _capturedImage.value = ImageResult(uri = imageUri)
        _steps.value = defaultSteps().map { it.copy(done = true) }
        _phase.value = AnalysisPhase.Results
    }
}

sealed interface AnalysisPhase {
    data object Analyzing : AnalysisPhase
    data object Results : AnalysisPhase
}

@Immutable
data class AnalysisStepUi(
    val title: String,
    val subtitle: String,
    val done: Boolean,
)

@Immutable
data class DiagnosisResult(
    val pestName: String,
    val confidence: Float,
    val severity: String,
    val affectedArea: String,
    val cause: String,
    val diagnosisText: String,
    val treatmentSteps: List<String>,
)

private fun defaultSteps(): List<AnalysisStepUi> = listOf(
    AnalysisStepUi(
        title = "Identificando patrones de hojas...",
        subtitle = "Analizando irregularidades celulares",
        done = false,
    ),
    AnalysisStepUi(
        title = "Consultando base de datos de plagas...",
        subtitle = "Sincronizando con AgroCloud Index",
        done = false,
    ),
    AnalysisStepUi(
        title = "Calculando severidad...",
        subtitle = "Estimación de impacto en cosecha",
        done = false,
    ),
    AnalysisStepUi(
        title = "Generando recomendaciones...",
        subtitle = "Análisis de tratamientos disponibles",
        done = false,
    ),
)

private fun mockDiagnosisResult(): DiagnosisResult = DiagnosisResult(
    pestName = "Plaga detectada",
    confidence = 0.95f,
    severity = "Problema iniciando",
    affectedArea = "Tallo y hoja",
    cause = "Hongo, Hemileia vastatrix",
    diagnosisText = "Se ha detectado una infección avanzada por Hemileia vastatrix. " +
        "El 45% del follaje muestra pústulas activas. Se requiere intervención " +
        "inmediata para evitar la pérdida total de la cosecha.",
    treatmentSteps = listOf(
        "Se ha detectado una infección avanzada por Hemileia vastatrix. " +
            "El 45% del follaje muestra pústulas activas. Se requiere intervención " +
            "inmediata para evitar la pérdida total de la cosecha.",
        "Se ha detectado una infección avanzada por Hemileia vastatrix. " +
            "El 45% del follaje muestra pústulas activas. Se requiere intervención " +
            "inmediata para evitar la pérdida total de la cosecha.",
    ),
)
