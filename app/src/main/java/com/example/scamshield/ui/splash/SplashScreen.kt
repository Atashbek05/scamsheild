package com.example.scamshield.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.R
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(700), label = "splash_in")

    LaunchedEffect(Unit) {
        visible = true
        delay(1900)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CyberCyan.copy(alpha = 0.06f),
                        CyberBgDeep,
                        CyberBgDeep,
                    ),
                    center = Offset(Float.POSITIVE_INFINITY / 2f, 0f),
                    radius = 900f,
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha),
        ) {
            ShieldRing(modifier = Modifier.size(148.dp))
            Spacer(Modifier.height(32.dp))
            Text(
                stringResource(R.string.splash_title),
                color         = CyberTextPrimary,
                fontSize      = 28.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 6.sp,
                textAlign     = TextAlign.Center,
                modifier      = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.splash_subtitle),
                color         = CyberTextSecondary,
                fontSize      = 12.sp,
                letterSpacing = 2.sp,
                textAlign     = TextAlign.Center,
            )
        }

        // Fade-in overlay
        Box(
            Modifier
                .fillMaxSize()
                .background(CyberBgDeep.copy(alpha = (1f - alpha).coerceIn(0f, 1f)))
        )
    }
}

@Composable
private fun ShieldRing(modifier: Modifier) {
    val infinite = rememberInfiniteTransition(label = "shield")
    val rot by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "rot",
    )
    val pulse by infinite.animateFloat(
        0.75f, 1f,
        infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.minDimension / 2f

            // Outer glow ring
            drawCircle(
                color  = CyberCyan.copy(alpha = 0.08f * pulse),
                radius = outerR - 2f,
            )
            // Rotating arc
            drawArc(
                color      = CyberCyan,
                startAngle = rot,
                sweepAngle = 100f,
                useCenter  = false,
                style      = Stroke(width = 3f, cap = StrokeCap.Round),
                topLeft    = androidx.compose.ui.geometry.Offset(4f, 4f),
                size       = androidx.compose.ui.geometry.Size(size.width - 8f, size.height - 8f),
            )
            drawArc(
                color      = CyberCyan.copy(alpha = 0.3f),
                startAngle = rot + 180f,
                sweepAngle = 60f,
                useCenter  = false,
                style      = Stroke(width = 2f, cap = StrokeCap.Round),
                topLeft    = androidx.compose.ui.geometry.Offset(4f, 4f),
                size       = androidx.compose.ui.geometry.Size(size.width - 8f, size.height - 8f),
            )
            // Mid ring
            drawCircle(
                color  = CyberCyan.copy(alpha = 0.15f),
                radius = outerR * 0.65f,
                style  = Stroke(1.5f),
            )
            // Inner pulse glow
            drawCircle(
                color  = CyberGreen.copy(alpha = 0.12f * pulse),
                radius = outerR * 0.42f * pulse,
            )
            // Core dot
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(CyberCyan, CyberCyan.copy(alpha = 0f)),
                    center = Offset(cx, cy),
                    radius = outerR * 0.12f,
                ),
                radius = outerR * 0.12f,
                center = Offset(cx, cy),
            )
        }
        Icon(
            Icons.Rounded.Shield,
            contentDescription = null,
            tint     = CyberCyan,
            modifier = Modifier.size(40.dp),
        )
    }
}
