package com.agrogem.app.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.agrogem.app.navigation.AgroGemRoute

@Composable
fun BottomNavigationBar(
    currentRoute: AgroGemRoute,
    onNavigate: (AgroGemRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        AgroGemRoute.all.forEach { route ->
            NavigationBarItem(
                selected = route == currentRoute,
                onClick = { onNavigate(route) },
                icon = { Text(route.icon) },
                label = { Text(route.title) },
            )
        }
    }
}

private val AgroGemRoute.icon: String
    get() = when (this) {
        AgroGemRoute.Dashboard -> "🏠"
        AgroGemRoute.Camera -> "📷"
        AgroGemRoute.Map -> "🗺️"
        AgroGemRoute.Analysis -> "📊"
        AgroGemRoute.Report -> "🧾"
    }
