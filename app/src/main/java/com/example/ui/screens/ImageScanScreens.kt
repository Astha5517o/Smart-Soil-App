package com.example.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.LightError
import com.example.ui.theme.LightErrorContainer
import com.example.ui.theme.LightOnError
import com.example.ui.theme.LightOnErrorContainer
import com.example.ui.theme.LightOnSurface
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceContainerLow
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.AsyncUiState
import java.io.File

@Composable
fun PhotoPickerAndCameraBar(
    selectedImageUri: Uri?,
    isLoading: Boolean,
    hasResult: Boolean,
    onImageSelected: (Uri) -> Unit,
    onStartScan: (Uri) -> Unit,
    onLoadDemoSample: () -> Unit,
    demoLabel: String = "Load Demo Sample",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
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

    fun launchCameraCapture() {
        try {
            val uri = createTempCaptureUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (_: ActivityNotFoundException) {
            permissionDeniedNote = "No camera app available. Use Upload Image to pick a photo."
        } catch (e: Exception) {
            permissionDeniedNote = "Could not open camera (${e.localizedMessage ?: "error"}). Use Upload Image instead."
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            permissionDeniedNote = "Camera permission was denied. Use Upload Image to pick a photo from your gallery."
        }
    }

    ClayCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClayPrimaryButton(
                text = "Take Photo",
                icon = Icons.Default.CameraAlt,
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        launchCameraCapture()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f),
                testTag = "take_photo_button"
            )
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("pick_photo_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload Image")
            }
        }

        if (selectedImageUri != null) {
            Spacer(modifier = Modifier.height(12.dp))
            AsyncImage(
                model = selectedImageUri,
                contentDescription = "Selected photo preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            if (!isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                ClayPrimaryButton(
                    text = if (hasResult) "Scan Photo Again" else "Start Scan",
                    icon = Icons.Default.Search,
                    onClick = { onStartScan(selectedImageUri) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "start_scan_button"
                )
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

        if (!isLoading) {
            Spacer(modifier = Modifier.height(4.dp))
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
}

private fun createTempCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
private fun ScanHeader(title: String, explanation: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("scan_back_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Scan Hub")
        }
        Column(modifier = Modifier.weight(1f).padding(top = 8.dp, end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = LightOnSurface
            )
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = LightOnSurfaceVariant
            )
        }
    }
}

