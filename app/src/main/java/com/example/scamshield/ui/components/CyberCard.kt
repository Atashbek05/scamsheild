package com.example.scamshield.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgCardHigh
import com.example.scamshield.ui.theme.CyberBorder
import com.example.scamshield.ui.theme.CyberGlassWhite

/**
 * Premium glass card — 20dp corners, layered gradient background,
 * top-edge highlight, and accent border glow.
 */
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    accent: Color = CyberBorder,
    fill: Color = CyberBgCard,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        CyberBgCardHigh,
                        fill,
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(600f, 600f),
                )
            )
            .drawBehind {
                // Top-edge glass highlight
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
                    colors = listOf(
                        accent.copy(alpha = 0.6f),
                        accent.copy(alpha = 0.2f),
                        accent.copy(alpha = 0.04f),
                    ),
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}
