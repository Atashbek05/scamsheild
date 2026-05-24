package com.example.scamshield.overlay

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.example.scamshield.util.logD
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.scamshield.R
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.call.CallAction
import com.example.scamshield.data.call.CallAnalysis
import com.example.scamshield.data.call.CallRisk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Real-time incoming-call warning overlay.
 *
 * Visual:
 *   ┌─ Red/amber neon card pinned near the top of the screen ─────────┐
 *   │  ⚠  INCOMING CALL THREAT       [ScamShield]                  ✕  │
 *   │  ───────────────────────────────────────────────────────────── │
 *   │             87%                                                │
 *   │  ████████████████░░░░  ← animated risk fill bar               │
 *   │  CALLER     Unknown                                            │
 *   │  NUMBER     +234 XXX XXXX                                      │
 *   │                                                                │
 *   │  WHY THIS CALL IS DANGEROUS                                    │
 *   │  • International prefix flagged for scam origins              │
 *   │  • Number not in your contacts                                 │
 *   │  • Ringing pattern matches robocall behaviour                  │
 *   │                                                                │
 *   │  [BLOCK]  [MUTE]  [COPY]  [REPORT]                             │
 *   └──────────────────────────────────────────────────────────────────┘
 *
 * Behind the card a pulsing red halo runs while the overlay is visible so it
 * reads instantly even peripherally.  RED is used for HIGH/CRITICAL risk;
 * AMBER for MEDIUM.
 *
 * Built entirely from View code so it can be inflated from a service context.
 */
object CallOverlayManager {

    private const val TAG = "CallOverlayManager"

    private const val AUTO_DISMISS_MS = 15_000L
    private const val ANIM_ENTER_MS   = 350L
    private const val ANIM_EXIT_MS    = 250L
    private const val ANIM_BAR_MS     = 700L
    private const val PULSE_PERIOD_MS = 1_100L

    // Cyber palette — mirrors ui/theme/Color.kt
    private val COLOR_BG_CARD    = Color.parseColor("#141E35")
    private val COLOR_BG_SURFACE = Color.parseColor("#0F1628")
    private val COLOR_CYAN       = Color.parseColor("#00D4FF")
    private val COLOR_RED        = Color.parseColor("#FF3B5C")
    private val COLOR_AMBER      = Color.parseColor("#FFB800")
    private val COLOR_GREEN      = Color.parseColor("#00FF88")
    private val COLOR_TEXT_PRI   = Color.parseColor("#E8F0FF")
    private val COLOR_TEXT_SEC   = Color.parseColor("#8BA5C8")
    private val COLOR_BORDER     = Color.parseColor("#1E3050")

    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable { dismiss() }
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var windowManager: WindowManager? = null
    private var currentView: View? = null
    private var currentNumber: String? = null
    private var isActive = false
    private var pulseAnimator: ValueAnimator? = null

    // ── Public API ────────────────────────────────────────────────────────────

    fun showWarning(context: Context, analysis: CallAnalysis) {
        val appContext = context.applicationContext
        mainHandler.post { showOnMainThread(appContext, analysis) }
    }

    fun dismiss() {
        mainHandler.post { dismissWithAnimation() }
    }

    // ── Main thread internals ────────────────────────────────────────────────

    private fun showOnMainThread(appContext: Context, analysis: CallAnalysis) {
        if (!Settings.canDrawOverlays(appContext)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted — overlay blocked for ${analysis.phoneNumber}")
            return
        }
        if (isActive) {
            logD(TAG, "Call overlay already active — dropping ${analysis.phoneNumber}")
            return
        }

        val density = appContext.resources.displayMetrics.density
        val accentColor = accentFor(analysis.risk)
        val card = buildCard(appContext, density, analysis, accentColor)
        val params = buildWindowParams(density)

        try {
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.addView(card, params)
            windowManager = wm
            currentView = card
            currentNumber = analysis.phoneNumber
            isActive = true

            card.translationY = -(380 * density)
            card.alpha = 0f
            card.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(ANIM_ENTER_MS)
                .setInterpolator(OvershootInterpolator(0.7f))
                .start()

            startPulse(card, accentColor)

            mainHandler.postDelayed(autoDismissRunnable, AUTO_DISMISS_MS)
            logD(
                TAG,
                "Call overlay shown | number=${analysis.phoneNumber} risk=${analysis.risk} " +
                    "prob=${"%.0f".format(analysis.probability * 100)}%",
            )
        } catch (e: Exception) {
            Log.e(TAG, "addView failed: ${e.message}", e)
            isActive = false
        }
    }

