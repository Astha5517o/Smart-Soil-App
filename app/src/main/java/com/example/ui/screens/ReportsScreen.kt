package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.SoilRecordEntity
import com.example.data.model.RecordCategory
import com.example.data.model.RecordOrigin
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.LightErrorContainer
import com.example.ui.theme.LightError
import com.example.ui.theme.LightOnError
import com.example.ui.theme.LightOnErrorContainer
import com.example.ui.theme.LightOnPrimary
import com.example.ui.theme.LightOnSurface
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSecondaryContainer
import com.example.ui.theme.LightOnSecondaryContainer
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceContainer
import com.example.ui.theme.MonoSmallStyle
import com.example.ui.theme.OnSoilContainer
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.SoilContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.WarningContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    records: List<SoilRecordEntity>,
    onDeleteRecord: (String) -> Unit,
    onClearDemoRecords: () -> Unit,
    onSeedDemoRecords: () -> Unit,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<RecordCategory?>(null) }
    var selectedRecord by remember { mutableStateOf<SoilRecordEntity?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val filteredRecords = remember(records, searchQuery, selectedCategory) {
        records.filter { rec ->
            val matchesCategory = selectedCategory == null || rec.category == selectedCategory!!.name
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                rec.title.contains(q, ignoreCase = true) ||
                rec.summary.contains(q, ignoreCase = true) ||
                rec.dataSource.contains(q, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightSurface)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("FARMERBUDDY · SMART SOIL", style = MaterialTheme.typography.labelSmall, color = LightPrimary)
                Text("Reports & History", style = MaterialTheme.typography.headlineLarge, color = LightOnSurface)
                Text(
                    text = "${records.size} saved record(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightOnSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (records.any { it.isDemo }) {
                    IconButton(
                        onClick = onClearDemoRecords,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_demo_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Demo Records", tint = LightPrimary)
                    }
                }
                IconButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("reset_all_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset All Data", tint = LightError)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search reports, crops, or sources...") },
            singleLine = true,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth().testTag("reports_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipPill(
                label = "All",
                selected = selectedCategory == null,
                onClick = { selectedCategory = null }
            )
            RecordCategory.entries.forEach { cat ->
                FilterChipPill(
                    label = cat.label,
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredRecords.isEmpty()) {
            ClayCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = if (records.isEmpty()) "No Reports Saved Yet" else "No Matching Reports",
                        style = MaterialTheme.typography.titleLarge,
                        color = LightOnSurface
                    )
                    Text(
                        text = if (records.isEmpty()) {
                            "Save a sensor snapshot, scan result, NPK report, or AI advisory plan—or load illustrative demo records for exhibition preview."
                        } else {
                            "Try clearing your search or category filter."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LightOnSurfaceVariant
                    )
                    if (records.isEmpty()) {
                        ClayPrimaryButton(
                            text = "Load Exhibition Sample Records",
                            onClick = onSeedDemoRecords,
                            testTag = "seed_demo_reports_button"
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRecords, key = { it.id }) { rec ->
                    val origin = runCatching { RecordOrigin.valueOf(rec.originBadge) }
                        .getOrDefault(if (rec.isDemo) RecordOrigin.DEMO_DATA else RecordOrigin.AI_INTERPRETATION)
                    val (badgeBg, badgeFg) = originColors(origin)

                    ClayCard(
                        modifier = Modifier.fillMaxWidth().testTag("report_card_${rec.id}"),
                        onClick = { selectedRecord = rec }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(text = origin.badgeText, containerColor = badgeBg, contentColor = badgeFg)
                            Text(
                                text = dateFormatter.format(Date(rec.timestampMs)),
                                style = MonoSmallStyle,
                                color = LightOnSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = rec.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = LightOnSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = rec.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = LightOnSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Source: ${rec.dataSource}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LightPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(
                                    onClick = { shareReportText(context, rec, dateFormatter) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Export report", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteRecord(rec.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete report", tint = LightError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Detail Sheet
    val detailRec = selectedRecord
    if (detailRec != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedRecord = null },
            sheetState = sheetState,
            containerColor = LightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val origin = runCatching { RecordOrigin.valueOf(detailRec.originBadge) }
                    .getOrDefault(RecordOrigin.DEMO_DATA)
                val (bg, fg) = originColors(origin)
                StatusPill(text = origin.badgeText, containerColor = bg, contentColor = fg)

                Text(detailRec.title, style = MaterialTheme.typography.headlineMedium, color = LightOnSurface)
                Text(
                    text = "ID: ${detailRec.id.take(8)} • ${dateFormatter.format(Date(detailRec.timestampMs))}",
                    style = MonoSmallStyle,
                    color = LightOnSurfaceVariant
                )
                Text("Data Source: ${detailRec.dataSource}", style = MaterialTheme.typography.labelMedium, color = LightPrimary)

                if (!detailRec.imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = detailRec.imageUri,
                        contentDescription = "Preserved report image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                ClayCard {
                    Text(
                        text = detailRec.fullDetails,
                        style = MaterialTheme.typography.bodySmall,
                        color = LightOnSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ClayPrimaryButton(
                        text = "Export / Share",
                        icon = Icons.Default.Share,
                        onClick = { shareReportText(context, detailRec, dateFormatter) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            onDeleteRecord(detailRec.id)
                            selectedRecord = null
                        },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = LightError, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", color = LightError)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Explicit Confirmation Dialog for Resetting All Project Data
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All Project Data?") },
            text = {
                Text("This permanently deletes all saved live/demo reports, uploaded NPK records, and resets crop & moisture calibration settings to defaults.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllData()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LightError, contentColor = LightOnError)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) LightPrimary else LightSurfaceContainer,
        contentColor = if (selected) LightOnPrimary else LightOnSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

private fun originColors(origin: RecordOrigin) = when (origin) {
    RecordOrigin.LIVE_SENSOR -> SuccessContainer to OnSuccessContainer
    RecordOrigin.DEMO_DATA -> WarningContainer to OnWarningContainer
    RecordOrigin.LAB_REPORT -> LightSecondaryContainer to LightOnSecondaryContainer
    RecordOrigin.REFERENCE_ESTIMATE -> LightErrorContainer to LightOnErrorContainer
    RecordOrigin.AI_INTERPRETATION -> SoilContainer to OnSoilContainer
}

private fun shareReportText(
    context: android.content.Context,
    record: SoilRecordEntity,
    formatter: SimpleDateFormat
) {
    val body = buildString {
        appendLine("FarmerBuddy (SMART SOIL) — Report Export")
        appendLine("Title: ${record.title}")
        appendLine("Origin: ${record.originBadge}")
        appendLine("Date: ${formatter.format(Date(record.timestampMs))}")
        appendLine("Source: ${record.dataSource}")
        appendLine()
        appendLine(record.fullDetails)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "FarmerBuddy: ${record.title}")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Export FarmerBuddy Report"))
}
