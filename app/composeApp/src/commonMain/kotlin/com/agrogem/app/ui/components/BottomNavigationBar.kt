package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.agrogem.app.navigation.AgroGemBottomTab

private val NavigationBackground = Color(0xF2FFFFFF)
private val NavigationActive = Color(0xFF6C9E00)
private val NavigationInactive = Color(0x99747474)
private val ScanBackground = Color(0xFF0D631B)

@Composable
fun BottomNavigationBar(
    currentTab: AgroGemBottomTab,
    onNavigate: (AgroGemBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(NavigationBackground)
            .padding(horizontal = 30.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BottomBarItem(
            tab = AgroGemBottomTab.Home,
            label = "HOME",
            icon = "⌂",
            currentTab = currentTab,
            onNavigate = onNavigate,
        )
        BottomBarItem(
            tab = AgroGemBottomTab.Fields,
            label = "FIELDS",
            icon = "⏚",
            currentTab = currentTab,
            onNavigate = onNavigate,
        )

        ScanFab(
            selected = currentTab == AgroGemBottomTab.Scan,
            onClick = { onNavigate(AgroGemBottomTab.Scan) },
        )

        BottomBarItem(
            tab = AgroGemBottomTab.Maps,
            label = "MAPS",
            icon = "⌖",
            currentTab = currentTab,
            onNavigate = onNavigate,
        )
        BottomBarItem(
            tab = AgroGemBottomTab.Profile,
            label = "PROFILE",
            icon = "◡",
            currentTab = currentTab,
            onNavigate = onNavigate,
        )
    }
}

@Composable
private fun BottomBarItem(
    tab: AgroGemBottomTab,
    label: String,
    icon: String,
    currentTab: AgroGemBottomTab,
    onNavigate: (AgroGemBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = currentTab == tab
    val tint = if (active) NavigationActive else NavigationInactive

    Column(
        modifier = modifier
            .padding(top = 4.dp)
            .clickable { onNavigate(tab) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(text = icon, color = tint, fontSize = 18.sp)
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ScanFab(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val activeGlow = if (selected) Color(0x66ABD557) else Color(0x44ABD557)

    Box(
        modifier = Modifier
            .size(64.dp)
            .background(activeGlow, CircleShape)
            .padding(2.dp)
            .background(ScanBackground, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "◉", color = Color.White, fontSize = 22.sp)
    }
}
