package com.agrogem.app.ui.screens.chat

import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConversationStoreTest {

    @Test
    fun `save stores conversation keyed by analysisId`() = runTest {
        val store = ConversationStore()
        val diagnosis = sampleDiagnosis()

        store.save("analysis_001", diagnosis)

        val conversation = store.get("analysis_001")
        assertNotNull(conversation)
        assertEquals("analysis_001", conversation.analysisId)
        assertEquals("Análisis: Roya", conversation.title)
        assertEquals("Infección detectada", conversation.preview)
    }

    @Test
    fun `get returns null for unknown analysisId`() {
        val store = ConversationStore()
        assertNull(store.get("unknown"))
    }

    @Test
    fun `save overwrites existing conversation with same analysisId`() = runTest {
        val store = ConversationStore()
        val first = sampleDiagnosis(pestName = "Roya")
        val second = sampleDiagnosis(pestName = "Mildiu")

        store.save("analysis_001", first)
        store.save("analysis_001", second)

        val conversation = store.get("analysis_001")
        assertNotNull(conversation)
        assertEquals("Análisis: Mildiu", conversation.title)
    }

    @Test
    fun `remove deletes conversation from store`() = runTest {
        val store = ConversationStore()
        store.save("analysis_001", sampleDiagnosis())

        store.remove("analysis_001")

        assertNull(store.get("analysis_001"))
    }

    @Test
    fun `conversations flow emits updated map on save`() = runTest {
        val store = ConversationStore()

        val initial = store.conversations.first()
        assertTrue(initial.isEmpty())

        store.save("analysis_001", sampleDiagnosis())

        val updated = store.conversations.first()
        assertEquals(1, updated.size)
        assertTrue(updated.containsKey("analysis_001"))
    }

    @Test
    fun `conversations flow emits updated map on remove`() = runTest {
        val store = ConversationStore()
        store.save("analysis_001", sampleDiagnosis())
        store.save("analysis_002", sampleDiagnosis(pestName = "Mildiu"))

        store.remove("analysis_001")

        val updated = store.conversations.first()
        assertEquals(1, updated.size)
        assertTrue(updated.containsKey("analysis_002"))
    }

    @Test
    fun `saved conversation contains diagnosis for context recovery`() = runTest {
        val store = ConversationStore()
        val diagnosis = sampleDiagnosis(pestName = "Broca")

        store.save("analysis_003", diagnosis)

        val conversation = store.get("analysis_003")
        assertNotNull(conversation)
        assertNotNull(conversation.diagnosis)
        assertEquals("Broca", conversation.diagnosis?.pestName)
    }

    private fun sampleDiagnosis(
        pestName: String = "Roya",
    ): DiagnosisResult = DiagnosisResult(
        pestName = pestName,
        confidence = 0.92f,
        severity = "Alta",
        affectedArea = "Hojas",
        cause = "Hongo",
        diagnosisText = "Infección detectada",
        treatmentSteps = listOf("Aplicar fungicida"),
    )
}
