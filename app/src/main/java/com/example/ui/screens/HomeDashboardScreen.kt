package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Healing
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.sp
import com.example.data.local.UserPreferencesState
import com.example.data.model.BleConnectionStatus
import com.example.data.model.BleState
import com.example.data.model.CropProfile
import com.example.data.model.SensorDisplayMode
import com.example.data.model.SensorReading
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayInsetProgressBar
import com.example.ui.components.ClayPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.LightError
import com.example.ui.theme.LightErrorContainer
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
import com.example.ui.theme.LightSurfaceContainerLowest
import com.example.ui.theme.MonoSmallStyle
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
    onOpenNpkReport: () -> Unit,
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
        // FarmerBuddy / SMART SOIL Top Bar
        Surface(
            color = LightSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(LightPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = "FarmerBuddy Logo",
                                tint = LightOnPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "FarmerBuddy",
                                style = MaterialTheme.typography.titleLarge,
                                color = LightOnSurface
                            )
                            Text(
                                text = "SMART SOIL · ${preferences.selectedCropName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LightPrimary,
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
                            BleConnectionStatus.CONNECTED -> Quad("ESP32 Live", SuccessContainer, OnSuccessContainer, SuccessGreen)
                            BleConnectionStatus.SCANNING, BleConnectionStatus.CONNECTING -> Quad("Connecting", WarningContainer, OnWarningContainer, WarningAmber)
                            BleConnectionStatus.BLE_UNSUPPORTED -> Quad("No BLE", LightErrorContainer, LightOnErrorContainer, LightError)
                            else -> Quad("ESP32 Off", LightSurfaceContainer, LightOnSurfaceVariant, LightOnSurfaceVariant)
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
                            text = if (isLiveMode && isEsp32Connected) "LIVE" else if (isLiveMode) "OFFLINE" else "DEMO",
                            containerColor = if (isLiveMode && isEsp32Connected) SuccessContainer else WarningContainer,
                            contentColor = if (isLiveMode && isEsp32Connected) OnSuccessContainer else OnWarningContainer
                        )
                    }
                }

                // Compact 3-segment mode switcher + Setup icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeSwitchChip(
                        label = "Live ESP32",
                        selected = displayMode == SensorDisplayMode.LIVE_BLE,
                        onClick = {
                            onSelectDisplayMode(SensorDisplayMode.LIVE_BLE)
                            if (!isEsp32Connected) onOpenBleSheet()
                        },
                        testTag = "mode_live_chip",
                        modifier = Modifier.weight(1f)
                    )
                    ModeSwitchChip(
                        label = "Demo",
                        selected = displayMode == SensorDisplayMode.DEMO_NORMAL,
                        onClick = { onSelectDisplayMode(SensorDisplayMode.DEMO_NORMAL) },
                        testTag = "mode_demo_normal_chip",
                        modifier = Modifier.weight(1f)
                    )
                    ModeSwitchChip(
                        label = "Dry Alert",
                        selected = displayMode == SensorDisplayMode.DEMO_ALERT,
                        onClick = { onSelectDisplayMode(SensorDisplayMode.DEMO_ALERT) },
                        testTag = "mode_demo_alert_chip",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onOpenBleSheet,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(LightSurfaceContainer)
                            .testTag("open_ble_settings_chip")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Sensor & Crop Settings",
                            tint = LightOnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = LightOutlineVariant.copy(alpha = 0.5f))
        }

        // Scrollable Body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 600.dp

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Honest offline banner when Live mode is chosen without ESP32
                    if (isLiveMode && !isEsp32Connected) {
                        ClayCard(
                            containerColor = WarningContainer.copy(alpha = 0.75f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sensor Not Connected",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OnWarningContainer
                                    )
                                    Text(
                                        text = "Connect ESP32 Bluetooth or tap Demo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnWarningContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
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
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Connect")
                                }
                            }
                        }
                    }

                    // 1. LARGE SOIL MOISTURE CARD (3-Second Readability)
                    LargeMoistureCard(
                        reading = activeReading,
                        crop = cropProfile,
                        isLiveMode = isLiveMode,
                        isConnected = isEsp32Connected,
                        onSaveSnapshot = onSaveSensorSnapshot
                    )

                    // 2. SMALLER pH & TEMPERATURE CARDS SIDE BY SIDE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CompactPhCard(
                            reading = activeReading,
                            crop = cropProfile,
                            isLiveMode = isLiveMode,
                            isConnected = isEsp32Connected,
                            modifier = Modifier.weight(1f)
                        )
                        CompactTemperatureCard(
                            reading = activeReading,
                            crop = cropProfile,
                            isLiveMode = isLiveMode,
                            isConnected = isEsp32Connected,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 3. PROMINENT SCAN YOUR SOIL BUTTON
                    ClayPrimaryButton(
                        text = "SCAN YOUR SOIL",
                        icon = Icons.Default.CameraAlt,
                        onClick = onStartScan,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        testTag = "home_start_scan_button"
                    )

                    // 4. FOUR SIMPLE FEATURE CARDS
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard("Soil Scan", "Check soil type", Icons.Default.Terrain, SoilContainer, OnSoilContainer, onOpenSoilScan, "quick_scan_soil", Modifier.weight(1f))
                            QuickActionCard("Identify Crop", "Confirm plant", Icons.Default.Eco, LightSecondaryContainer, LightOnSecondaryContainer, onOpenCropId, "quick_identify_crop", Modifier.weight(1f))
                            QuickActionCard("Plant Health", "Leaf check", Icons.Default.Healing, WarningContainer, OnWarningContainer, onOpenPlantHealth, "quick_plant_health", Modifier.weight(1f))
                            QuickActionCard("NPK Report", "Lab card / guide", Icons.Default.Biotech, LightPrimary, LightOnPrimary, onOpenNpkReport, "quick_npk_report", Modifier.weight(1f))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard("Soil Scan", "Check soil type", Icons.Default.Terrain, SoilContainer, OnSoilContainer, onOpenSoilScan, "quick_scan_soil", Modifier.weight(1f))
                            QuickActionCard("Identify Crop", "Confirm plant", Icons.Default.Eco, LightSecondaryContainer, LightOnSecondaryContainer, onOpenCropId, "quick_identify_crop", Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard("Plant Health", "Leaf check", Icons.Default.Healing, WarningContainer, OnWarningContainer, onOpenPlantHealth, "quick_plant_health", Modifier.weight(1f))
                            QuickActionCard("NPK Report", "Lab card / guide", Icons.Default.Biotech, LightPrimary, LightOnPrimary, onOpenNpkReport, "quick_npk_report", Modifier.weight(1f))
                        }
                    }

                    // 5. Compact AI Advisor Link
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = LightPrimaryContainer,
                        contentColor = LightOnPrimaryContainer,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ASK FARMERBUDDY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LightOnPrimaryContainer.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "Get simple next steps for your crop & soil",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightOnPrimaryContainer
                                )
                            }
                            Button(
                                onClick = onOpenAiAdvisor,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LightSurfaceContainerLowest,
                                    contentColor = LightPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_open_advisor_button")
                            ) {
                                Text("AI Advisor", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun LargeMoistureCard(
    reading: SensorReading?,
    crop: CropProfile,
    isLiveMode: Boolean,
    isConnected: Boolean,
    onSaveSnapshot: () -> Unit
) {
    val pct = reading?.moisturePercent
    val dryCutoff = (crop.minMoisturePct - 6).coerceAtLeast(5)

    val (statusLabel, hindiLabel, badgeBg, badgeFg, barColor, borderColor, hint) = when {
        pct == null -> MoistureVisual(
            "Not connected",
            "सेंसर जुड़ा नहीं है",
            LightSurfaceContainer,
            LightOnSurfaceVariant,
            LightOutlineVariant,
            LightOutlineVariant.copy(alpha = 0.5f),
            "Connect ESP32 or tap Demo to preview."
        )
        pct < dryCutoff -> MoistureVisual(
            "Dry",
            "सूखी मिट्टी",
            LightErrorContainer,
            LightOnErrorContainer,
            LightError,
            LightError.copy(alpha = 0.4f),
            "Dry — check plot before watering."
        )
        pct < crop.minMoisturePct -> MoistureVisual(
            "Moderate",
            "हल्की नमी",
            WarningContainer,
            OnWarningContainer,
            WarningAmber,
            WarningAmber.copy(alpha = 0.4f),
            "Getting dry — watch moisture closely."
        )
        pct > crop.maxMoisturePct -> MoistureVisual(
            "Moderate",
            "अधिक नमी",
            WarningContainer,
            OnWarningContainer,
            WarningAmber,
            WarningAmber.copy(alpha = 0.4f),
            "Very wet — pause watering and check drainage."
        )
        else -> MoistureVisual(
            "Moist",
            "अच्छी नमी",
            SuccessContainer,
            OnSuccessContainer,
            SuccessGreen,
            SuccessGreen.copy(alpha = 0.35f),
            "Good moisture for ${crop.name}."
        )
    }

    val updatedText = reading?.timestampMs?.let {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
    } ?: "Not connected"
    val connText = if (isLiveMode && isConnected) "ESP32 Live" else if (isLiveMode) "Not connected" else "Demo Preview"

    ClayCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LightSecondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = LightOnSecondaryContainer, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text("Soil Moisture", style = MaterialTheme.typography.titleLarge, color = LightOnSurface)
                    Text(
                        text = "$hindiLabel · Target ${crop.minMoisturePct}–${crop.maxMoisturePct}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightOnSurfaceVariant
                    )
                }
            }
            StatusPill(text = statusLabel, containerColor = badgeBg, contentColor = badgeFg)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pct?.let { "$it%" } ?: "Not connected",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if (pct != null) 42.sp else 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = LightOnSurface
            )
            if (reading != null) {
                OutlinedButton(
                    onClick = onSaveSnapshot,
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("save_sensor_snapshot_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Reading", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        ClayInsetProgressBar(
            fraction = (pct ?: 0) / 100f,
            fillColor = barColor
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = hint, style = MaterialTheme.typography.bodySmall, color = LightOnSurface)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$connText · Updated $updatedText",
            style = MonoSmallStyle,
            color = LightOnSurfaceVariant
        )
    }
}