// ==========================================
// A. SOIL SCAN SCREEN
// ==========================================
@Composable
fun SoilScanScreen(
    uiState: AsyncUiState<SoilScanResult>,
    onAnalyzePhoto: (Uri) -> Unit,
    onSaveResult: (SoilScanResult, Uri?) -> Unit,
    onResetScan: () -> Unit,
    onLoadDemo: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
    ) {
        ScanHeader(
            title = "Soil Scan",
            explanation = "Take or upload a clear daylight soil photo to check texture and type.",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PhotoPickerAndCameraBar(
                selectedImageUri = selectedUri,
                isLoading = uiState is AsyncUiState.Loading,
                hasResult = uiState is AsyncUiState.Success,
                onImageSelected = { uri ->
                    selectedUri = uri
                    onResetScan()
                },
                onStartScan = { uri -> onAnalyzePhoto(uri) },
                onLoadDemoSample = {
                    selectedUri = null
                    onLoadDemo()
                }
            )

            when (uiState) {
                is AsyncUiState.Idle -> Unit
                is AsyncUiState.Loading -> LoadingAnalysisCard("Analysing...")
                is AsyncUiState.Error -> ErrorAnalysisCard(
                    message = uiState.message,
                    onTryAgain = selectedUri?.let { uri -> { onAnalyzePhoto(uri) } }
                )
                is AsyncUiState.Success -> {
                    val res = uiState.data
                    var saved by remember(res) { mutableStateOf(false) }

                    ClayCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SCAN COMPLETE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessGreen
                                )
                                Text(res.possibleSoilType, style = MaterialTheme.typography.titleLarge, color = LightOnSurface)
                            }
                            StatusPill(
                                text = if (res.isDemo) "DEMO" else if (saved) "SAVED" else "READY",
                                containerColor = if (res.isDemo) WarningContainer else SuccessContainer,
                                contentColor = if (res.isDemo) OnWarningContainer else OnSuccessContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LabeledSection("Visible Look", res.visibleCharacteristics)
                        LabeledSection("Organic Matter", res.organicMatterIndicators)
                        LabeledSection("Note", res.uncertaintyExplanation)
                        BulletSection("Helpful Next Tests", res.recommendedAdditionalTests)

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClayPrimaryButton(
                                text = if (saved) "Saved" else "Save Result",
                                icon = Icons.Default.Save,
                                enabled = !saved,
                                onClick = {
                                    onSaveResult(res, selectedUri)
                                    saved = true
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "save_soil_scan_button"
                            )
                            OutlinedButton(
                                onClick = {
                                    selectedUri = null
                                    onResetScan()
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
    onResetScan: () -> Unit,
    onLoadDemo: () -> Unit,
    onConfirmAndSave: (String, String, CropIdResult, Uri?) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
    ) {
        ScanHeader(
            title = "Identify Crop",
            explanation = "Take or upload a photo of your plant leaves to confirm your active crop.",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PhotoPickerAndCameraBar(
                selectedImageUri = selectedUri,
                isLoading = uiState is AsyncUiState.Loading,
                hasResult = uiState is AsyncUiState.Success,
                onImageSelected = { uri ->
                    selectedUri = uri
                    onResetScan()
                },
                onStartScan = { uri -> onIdentifyPhoto(uri) },
                onLoadDemoSample = {
                    selectedUri = null
                    onLoadDemo()
                }
            )

            when (uiState) {
                is AsyncUiState.Idle -> Unit
                is AsyncUiState.Loading -> LoadingAnalysisCard("Analysing...")
                is AsyncUiState.Error -> ErrorAnalysisCard(
                    message = uiState.message,
                    onTryAgain = selectedUri?.let { uri -> { onIdentifyPhoto(uri) } }
                )
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SCAN COMPLETE", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                                Text("Confirm Your Crop", style = MaterialTheme.typography.titleLarge)
                            }
                            StatusPill(
                                text = if (res.isDemo) "DEMO · ${res.confidenceLevel}" else "${res.confidenceLevel} Confidence",
                                containerColor = if (res.isDemo) WarningContainer else LightSurfaceContainerLow,
                                contentColor = if (res.isDemo) OnWarningContainer else LightOnSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LabeledSection("Visible Features", res.identifyingFeatures)
                        LabeledSection("Check", res.uncertaintyNotes)

                        OutlinedTextField(
                            value = confirmedCrop,
                            onValueChange = { confirmedCrop = it; confirmedDone = false },
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
                                            confirmedDone = false
                                            stageExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClayPrimaryButton(
                                text = if (confirmedDone) "Saved Active Crop" else "Save Result",
                                icon = Icons.Default.CheckCircle,
                                enabled = confirmedCrop.isNotBlank() && !confirmedDone,
                                onClick = {
                                    onConfirmAndSave(confirmedCrop.trim(), confirmedStage, res, selectedUri)
                                    confirmedDone = true
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "confirm_crop_save_button"
                            )
                            OutlinedButton(
                                onClick = {
                                    selectedUri = null
                                    onResetScan()
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
    onSaveResult: (PlantHealthResult, Uri?) -> Unit,
    onResetScan: () -> Unit,
    onLoadDemo: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
    ) {
        ScanHeader(
            title = "Plant Health",
            explanation = "Take or upload a close-up leaf photo for $selectedCropName to check visible stress.",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PhotoPickerAndCameraBar(
                selectedImageUri = selectedUri,
                isLoading = uiState is AsyncUiState.Loading,
                hasResult = uiState is AsyncUiState.Success,
                onImageSelected = { uri ->
                    selectedUri = uri
                    onResetScan()
                },
                onStartScan = { uri -> onAnalyzePhoto(uri) },
                onLoadDemoSample = {
                    selectedUri = null
                    onLoadDemo()
                }
            )

            when (uiState) {
                is AsyncUiState.Idle -> Unit
                is AsyncUiState.Loading -> LoadingAnalysisCard("Analysing...")
                is AsyncUiState.Error -> ErrorAnalysisCard(
                    message = uiState.message,
                    onTryAgain = selectedUri?.let { uri -> { onAnalyzePhoto(uri) } }
                )
                is AsyncUiState.Success -> {
                    val res = uiState.data
                    var saved by remember(res) { mutableStateOf(false) }

                    ClayCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SCAN COMPLETE", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                                Text("Leaf & Plant Check", style = MaterialTheme.typography.titleLarge)
                            }
                            StatusPill(
                                text = if (res.isDemo) "DEMO" else if (saved) "SAVED" else "READY",
                                containerColor = if (res.isDemo) WarningContainer else SuccessContainer,
                                contentColor = if (res.isDemo) OnWarningContainer else OnSuccessContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LabeledSection("Visible Signs", res.visibleSymptoms)
                        BulletSection("Possible Reasons (Not Final Diagnosis)", res.possibleExplanations)
                        BulletSection("What to Check in Field", res.suggestedChecks)
                        BulletSection("Safe Next Steps", res.cautiousNextSteps)
                        LabeledSection("Important Limitation", res.diagnosisLimitations)

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClayPrimaryButton(
                                text = if (saved) "Saved" else "Save Result",
                                icon = Icons.Default.Save,
                                enabled = !saved,
                                onClick = {
                                    onSaveResult(res, selectedUri)
                                    saved = true
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "save_plant_health_button"
                            )
                            OutlinedButton(
                                onClick = {
                                    selectedUri = null
                                    onResetScan()
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
            Column {
                Text(text = message, style = MaterialTheme.typography.titleMedium, color = LightOnSurface)
                Text(
                    text = "Checking your photo with FarmerBuddy AI...",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ErrorAnalysisCard(
    message: String,
    onTryAgain: (() -> Unit)? = null
) {
    ClayCard(containerColor = LightErrorContainer) {
        Text("Scan could not finish", style = MaterialTheme.typography.titleMedium, color = LightOnErrorContainer)
        Spacer(modifier = Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = LightOnErrorContainer)
        if (onTryAgain != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onTryAgain,
                colors = ButtonDefaults.buttonColors(containerColor = LightError, contentColor = LightOnError),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("Try Again")
            }
        }
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
