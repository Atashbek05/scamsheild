package com.example.scamshield.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val accent: androidx.compose.ui.graphics.Color,
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            Icons.Filled.Shield,
            stringResource(R.string.onboarding_page1_title),
            stringResource(R.string.onboarding_page1_body),
            CyberCyan,
        ),
        OnboardingPage(
            Icons.Filled.Bolt,
            stringResource(R.string.onboarding_page2_title),
            stringResource(R.string.onboarding_page2_body),
            CyberGreen,
        ),
        OnboardingPage(
            Icons.Filled.Visibility,
            stringResource(R.string.onboarding_page3_title),
            stringResource(R.string.onboarding_page3_body),
            CyberCyan,
        ),
        OnboardingPage(
            Icons.Filled.NotificationsActive,
            stringResource(R.string.onboarding_page4_title),
            stringResource(R.string.onboarding_page4_body),
            CyberGreen,
        ),
    )

    val state = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .padding(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) {
                Text(stringResource(R.string.onboarding_skip), color = CyberTextSecondary)
            }
        }

        HorizontalPager(state = state, modifier = Modifier.weight(1f)) { page ->
            val p = pages[page]
            Column(
                Modifier.fillMaxSize().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(p.accent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(p.icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(56.dp))
                }
                Spacer(Modifier.height(28.dp))
                Text(p.title, color = CyberTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(p.body, color = CyberTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { i ->
                val active = state.currentPage == i
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (active) 10.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (active) CyberCyan else CyberTextSecondary.copy(alpha = 0.4f)),
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (state.currentPage == pages.lastIndex) onFinish()
                else scope.launch { state.animateScrollToPage(state.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBgDeep),
        ) {
            Text(
                stringResource(if (state.currentPage == pages.lastIndex) R.string.onboarding_get_started else R.string.onboarding_continue),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
