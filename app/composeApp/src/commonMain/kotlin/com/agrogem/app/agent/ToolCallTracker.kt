package com.agrogem.app.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ToolCallTracker {
    private val _calledTools = MutableStateFlow<Set<String>>(emptySet())
    val calledTools: StateFlow<Set<String>> = _calledTools.asStateFlow()

    fun markCalled(displayName: String) {
        _calledTools.value = _calledTools.value + displayName
    }

    fun reset() {
        _calledTools.value = emptySet()
    }
}

val toolCallTracker = ToolCallTracker()
