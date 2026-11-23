package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.CropCatalog
import com.example.data.model.CropIdResult
import com.example.data.model.PlantHealthResult
import com.example.data.model.SoilScanResult
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.LightErrorContainer
import com.example.ui.theme.LightOnErrorContainer
import com.example.ui.theme.LightOnPrimary
import com.example.ui.theme.LightOnSurface
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceContainerLow
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.AsyncUiState
import java.io.File

@Composable
fun PhotoPickerAndCameraBar(
    selectedImageUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    onLoadDemoSample: () -> Unit,
    demoLabel: String = "Load Demo Sample Result",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var permissionDeniedNote by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            permissionDeniedNote = null
            onImageSelected(uri)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            permissionDeniedNote = null
            onImageSelected(uri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempCaptureUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            permissionDeniedNote = "Camera permission was denied. You can still pick an image from your gallery."
        }
    }

    ClayCard(modifier = modifier.fillMaxWidth()) {
        if (selectedImageUri != null) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = "Selected photo preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClayPrimaryButton(
                text = "Pick Photo",
                icon = Icons.Default.PhotoLibrary,
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f),
                testTag = "pick_photo_button"
            )
            OutlinedButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        val uri = createTempCaptureUri(context)
                        pendingCameraUri = uri
                        takePictureLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("take_photo_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Take Photo")
            }
        }

        if (permissionDeniedNote != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = permissionDeniedNote!!,
                style = MaterialTheme.typography.bodySmall,
                color = OnWarningContainer
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        TextButton(
            onClick = onLoadDemoSample,
            modifier = Modifier
                .align(Alignment.End)
                .minimumInteractiveComponentSize()
                .testTag("load_demo_sample_button")
        ) {
            Text(demoLabel)
        }
    }
}

private fun createTempCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
private fun ScanTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("scan_back_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Scan Hub")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = LightOnSurface
        )
    }
}

