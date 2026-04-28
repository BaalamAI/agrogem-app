package com.agrogem.app.ui.screens.analysis

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.data.ImageResult
import com.agrogem.app.data.pest.domain.PestFailure
import com.agrogem.app.data.pest.domain.PestResult
import com.agrogem.app.data.pest.domain.PlantAnalysisRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

class AnalysisFlowViewModel(
    private val plantAnalysisRepository: PlantAnalysisRepository,
) : ViewModel() {

    private val _capturedImage = MutableStateFlow<ImageResult?>(null)
    val capturedImage: StateFlow<ImageResult?> = _capturedImage.asStateFlow()

    private val _phase = MutableStateFlow<AnalysisPhase>(AnalysisPhase.Analyzing)
    val phase: StateFlow<AnalysisPhase> = _phase.asStateFlow()

    private val _steps = MutableStateFlow(defaultSteps())
    val steps: StateFlow<List<AnalysisStepUi>> = _steps.asStateFlow()

    private var analysisJob: Job? = null

    private var _diagnosisResult: DiagnosisResult = defaultDiagnosisResult()
    val diagnosisResult: DiagnosisResult get() = _diagnosisResult

    private val _analysisId = MutableStateFlow<String?>(null)
    val analysisId: StateFlow<String?> = _analysisId.asStateFlow()

    fun setCapturedImage(image: ImageResult) {
        _capturedImage.value = image
    }

    fun startAnalysis(image: ImageResult) {
        _capturedImage.value = image
        _phase.value = AnalysisPhase.Analyzing
        _steps.value = defaultSteps()
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            runRealAnalysis(image)
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        _phase.value = AnalysisPhase.Analyzing
        _steps.value = defaultSteps()
        _capturedImage.value = null
        _analysisId.value = null
    }

    fun clearAll() {
        analysisJob?.cancel()
        _capturedImage.value = null
        _phase.value = AnalysisPhase.Analyzing
        _steps.value = defaultSteps()
        _analysisId.value = null
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

    private suspend fun runRealAnalysis(image: ImageResult) {
        markStepDone(0)

        when (val result = plantAnalysisRepository.analyze(image)) {
            is PestResult.Success -> {
                markStepDone(1)
                _diagnosisResult = result.diagnosis
                _analysisId.value = generateAnalysisId()
                markStepDone(2)
                _phase.value = AnalysisPhase.Results
            }
            is PestResult.Failure -> {
                val (message, retryable) = mapFailure(result.reason)
                _phase.value = AnalysisPhase.Error(message = message, retryable = retryable)
            }
        }
    }

    private fun generateAnalysisId(): String =
        "analysis_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(10000)}"

    private fun markStepDone(index: Int) {
        _steps.update { current ->
            current.mapIndexed { i, step ->
                if (i == index) step.copy(done = true) else step
            }
        }
    }

    private fun mapFailure(failure: PestFailure): Pair<String, Boolean> = when (failure) {
        is PestFailure.Network -> "Error de red. Verificá tu conexión e intentá de nuevo." to true
        is PestFailure.Server -> "Error del servidor. Intentá de nuevo en unos momentos." to true
        is PestFailure.UploadFailed -> "No se pudo subir la imagen. Intentá de nuevo." to true
        is PestFailure.ExpiredUrl -> "El enlace de subida expiró. Intentá de nuevo." to true
        is PestFailure.NoMatchFound -> "No se encontró coincidencia con plagas conocidas." to false
        is PestFailure.UnsupportedPlatform -> "Esta plataforma no soporta análisis de plagas." to false
        is PestFailure.MissingImageBytes -> "No se pudieron leer los bytes de la imagen." to true
    }
}

sealed interface AnalysisPhase {
    data object Analyzing : AnalysisPhase
    data object Results : AnalysisPhase
    data class Error(val message: String, val retryable: Boolean) : AnalysisPhase
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
        title = "Subiendo imagen...",
        subtitle = "Preparando fotografía para análisis",
        done = false,
    ),
    AnalysisStepUi(
        title = "Identificando plaga...",
        subtitle = "Sincronizando con AgroCloud Index",
        done = false,
    ),
    AnalysisStepUi(
        title = "Procesando resultados...",
        subtitle = "Generando diagnóstico y tratamiento",
        done = false,
    ),
)

private fun defaultDiagnosisResult(): DiagnosisResult = DiagnosisResult(
    pestName = "Esperando análisis",
    confidence = 0f,
    severity = "—",
    affectedArea = "—",
    cause = "—",
    diagnosisText = "Realizá un análisis para obtener el diagnóstico completo.",
    treatmentSteps = emptyList(),
)
