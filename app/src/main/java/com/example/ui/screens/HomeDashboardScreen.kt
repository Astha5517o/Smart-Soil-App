package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compost
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferencesState
import com.example.data.model.BleConnectionStatus
import com.example.data.model.BleState
import com.example.data.model.CropProfile
import com.example.data.model.SensorDisplayMode
import com.example.data.model.SensorReading
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayInsetProgressBar
import com.example.ui.components.ClayPrimaryButton
import com.example.ui.components.SoilPhScaleBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.LightErrorContainer
import com.example.ui.theme.LightError
import com.example.ui.theme.LightOnErrorContainer
import com.example.ui.theme.LightOnPrimary
import com.example.ui.theme.LightOnPrimaryContainer
import com.example.ui.theme.LightOnSecondaryContainer
import com.example.ui.theme.LightOnSurface
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightOutlineVariant
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightPrimaryContainer
import com.example.ui.theme.LightSecondaryContainer
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceContainer
import com.example.ui.theme.LightSurfaceContainerLow
import com.example.ui.theme.LightSurfaceContainerLowest
import com.example.ui.theme.MonoLargeStyle
import com.example.ui.theme.OnSoilContainer
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.SoilBrown
import com.example.ui.theme.SoilContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    preferences: UserPreferencesState,
    bleState: BleState,
    displayMode: SensorDisplayMode,
    activeReading: SensorReading?,
    onSelectDisplayMode: (SensorDisplayMode) -> Unit,
    onOpenBleSheet: () -> Unit,
    onStartScan: () -> Unit,
    onOpenSoilScan: () -> Unit,
    onOpenCropId: () -> Unit,
    onOpenPlantHealth: () -> Unit,
    onOpenAiAdvisor: () -> Unit,
    onSaveSensorSnapshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cropProfile = preferences.toEffectiveCropProfile()
    val isLiveMode = displayMode == SensorDisplayMode.LIVE_BLE
    val isEsp32Connected = bleState.status == BleConnectionStatus.CONNECTED

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightSurface)
    ) {
        // Sticky Stitch Header
        Surface(
            color = LightSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenBleSheet)
                            .padding(end = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(LightPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compost,
                                contentDescription = "Smart Soil Logo",
                                tint = LightOnPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SMART SOIL",
                                style = MaterialTheme.typography.titleLarge,
                                color = LightOnSurface
                            )
                            Text(
                                text = "Crop: ${preferences.selectedCropName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LightOnSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (connText, connBg, connFg, dotColor) = when (bleState.status) {
                            BleConnectionStatus.CONNECTED -> Quad("ESP32 Connected", SuccessContainer, OnSuccessContainer, SuccessGreen)
                            BleConnectionStatus.SCANNING, BleConnectionStatus.CONNECTING -> Quad("Connecting...", WarningContainer, OnWarningContainer, WarningAmber)
                            BleConnectionStatus.BLE_UNSUPPORTED -> Quad("No BLE", LightErrorContainer, LightOnErrorContainer, LightError)
                            else -> Quad("ESP32 Offline", LightSurfaceContainer, LightOnSurfaceVariant, LightOnSurfaceVariant)
                        }
                        StatusPill(
                            text = connText,
                            containerColor = connBg,
                            contentColor = connFg,
                            leadingDotColor = dotColor,
                            onClick = onOpenBleSheet,
                            modifier = Modifier.testTag("ble_status_badge")
                        )

                        StatusPill(
                            text = if (isLiveMode) "LIVE DATA" else "DEMO DATA",
                            containerColor = if (isLiveMode && isEsp32Connected) SuccessContainer else WarningContainer,
                            contentColor = if (isLiveMode && isEsp32Connected) OnSuccessContainer else OnWarningContainer
                        )
                    }
                }

                // Exhibition & BLE Mode Switcher Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeSwitchChip(
                        label = if (isEsp32Connected) "Live ESP32" else "Live ESP32 (Connect)",
                        selected = displayMode == SensorDisplayMode.LIVE_BLE,
                        onClick = {
                            onSelectDisplayMode(SensorDisplayMode.LIVE_BLE)
                            if (!isEsp32Connected) onOpenBleSheet()
                        },
                        testTag = "mode_live_chip"
                    )
                    ModeSwitchChip(
                        label = "Demo (Normal)",
                        selected = displayMode == SensorDisplayMode.DEMO_NORMAL,
                        onClick = { onSelectDisplayMode(SensorDisplayMode.DEMO_NORMAL) },
                        testTag = "mode_demo_normal_chip"
                    )
                    ModeSwitchChip(
                        label = "Demo (Low Moisture & High pH Alert)",
                        selected = displayMode == SensorDisplayMode.DEMO_ALERT,
                        onClick = { onSelectDisplayMode(SensorDisplayMode.DEMO_ALERT) },
                        testTag = "mode_demo_alert_chip"
                    )
                    ModeSwitchChip(
                        label = "BLE & Thresholds",
                        selected = false,
                        icon = Icons.Default.Tune,
                        onClick = onOpenBleSheet,
                        testTag = "open_ble_settings_chip"
                    )
                }
            }
            HorizontalDivider(color = LightOutlineVariant.copy(alpha = 0.6f))
        }

        // Scrollable Dashboard Body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 600.dp

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Heading + Demo Disclaimer + Prominent START SCAN CTA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your Soil Health at a Glance",
                                style = MaterialTheme.typography.headlineMedium,
                                color = LightOnSurface
                            )
                            if (!isLiveMode) {
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusPill(
                                    text = "Illustrative Example Readings (Demo)",
                                    containerColor = WarningContainer,
                                    contentColor = OnWarningContainer
                                )
                            } else if (activeReading?.timestampMs != null) {
                                val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                Text(
                                    text = "Last ESP32 packet: ${fmt.format(Date(activeReading.timestampMs))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightOnSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        ClayPrimaryButton(
                            text = "START SCAN",
                            icon = Icons.Default.CameraAlt,
                            onClick = onStartScan,
                            testTag = "home_start_scan_button"
                        )
                    }

                    // If Live mode selected but ESP32 disconnected, show honest disconnected state (NEVER fake live data)
                    if (isLiveMode && !isEsp32Connected) {
                        ClayCard(
                            containerColor = WarningContainer.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ESP32 Not Connected — Live Readings Unavailable",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OnWarningContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = bleState.errorMessage
                                            ?: "Connect your ESP32 DevKit V1 over Bluetooth Low Energy to view real moisture, pH, and temperature packets, or switch to Demo Mode.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnWarningContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = onOpenBleSheet,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LightPrimary,
                                        contentColor = LightOnPrimary
                                    ),
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("connect_esp32_cta")
                                ) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Connect")
                                }
                            }
                        }
                    }

                    // Three Prominent Sensor Cards (1 col mobile, 3 col tablet)
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            MoistureCard(activeReading, cropProfile, Modifier.weight(1f))
                            PhCard(activeReading, cropProfile, Modifier.weight(1f))
                            TemperatureCard(activeReading, cropProfile, Modifier.weight(1f))
                        }
                    } else {
                        MoistureCard(activeReading, cropProfile, Modifier.fillMaxWidth())
                        PhCard(activeReading, cropProfile, Modifier.fillMaxWidth())
                        TemperatureCard(activeReading, cropProfile, Modifier.fillMaxWidth())
                    }

                    // Save current reading snapshot button
                    if (activeReading != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = onSaveSensorSnapshot,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("save_sensor_snapshot_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (activeReading.isDemo) "Save Demo Snapshot to Reports"
                                    else "Save Live Reading to Reports"
                                )
                            }
                        }
                    }

                    // Prominent "Today's Soil Advice" Banner (Exact Stitch #1E4D36)
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = LightPrimaryContainer,
                        contentColor = LightOnPrimaryContainer,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp).padding(top = 2.dp)
                                )
                                Column {
                                    Text(
                                        text = "TODAY'S SOIL ADVICE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LightOnPrimaryContainer.copy(alpha = 0.88f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = buildDailyAdviceSummary(activeReading, cropProfile),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LightOnPrimaryContainer
                                    )
                                }
                            }

                            Button(
                                onClick = onOpenAiAdvisor,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LightSurfaceContainerLowest,
                                    contentColor = LightPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_open_advisor_button")
                            ) {
                                Text("Open AI Advisor", style = MaterialTheme.typography.labelLarge)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Four Quick-Action Cards (2x2 mobile, 4-across wide)
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard(
                                title = "Scan Soil",
                                subtitle = "Visual soil check",
                                icon = Icons.Default.Terrain,
                                iconBg = SoilContainer,
                                iconTint = OnSoilContainer,
                                onClick = onOpenSoilScan,
                                testTag = "quick_scan_soil",
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                title = "Identify Crop",
                                subtitle = "Confirm crop & stage",
                                icon = Icons.Default.Eco,
                                iconBg = LightSecondaryContainer,
                                iconTint = LightOnSecondaryContainer,
                                onClick = onOpenCropId,
                                testTag = "quick_identify_crop",
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                title = "Check Plant Health",
                                subtitle = "Leaf symptom scan",
                                icon = Icons.Default.Healing,
                                iconBg = WarningContainer,
                                iconTint = OnWarningContainer,
                                onClick = onOpenPlantHealth,
                                testTag = "quick_plant_health",
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                title = "Get AI Advice",
                                subtitle = "Guided soil plan",
                                icon = Icons.Default.Psychology,
                                iconBg = LightPrimary,
                                iconTint = LightOnPrimary,
                                onClick = onOpenAiAdvisor,
                                testTag = "quick_ai_advice",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard(
                                title = "Scan Soil",
                                subtitle = "Visual soil check",
                                icon = Icons.Default.Terrain,
                                iconBg = SoilContainer,
                                iconTint = OnSoilContainer,
                                onClick = onOpenSoilScan,
                                testTag = "quick_scan_soil",
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                title = "Identify Crop",
                                subtitle = "Confirm crop & stage",
                                icon = Icons.Default.Eco,
                                iconBg = LightSecondaryContainer,
                                iconTint = LightOnSecondaryContainer,
                                onClick = onOpenCropId,
                                testTag = "quick_identify_crop",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard(
                                title = "Check Plant Health",
                                subtitle = "Leaf symptom scan",
                                icon = Icons.Default.Healing,
                                iconBg = WarningContainer,
                                iconTint = OnWarningContainer,
                                onClick = onOpenPlantHealth,
                                testTag = "quick_plant_health",
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                title = "Get AI Advice",
                                subtitle = "Guided soil plan",
                                icon = Icons.Default.Psychology,
                                iconBg = LightPrimary,
                                iconTint = LightOnPrimary,
                                onClick = onOpenAiAdvisor,
                                testTag = "quick_ai_advice",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MoistureCard(
    reading: SensorReading?,
    crop: CropProfile,
    modifier: Modifier = Modifier
) {
    val pct = reading?.moisturePercent
    val (statusLabel, isWarning) = when {
        pct == null -> "Unavailable" to true
        pct < crop.minMoisturePct -> "Low" to true
        pct > crop.maxMoisturePct -> "High" to true
        else -> "Suitable" to false
    }

    ClayCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = LightPrimary, modifier = Modifier.size(22.dp))
                Text("Moisture", style = MaterialTheme.typography.titleLarge, color = LightOnSurface)
            }
            StatusPill(
                text = statusLabel,
                containerColor = if (isWarning) WarningContainer else SuccessContainer,
                contentColor = if (isWarning) OnWarningContainer else OnSuccessContainer
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = pct?.let { "$it%" } ?: "--%",
                style = MonoLargeStyle,
                color = LightOnSurface
            )
            Text(
                text = "Target: ${crop.minMoisturePct}–${crop.maxMoisturePct}%",
                style = MaterialTheme.typography.bodySmall,
                color = LightOnSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (reading?.moistureRaw != null) {
            Text(
                text = "HW-390 Raw ADC: ${reading.moistureRaw}",
                style = MaterialTheme.typography.labelSmall,
                color = LightOnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        ClayInsetProgressBar(
            fraction = (pct ?: 0) / 100f,
            fillColor = if (isWarning && pct != null && pct < crop.minMoisturePct) WarningAmber else LightPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        val alertBg = if (isWarning && pct != null) WarningContainer else LightSurfaceContainerLow
        val alertFg = if (isWarning && pct != null) OnWarningContainer else LightOnSurfaceVariant
        val (title, next) = when {
            pct == null -> "Sensor unavailable" to "Connect ESP32 or check HW-390 wiring."
            pct < crop.minMoisturePct -> "Alert: Below ${crop.minMoisturePct}% threshold" to "Next: Check another plot spot & plan irrigation."
            pct > crop.maxMoisturePct -> "Alert: Above ${crop.maxMoisturePct}% threshold" to "Next: Pause irrigation & check field drainage."
            else -> "Within crop range" to "Next: Check again tomorrow morning."
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(alertBg)
                .padding(10.dp)
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (isWarning && pct != null) alertFg else LightOnSurface)
                Text(next, style = MaterialTheme.typography.bodySmall, color = alertFg)
            }
        }
    }
}

@Composable
private fun PhCard(
    reading: SensorReading?,
    crop: CropProfile,
    modifier: Modifier = Modifier
) {
    val ph = reading?.ph
    val (statusLabel, isOutOfRange) = when {
        ph == null -> "Unavailable" to true
        ph < crop.minPh -> "Acidic" to true
        ph > crop.maxPh -> "Alkaline" to true
        else -> "Near Neutral" to false
    }

    ClayCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Science, contentDescription = null, tint = SoilBrown, modifier = Modifier.size(22.dp))
                Text("Soil pH", style = MaterialTheme.typography.titleLarge, color = LightOnSurface)
            }
            StatusPill(
                text = statusLabel,
                containerColor = if (isOutOfRange) WarningContainer else SuccessContainer,
                contentColor = if (isOutOfRange) OnWarningContainer else OnSuccessContainer
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = ph?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                style = MonoLargeStyle,
                color = LightOnSurface
            )
            Text(
                text = "Preferred: ${crop.minPh}–${crop.maxPh}",
                style = MaterialTheme.typography.bodySmall,
                color = LightOnSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        SoilPhScaleBar(phValue = ph)
        Spacer(modifier = Modifier.height(12.dp))

        val alertBg = if (isOutOfRange && ph != null) WarningContainer else LightSurfaceContainerLow
        val alertFg = if (isOutOfRange && ph != null) OnWarningContainer else LightOnSurfaceVariant
        val (title, next) = when {
            ph == null -> "pH probe unavailable" to "Verify ADIY BNC calibration & 3.3V divider first."
            ph < crop.minPh -> "Alert: Below preferred ${crop.minPh}–${crop.maxPh} range" to "Next: Verify with buffer calibration & soil lab test before amending."
            ph > crop.maxPh -> "Alert: Above preferred ${crop.minPh}–${crop.maxPh} range" to "Next: Review a soil test before choosing an amendment."
            else -> "Balanced for ${crop.name.lowercase()}" to "Next: No pH amendment needed today."
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(alertBg)
                .padding(10.dp)
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (isOutOfRange && ph != null) alertFg else LightOnSurface)
                Text(next, style = MaterialTheme.typography.bodySmall, color = alertFg)
            }
        }
    }
}

