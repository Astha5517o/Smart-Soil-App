package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.BleAndHardwareSheet
import com.example.ui.screens.CropIdentificationScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.NpkReportScreen
import com.example.ui.screens.PlantHealthScanScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ScanHubScreen
import com.example.ui.screens.SoilScanScreen
import com.example.ui.theme.ClayShadowBrown
import com.example.ui.theme.LightOnSecondaryContainer
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSecondaryContainer
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceContainerLowest
import com.example.ui.viewmodel.SmartSoilViewModel

object Routes {
    const val HOME = "home"
    const val SCAN_HUB = "scan_hub"
    const val SCAN_SOIL = "scan_soil"
    const val SCAN_CROP = "scan_crop"
    const val SCAN_PLANT = "scan_plant"
    const val SCAN_NPK = "scan_npk"
    const val ADVISOR = "advisor"
    const val REPORTS = "reports"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val primaryTabs = listOf(
    BottomNavTab(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    BottomNavTab(Routes.SCAN_HUB, "Scan", Icons.Filled.PhotoCamera, Icons.Outlined.PhotoCamera, "nav_scan"),
    BottomNavTab(Routes.ADVISOR, "AI Advisor", Icons.Filled.Psychology, Icons.Outlined.Psychology, "nav_advisor"),
    BottomNavTab(Routes.REPORTS, "Reports", Icons.Filled.Description, Icons.Outlined.Description, "nav_reports")
)

@Composable
fun SmartSoilApp(
    viewModel: SmartSoilViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val bleState by viewModel.bleState.collectAsStateWithLifecycle()
    val displayMode by viewModel.displayMode.collectAsStateWithLifecycle()
    val activeReading by viewModel.activeReading.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()

    val soilScanState by viewModel.soilScanState.collectAsStateWithLifecycle()
    val cropIdState by viewModel.cropIdState.collectAsStateWithLifecycle()
    val plantHealthState by viewModel.plantHealthState.collectAsStateWithLifecycle()
    val npkLabState by viewModel.npkLabState.collectAsStateWithLifecycle()
    val npkRegionalState by viewModel.npkRegionalState.collectAsStateWithLifecycle()
    val advisorState by viewModel.advisorState.collectAsStateWithLifecycle()
    val statusToast by viewModel.statusToast.collectAsStateWithLifecycle()

    var showBleSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusToast) {
        val msg = statusToast
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun isTabSelected(tabRoute: String): Boolean {
        if (tabRoute == Routes.SCAN_HUB) {
            val route = currentDestination?.route
            return route == Routes.SCAN_HUB ||
                route == Routes.SCAN_SOIL ||
                route == Routes.SCAN_CROP ||
                route == Routes.SCAN_PLANT ||
                route == Routes.SCAN_NPK
        }
        return currentDestination?.hierarchy?.any { it.route == tabRoute } == true
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = LightSurface,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = LightSurfaceContainerLowest,
                        modifier = Modifier
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                ambientColor = ClayShadowBrown,
                                spotColor = ClayShadowBrown
                            )
                    ) {
                        NavigationBar(
                            containerColor = LightSurfaceContainerLowest,
                            tonalElevation = 0.dp,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            primaryTabs.forEach { tab ->
                                val selected = isTabSelected(tab.route)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = LightPrimary,
                                        selectedTextColor = LightPrimary,
                                        indicatorColor = LightSecondaryContainer.copy(alpha = 0.7f),
                                        unselectedIconColor = LightOnSurfaceVariant,
                                        unselectedTextColor = LightOnSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = LightSurfaceContainerLowest,
                        modifier = Modifier.fillMaxHeight().statusBarsPadding()
                    ) {
                        primaryTabs.forEach { tab ->
                            val selected = isTabSelected(tab.route)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = LightPrimary,
                                    selectedTextColor = LightPrimary,
                                    indicatorColor = LightSecondaryContainer,
                                    unselectedIconColor = LightOnSurfaceVariant,
                                    unselectedTextColor = LightOnSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME
                    ) {
                        composable(Routes.HOME) {
                            HomeDashboardScreen(
                                preferences = preferences,
                                bleState = bleState,
                                displayMode = displayMode,
                                activeReading = activeReading,
                                onSelectDisplayMode = { viewModel.setDisplayMode(it) },
                                onOpenBleSheet = { showBleSheet = true },
                                onStartScan = { navigateTopTab(Routes.SCAN_HUB) },
                                onOpenSoilScan = { navController.navigate(Routes.SCAN_SOIL) },
                                onOpenCropId = { navController.navigate(Routes.SCAN_CROP) },
                                onOpenPlantHealth = { navController.navigate(Routes.SCAN_PLANT) },
                                onOpenNpkReport = { navController.navigate(Routes.SCAN_NPK) },
                                onOpenAiAdvisor = { navigateTopTab(Routes.ADVISOR) },
                                onSaveSensorSnapshot = { viewModel.saveCurrentSensorSnapshot() }
                            )
                        }

                        composable(Routes.SCAN_HUB) {
                            ScanHubScreen(
                                selectedCropName = preferences.selectedCropName,
                                onOpenSoilScan = { navController.navigate(Routes.SCAN_SOIL) },
                                onOpenCropId = { navController.navigate(Routes.SCAN_CROP) },
                                onOpenPlantHealth = { navController.navigate(Routes.SCAN_PLANT) },
                                onOpenNpkReport = { navController.navigate(Routes.SCAN_NPK) }
                            )
                        }

                        composable(Routes.SCAN_SOIL) {
                            SoilScanScreen(
                                uiState = soilScanState,
                                onAnalyzePhoto = { viewModel.analyzeSoilPhoto(it) },
                                onSaveResult = { res, uri -> viewModel.saveSoilScanResult(res, uri) },
                                onResetScan = { viewModel.resetSoilScan() },
                                onLoadDemo = { viewModel.loadDemoSoilScan() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.SCAN_CROP) {
                            CropIdentificationScreen(
                                uiState = cropIdState,
                                onIdentifyPhoto = { viewModel.identifyCropPhoto(it) },
                                onResetScan = { viewModel.resetCropId() },
                                onLoadDemo = { viewModel.loadDemoCropId() },
                                onConfirmAndSave = { crop, stage, res, uri ->
                                    viewModel.confirmAndSaveCrop(crop, stage, res, uri)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.SCAN_PLANT) {
                            PlantHealthScanScreen(
                                selectedCropName = preferences.selectedCropName,
                                uiState = plantHealthState,
                                onAnalyzePhoto = { viewModel.analyzePlantHealthPhoto(it) },
                                onSaveResult = { res, uri -> viewModel.savePlantHealthResult(res, uri) },
                                onResetScan = { viewModel.resetPlantHealth() },
                                onLoadDemo = { viewModel.loadDemoPlantHealth() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.SCAN_NPK) {
                            NpkReportScreen(
                                defaultCropName = preferences.selectedCropName,
                                labState = npkLabState,
                                regionalState = npkRegionalState,
                                onExtractLabReport = { viewModel.extractNpkLabReport(it) },
                                onResetLab = { viewModel.resetNpkLab() },
                                onLoadDemoLab = { viewModel.loadDemoLabReport() },
                                onSaveVerifiedLab = { n, p, k, u, src, dt, isDemo, uri ->
                                    viewModel.saveVerifiedLabReport(n, p, k, u, src, dt, isDemo, uri)
                                },
                                onSearchRegionalReference = { c, r, s ->
                                    viewModel.fetchRegionalNpkReference(c, r, s)
                                },
                                onSaveRegionalReference = { res ->
                                    viewModel.saveRegionalNpkReference(res)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.ADVISOR) {
                            AiAdvisorScreen(
                                contextSnapshot = viewModel.buildAdvisorContextSnapshot(),
                                uiState = advisorState,
                                onGenerateAdvice = { topic, q -> viewModel.requestAiAdvice(topic, q) },
                                onLoadDemoAdvice = { viewModel.loadDemoAdvice() },
                                onSavePlan = { viewModel.saveAdvisoryPlanToReports(it) }
                            )
                        }

                        composable(Routes.REPORTS) {
                            ReportsScreen(
                                records = allRecords,
                                onDeleteRecord = { viewModel.deleteRecord(it) },
                                onClearDemoRecords = { viewModel.clearDemoRecords() },
                                onSeedDemoRecords = { viewModel.seedDemoRecords() },
                                onResetAllData = { viewModel.resetAllProjectData() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBleSheet) {
        BleAndHardwareSheet(
            bleManager = viewModel.bleManager,
            bleState = bleState,
            preferences = preferences,
            onUpdateCrop = { crop, stage -> viewModel.updateCropAndStage(crop, stage) },
            onUpdateThresholds = { minM, maxM, minP, maxP ->
                viewModel.updateThresholds(minM, maxM, minP, maxP)
            },
            onUpdateCalibration = { dry, wet -> viewModel.updateCalibration(dry, wet) },
            onDismiss = { showBleSheet = false }
        )
    }
}
