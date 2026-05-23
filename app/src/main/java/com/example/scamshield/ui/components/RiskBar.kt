package com.example.scamshield.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.scamshield.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.data.RiskLevel
import com.example.scamshield.ui.theme.CyberAmber
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextSecondary

fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.SAFE     -> CyberGreen
    RiskLevel.LOW      -> CyberGreen
    RiskLevel.MEDIUM   -> CyberAmber
    RiskLevel.HIGH     -> CyberRed
    RiskLevel.CRITICAL -> CyberRed
}

@Composable
fun RiskBar(
    probability: Float,
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val target = probability.coerceIn(0f, 1f)
    val animated by animateFloatAsState(target, animationSpec = tween(600), label = "risk")

    Column(modifier) {
        if (showLabel) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.label_risk_score), color = CyberTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${(target * 100).toInt()}%  ·  ${stringResource(riskLevel.labelRes).uppercase()}",
                    color = riskColor(riskLevel),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CyberBgSurface),
        ) {
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                drawRect(
                    brush = Brush.horizontalGradient(listOf(CyberCyan, riskColor(riskLevel))),
                    size = androidx.compose.ui.geometry.Size(size.width * animated, size.height),
                )
            }
        }
    }
}
