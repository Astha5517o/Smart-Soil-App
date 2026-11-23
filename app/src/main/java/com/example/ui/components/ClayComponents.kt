package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ClayHighlightWhite
import com.example.ui.theme.ClayShadowBrown
import com.example.ui.theme.LightOnPrimary
import com.example.ui.theme.LightOnSurface
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightOutlineVariant
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSurfaceContainer
import com.example.ui.theme.LightSurfaceContainerLowest
import com.example.ui.theme.MonoSmallStyle
import com.example.ui.theme.SoilBrown
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import com.example.ui.theme.OnWarningContainer

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    containerColor: Color = LightSurfaceContainerLowest,
    cornerRadius: Dp = 28.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = ClayShadowBrown,
            spotColor = ClayShadowBrown
        )
        .clip(shape)
        .background(containerColor)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(ClayHighlightWhite, LightOutlineVariant.copy(alpha = 0.45f))
            ),
            shape = shape
        )
        .let { m ->
            if (onClick != null) m.clickable(onClick = onClick) else m
        }

    Column(
        modifier = baseModifier.padding(16.dp),
        content = content
    )
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    leadingDotColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(999.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m },
        shape = shape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingDotColor != null) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(leadingDotColor)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ClayPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LightPrimary,
            contentColor = LightOnPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .let { m -> if (testTag != null) m.testTag(testTag) else m }
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun ClayInsetProgressBar(
    fraction: Float,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(stiffness = 280f),
        label = "clay_bar_progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(LightSurfaceContainer)
            .border(1.dp, LightOutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(fillColor)
        )
    }
}

@Composable
fun SoilPhScaleBar(
    phValue: Double?,
    modifier: Modifier = Modifier
) {
    // Map pH 3.5..10.5 across 0f..1f for balanced visual spread matching Stitch HTML
    val normalized = if (phValue != null) {
        ((phValue - 3.5) / 7.0).toFloat().coerceIn(0.03f, 0.97f)
    } else {
        0.5f
    }
    val animatedPos by animateFloatAsState(
        targetValue = normalized,
        animationSpec = spring(stiffness = 280f),
        label = "ph_marker"
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(999.dp))
        ) {
            val totalWidth = maxWidth
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(WarningAmber.copy(alpha = 0.72f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(SuccessGreen.copy(alpha = 0.78f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(SoilBrown.copy(alpha = 0.72f))
                )
            }
            if (phValue != null) {
                Box(
                    modifier = Modifier
                        .offset(x = (totalWidth - 8.dp) * animatedPos)
                        .width(8.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(LightOnSurface)
                        .border(1.5.dp, LightSurfaceContainerLowest, RoundedCornerShape(999.dp))
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Acidic", style = MonoSmallStyle, color = LightOnSurfaceVariant)
            Text("Neutral (7.0)", style = MonoSmallStyle, color = LightOnSurfaceVariant)
            Text("Alkaline", style = MonoSmallStyle, color = LightOnSurfaceVariant)
        }
    }
}

@Composable
fun SensorNoticeBanner(
    isDemo: Boolean,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isDemo) WarningContainer else SuccessContainer,
        border = BorderStroke(1.dp, if (isDemo) WarningAmber.copy(alpha = 0.35f) else SuccessGreen.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isDemo) OnWarningContainer else OnSuccessContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
