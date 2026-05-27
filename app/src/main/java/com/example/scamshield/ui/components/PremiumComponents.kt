package com.example.scamshield.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgCardHigh
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBorder
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGlassWhite
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary

/**
 * Premium card — 24dp corners, layered glass, optional titled header with icon.
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    accent: Color = CyberCyan,
    title: String? = null,
    titleIcon: ImageVector? = null,
    cornerRadius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    trailingHeader: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(CyberBgCardHigh, CyberBgCard),
                    start  = Offset(0f, 0f),
                    end    = Offset(700f, 700f),
                )
            )
            .drawBehind {
                drawLine(
                    color       = CyberGlassWhite,
                    start       = Offset(cornerRadius.toPx(), 1.5f),
                    end         = Offset(size.width - cornerRadius.toPx(), 1.5f),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.55f),
                        accent.copy(alpha = 0.18f),
                        Color.Transparent,
                    )
                ),
                shape = shape,
            )
            .padding(contentPadding),
    ) {
        if (title != null) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                titleIcon?.let { icon ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    title,
                    color         = accent,
                    fontWeight    = FontWeight.SemiBold,
                    fontSize      = 13.sp,
                    letterSpacing = 0.8.sp,
                    modifier      = Modifier.weight(1f),
                )
                trailingHeader?.invoke()
            }
            Spacer(Modifier.height(14.dp))
        }
        content()
    }
}

/**
 * Full-width premium button with gradient fill and spring press animation.
 */
@Composable
fun PremiumButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = CyberCyan,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.6f),
        label = "btn_scale",
    )
    val activeColor = if (enabled) accent else CyberTextMuted

    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled)
                    Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.75f)))
                else
                    Brush.horizontalGradient(listOf(CyberBgCard, CyberBgCard))
            )
            .border(
                1.dp,
                if (enabled) accent.copy(alpha = 0.4f) else CyberBorder,
                RoundedCornerShape(16.dp),
            )
            .clickable(source, null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    it, null,
                    tint     = if (enabled) CyberBgDeep else CyberTextMuted,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                label,
                color      = if (enabled) CyberBgDeep else CyberTextMuted,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

/**
 * Transparent glass container with subtle border.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(CyberBgCard.copy(alpha = 0.85f))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        CyberTextPrimary.copy(alpha = 0.1f),
                        CyberTextPrimary.copy(alpha = 0.03f),
                    )
                ),
                shape,
            ),
        content = content,
    )
}

/**
 * Vertical gradient overlay for screen tops — creates the hero glow effect.
 */
@Composable
fun GradientHeader(
    modifier: Modifier = Modifier,
    accent: Color = CyberCyan,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.07f),
                            Color.Transparent,
                        )
                    )
                )
        )
        content()
    }
}

/**
 * Premium top bar — icon badge + title/subtitle + optional trailing slot.
 */
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accent: Color = CyberCyan,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier            = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        icon?.let {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(it, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color         = CyberTextPrimary,
                fontWeight    = FontWeight.Bold,
                fontSize      = 18.sp,
                letterSpacing = 0.5.sp,
            )
            subtitle?.let { sub ->
                Text(
                    sub,
                    color    = CyberTextSecondary,
                    fontSize = 11.sp,
                )
            }
        }
        trailingContent?.invoke()
    }
}
