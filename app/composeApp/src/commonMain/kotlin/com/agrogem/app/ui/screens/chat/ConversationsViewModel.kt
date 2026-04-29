package com.agrogem.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import com.agrogem.app.data.chat.domain.LocalChatRepository
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConversationsViewModel(
    private val localChatRepository: LocalChatRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    init {
        val repository = localChatRepository
        if (repository != null) {
            _uiState.value = _uiState.value.copy(
                analysisConversations = repository.listRecent(),
                normalConversations = emptyList(),
                isLoading = false,
            )
        } else {
            _uiState.value = createMockState()
        }
    }

    private fun createMockState(): ConversationsUiState {
        val analysisConversations = listOf(
            Conversation(
                id = "conv_analysis_1",
                title = "Análisis: Hemileia vastatrix",
                preview = "Se ha detectado una infección avanzada. El 45% del follaje muestra pústulas activas...",
                timestamp = 1_714_000_000_000L,
                timestampLabel = "Hoy, 10:30",
                analysisId = "analysis_abc123",
                diagnosis = DiagnosisResult(
                    pestName = "Hemileia vastatrix",
                    confidence = 0.95f,
                    severity = "Crítica",
                    affectedArea = "Tallo y hoja",
                    cause = "Hongo, Hemileia vastatrix",
                    diagnosisText = "Se ha detectado una infección avanzada por Hemileia vastatrix. " +
                        "El 45% del follaje muestra pústulas activas. Se requiere intervención " +
                        "inmediata para evitar la pérdida total de la cosecha.",
                    treatmentSteps = listOf(
                        "Aplicar fungicida sistémico de cobre.",
                        "Monitorear semanalmente el follaje.",
                    ),
                ),
            ),
            Conversation(
                id = "conv_analysis_2",
                title = "Análisis: Araña roja",
                preview = "Infestación moderada detectada en el envés de las hojas...",
                timestamp = 1_713_900_000_000L,
                timestampLabel = "Ayer, 16:45",
                analysisId = "analysis_def456",
                diagnosis = DiagnosisResult(
                    pestName = "Araña roja",
                    confidence = 0.87f,
                    severity = "Atención",
                    affectedArea = "Hoja",
                    cause = "Ácaro, Tetranychus urticae",
                    diagnosisText = "Infestación moderada de araña roja en el envés de las hojas. " +
                        "Se observan telarañas y puntos amarillos en el 30% del follaje.",
                    treatmentSteps = listOf(
                        "Aplicar acaricida específico.",
                        "Incrementar riego por aspersión.",
                    ),
                ),
            ),
        )

        val normalConversations = listOf(
            Conversation(
                id = "conv_normal_1",
                title = "Consejos de fertilización",
                preview = "Te recomiendo aplicar nitrógeno en forma de urea durante el primer mes...",
                timestamp = 1_713_800_000_000L,
                timestampLabel = "24 abr, 09:15",
            ),
            Conversation(
                id = "conv_normal_2",
                title = "Nueva conversación",
                preview = "Hola, ¿en qué puedo ayudarte con tus cultivos hoy?",
                timestamp = 1_713_700_000_000L,
                timestampLabel = "23 abr, 14:20",
            ),
        )

        return ConversationsUiState(
            analysisConversations = analysisConversations,
            normalConversations = normalConversations,
            isLoading = false,
        )
    }
}
