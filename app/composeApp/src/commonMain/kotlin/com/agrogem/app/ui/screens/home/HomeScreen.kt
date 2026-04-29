package com.agrogem.app.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.ic_action_notifications
import app.composeapp.generated.resources.ic_metric_cloud
import app.composeapp.generated.resources.ic_metric_location
import app.composeapp.generated.resources.ic_metric_uv
import app.composeapp.generated.resources.ic_metric_water
import app.composeapp.generated.resources.ic_weather_sunny
import com.agrogem.app.data.rememberLocationPermissionRequester
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.theme.AgroGemIconSizes
import com.agrogem.app.ui.components.AgroGemIcon
import com.agrogem.app.ui.components.BlockingLoadingOverlay
import com.agrogem.app.ui.components.RoundIconButton
import com.agrogem.app.data.soil.domain.SoilSummary
import com.agrogem.app.ui.components.SeverityBadge
import com.agrogem.app.ui.components.LeafThumb
import com.agrogem.app.ui.preview.RecentAnalysisItem
import com.agrogem.app.ui.preview.dashboardRecentItems
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenCamera: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenEnvironmentDetail: () -> Unit,
    onOpenGemmaDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isResolvingLocation by viewModel.isResolvingLocation.collectAsStateWithLifecycle()
    val locationRequester = rememberLocationPermissionRequester { granted ->
        viewModel.onLocationPermissionResult(granted)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.Screen),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (uiState) {
                    is HomeUiState.Data -> LocationChip(text = topLocationChipLabel((uiState as HomeUiState.Data).locationInfo.display))
                    is HomeUiState.Loading -> LocationChipShimmer()
                    else -> LocationChip(text = "—")
                }
                RoundIconButton(
                    label = "🔔",
                    icon = Res.drawable.ic_action_notifications,
                    contentDescription = "Notifications",
                    background = AgroGemColors.Surface,
                    foreground = AgroGemColors.IconBellTint,
                    size = 42.dp,
                    onClick = {},
                )
            }

            when (uiState) {
                is HomeUiState.Loading -> {
                    WeatherCardShimmer()
                    MetricsCardShimmer()
                    EnvironmentCardShimmer()
                }
                is HomeUiState.Data -> {
                    val data = uiState as HomeUiState.Data
                    val weatherDateTime = formatWeatherDateTime(data.weather.dateLabel)
                    WeatherCard(
                        locationLabel = shortLocationLabel(data.locationInfo.display.primary).uppercase(),
                        temperature = data.weather.temperatureCelsius,
                        description = data.weather.description,
                        timeLabel = weatherDateTime.time,
                        dateLabel = weatherDateTime.date,
                    )
                    MetricsCard(
                        humidity = data.metrics.humidity,
                        windSpeed = data.metrics.windSpeed,
                        precipitation = data.metrics.precipitation,
                        maxMin = data.metrics.maxMin,
                        uvIndex = data.metrics.uvIndex,
                    )
                    EnvironmentCard(
                        soilSummary = data.soilSummary,
                        elevationMeters = data.locationInfo.elevationMeters,
                        cropContext = data.cropContext,
                        onOpenDetail = onOpenEnvironmentDetail,
                    )
                }
                is HomeUiState.LocationMissing -> MessageCard(
                    title = "Sin ubicación",
                    subtitle = "Seleccioná una ubicación para ver el clima y métricas.",
                    cta = "Seleccionar ubicación",
                    onCta = { locationRequester.request() },
                )
                is HomeUiState.Error -> MessageCard(
                    title = "Error de conexión",
                    subtitle = (uiState as HomeUiState.Error).message,
                    cta = "Reintentar",
                    onCta = { viewModel.refresh() },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AgroGemColors.Surface, RoundedCornerShape(30.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Análisis Recientes",
                        color = AgroGemColors.TextPrimary,
                        fontSize = 32.sp / 1.75f,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Ver todo",
                        color = AgroGemColors.TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(onClick = onOpenHistory),
                    )
                }

                dashboardRecentItems.forEachIndexed { index, item ->
                    RecentAnalysisRow(item = item, seed = index)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(AgroGemColors.Primary, RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenGemmaDemo),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PROBAR GEMMA 4 (DEMO)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isResolvingLocation) {
            BlockingLoadingOverlay(message = "Obteniendo tu ubicación y cargando el clima...")
        }
    }
}