// ==========================================
// A. SOIL SCAN SCREEN
// ==========================================
@Composable
fun SoilScanScreen(
    uiState: AsyncUiState<SoilScanResult>,
    onAnalyzePhoto: (Uri) -> Unit,
    onLoadDemo: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
    ) {
        ScanTopBar(title = "Soil Scan", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ClayCard(containerColor = LightSurfaceContainerLow) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = LightPrimary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Visual soil assessment only. An image cannot measure soil pH, moisture %, or NPK nutrients—use your ESP32 probes or a lab test for chemical values.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightOnSurfaceVariant
                    )
                }
            }

            PhotoPickerAndCameraBar(
                selectedImageUri = selectedUri,
                onImageSelected = { uri ->
                    selectedUri = uri
                    onAnalyzePhoto(uri)
                },
                onLoadDemoSample = onLoadDemo
            )

            when (uiState) {
                is AsyncUiState.Idle -> {
                    ClayCard {
                        Text(
                            text = "Take or upload a clear daylight photo of topsoil crumbs to begin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LightOnSurfaceVariant
                        )
                    }
                }
                is AsyncUiState.Loading -> LoadingAnalysisCard("Analyzing soil characteristics with Gemini Vision...")
                is AsyncUiState.Error -> ErrorAnalysisCard(uiState.message)
                is AsyncUiState.Success -> {
                    val res = uiState.data
                    ClayCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Soil Scan Result", style = MaterialTheme.typography.titleLarge, color = LightOnSurface)
                            StatusPill(
                                text = if (res.isDemo) "DEMO SAMPLE" else "SAVED TO REPORTS",
                                containerColor = if (res.isDemo) WarningContainer else SuccessContainer,
                                contentColor = if (res.isDemo) OnWarningContainer else OnSuccessContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LabeledSection("Possible Soil Type", res.possibleSoilType)
                        LabeledSection("Visible Characteristics", res.visibleCharacteristics)
                        LabeledSection("Organic Matter Indicators", res.organicMatterIndicators)
                        LabeledSection("Uncertainty & Limitations", res.uncertaintyExplanation)

                        Text(
                            text = "Recommended Additional Tests",
                            style = MaterialTheme.typography.labelLarge,
                            color = LightOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        res.recommendedAdditionalTests.forEach { test ->
                            Text("• $test", style = MaterialTheme.typography.bodySmall, color = LightOnSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ==========================================
// B. CROP IDENTIFICATION SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropIdentificationScreen(
    uiState: AsyncUiState<CropIdResult>,
    onIdentifyPhoto: (Uri) -> Unit,
    onLoadDemo: () -> Unit,
    onConfirmAndSave: (String, String, CropIdResult, Uri?) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
    ) {
        ScanTopBar(title = "Crop Identification", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PhotoPickerAndCameraBar(
                selectedImageUri = selectedUri,
                onImageSelected = { uri ->
                    selectedUri = uri
                    onIdentifyPhoto(uri)
                },
                onLoadDemoSample = onLoadDemo
            )

            when (uiState) {
                is AsyncUiState.Idle -> {
                    ClayCard {
                        Text(
                            text = "Upload or capture a photo of your crop leaves or canopy. You will review and confirm the crop before it is saved.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LightOnSurfaceVariant
                        )
                    }
                }
                is AsyncUiState.Loading -> LoadingAnalysisCard("Identifying crop features with Gemini Vision...")
                is AsyncUiState.Error -> ErrorAnalysisCard(uiState.message)
                is AsyncUiState.Success -> {
                    val res = uiState.data
                    var confirmedCrop by remember(res) { mutableStateOf(res.possibleCropName) }
                    var confirmedStage by remember(res) { mutableStateOf(res.suggestedGrowthStage) }
                    var stageExpanded by remember { mutableStateOf(false) }
                    var confirmedDone by remember(res) { mutableStateOf(false) }

                    ClayCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Possible Crop Match", style = MaterialTheme.typography.titleLarge)
                            StatusPill(
                                text = if (res.isDemo) "DEMO • ${res.confidenceLevel}" else "${res.confidenceLevel} Confidence",
                                containerColor = if (res.isDemo) WarningContainer else LightSurfaceContainerLow,
                                contentColor = if (res.isDemo) OnWarningContainer else LightOnSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LabeledSection("Visible Identifying Features", res.identifyingFeatures)
                        LabeledSection("Confidence & Uncertainty", res.uncertaintyNotes)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Confirm Before Saving",
                            style = MaterialTheme.typography.titleMedium,
                            color = LightPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = confirmedCrop,
                            onValueChange = { confirmedCrop = it },
                            label = { Text("Crop Name (edit if needed)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("confirm_crop_name_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = stageExpanded,
                            onExpandedChange = { stageExpanded = !stageExpanded }
                        ) {
                            OutlinedTextField(
                                value = confirmedStage,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Growth Stage") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = stageExpanded,
                                onDismissRequest = { stageExpanded = false }
                            ) {
                                CropCatalog.growthStages.forEach { stage ->
                                    DropdownMenuItem(
                                        text = { Text(stage) },
                                        onClick = {
                                            confirmedStage = stage
                                            stageExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        ClayPrimaryButton(
                            text = if (confirmedDone) "Saved as Active Crop" else "Confirm & Set Active Crop",
                            icon = Icons.Default.CheckCircle,
                            enabled = confirmedCrop.isNotBlank() && !confirmedDone,
                            onClick = {
                                onConfirmAndSave(confirmedCrop.trim(), confirmedStage, res, selectedUri)
                                confirmedDone = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "confirm_crop_save_button"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ==========================================
// C. PLANT HEALTH SCAN SCREEN
// ==========================================
@Composable
fun PlantHealthScanScreen(
    selectedCropName: String,
    uiState: AsyncUiState<PlantHealthResult>,
    onAnalyzePhoto: (Uri) -> Unit,
    onLoadDemo: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
    ) {
        ScanTopBar(title = "Plant Health Scan", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ClayCard(containerColor = WarningContainer.copy(alpha = 0.7f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = OnWarningContainer, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Cautious screening for $selectedCropName: an image alone cannot provide a definitive disease diagnosis. Use results to guide field checks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnWarningContainer
                    )
                }
            }

            PhotoPickerAndCameraBar(
                selectedImageUri = selectedUri,
                onImageSelected = { uri ->
                    selectedUri = uri
                    onAnalyzePhoto(uri)
                },
                onLoadDemoSample = onLoadDemo
            )

            when (uiState) {
                is AsyncUiState.Idle -> {
                    ClayCard {
                        Text(
                            text = "Upload or capture a close-up photo of leaves or stems showing spots, discoloration, or wilting.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LightOnSurfaceVariant
                        )
                    }
                }
                is AsyncUiState.Loading -> LoadingAnalysisCard("Inspecting visible plant symptoms...")
                is AsyncUiState.Error -> ErrorAnalysisCard(uiState.message)
                is AsyncUiState.Success -> {
                    val res = uiState.data
                    ClayCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Plant Health Observations", style = MaterialTheme.typography.titleLarge)
                            StatusPill(
                                text = if (res.isDemo) "DEMO SAMPLE" else "SAVED TO REPORTS",
                                containerColor = if (res.isDemo) WarningContainer else SuccessContainer,
                                contentColor = if (res.isDemo) OnWarningContainer else OnSuccessContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LabeledSection("Visible Symptoms", res.visibleSymptoms)
                        BulletSection("Possible Explanations (Non-Definitive)", res.possibleExplanations)
                        BulletSection("Suggested Field Checks", res.suggestedChecks)
                        BulletSection("Cautious Next Steps", res.cautiousNextSteps)
                        LabeledSection("Limitations of Image-Based Diagnosis", res.diagnosisLimitations)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LoadingAnalysisCard(message: String) {
    ClayCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LightPrimary, strokeWidth = 2.5.dp)
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = LightOnSurface)
        }
    }
}

@Composable
fun ErrorAnalysisCard(message: String) {
    ClayCard(containerColor = LightErrorContainer) {
        Text("Could not complete analysis", style = MaterialTheme.typography.titleMedium, color = LightOnErrorContainer)
        Spacer(modifier = Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = LightOnErrorContainer)
    }
}

@Composable
fun LabeledSection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = LightOnSurface)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = body, style = MaterialTheme.typography.bodySmall, color = LightOnSurfaceVariant)
    }
}

@Composable
fun BulletSection(title: String, items: List<String>) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = LightOnSurface)
        Spacer(modifier = Modifier.height(2.dp))
        items.forEach { item ->
            Text(text = "• $item", style = MaterialTheme.typography.bodySmall, color = LightOnSurfaceVariant)
        }
    }
}
