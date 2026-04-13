package com.agrogem.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agrogem.app.navigation.AgroGemRoute
import com.agrogem.app.navigation.AppNavHost
import com.agrogem.app.navigation.navigateTo
import com.agrogem.app.ui.components.BottomNavigationBar

@Composable
fun AppShell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = AgroGemRoute.fromRoute(backStackEntry?.destination?.route)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    if (destination != currentRoute) {
                        navController.navigateTo(destination)
                    }
                },
            )
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