@Composable
private fun LocationChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(AgroGemColors.Surface, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgroGemIcon(
            icon = Res.drawable.ic_metric_location,
            contentDescription = "Location",
            tint = AgroGemColors.TextLocation,
            size = null,
            modifier = Modifier
                .width(8.dp)
                .height(11.dp),
        )
        Text(
            text = text,
            color = AgroGemColors.TextDark,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun WeatherCard(
    locationLabel: String,
    temperature: String,
    description: String,
    timeLabel: String,
    dateLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_metric_location,
                    contentDescription = "Weather location",
                    tint = AgroGemColors.TextLocationSmall,
                    size = null,
                    modifier = Modifier
                        .width(7.dp)
                        .height(10.dp),
                )
                Text(
                    text = locationLabel,
                    color = AgroGemColors.TextMedium,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                )
            }
            Text(
                text = temperature,
                color = AgroGemColors.TextBody,
                fontSize = 52.sp / 1.75f,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                color = AgroGemColors.TextMedium,
                fontSize = 16.sp,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AgroGemIcon(
                icon = Res.drawable.ic_weather_sunny,
                contentDescription = "Sunny weather",
                tint = AgroGemColors.TextIconGray,
                size = AgroGemIconSizes.Lg,
            )
            Text(
                text = timeLabel,
                color = AgroGemColors.TextLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = dateLabel,
                color = AgroGemColors.TextLabel,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun MetricsCard(
    humidity: String,
    windSpeed: String,
    precipitation: String,
    maxMin: String,
    uvIndex: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(15.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricItem(icon = Res.drawable.ic_metric_water, value = humidity, label = "HUMEDAD")
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(1.dp)
                .background(AgroGemColors.MetricDivider),
        )
        MetricItem(icon = Res.drawable.ic_metric_cloud, value = windSpeed, label = "VIENTO")
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(1.dp)
                .background(AgroGemColors.MetricDivider),
        )
        MetricItem(icon = Res.drawable.ic_metric_water, value = precipitation, label = "LLUVIA")
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(1.dp)
                .background(AgroGemColors.MetricDivider),
        )
        MetricItem(icon = Res.drawable.ic_metric_uv, value = "$uvIndex / $maxMin", label = "UV · MAX/MIN")
    }
}

private fun shortLocationLabel(value: String): String {
    if (value.isBlank()) return "—"
    val parts = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    return parts.take(2).joinToString(", ").ifBlank { value }
}

private data class WeatherDateTimeLabel(
    val time: String,
    val date: String,
)

private fun formatWeatherDateTime(raw: String): WeatherDateTimeLabel {
    val cleaned = raw.trim()
    if (!cleaned.contains('T')) return WeatherDateTimeLabel(time = "--:--", date = cleaned.ifBlank { "--" })

    val parts = cleaned.split('T')
    if (parts.size != 2) return WeatherDateTimeLabel(time = "--:--", date = cleaned)

    val date = formatIsoDate(parts[0])
    val time = formatIsoTime(parts[1])
    return WeatherDateTimeLabel(time = time, date = date)
}

private fun formatIsoDate(rawDate: String): String {
    val chunks = rawDate.split('-')
    if (chunks.size != 3) return rawDate
    val year = chunks[0]
    val month = chunks[1].padStart(2, '0')
    val day = chunks[2].padStart(2, '0')
    return "$day/$month/$year"
}

private fun formatIsoTime(rawTime: String): String {
    val timePart = rawTime.substringBefore('.').substringBefore('Z')
    val chunks = timePart.split(':')
    if (chunks.size < 2) return rawTime

    val hour = chunks[0].toIntOrNull() ?: return rawTime
    val minute = chunks[1].toIntOrNull() ?: return rawTime
    val suffix = if (hour >= 12) "PM" else "AM"
    val hour12 = when (val normalized = hour % 12) {
        0 -> 12
        else -> normalized
    }
    return "${hour12.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} $suffix"
}

