package com.agrogem.app.ui.screens.environment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agrogem.app.data.climate.domain.ClimateDataPoint
import com.agrogem.app.data.climate.domain.ClimateHistory
import com.agrogem.app.data.soil.domain.Horizon
import com.agrogem.app.data.soil.domain.SoilProfile
import androidx.compose.ui.graphics.Color
import com.agrogem.app.theme.AgroGemColors

@Composable
fun EnvironmentDetailScreen(
    viewModel: EnvironmentDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.Screen)
            .verticalScroll(rememberScrollState()),
    ) {
        when (uiState) {
            is EnvironmentDetailUiState.Loading -> EnvironmentDetailLoading()
            is EnvironmentDetailUiState.Error -> {
                val state = uiState as EnvironmentDetailUiState.Error
                EnvironmentDetailError(
                    message = state.message,
                    canRetry = state.canRetry,
                    onRetry = { viewModel.retry() },
                    onBack = onBack,
                )
            }
            is EnvironmentDetailUiState.Success -> {
                val state = uiState as EnvironmentDetailUiState.Success
                EnvironmentDetailContent(
                    soil = state.soil,
                    climate = state.climate,
                    interpretation = state.interpretation,
                    locationName = state.locationName,
                    elevationMeters = state.elevationMeters,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun EnvironmentDetailLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun EnvironmentDetailError(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Error de carga",
            color = AgroGemColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = AgroGemColors.TextMedium,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (canRetry) {
            Text(
                text = "Reintentar",
                color = AgroGemColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(AgroGemColors.Surface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickableSafe(onRetry),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = "Volver",
            color = AgroGemColors.TextMedium,
            fontSize = 14.sp,
            modifier = Modifier.clickableSafe(onBack),
        )
    }
}

@Composable
private fun EnvironmentDetailContent(
    soil: SoilProfile?,
    climate: ClimateHistory?,
    interpretation: String,
    locationName: String,
    elevationMeters: Double?,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = locationName.ifBlank { "Perfil ambiental" },
            color = AgroGemColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        if (elevationMeters != null) {
            Text(
                text = "${elevationMeters.toInt()} msnm",
                color = AgroGemColors.TextMedium,
                fontSize = 14.sp,
            )
        }

        Text(
            text = "Volver",
            color = AgroGemColors.TextMedium,
            fontSize = 14.sp,
            modifier = Modifier.clickableSafe(onBack),
        )

        if (!interpretation.isBlank()) {
            Text(
                text = interpretation,
                color = AgroGemColors.TextMedium,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AgroGemColors.Surface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            )
        }

        if (soil != null) {
            SoilSection(soil)
        }

        if (climate != null) {
            ClimateSection(climate)
        }
    }
}

@Composable
private fun SoilSection(soil: SoilProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Perfil de suelo",
            color = AgroGemColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Textura dominante: ${soil.dominantTexture}",
            color = AgroGemColors.TextMedium,
            fontSize = 14.sp,
        )
        soil.domainHorizons.forEach { horizon ->
            HorizonRow(horizon)
        }
    }
}

@Composable
private fun HorizonRow(horizon: Horizon) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = "Profundidad: ${horizon.depth}",
            color = AgroGemColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DetailLabel("pH", horizon.ph?.toString() ?: "--")
            DetailLabel("SOC", "${horizon.socGPerKg} g/kg")
            DetailLabel("N", "${horizon.nitrogenGPerKg} g/kg")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DetailLabel("Arcilla", "${horizon.clayPct}%")
            DetailLabel("Arena", "${horizon.sandPct}%")
            DetailLabel("Limo", "${horizon.siltPct}%")
        }
        DetailLabel("CEC", "${horizon.cecMmolPerKg} mmol/kg")
    }
}

@Composable
private fun ClimateSection(climate: ClimateHistory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Historial climático (${climate.granularity})",
            color = AgroGemColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        climate.domainSeries.forEach { point ->
            ClimateRow(point)
        }
    }
}

@Composable
private fun ClimateRow(point: ClimateDataPoint) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = point.date,
            color = AgroGemColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "${point.t2m}C / ${point.precipitationMm}mm / ${point.rhPct}%",
            color = AgroGemColors.TextMedium,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun DetailLabel(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = AgroGemColors.TextMedium,
        fontSize = 12.sp,
    )
}

private fun Modifier.clickableSafe(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(onClick = onClick)
    )
