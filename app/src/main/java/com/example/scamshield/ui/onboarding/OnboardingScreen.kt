package com.example.scamshield.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.R
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val accent: Color,
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit, onOpenPrivacyPolicy: () -> Unit = {}) {
    val pages = listOf(
        OnboardingPage(Icons.Rounded.Shield,              stringResource(R.string.onboarding_page1_title), stringResource(R.string.onboarding_page1_body), CyberCyan),
        OnboardingPage(Icons.Rounded.Bolt,               stringResource(R.string.onboarding_page2_title), stringResource(R.string.onboarding_page2_body), CyberGreen),
        OnboardingPage(Icons.Rounded.Visibility,         stringResource(R.string.onboarding_page3_title), stringResource(R.string.onboarding_page3_body), CyberCyan),
        OnboardingPage(Icons.Rounded.NotificationsActive, stringResource(R.string.onboarding_page4_title), stringResource(R.string.onboarding_page4_body), CyberGreen),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope      = rememberCoroutineScope()
    var privacyAccepted by rememberSaveable { mutableStateOf(false) }
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .padding(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!isLastPage) {
                TextButton(onClick = onFinish) {
                    Text(
                        stringResource(R.string.onboarding_skip),
                        color    = CyberTextSecondary,
                        fontSize = 13.sp,
                    )
                }
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }

        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val p = pages[page]
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    p.accent.copy(alpha = 0.2f),
                                    p.accent.copy(alpha = 0.04f),
                                )
                            ),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(p.icon, null, tint = p.accent, modifier = Modifier.size(52.dp))
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    p.title,
                    color      = CyberTextPrimary,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center,
                    lineHeight = 30.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    p.body,
                    color      = CyberTextSecondary,
                    fontSize   = 14.sp,
                    textAlign  = TextAlign.Center,
                    lineHeight = 22.sp,
                )
                if (page == pages.lastIndex) {
                    Spacer(Modifier.height(24.dp))
                    TextButton(onClick = onOpenPrivacyPolicy) {
                        Text(
                            stringResource(R.string.privacy_read_link),
                            color    = CyberCyan,
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberBgSurface)
                            .clickable { privacyAccepted = !privacyAccepted }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked         = privacyAccepted,
                            onCheckedChange = { privacyAccepted = it },
                            colors          = CheckboxDefaults.colors(
                                checkedColor   = CyberCyan,
                                uncheckedColor = CyberTextMuted,
                                checkmarkColor = CyberBgDeep,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.privacy_agree),
                            color      = CyberTextSecondary,
                            fontSize   = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }

        // Page indicator dots
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            repeat(pages.size) { i ->
                val active = pagerState.currentPage == i
                val w by animateDpAsState(
                    targetValue   = if (active) 24.dp else 6.dp,
                    animationSpec = tween(250),
                    label         = "dot_w",
                )
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = w, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (active) CyberCyan
                            else CyberTextSecondary.copy(alpha = 0.3f)
                        ),
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (pagerState.currentPage == pages.lastIndex) onFinish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            enabled  = if (isLastPage) privacyAccepted else true,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = CyberCyan,
                contentColor           = CyberBgDeep,
                disabledContainerColor = CyberCyan.copy(alpha = 0.25f),
                disabledContentColor   = CyberBgDeep.copy(alpha = 0.5f),
            ),
        ) {
            Text(
                stringResource(
                    if (pagerState.currentPage == pages.lastIndex)
                        R.string.onboarding_get_started
                    else
                        R.string.onboarding_continue
                ),
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
            )
        }
    }
}
