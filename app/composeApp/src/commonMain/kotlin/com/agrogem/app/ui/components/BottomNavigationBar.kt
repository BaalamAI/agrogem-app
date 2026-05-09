package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.navigation.AgroGemBottomTab
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.theme.AgroGemIconSizes

enum class BottomTabIcons(val resourceName: String) {
    Home("home"),
    Fields("agriculture"),
    Scan("bug_report"),
    Maps("map"),
    Chat("forum"),
}

@Composable
fun BottomNavigationBar(
    currentTab: AgroGemBottomTab,
    onNavigate: (AgroGemBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AgroGemColors.NavBackground)
            .navigationBarsPadding()
            .height(68.dp)
            .padding(horizontal = 30.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BottomBarItem(
            tab = AgroGemBottomTab.Home,
            label = "HOME",
            icon = Icons.Filled.Home,
            currentTab = currentTab,
            onNavigate = onNavigate,
        )
        // POC reducido: solo Home + Chat
        /*
        BottomBarItem(
            tab = AgroGemBottomTab.Fields,
            label = "FIELDS",
            icon = Icons.Filled.Agriculture,
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
            icon = Icons.Filled.Map,
            currentTab = currentTab,
            onNavigate = onNavigate,
        )
        */
        BottomBarItem(
            tab = AgroGemBottomTab.Chat,
            label = "CHAT",
            icon = Icons.Filled.Forum,
            currentTab = currentTab,
            onNavigate = onNavigate,
        )
    }
}

@Composable
private fun BottomBarItem(
    tab: AgroGemBottomTab,
    label: String,
    icon: ImageVector,
    currentTab: AgroGemBottomTab,
    onNavigate: (AgroGemBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = currentTab == tab
    val tint = if (active) AgroGemColors.PrimaryNavActive else AgroGemColors.NavInactive

    Column(
        modifier = modifier
            .padding(top = 4.dp)
            .clickable { onNavigate(tab) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(AgroGemIconSizes.Md),
        )
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
    val activeGlow = if (selected) AgroGemColors.PrimaryNavGlow else AgroGemColors.PrimaryNavGlowDim

    Box(
        modifier = Modifier
            .size(64.dp)
            .background(activeGlow, CircleShape)
            .padding(2.dp)
            .background(AgroGemColors.ScanBackground, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.BugReport,
            contentDescription = "Scan",
            tint = Color.White,
            modifier = Modifier.size(AgroGemIconSizes.Lg),
        )
    }
}