    private fun dismissWithAnimation() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        pulseAnimator?.cancel()
        pulseAnimator = null
        val view = currentView ?: return
        view.animate().cancel()
        view.animate()
            .translationY(-(380f * view.resources.displayMetrics.density))
            .alpha(0f)
            .setDuration(ANIM_EXIT_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                runCatching { windowManager?.removeView(view) }
                    .onFailure { Log.e(TAG, "removeView failed", it) }
                currentView = null
                windowManager = null
                isActive = false
                currentNumber = null
                logD(TAG, "Call overlay dismissed")
            }
            .start()
    }

    // ── Pulse animation (danger halo) ────────────────────────────────────────

    private fun startPulse(card: View, accentColor: Int) {
        val tag = card.tag as? PulseTargets ?: return
        pulseAnimator = ValueAnimator.ofFloat(0.45f, 1f).apply {
            duration = PULSE_PERIOD_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val v = anim.animatedValue as Float
                tag.haloRing.alpha = v
                tag.haloRing.scaleX = 0.9f + 0.12f * v
                tag.haloRing.scaleY = 0.9f + 0.12f * v
                tag.iconCircle.alpha = 0.6f + 0.4f * v
            }
            start()
        }
        logD(TAG, "Danger pulse started, accent=${"%08X".format(accentColor)}")
    }

    // ── Card construction ────────────────────────────────────────────────────

    /** Stores pulse-animated views so the animator can reach them after view assembly. */
    private data class PulseTargets(
        val haloRing: View,
        val iconCircle: View,
    )

    private fun accentFor(risk: CallRisk): Int = when (risk) {
        CallRisk.CRITICAL, CallRisk.HIGH -> COLOR_RED
        CallRisk.MEDIUM                  -> COLOR_AMBER
        else                              -> COLOR_CYAN
    }

    private fun buildCard(
        context: Context,
        density: Float,
        analysis: CallAnalysis,
        accentColor: Int,
    ): View {
        fun Int.dp(): Int = (this * density).toInt()
        fun Float.dp(): Int = (this * density).toInt()

        val accent15 = Color.argb(38, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        val accent35 = Color.argb(90, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        val accent55 = Color.argb(140, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))

        val cardCorner = 16.dp().toFloat()

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(COLOR_BG_CARD)
                setCornerRadius(cardCorner)
                setStroke(2.dp(), accentColor)
            }
            elevation = 18.dp().toFloat()
            clipToOutline = true
        }

        // ── Header row (icon + halo + title + close) ────────────────────────
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.argb(230, 18, 4, 12), COLOR_BG_SURFACE),
            ).apply {
                setCornerRadii(floatArrayOf(cardCorner, cardCorner, cardCorner, cardCorner, 0f, 0f, 0f, 0f))
            }
            setPadding(14.dp(), 12.dp(), 8.dp(), 12.dp())
        }

        // Icon + animated halo
        val iconStack = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(40.dp(), 40.dp())
        }
        val halo = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent15)
                setStroke(2.dp(), accent55)
            }
            layoutParams = FrameLayout.LayoutParams(40.dp(), 40.dp())
            alpha = 0.6f
        }
        val iconCircle = TextView(context).apply {
            text = "⚠"
            textSize = 17f
            setTextColor(accentColor)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent35)
            }
            val size = 28.dp()
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
        }
        iconStack.addView(halo)
        iconStack.addView(iconCircle)

        val titleView = TextView(context).apply {
            text = context.getString(
                if (analysis.risk == CallRisk.MEDIUM) R.string.call_overlay_suspicious_call
                else R.string.call_overlay_incoming_threat
            )
            textSize = 13f
            setTextColor(accentColor)
            setTypeface(Typeface.DEFAULT_BOLD)
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
            ).apply { marginStart = 12.dp() }
        }

        val brand = TextView(context).apply {
            text = context.getString(R.string.app_name)
            textSize = 9f
            setTextColor(COLOR_TEXT_SEC)
            setPadding(6.dp(), 3.dp(), 6.dp(), 3.dp())
            background = GradientDrawable().apply {
                setColor(COLOR_BG_SURFACE)
                setCornerRadius(4.dp().toFloat())
                setStroke(1.dp(), COLOR_BORDER)
            }
        }

        val close = TextView(context).apply {
            text = "✕"
            textSize = 17f
            setTextColor(COLOR_TEXT_SEC)
            gravity = Gravity.CENTER
            minimumWidth = 44.dp()
            minimumHeight = 44.dp()
            setPadding(8.dp(), 0, 8.dp(), 0)
            setOnClickListener { dismiss() }
        }

        header.addView(iconStack)
        header.addView(titleView)
        header.addView(brand)
        header.addView(close)
        card.addView(header)

        // Hairline divider
        card.addView(View(context).apply {
            setBackgroundColor(accent55)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        })

        // ── Body ────────────────────────────────────────────────────────────
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 12.dp(), 16.dp(), 16.dp())
        }

        val pct = (analysis.probability * 100).toInt().coerceIn(0, 100)

        // Probability headline
        body.addView(TextView(context).apply {
            text = context.getString(R.string.call_overlay_scam_probability)
            textSize = 10f
            setTextColor(COLOR_TEXT_SEC)
            setTypeface(Typeface.DEFAULT_BOLD)
            letterSpacing = 0.15f
        })
        body.addView(TextView(context).apply {
            text = "$pct%"
            textSize = 34f
            setTextColor(accentColor)
            setTypeface(Typeface.DEFAULT_BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 2.dp() }
        })

        // Animated risk bar
        val barTrack = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 6.dp(),
            ).apply { topMargin = 6.dp() }
            background = GradientDrawable().apply {
                setColor(COLOR_BG_SURFACE)
                setCornerRadius(3.dp().toFloat())
            }
            clipToOutline = true
        }
        val barFill = View(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(COLOR_CYAN, accentColor),
            ).apply { setCornerRadius(3.dp().toFloat()) }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            pivotX = 0f
            scaleX = 0f
        }
        barTrack.addView(barFill)
        body.addView(barTrack)
        ValueAnimator.ofFloat(0f, analysis.probability.coerceIn(0f, 1f)).apply {
            duration = ANIM_BAR_MS
            startDelay = 250L
            interpolator = DecelerateInterpolator()
            addUpdateListener { barFill.scaleX = it.animatedValue as Float }
            start()
        }

        // Caller meta rows
        body.addView(metaRow(context, density, R.string.call_overlay_caller, analysis.displayName))
        body.addView(metaRow(
            context, density, R.string.call_overlay_number,
            analysis.phoneNumber.ifBlank { context.getString(R.string.call_unknown_caller) },
        ))

        // Risk-level chip
        val riskChip = TextView(context).apply {
            text = context.getString(R.string.call_overlay_risk_level, context.getString(analysis.risk.labelRes))
            textSize = 10f
            setTextColor(accentColor)
            setTypeface(Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
            setPadding(10.dp(), 5.dp(), 10.dp(), 5.dp())
            background = GradientDrawable().apply {
                setColor(accent15)
                setCornerRadius(12.dp().toFloat())
                setStroke(1.dp(), accent55)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 10.dp() }
        }
        body.addView(riskChip)

        // "Why this call is dangerous" section
        body.addView(TextView(context).apply {
            text = context.getString(R.string.call_overlay_why_dangerous)
            textSize = 10f
            setTextColor(COLOR_TEXT_SEC)
            setTypeface(Typeface.DEFAULT_BOLD)
            letterSpacing = 0.15f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 12.dp() }
        })
        body.addView(TextView(context).apply {
            text = analysis.explanation
            textSize = 12f
            setTextColor(COLOR_TEXT_PRI)
            setLineSpacing(2.dp().toFloat(), 1f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 4.dp() }
        })

        // Action buttons row
        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 14.dp() }
        }
        actionRow.addView(actionButton(context, density,
            label = context.getString(R.string.call_action_block),
            accent = COLOR_RED) {
            handleBlock(context, analysis)
            dismiss()
        })
        actionRow.addView(actionButton(context, density,
            label = context.getString(R.string.call_action_mute),
            accent = COLOR_AMBER) {
            handleMute(context)
        })
        actionRow.addView(actionButton(context, density,
            label = context.getString(R.string.call_action_copy),
            accent = COLOR_CYAN) {
            handleCopy(context, analysis.phoneNumber)
        })
        actionRow.addView(actionButton(context, density,
            label = context.getString(R.string.call_action_report),
            accent = COLOR_GREEN,
            marginEnd = 0) {
            handleReport(analysis)
            dismiss()
        })
        body.addView(actionRow)

        card.addView(body)

        // Cache halo + icon for the pulse animator
        card.tag = PulseTargets(haloRing = halo, iconCircle = iconCircle)

        // Outer wrapper with gutters
        return FrameLayout(context).apply {
            val gutter = 10.dp()
            setPadding(gutter, 0, gutter, 0)
            addView(
                card,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            // Expose tag from the inner card on the outer wrapper too
            tag = card.tag
        }
    }

    private fun metaRow(context: Context, density: Float, labelRes: Int, value: String): View {
        fun Int.dp(): Int = (this * density).toInt()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 10.dp() }
        }
        row.addView(TextView(context).apply {
            text = context.getString(labelRes)
            textSize = 10f
            setTextColor(COLOR_TEXT_SEC)
            setTypeface(Typeface.DEFAULT_BOLD)
            letterSpacing = 0.15f
            layoutParams = LinearLayout.LayoutParams(80.dp(), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(context).apply {
            text = value
            textSize = 13f
            setTextColor(COLOR_TEXT_PRI)
            setTypeface(Typeface.DEFAULT_BOLD)
            maxLines = 1
        })
        return row
    }

    private fun actionButton(
        context: Context,
        density: Float,
        label: String,
        accent: Int,
        marginEnd: Int = 6,
        onClick: () -> Unit,
    ): TextView {
        fun Int.dp(): Int = (this * density).toInt()
        val fill = Color.argb(40, Color.red(accent), Color.green(accent), Color.blue(accent))
        val stroke = Color.argb(150, Color.red(accent), Color.green(accent), Color.blue(accent))
        return TextView(context).apply {
            text = label
            textSize = 11f
            setTextColor(accent)
            setTypeface(Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
            background = GradientDrawable().apply {
                setColor(fill)
                setCornerRadius(10.dp().toFloat())
                setStroke(1.dp(), stroke)
            }
            setPadding(0, 11.dp(), 0, 11.dp())
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
            ).apply { this.marginEnd = marginEnd.dp() }
            setOnClickListener { onClick() }
        }
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private fun handleBlock(context: Context, analysis: CallAnalysis) {
        val number = analysis.phoneNumber.takeIf { it.isNotBlank() } ?: return
        ioScope.launch {
            runCatching {
                val repo = ScamShieldApp.container().callRepository
                repo.block(number, reason = "Blocked from overlay")
                repo.recordCall(analysis.copy(isBlocked = true), CallAction.BLOCKED)
            }.onFailure { Log.e(TAG, "Block action failed", it) }
        }
        Log.i(TAG, "Block tapped for $number")
    }

    private fun handleMute(context: Context) {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.ringerMode = AudioManager.RINGER_MODE_SILENT
            Log.i(TAG, "Ringer muted from overlay")
        }.onFailure { Log.e(TAG, "Mute failed", it) }
    }

    private fun handleCopy(context: Context, number: String) {
        if (number.isBlank()) return
        runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("phone", number))
            Log.i(TAG, "Copied $number to clipboard")
        }.onFailure { Log.e(TAG, "Copy failed", it) }
    }

    private fun handleReport(analysis: CallAnalysis) {
        ioScope.launch {
            runCatching {
                ScamShieldApp.container().callRepository.recordCall(analysis, CallAction.REPORTED)
            }.onFailure { Log.e(TAG, "Report failed", it) }
        }
        Log.i(TAG, "Reported ${analysis.phoneNumber}")
    }

    // ── Window params ─────────────────────────────────────────────────────────

    private fun buildWindowParams(density: Float): WindowManager.LayoutParams {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            y = (40 * density).toInt()
        }
    }
}
