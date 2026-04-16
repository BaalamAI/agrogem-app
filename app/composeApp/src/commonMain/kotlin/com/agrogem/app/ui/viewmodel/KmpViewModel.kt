package com.agrogem.app.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel

@Composable
inline fun <reified VM : ViewModel> kmpViewModel(
    noinline create: () -> VM,
): VM = remember { create() }
