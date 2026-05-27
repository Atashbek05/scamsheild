package com.example.scamshield.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberBorder
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberTextMuted

data class NavTabConfig(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
)

/**
 * Floating premium bottom navigation bar.
 * Rounded container, glass-like surface, spring-animated active state.
 */
@Composable
fun AppNavigation(
    tabs: List<NavTabConfig>,
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(CyberBgSurface, CyberBgCard),
                    )
                )
                .drawBehind {
                    // Subtle top highlight
                    drawLine(
                        color       = Color.White.copy(alpha = 0.07f),
                        start       = Offset(32.dp.toPx(), 1.5f),
                        end         = Offset(size.width - 32.dp.toPx(), 1.5f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .border(1.dp, CyberBorder, shape),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                FloatingNavItem(
                    tab      = tab,
                    selected = selected,
                    onClick  = { onTabSelected(tab.route) },
                )
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    tab: NavTabConfig,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        if (selected) 1.05f else 1f,
        spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "nav_scale",
    )

    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) CyberCyan.copy(alpha = 0.18f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = tab.icon,
                contentDescription = stringResource(tab.labelRes),
                tint               = if (selected) CyberCyan else CyberTextMuted,
                modifier           = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(tab.labelRes),
            color      = if (selected) CyberCyan else CyberTextMuted,
            fontSize   = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