@Composable
private fun TemperatureCard(
    reading: SensorReading?,
    crop: CropProfile,
    modifier: Modifier = Modifier
) {
    val temp = reading?.temperatureC
    val (statusLabel, isWarmOrCool) = when {
        temp == null -> "Unavailable" to true
        temp > crop.optimalTempMaxC -> "Warm" to true
        temp < crop.optimalTempMinC -> "Cool" to true
        else -> "Optimal" to false
    }

    ClayCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Thermostat, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(22.dp))
                Text("Temperature", style = MaterialTheme.typography.titleLarge, color = LightOnSurface)
            }
            StatusPill(
                text = statusLabel,
                containerColor = if (isWarmOrCool && temp != null) WarningContainer else if (temp == null) LightSurfaceContainer else SuccessContainer,
                contentColor = if (isWarmOrCool && temp != null) OnWarningContainer else if (temp == null) LightOnSurfaceVariant else OnSuccessContainer
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = temp?.let { String.format(Locale.US, "%.1f°C", it) } ?: "--°C",
                style = MonoLargeStyle,
                color = LightOnSurface
            )
            Text(
                text = "DS18B20 Probe",
                style = MaterialTheme.typography.bodySmall,
                color = LightOnSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        ClayInsetProgressBar(
            fraction = if (temp != null) ((temp / 45.0).toFloat().coerceIn(0.05f, 1f)) else 0f,
            fillColor = SoilBrown
        )
        Spacer(modifier = Modifier.height(12.dp))

        val desc = when {
            temp == null -> "Connect DS18B20 probe with 4.7kΩ pull-up resistor on ESP32."
            temp > crop.optimalTempMaxC -> "Higher surface warmth; organic mulch can help shade roots."
            temp < crop.optimalTempMinC -> "Cooler root-zone temperature; germination and uptake may slow."
            else -> "Supports steady root activity without midday heat stress."
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(LightSurfaceContainerLow)
                .padding(10.dp)
        ) {
            Column {
                Text("Root-zone warmth", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = LightOnSurface)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = LightOnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    ClayCard(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LightOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = LightOnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModeSwitchChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    icon: ImageVector? = null
) {
    val bg = if (selected) LightPrimary else LightSurfaceContainer
    val fg = if (selected) LightOnPrimary else LightOnSurfaceVariant
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        contentColor = fg,
        shadowElevation = if (selected) 4.dp else 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else LightOutlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(999.dp)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            }
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun buildDailyAdviceSummary(reading: SensorReading?, crop: CropProfile): String {
    if (reading == null) {
        return "Connect your ESP32 sensor node via BLE for live soil moisture, pH, and temperature guidance—or switch to Demo Mode to explore the exhibition dashboard."
    }
    val m = reading.moisturePercent
    val ph = reading.ph
    val prefix = if (reading.isDemo) "[DEMO] " else ""
    return when {
        m != null && m < crop.minMoisturePct && ph != null && ph > crop.maxPh ->
            "${prefix}Soil moisture ($m%) is below your ${crop.name} threshold and pH ($ph) is above its preferred range. Check another area of the plot and review a reliable soil test before choosing an amendment."
        m != null && m < crop.minMoisturePct ->
            "${prefix}Soil moisture ($m%) is below the ${crop.minMoisturePct}% target for ${crop.name}. Verify across two plot spots before scheduling irrigation."
        ph != null && (ph < crop.minPh || ph > crop.maxPh) ->
            "${prefix}Soil pH ($ph) is outside ${crop.name}'s preferred ${crop.minPh}–${crop.maxPh} range. Do not apply amendments from a single reading—confirm with a laboratory soil test."
        else ->
            "${prefix}Moisture (${m ?: "--"}%) and pH (${ph ?: "--"}) are within your ${crop.name} crop's preferred range. Keep monitoring morning probe readings before scheduling irrigation."
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
