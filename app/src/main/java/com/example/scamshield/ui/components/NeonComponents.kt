package com.example.scamshield.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberTextSecondary

/**
 * Animated pulse dot — "AI online" / "actively monitoring" indicator.
 */
@Composable
fun NeonPulse(
    modifier: Modifier = Modifier,
    color: Color = CyberCyan,
    diameter: androidx.compose.ui.unit.Dp = 10.dp,
) {
    val infinite = rememberInfiniteTransition(label = "neon_pulse")
    val scale by infinite.animateFloat(
        initialValue = 0.5f,
        targetValue  = 1.5f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale",
    )
    val alpha by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue  = 0.15f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(modifier.size(diameter * 2), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter * 2)) {
            val r = size.minDimension / 2f
            drawCircle(color = color.copy(alpha = alpha), radius = r * scale)
            drawCircle(color = color, radius = r * 0.38f)
        }
    }
}

/**
 * Sweeping animated progress bar for loading / scan-in-progress states.
 */
@Composable
fun NeonProgressBar(
    modifier: Modifier = Modifier,
    accent: Color = CyberCyan,
) {
    val infinite = rememberInfiniteTransition(label = "neon_progress")
    val offset by infinite.animateFloat(
        initialValue  = -1f,
        targetValue   = 2f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "offset",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(CyberBgSurface),
    ) {
        Canvas(Modifier.fillMaxWidth().height(4.dp)) {
            val w = size.width
            val start = (offset - 0.4f) * w
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, accent.copy(alpha = 0.9f), Color.Transparent),
                    start  = Offset(start, 0f),
                    end    = Offset(start + w * 0.5f, 0f),
                ),
            )
        }
    }
}

/**
 * Stat tile with large value and small label.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.12f),
                        accent.copy(alpha = 0.04f),
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(200f, 200f),
                )
            )
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        androidx.compose.foundation.layout.Column {
            Text(
                value,
                color     = accent,
                fontSize  = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label.uppercase(),
                color     = CyberTextSecondary,
                fontSize  = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Pill chip for keywords, tags and filter badges.
 */
@Composable
fun NeonChip(
    text: String,
    accent: Color = CyberCyan,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.1f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            color      = accent,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines   = 1,
        )
    }
}

/**
 * Wrapping row of small inline metric badges.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineMetrics(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement   = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = CyberCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                Text(label, color = CyberTextSecondary, fontSize = 10.sp, letterSpacing = 0.5.sp, maxLines = 1)
            }
        }
    }
}