@Composable
private fun CompactPhCard(
    reading: SensorReading?,
    crop: CropProfile,
    isLiveMode: Boolean,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val ph = reading?.ph
    val (statusLabel, isOutOfRange) = when {
        ph == null && isLiveMode && !isConnected -> "Not connected" to false
        ph == null -> "Unavailable" to false
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Science, contentDescription = null, tint = SoilBrown, modifier = Modifier.size(18.dp))
                Text("Soil pH", style = MaterialTheme.typography.titleMedium, color = LightOnSurface)
            }
            StatusPill(
                text = statusLabel,
                containerColor = if (ph == null) LightSurfaceContainer else if (isOutOfRange) WarningContainer else SuccessContainer,
                contentColor = if (ph == null) LightOnSurfaceVariant else if (isOutOfRange) OnWarningContainer else OnSuccessContainer
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = ph?.let { String.format(Locale.US, "%.1f", it) } ?: if (isLiveMode && !isConnected) "Not connected" else "Unavailable",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (ph != null) 26.sp else 16.sp, fontWeight = FontWeight.Bold),
            color = LightOnSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text("Ideal ${crop.minPh}–${crop.maxPh}", style = MaterialTheme.typography.bodySmall, color = LightOnSurfaceVariant)
    }
}

@Composable
private fun CompactTemperatureCard(
    reading: SensorReading?,
    crop: CropProfile,
    isLiveMode: Boolean,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val temp = reading?.temperatureC
    val (statusLabel, isWarmOrCool) = when {
        temp == null && isLiveMode && !isConnected -> "Not connected" to false
        temp == null -> "Unavailable" to false
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Thermostat, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                Text("Temp", style = MaterialTheme.typography.titleMedium, color = LightOnSurface)
            }
            StatusPill(
                text = statusLabel,
                containerColor = if (temp == null) LightSurfaceContainer else if (isWarmOrCool) WarningContainer else SuccessContainer,
                contentColor = if (temp == null) LightOnSurfaceVariant else if (isWarmOrCool) OnWarningContainer else OnSuccessContainer
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = temp?.let { String.format(Locale.US, "%.1f°C", it) } ?: if (isLiveMode && !isConnected) "Not connected" else "Unavailable",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (temp != null) 26.sp else 16.sp, fontWeight = FontWeight.Bold),
            color = LightOnSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text("Root zone · तापमान", style = MaterialTheme.typography.bodySmall, color = LightOnSurfaceVariant)
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
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = LightOnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = LightOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ModeSwitchChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) LightPrimary else LightSurfaceContainer
    val fg = if (selected) LightOnPrimary else LightOnSurfaceVariant
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        contentColor = fg,
        shadowElevation = if (selected) 3.dp else 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else LightOutlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(999.dp)
            )
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private data class MoistureVisual(
    val statusLabel: String,
    val hindiLabel: String,
    val badgeBg: Color,
    val badgeFg: Color,
    val barColor: Color,
    val borderColor: Color,
    val hint: String
)

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
