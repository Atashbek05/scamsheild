package com.example.scamshield.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBorder

/**
 * Standard rounded panel with a faint neon outline. Uses Column so multiple
 * children stack vertically — previously used Box which caused all children
 * to render on top of each other.
 */
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    accent: Color = CyberBorder,
    fill: Color = CyberBgCard,
    cornerRadius: Dp = 14.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(fill)
            .border(
                1.dp,
                Brush.linearGradient(listOf(accent.copy(alpha = 0.6f), accent.copy(alpha = 0.2f))),
                RoundedCornerShape(cornerRadius),
            )
            .padding(contentPadding),
    ) {
        content()
    }
}
