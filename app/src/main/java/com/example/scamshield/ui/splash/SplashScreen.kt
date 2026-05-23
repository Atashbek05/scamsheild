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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.scamshield.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary
import kotlinx.coroutines.delay

/**
 * Animated cyber-style splash. Rotating shield ring + pulsing core + scrolling
 * scan-lines. Stays on-screen for [DURATION_MS] before invoking [onFinished].
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val DURATION_MS = 1800L

    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(600), label = "splash_alpha")

    LaunchedEffect(Unit) {
        visible = true
        delay(DURATION_MS)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(CyberBgDeep, CyberBgDeep, androidx.compose.ui.graphics.Color.Black),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RotatingShield(modifier = Modifier.size(160.dp))
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.splash_title),
                color = CyberTextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.splash_subtitle),
                color = CyberTextSecondary,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            ScanlineBar()
        }
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 1f - alpha)))
    }
}

@Composable
private fun RotatingShield(modifier: Modifier) {
    val infinite = rememberInfiniteTransition(label = "shield")
    val rot by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(5500, easing = LinearEasing)), label = "rot")
    val pulse by infinite.animateFloat(0.7f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(160.dp)) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            val outer = size.minDimension / 2f

            // outer dashed ring
            for (i in 0 until 36) {
                val a = (i * 10f + rot) * Math.PI.toFloat() / 180f
                val len = if (i % 3 == 0) 14f else 6f
                val sx = center.x + (outer - 4f) * kotlin.math.cos(a)
                val sy = center.y + (outer - 4f) * kotlin.math.sin(a)
                val ex = center.x + (outer - 4f - len) * kotlin.math.cos(a)
                val ey = center.y + (outer - 4f - len) * kotlin.math.sin(a)
                drawLine(CyberCyan.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(sx, sy), androidx.compose.ui.geometry.Offset(ex, ey), strokeWidth = 2f)
            }
            // mid ring
            drawCircle(CyberCyan.copy(alpha = 0.25f), radius = outer * 0.65f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            // inner pulse
            drawCircle(CyberGreen.copy(alpha = 0.18f * pulse), radius = outer * 0.45f * pulse)
            drawCircle(CyberCyan.copy(alpha = 0.45f), radius = outer * 0.3f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            drawCircle(CyberCyan, radius = outer * 0.08f)
        }
        Text("◈", color = CyberCyan, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScanlineBar() {
    val infinite = rememberInfiniteTransition(label = "scanline")
    val offset by infinite.animateFloat(-1f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "off")
    Box(
        Modifier
            .width(180.dp)
            .height(2.dp)
            .background(CyberCyan.copy(alpha = 0.15f)),
    ) {
        Canvas(Modifier.width(180.dp).height(2.dp)) {
            val xc = (offset + 1f) / 2f * size.width
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color.Transparent,
                        CyberCyan,
                        androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    startX = xc - 60f,
                    endX = xc + 60f,
                ),
            )
        }
    }
}
