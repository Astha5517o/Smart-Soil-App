package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AiAdvisoryPlan
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.LightOnPrimary
import com.example.ui.theme.LightOnSurface
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightPrimaryContainer
import com.example.ui.theme.LightOnPrimaryContainer
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceContainer
import com.example.ui.theme.LightSurfaceContainerLow
import com.example.ui.theme.MonoSmallStyle
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.AsyncUiState

private val advisorTopics = listOf(
    "Full Soil Plan",
    "Irrigation Guidance",
    "pH Management",
    "Natural & Organic Amendments",
    "Crop Nutrient Management",
    "Indigenous Practices"
)

@Composable
fun AiAdvisorScreen(
    contextSnapshot: String,
    uiState: AsyncUiState<AiAdvisoryPlan>,
    onGenerateAdvice: (topic: String, question: String) -> Unit,
    onLoadDemoAdvice: () -> Unit,
    onSavePlan: (AiAdvisoryPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTopic by remember { mutableStateOf(advisorTopics.first()) }
    var questionText by remember { mutableStateOf("") }
    var showContext by remember { mutableStateOf(false) }

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
                text = "FARMERBUDDY · SMART SOIL",
                style = MaterialTheme.typography.labelSmall,
                color = LightPrimary
            )
            Text(
                text = "AI Soil Advisor",
                style = MaterialTheme.typography.headlineLarge,
                color = LightOnSurface
            )
            Text(
                text = "Simple soil & crop steps from your sensors and scans.",
                style = MaterialTheme.typography.bodyMedium,
                color = LightOnSurfaceVariant
            )
        }

        // Collapsible Context Card
        ClayCard(containerColor = LightSurfaceContainerLow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showContext = !showContext },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Context Used by Advisor",
                    style = MaterialTheme.typography.labelLarge,
                    color = LightOnSurface
                )
                Text(
                    text = if (showContext) "Hide" else "Inspect",
                    style = MaterialTheme.typography.labelMedium,
                    color = LightPrimary
                )
            }
            if (showContext) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = contextSnapshot.trim(),
                    style = MonoSmallStyle,
                    color = LightOnSurfaceVariant
                )
            }
        }

        // Topic Chips
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Guidance Focus",
                style = MaterialTheme.typography.labelLarge,
                color = LightOnSurface
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                advisorTopics.forEach { topic ->
                    val selected = selectedTopic == topic
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (selected) LightPrimary else LightSurfaceContainer,
                        contentColor = if (selected) LightOnPrimary else LightOnSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { selectedTopic = topic }
                    ) {
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Question Input & Generate Button
        ClayCard {
            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                label = { Text("Ask a specific soil or crop question (optional)") },
                placeholder = { Text("e.g., Should I irrigate today or wait? How can compost help?") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("advisor_question_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            ClayPrimaryButton(
                text = "Get Soil Advice",
                icon = Icons.Default.Psychology,
                onClick = { onGenerateAdvice(selectedTopic, questionText) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "advisor_generate_button"
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onLoadDemoAdvice,
                modifier = Modifier
                    .align(Alignment.End)
                    .minimumInteractiveComponentSize()
                    .testTag("advisor_demo_button")
            ) {
                Text("Load Offline Demo Advice")
            }
        }

        // Results
        when (uiState) {
            is AsyncUiState.Idle -> {
                ClayCard {
                    Text(
                        text = "Tap \"Get Soil Advice\" for clear recommendations with evidence and explicit uncertainty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightOnSurfaceVariant
                    )
                }
            }
            is AsyncUiState.Loading -> LoadingAnalysisCard("Analysing...")
            is AsyncUiState.Error -> ErrorAnalysisCard(
                message = uiState.message,
                onTryAgain = { onGenerateAdvice(selectedTopic, questionText) }
            )
            is AsyncUiState.Success -> {
                val plan = uiState.data
                var saved by remember(plan) { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = LightPrimaryContainer,
                    contentColor = LightOnPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ADVISORY SUMMARY", style = MaterialTheme.typography.labelSmall)
                            StatusPill(
                                text = if (plan.isDemo) "DEMO ADVICE" else "AI GENERATED",
                                containerColor = if (plan.isDemo) WarningContainer else SuccessContainer,
                                contentColor = if (plan.isDemo) OnWarningContainer else OnSuccessContainer
                            )
                        }
                        Text(text = plan.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                plan.recommendations.forEachIndexed { idx, rec ->
                    ClayCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}. ${rec.category}",
                                style = MaterialTheme.typography.titleMedium,
                                color = LightPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AdvisorField("1. Identified Issue", rec.identifiedIssue)
                        AdvisorField("2. Why It Matters", rec.whyItMatters)
                        AdvisorField("3. Suggested Next Step", rec.suggestedNextStep)
                        AdvisorField("4. Supporting Evidence / Source", rec.supportingEvidence)
                        AdvisorField("5. Limitations & Uncertainty", rec.limitationsAndUncertainty)
                    }
                }

                OutlinedButton(
                    onClick = {
                        onSavePlan(plan)
                        saved = true
                    },
                    enabled = !saved,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("save_advice_button")
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (saved) "Saved to Reports" else "Save AI Plan to Reports")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AdvisorField(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = LightOnSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = LightOnSurfaceVariant
        )
    }
}
