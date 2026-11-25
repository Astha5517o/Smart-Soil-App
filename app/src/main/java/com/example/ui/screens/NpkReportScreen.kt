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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.ui.theme.LightErrorContainer
import com.example.ui.theme.LightOnErrorContainer
import com.example.ui.theme.LightOnSurface
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightSurface
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.AsyncUiState

private fun isFieldUnclear(value: String): Boolean {
    val v = value.trim().lowercase()
    return v.isEmpty() || v == "not readable" || v == "unreadable" || v == "unclear" || v == "missing" || v == "n/a" || v == "unknown"
}

@Composable
fun NpkReportScreen(
    defaultCropName: String,
    labState: AsyncUiState<NpkLabExtractionResult>,
    regionalState: AsyncUiState<NpkRegionalReferenceResult>,
    onExtractLabReport: (Uri) -> Unit,
    onResetLab: () -> Unit,
    onLoadDemoLab: () -> Unit,
    onSaveVerifiedLab: (String, String, String, String, String, String, Boolean, Uri?) -> Unit,
    onSearchRegionalReference: (String, String, String) -> Unit,
    onSaveRegionalReference: (NpkRegionalReferenceResult) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedMethod by rememberSaveable { mutableIntStateOf(0) } // 0 = Scan Lab Card, 1 = Regional Guide
    var uploadedReportUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("npk_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Scan Hub")
            }
            Column(modifier = Modifier.weight(1f).padding(top = 8.dp, end = 8.dp)) {
                Text("NPK Report", style = MaterialTheme.typography.headlineMedium, color = LightOnSurface)
                Text(
                    text = "Scan a Soil Health Card photo or view a cautious regional nutrient guide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightOnSurfaceVariant
                )
            }
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
                    Text("1. Scan Lab Card")
                }
                SegmentedButton(
                    selected = selectedMethod == 1,
                    onClick = { selectedMethod = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("npk_tab_regional")
                ) {
                    Text("2. Regional Guide")
                }
            }

            if (selectedMethod == 0) {
                PhotoPickerAndCameraBar(
                    selectedImageUri = uploadedReportUri,
                    isLoading = labState is AsyncUiState.Loading,
                    hasResult = labState is AsyncUiState.Success,
                    onImageSelected = { uri ->
                        uploadedReportUri = uri
                        onResetLab()
                    },
                    onStartScan = { uri -> onExtractLabReport(uri) },
                    onLoadDemoSample = {
                        uploadedReportUri = null
                        onLoadDemoLab()
                    },
                    demoLabel = "Load Sample Lab Card (Demo)"
                )

                when (labState) {
                    is AsyncUiState.Idle -> Unit
                    is AsyncUiState.Loading -> LoadingAnalysisCard("Analysing...")
                    is AsyncUiState.Error -> ErrorAnalysisCard(
                        message = labState.message,
                        onTryAgain = uploadedReportUri?.let { uri -> { onExtractLabReport(uri) } }
                    )
                    is AsyncUiState.Success -> {
                        val res = labState.data
                        var nVal by remember(res) { mutableStateOf(res.nitrogen) }
                        var pVal by remember(res) { mutableStateOf(res.phosphorus) }
                        var kVal by remember(res) { mutableStateOf(res.potassium) }
                        var unitVal by remember(res) { mutableStateOf(res.unit) }
                        var labName by remember(res) { mutableStateOf(res.labSource) }
                        var dateVal by remember(res) { mutableStateOf(res.reportDate) }
                        var saved by remember(res) { mutableStateOf(false) }
                        val stillUnreadable = res.isUnreadable && isFieldUnclear(nVal) && isFieldUnclear(pVal) && isFieldUnclear(kVal)

                        ClayCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SCAN COMPLETE", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                                    Text("Check Extracted NPK", style = MaterialTheme.typography.titleLarge)
                                }
                                StatusPill(
                                    text = if (res.isDemo) "DEMO" else if (stillUnreadable) "UNCLEAR" else if (saved) "SAVED" else "EDIT & SAVE",
                                    containerColor = if (res.isDemo) WarningContainer else if (stillUnreadable) LightErrorContainer else SuccessContainer,
                                    contentColor = if (res.isDemo) OnWarningContainer else if (stillUnreadable) LightOnErrorContainer else OnSuccessContainer
                                )
                            }

                            if (stillUnreadable) {
                                Spacer(modifier = Modifier.height(8.dp))
                                ClayCard(containerColor = LightErrorContainer) {
                                    Text(
                                        text = "Could not clearly read N, P, K numbers from this photo. Take a sharper photo or enter values from your card below.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LightOnErrorContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(res.extractionNotes, style = MaterialTheme.typography.bodySmall, color = LightOnSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = nVal,
                                    onValueChange = { nVal = it; saved = false },
                                    label = { Text("N") },
                                    isError = isFieldUnclear(nVal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = pVal,
                                    onValueChange = { pVal = it; saved = false },
                                    label = { Text("P") },
                                    isError = isFieldUnclear(pVal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = kVal,
                                    onValueChange = { kVal = it; saved = false },
                                    label = { Text("K") },
                                    isError = isFieldUnclear(kVal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = unitVal,
                                    onValueChange = { unitVal = it; saved = false },
                                    label = { Text("Unit") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = dateVal,
                                    onValueChange = { dateVal = it; saved = false },
                                    label = { Text("Report Date") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = labName,
                                onValueChange = { labName = it; saved = false },
                                label = { Text("Lab / Soil Health Card Source") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ClayPrimaryButton(
                                    text = if (saved) "Saved" else "Save Result",
                                    icon = Icons.Default.CheckCircle,
                                    enabled = !saved,
                                    onClick = {
                                        onSaveVerifiedLab(nVal, pVal, kVal, unitVal, labName, dateVal, res.isDemo, uploadedReportUri)
                                        saved = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "save_verified_lab_button"
                                )
                                OutlinedButton(
                                    onClick = {
                                        uploadedReportUri = null
                                        onResetLab()
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan Again")
                                }
                            }
                        }
                    }
                }
            } else {
                var cropInput by remember { mutableStateOf(defaultCropName) }
                var regionInput by remember { mutableStateOf("") }
                var soilTypeInput by remember { mutableStateOf("") }

                ClayCard(containerColor = WarningContainer.copy(alpha = 0.7f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = OnWarningContainer, modifier = Modifier.size(20.dp))
                        Text(
                            text = "REFERENCE ESTIMATE — NOT A LAB SOIL TEST. General area guidance only; never invents field NPK numbers.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = OnWarningContainer
                        )
                    }
                }

                ClayCard {
                    Text("Regional Guide", style = MaterialTheme.typography.titleLarge)
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
                        text = "Get Regional Guide",
                        icon = Icons.Default.Public,
                        onClick = { onSearchRegionalReference(cropInput, regionInput, soilTypeInput) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "fetch_regional_npk_button"
                    )
                }

                when (regionalState) {
                    is AsyncUiState.Idle -> Unit
                    is AsyncUiState.Loading -> LoadingAnalysisCard("Analysing...")
                    is AsyncUiState.Error -> ErrorAnalysisCard(
                        message = regionalState.message,
                        onTryAgain = { onSearchRegionalReference(cropInput, regionInput, soilTypeInput) }
                    )
                    is AsyncUiState.Success -> {
                        val res = regionalState.data
                        var regSaved by remember(res) { mutableStateOf(false) }
                        ClayCard {
                            StatusPill(
                                text = "REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST",
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
                            Spacer(modifier = Modifier.height(8.dp))
                            ClayPrimaryButton(
                                text = if (regSaved) "Saved to Reports" else "Save Result",
                                icon = Icons.Default.Save,
                                enabled = !regSaved,
                                onClick = {
                                    onSaveRegionalReference(res)
                                    regSaved = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "save_regional_npk_button"
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
