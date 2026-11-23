package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.ClayCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.LightOnPrimary
import com.example.ui.theme.LightOnSecondaryContainer
import com.example.ui.theme.LightOnSurface
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSecondaryContainer
import com.example.ui.theme.LightSurface
import com.example.ui.theme.OnSoilContainer
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.SoilContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.WarningContainer

@Composable
fun ScanHubScreen(
    selectedCropName: String,
    onOpenSoilScan: () -> Unit,
    onOpenCropId: () -> Unit,
    onOpenPlantHealth: () -> Unit,
    onOpenNpkReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightSurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Scanning System",
                style = MaterialTheme.typography.headlineLarge,
                color = LightOnSurface
            )
            Text(
                text = "Choose a visual or Soil Health Card scan. Active crop: $selectedCropName",
                style = MaterialTheme.typography.bodyMedium,
                color = LightOnSurfaceVariant
            )
        }

        ScanOptionCard(
            title = "A. Soil Scan",
            badge = "Gemini Vision",
            badgeBg = SoilContainer,
            badgeFg = OnSoilContainer,
            description = "Upload or capture a topsoil photo to describe visible characteristics, possible soil type, and recommended follow-up tests. Never claims to measure pH or NPK from a photo.",
            icon = Icons.Default.Terrain,
            iconBg = SoilContainer,
            iconTint = OnSoilContainer,
            onClick = onOpenSoilScan,
            testTag = "scan_option_soil"
        )

        ScanOptionCard(
            title = "B. Crop Identification",
            badge = "Confirm Crop",
            badgeBg = LightSecondaryContainer,
            badgeFg = LightOnSecondaryContainer,
            description = "Identify a crop from leaf and stem features with confidence & uncertainty notes. Requires your confirmation before saving to your crop profile.",
            icon = Icons.Default.Eco,
            iconBg = LightSecondaryContainer,
            iconTint = LightOnSecondaryContainer,
            onClick = onOpenCropId,
            testTag = "scan_option_crop"
        )

        ScanOptionCard(
            title = "C. Plant Health Scan",
            badge = "Cautious Check",
            badgeBg = WarningContainer,
            badgeFg = OnWarningContainer,
            description = "Inspect visible leaf symptoms, possible explanations, and safe field checks. Clearly states image-only limitations without claiming a definitive diagnosis.",
            icon = Icons.Default.Healing,
            iconBg = WarningContainer,
            iconTint = OnWarningContainer,
            onClick = onOpenPlantHealth,
            testTag = "scan_option_plant"
        )

        ScanOptionCard(
            title = "D. NPK Report",
            badge = "Lab or Regional",
            badgeBg = SuccessContainer,
            badgeFg = OnSuccessContainer,
            description = "Method 1: Upload a Soil Health Card / lab report to extract & edit N, P, K values. Method 2: Check a cautious regional reference estimate if no lab report is available.",
            icon = Icons.Default.Biotech,
            iconBg = LightPrimary,
            iconTint = LightOnPrimary,
            onClick = onOpenNpkReport,
            testTag = "scan_option_npk"
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ScanOptionCard(
    title: String,
    badge: String,
    badgeBg: Color,
    badgeFg: Color,
    description: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    ClayCard(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = LightOnSurface
                    )
                    StatusPill(text = badge, containerColor = badgeBg, contentColor = badgeFg)
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LightOnSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Open tool",
                        style = MaterialTheme.typography.labelMedium,
                        color = LightPrimary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