private fun topLocationChipLabel(display: com.agrogem.app.data.geolocation.domain.LocationDisplay): String {
    val country = display.country?.trim()?.takeIf { it.isNotEmpty() }?.uppercase()
    val state = display.state
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let(::normalizeLocationRegionLabel)
        ?.takeIf { it.isNotEmpty() }
    return listOfNotNull(country, state).joinToString(", ").ifBlank { "—" }
}

internal fun normalizeLocationRegionLabel(value: String): String {
    val normalized = value.trim()
    val prefixes = listOf("Departamento de ", "Estado de ", "Department of ", "State of ")
    val withoutPrefix = prefixes.firstNotNullOfOrNull { prefix ->
        normalized.takeIf { it.startsWith(prefix, ignoreCase = true) }?.substring(prefix.length)
    } ?: normalized
    return withoutPrefix.trim()
}

@Composable
private fun LocationChipShimmer() {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(999.dp))
            .shimmerBackground(),
    )
}

@Composable
private fun WeatherCardShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .shimmerBackground(),
    )
}

@Composable
private fun MetricsCardShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(15.dp))
            .shimmerBackground(),
    )
}

@Composable
private fun EnvironmentCard(
    soilSummary: SoilSummary?,
    elevationMeters: Double?,
    cropContext: String?,
    onOpenDetail: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenDetail)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Suelo y Elevación",
            color = AgroGemColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        cropContext?.let { context ->
            Text(
                text = "Contexto cultivo: $context",
                color = AgroGemColors.TextMedium,
                fontSize = 12.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EnvironmentItem(
                label = "TEXTURA",
                value = soilSummary?.dominantTexture?.ifBlank { "--" } ?: "--",
            )
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .width(1.dp)
                    .background(AgroGemColors.MetricDivider),
            )
            EnvironmentItem(
                label = "pH SUPERFICIAL",
                value = soilSummary?.topHorizonPh?.let { formatOneDecimal(it) } ?: "--",
            )
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .width(1.dp)
                    .background(AgroGemColors.MetricDivider),
            )
            EnvironmentItem(
                label = "ELEVACIÓN",
                value = elevationMeters?.let { "${it.toInt()} m" } ?: "--",
            )
        }
    }
}

private fun formatOneDecimal(value: Double): String {
    val scaled = (value * 10).toInt()
    return "${scaled / 10}.${scaled % 10}"
}

@Composable
private fun EnvironmentItem(
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            color = AgroGemColors.TextGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            color = AgroGemColors.MetricTextAlpha,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
private fun EnvironmentCardShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .shimmerBackground(),
    )
}

@Composable
private fun MessageCard(
    title: String,
    subtitle: String,
    cta: String,
    onCta: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = AgroGemColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            color = AgroGemColors.TextMedium,
            fontSize = 14.sp,
        )
        Text(
            text = cta,
            color = AgroGemColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClick = onCta)
                .padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun Modifier.shimmerBackground(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.1f),
        Color.LightGray.copy(alpha = 0.3f),
    )
    return this.background(
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 200f, 0f),
            end = Offset(translateAnim + 200f, 0f),
        )
    )
}

@Composable
private fun MetricItem(
    icon: DrawableResource,
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AgroGemIcon(
            icon = icon,
            contentDescription = label,
            size = AgroGemIconSizes.Sm,
        )
        Text(text = value, color = AgroGemColors.TextGray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(text = label, color = AgroGemColors.MetricTextAlpha, fontSize = 10.sp, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun RecentAnalysisRow(
    item: RecentAnalysisItem,
    seed: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeafThumb(seed = seed)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    color = AgroGemColors.TextGrayMuted,
                    fontSize = 12.sp,
                    letterSpacing = 1.1.sp,
                )
                SeverityBadge(severity = item.severity)
            }

            Text(
                text = item.health,
                color = AgroGemColors.TextPrimary,
                fontSize = 18.sp,
                lineHeight = 22.sp,
            )
            Text(
                text = item.subtitle,
                color = AgroGemColors.TextPrimary,
                fontSize = 14.sp,
            )
        }
    }
}
