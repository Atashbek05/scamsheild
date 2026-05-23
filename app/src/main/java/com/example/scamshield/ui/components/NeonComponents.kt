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
import androidx.compose.runtime.setValue
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
 * Animated pulse dot — used as the "active monitoring" / "AI online" indicator.
 * Cheaper than a Lottie animation and matches the neon theme.
 */
@Composable
fun NeonPulse(
    modifier: Modifier = Modifier,
    color: Color = CyberCyan,
    diameter: androidx.compose.ui.unit.Dp = 10.dp,
) {
    val infinite = rememberInfiniteTransition(label = "neon_pulse")
    val scale by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale",
    )
    val alpha by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(modifier.size(diameter * 2), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter * 2)) {
            drawCircle(color = color.copy(alpha = alpha), radius = size.minDimension / 2f * scale)
            drawCircle(color = color, radius = size.minDimension / 4f)
        }
    }
}

/**
 * Horizontal animated progress bar used on the dashboard "AI Engine" tile and
 * during simulator scans. Direction is fixed left-to-right at constant speed.
 */
@Composable
fun NeonProgressBar(
    modifier: Modifier = Modifier,
    accent: Color = CyberCyan,
) {
    val infinite = rememberInfiniteTransition(label = "neon_progress")
    val offset by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "offset",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(CyberBgSurface),
    ) {
        Canvas(Modifier.fillMaxWidth().height(6.dp)) {
            val w = size.width
            val sweepStart = (offset - 0.4f) * w
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, accent.copy(alpha = 0.9f), Color.Transparent),
                    start  = Offset(sweepStart, 0f),
                    end    = Offset(sweepStart + w * 0.5f, 0f),
                ),
            )
        }
    }
}

/**
 * Cyber-style stat box used on the dashboard / analytics screen.
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
            .clip(RoundedCornerShape(12.dp))
            .background(CyberBgCard)
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        androidx.compose.foundation.layout.Column {
            Text(
                value,
                color = accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label.uppercase(),
                color = CyberTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Neon "chip" — used for keyword pills and category tags.
 */
@Composable
fun NeonChip(
    text: String,
    accent: Color = CyberCyan,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * Wrapping row of small inline metric badges — uses FlowRow so items
 * wrap to the next line on narrow screens instead of clipping.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineMetrics(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                Text(label, color = CyberTextSecondary, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}
