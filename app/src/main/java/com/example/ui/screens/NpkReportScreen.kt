package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.NpkLabExtractionResult
import com.example.data.model.NpkRegionalReferenceResult
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.LightOnSurface
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSurface
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.AsyncUiState

@Composable
fun NpkReportScreen(
    defaultCropName: String,
    labState: AsyncUiState<NpkLabExtractionResult>,
    regionalState: AsyncUiState<NpkRegionalReferenceResult>,
    onExtractLabReport: (Uri) -> Unit,
    onLoadDemoLab: () -> Unit,
    onSaveVerifiedLab: (String, String, String, String, String, String, Boolean, Uri?) -> Unit,
    onSearchRegionalReference: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedMethod by remember { mutableIntStateOf(0) } // 0 = Uploaded Lab Report, 1 = Regional Reference
    var uploadedReportUri by remember { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("npk_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Scan Hub")
            }
            Text("NPK Report", style = MaterialTheme.typography.headlineMedium, color = LightOnSurface)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedMethod == 0,
                    onClick = { selectedMethod = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("npk_tab_lab")
                ) {
                    Text("1. Lab Report")
                }
                SegmentedButton(
                    selected = selectedMethod == 1,
                    onClick = { selectedMethod = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("npk_tab_regional")
                ) {
                    Text("2. Regional Estimate")
                }
            }

            if (selectedMethod == 0) {
                // Method 1: Uploaded Soil Report
                PhotoPickerAndCameraBar(
                    selectedImageUri = uploadedReportUri,
                    onImageSelected = { uri ->
                        uploadedReportUri = uri
                        onExtractLabReport(uri)
                    },
                    onLoadDemoSample = onLoadDemoLab,
                    demoLabel = "Load Sample Lab Card (Demo)"
                )

                when (labState) {
                    is AsyncUiState.Idle -> {
                        ClayCard {
                            Text(
                                text = "Upload a Soil Health Card or laboratory test photo. Gemini will extract readable N, P, and K values for you to review and edit before saving.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LightOnSurfaceVariant
                            )
                        }
                    }
                    is AsyncUiState.Loading -> LoadingAnalysisCard("Extracting N, P, K values from report...")
                    is AsyncUiState.Error -> ErrorAnalysisCard(labState.message)
                    is AsyncUiState.Success -> {
                        val res = labState.data
                        var nVal by remember(res) { mutableStateOf(res.nitrogen) }
                        var pVal by remember(res) { mutableStateOf(res.phosphorus) }
                        var kVal by remember(res) { mutableStateOf(res.potassium) }
                        var unitVal by remember(res) { mutableStateOf(res.unit) }
                        var labName by remember(res) { mutableStateOf(res.labSource) }
                        var dateVal by remember(res) { mutableStateOf(res.reportDate) }
                        var saved by remember(res) { mutableStateOf(false) }

                        ClayCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Review & Correct Values", style = MaterialTheme.typography.titleLarge)
                                StatusPill(
                                    text = if (res.isDemo) "DEMO SAMPLE" else "VERIFY OCR",
                                    containerColor = if (res.isDemo) WarningContainer else SuccessContainer,
                                    contentColor = if (res.isDemo) OnWarningContainer else OnSuccessContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(res.extractionNotes, style = MaterialTheme.typography.bodySmall, color = LightOnSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = nVal,
                                    onValueChange = { nVal = it },
                                    label = { Text("Nitrogen (N)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = pVal,
                                    onValueChange = { pVal = it },
                                    label = { Text("Phosphorus (P)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = kVal,
                                    onValueChange = { kVal = it },
                                    label = { Text("Potassium (K)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = unitVal,
                                    onValueChange = { unitVal = it },
                                    label = { Text("Unit") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = dateVal,
                                    onValueChange = { dateVal = it },
                                    label = { Text("Report Date") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = labName,
                                onValueChange = { labName = it },
                                label = { Text("Laboratory / Soil Health Card Source") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ClayPrimaryButton(
                                text = if (saved) "Saved (Original Report Preserved)" else "Save Verified Lab Report",
                                icon = Icons.Default.CheckCircle,
                                enabled = !saved,
                                onClick = {
                                    onSaveVerifiedLab(nVal, pVal, kVal, unitVal, labName, dateVal, res.isDemo, uploadedReportUri)
                                    saved = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "save_verified_lab_button"
                            )
                        }
                    }
                }
            } else {
                // Method 2: Regional Reference Estimate
                var cropInput by remember { mutableStateOf(defaultCropName) }
                var regionInput by remember { mutableStateOf("") }
                var soilTypeInput by remember { mutableStateOf("") }

                ClayCard(containerColor = WarningContainer.copy(alpha = 0.7f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = OnWarningContainer, modifier = Modifier.size(20.dp))
                        Text(
                            text = "REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST. Use this only when no lab report is available; it will never invent field NPK test numbers.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = OnWarningContainer
                        )
                    }
                }

                ClayCard {
                    Text("Regional Agricultural Reference", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cropInput,
                        onValueChange = { cropInput = it },
                        label = { Text("Crop *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("regional_crop_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = regionInput,
                        onValueChange = { regionInput = it },
                        label = { Text("State / District / Region *") },
                        placeholder = { Text("e.g., Ludhiana, Punjab") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("regional_location_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = soilTypeInput,
                        onValueChange = { soilTypeInput = it },
                        label = { Text("Soil Type (optional)") },
                        placeholder = { Text("e.g., Alluvial loam") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ClayPrimaryButton(
                        text = "Look Up Regional Reference",
                        icon = Icons.Default.Public,
                        onClick = { onSearchRegionalReference(cropInput, regionInput, soilTypeInput) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "fetch_regional_npk_button"
                    )
                }

                when (regionalState) {
                    is AsyncUiState.Idle -> Unit
                    is AsyncUiState.Loading -> LoadingAnalysisCard("Checking agronomy reference context...")
                    is AsyncUiState.Error -> ErrorAnalysisCard(regionalState.message)
                    is AsyncUiState.Success -> {
                        val res = regionalState.data
                        ClayCard {
                            StatusPill(
                                text = "REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST.",
                                containerColor = WarningContainer,
                                contentColor = OnWarningContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (res.isInsufficientInfo) {
                                Text(
                                    text = "Insufficient reliable reference information found for this specific region/crop combination.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnWarningContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            LabeledSection("Crop & Region", "${res.crop} — ${res.region} (Soil: ${res.soilType})")
                            LabeledSection("Reference Status", res.sufficiencyStatus)
                            LabeledSection("Agronomy Overview", res.nutrientOverview)
                            LabeledSection("Source & Date", "${res.source} (${res.referenceDate})")
                            LabeledSection("Limitations", res.limitations)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
