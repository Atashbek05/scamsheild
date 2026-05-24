package com.example.scamshield.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.example.scamshield.util.logD
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.scamshield.R

/**
 * Singleton that manages the full lifecycle of the real-time scam-warning overlay.
 *
 * ═══════════════════════════════════════════════════════════════════
 * ARCHITECTURE OVERVIEW
 * ═══════════════════════════════════════════════════════════════════
 *
 * Data flow:
 *   Service (IO coroutine) → showWarning(context, data)
 *     └─ mainHandler.post → showOnMainThread()
 *           ├─ [suppression checks] ────────────► drop silently
 *           └─ WindowManager.addView(card)
 *                 ├─ enter animation (slide-down + fade-in, 350ms)
 *                 ├─ risk-bar fill animation (ValueAnimator, 600ms)
 *                 └─ AUTO_DISMISS_MS timer
 *                       ├─ timer fires → dismissWithAnimation()
 *                       └─ user taps Dismiss → dismissWithAnimation()
 *                             └─ exit animation (slide-up + fade-out, 250ms)
 *                                   └─ withEndAction → WindowManager.removeView()
 *
 * Suppression rules (all evaluated on the main thread — no locks needed):
 *   1. SYSTEM_ALERT_WINDOW not granted          → skip + warn log
 *   2. Another overlay is active or animating   → drop (single-instance)
 *   3. Same package within PER_PACKAGE_COOLDOWN → drop + debug log
 *   4. Same message content (hash) within
 *      MESSAGE_DEDUP_WINDOW                     → drop + debug log  (overlay-layer dedup)
 *
 * UI theme: cyber-security dark neon — matches DashboardScreen palette.
 *   Background : #141E35 / #0F1628
 *   Border     : #FF3B5C (RED) or #FFB800 (YELLOW) — 2dp neon stroke
 *   Text       : #E8F0FF primary, #8BA5C8 secondary
 *   Risk bar   : #00D4FF → accent gradient, animated fill
 *
 * All WindowManager / View operations are posted to [mainHandler] so services
 * can call [showWarning] safely from any thread or coroutine context.
 * ═══════════════════════════════════════════════════════════════════
 */
object OverlayManager {

    private const val TAG = "OverlayManager"

    // ── Timing ───────────────────────────────────────────────────────────────────

    /** Overlay visible duration before auto-dismiss animation fires. */
    private const val AUTO_DISMISS_MS = 7_000L

    /** After an overlay fires for a package, that package is silent for this long. */
    private const val PER_PACKAGE_COOLDOWN_MS = 30_000L

    /**
     * Same message preview hash within this window is suppressed at the overlay layer.
     * Acts as a second dedup fence after AnalysisCooldown (which deduplicates at the
     * analysis-request level).  Catches the unlikely case where both the
     * NotificationListener and AccessibilityService analyse the same text and both
     * receive a scam response before either overlay fires.
     */
    private const val MESSAGE_DEDUP_WINDOW_MS = 60_000L

    /** Slide-in duration for the enter animation. */
    private const val ANIM_ENTER_MS = 350L

    /** Slide-out duration for the exit animation. */
    private const val ANIM_EXIT_MS  = 250L

    /** Risk bar fill animation duration. */
    private const val ANIM_BAR_MS   = 600L

    /** Risk bar fill animation start delay (after card enters). */
    private const val ANIM_BAR_DELAY_MS = 300L

    // ── Cyber-security colour palette (mirrors Color.kt) ─────────────────────────

    private val COLOR_BG_CARD    = Color.parseColor("#141E35")
    private val COLOR_BG_SURFACE = Color.parseColor("#0F1628")
    private val COLOR_CYAN       = Color.parseColor("#00D4FF")
    private val COLOR_RED        = Color.parseColor("#FF3B5C")
    private val COLOR_AMBER      = Color.parseColor("#FFB800")
    private val COLOR_TEXT_PRI   = Color.parseColor("#E8F0FF")
    private val COLOR_TEXT_SEC   = Color.parseColor("#8BA5C8")
    private val COLOR_BORDER     = Color.parseColor("#1E3050")

    // ── Main-thread handler ───────────────────────────────────────────────────────

    private val mainHandler = Handler(Looper.getMainLooper())

    private val autoDismissRunnable = Runnable { dismissWithAnimation() }

    // ── State — only mutated on the main thread ───────────────────────────────────

    /**
     * True while a card is visible or its exit animation is still running.
     * Blocks new overlay requests so only one warning is ever on screen.
     */
    private var isActive = false

    /** Tracks when the last overlay was shown for each package (cooldown). */
    private val packageCooldowns = mutableMapOf<String, Long>()

    /**
     * Maps message-preview hashCode → show timestamp for overlay-layer dedup.
     * Cleaned up on each [showOnMainThread] call to avoid unbounded growth.
     */
    private val recentHashes = LinkedHashMap<Int, Long>()

    private var windowManager: WindowManager? = null
    private var currentView: View? = null

    // ── Public API ────────────────────────────────────────────────────────────────

    /**
     * Requests a warning overlay for [data].  Safe to call from any thread.
     * Uses [appContext] to avoid leaking service/activity references across
     * the async boundary into WindowManager.
     */
    fun showWarning(context: Context, data: OverlayData) {
        val appContext = context.applicationContext
        mainHandler.post { showOnMainThread(appContext, data) }
    }

    /**
     * Triggers the exit animation and removes the overlay.
     * Safe to call from any thread; no-op if no overlay is active.
     */
    fun dismiss() {
        mainHandler.post { dismissWithAnimation() }
    }

    // ── Internal — main thread only ───────────────────────────────────────────────

    private fun showOnMainThread(appContext: Context, data: OverlayData) {

        // ── Guard 1: SYSTEM_ALERT_WINDOW ─────────────────────────────────────────
        if (!Settings.canDrawOverlays(appContext)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted — overlay blocked for ${data.packageName}")
            return
        }

        // ── Guard 2: single-instance ─────────────────────────────────────────────
        if (isActive) {
            logD(TAG, "Overlay already active — dropping event from ${data.packageName}")
            return
        }

        // ── Guard 3: per-package cooldown ─────────────────────────────────────────
        val now       = System.currentTimeMillis()
        val lastShown = packageCooldowns[data.packageName] ?: 0L
        val elapsed   = now - lastShown
        if (elapsed < PER_PACKAGE_COOLDOWN_MS) {
            val remainSec = (PER_PACKAGE_COOLDOWN_MS - elapsed) / 1_000
            logD(TAG, "Cooldown active for ${data.packageName} — ${remainSec}s remaining")
            return
        }

        // ── Guard 4: message-level dedup ─────────────────────────────────────────
        purgeExpiredHashes(now)
        val msgHash = data.messagePreview.hashCode()
        if (recentHashes.containsKey(msgHash)) {
            logD(TAG, "Duplicate message suppressed for ${data.packageName}")
            return
        }
        recentHashes[msgHash] = now

        // ── Build and attach the overlay ──────────────────────────────────────────
        val density = appContext.resources.displayMetrics.density
        val view    = buildOverlayCard(appContext, density, data)
        val params  = buildWindowParams(density)

        try {
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.addView(view, params)

            windowManager = wm
            currentView   = view
            isActive      = true
            packageCooldowns[data.packageName] = now

            // Prime off-screen position before the first draw so there is no flash
            view.translationY = -(320 * density)
            view.alpha        = 0f

            // Enter: slide down from above the screen + fade in + gentle overshoot
            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(ANIM_ENTER_MS)
                .setInterpolator(OvershootInterpolator(0.8f))
                .start()

            mainHandler.postDelayed(autoDismissRunnable, AUTO_DISMISS_MS)

            logD(
                TAG,
                "Overlay shown | pkg=${data.packageName} | app=\"${data.sourceAppName}\" | " +
                    "level=${data.threatLevel} | prob=${"%.0f".format(data.probability * 100)}% | " +
                    "keywords=${data.keywords}",
            )
        } catch (e: Exception) {
            Log.e(TAG, "WindowManager.addView failed: ${e.message}", e)
            isActive = false   // Reset so the next event can try again
        }
    }

    private fun dismissWithAnimation() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        val view = currentView ?: return

        // Cancel any in-progress enter animation before starting the exit
        view.animate().cancel()

        val slideUpPx = -(320f * view.resources.displayMetrics.density)

        // Exit: slide up above the screen + fade out
        view.animate()
            .translationY(slideUpPx)
            .alpha(0f)
            .setDuration(ANIM_EXIT_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                try {
                    windowManager?.removeView(view)
                } catch (e: Exception) {
                    Log.e(TAG, "WindowManager.removeView failed: ${e.message}", e)
                } finally {
                    currentView   = null
                    windowManager = null
                    isActive      = false
                    logD(TAG, "Overlay dismissed")
                }
            }
            .start()
    }

    /** Removes hash entries older than [MESSAGE_DEDUP_WINDOW_MS] to bound memory use. */
    private fun purgeExpiredHashes(now: Long) {
        val expired = recentHashes.entries
            .filter { (_, ts) -> now - ts > MESSAGE_DEDUP_WINDOW_MS }
            .map { it.key }
        expired.forEach { recentHashes.remove(it) }
    }

    // ── WindowManager layout parameters ──────────────────────────────────────────

    private fun buildWindowParams(density: Float): WindowManager.LayoutParams {
        // TYPE_APPLICATION_OVERLAY is the correct type for Android 8+ app overlays.
        // It sits above app windows but below the system status bar and IME.
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
            // FLAG_NOT_FOCUSABLE   : overlay won't steal keyboard focus from the app below
            // FLAG_NOT_TOUCH_MODAL : touches outside the card bounds pass through to the app
            // FLAG_HARDWARE_ACCELERATED: enables GPU rendering for smooth card animations
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE       or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            y = (60 * density).toInt()   // Offset below the system status bar
        }
    }

    // ── Programmatic card construction ────────────────────────────────────────────

    /**
     * Builds the warning card entirely in code — no XML layouts, no Compose —
     * so it can be constructed from an application context inside a service.
     *
     * Visual layout (dark neon theme):
     *
     *   ┌─ FrameLayout wrapper (MATCH_PARENT, 10dp horizontal gutters) ──────────┐
     *   │  ┌─ LinearLayout card (rounded 14dp, dark bg, 2dp neon stroke) ──────┐ │
     *   │  │  ┌─ Header row ──────────────────────────────────────────────────┐ │ │
     *   │  │  │  [⚠ icon]  [SCAM DETECTED]  [ScamShield]  [✕ close]         │ │ │
     *   │  │  └──────────────────────────────────────────────────────────────┘ │ │
     *   │  │  ── accent hairline divider ──                                    │ │
     *   │  │  ┌─ Body ────────────────────────────────────────────────────────┐ │ │
     *   │  │  │  RISK SCORE                                         87%       │ │ │
     *   │  │  │  ████████████████████░░░░░░  ← animated fill bar             │ │ │
     *   │  │  │  FROM   Telegram                                              │ │ │
     *   │  │  │  FLAGGED KEYWORDS                                             │ │ │
     *   │  │  │  [win] [prize] [click now] [+2]  ← neon chips                │ │ │
     *   │  │  │  ╔═══════════════════════════╗                                │ │ │
     *   │  │  │  ║        DISMISS            ║  ← cyan outlined button        │ │ │
     *   │  │  │  ╚═══════════════════════════╝                                │ │ │
     *   │  │  └──────────────────────────────────────────────────────────────┘ │ │
     *   │  └──────────────────────────────────────────────────────────────────┘ │
     *   └────────────────────────────────────────────────────────────────────────┘
     */
    private fun buildOverlayCard(context: Context, density: Float, data: OverlayData): View {

        fun Int.dp(): Int   = (this * density).toInt()
        fun Float.dp(): Int = (this * density).toInt()

        val accentColor = when (data.threatLevel) {
            ThreatLevel.RED    -> COLOR_RED
            ThreatLevel.YELLOW -> COLOR_AMBER
        }

        // Semi-transparent tint of the accent used for icon backgrounds and chip fills
        val accentAlpha15 = Color.argb(38, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        val accentAlpha50 = Color.argb(127, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))

        val headerLabel = when (data.threatLevel) {
            ThreatLevel.RED    -> context.getString(R.string.overlay_scam_detected)
            ThreatLevel.YELLOW -> context.getString(R.string.overlay_suspicious_message)
        }

        val cardCorner = 14.dp().toFloat()

        // ─────────────────────────────────────────────────────────────────────────
        // Card shell
        // ─────────────────────────────────────────────────────────────────────────
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(COLOR_BG_CARD)
                setCornerRadius(cardCorner)
                setStroke(2.dp(), accentColor)
            }
            elevation    = 16.dp().toFloat()
            clipToOutline = true
        }

        // ─────────────────────────────────────────────────────────────────────────
        // Header row — dark gradient bg, warning icon, title, app badge, close btn
        // ─────────────────────────────────────────────────────────────────────────
        val headerBg = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.argb(230, 18, 4, 12),   // near-black with warm purple tint
                COLOR_BG_SURFACE,
            ),
        ).apply {
            // Round only the top-left and top-right corners to blend into the card
            setCornerRadii(floatArrayOf(cardCorner, cardCorner, cardCorner, cardCorner, 0f, 0f, 0f, 0f))
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background  = headerBg
            setPadding(14.dp(), 11.dp(), 8.dp(), 11.dp())
            gravity     = Gravity.CENTER_VERTICAL
        }

        // Warning icon inside a small tinted circle
        val iconCircle = TextView(context).apply {
            text      = "⚠"
            textSize  = 15f
            setTextColor(accentColor)
            gravity   = Gravity.CENTER
            val size  = 30.dp()
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = GradientDrawable().apply {
                shape         = GradientDrawable.OVAL
                setColor(accentAlpha15)
                setStroke(1.dp(), accentAlpha50)
            }
        }

        val titleView = TextView(context).apply {
            text          = headerLabel
            textSize      = 13f
            setTextColor(accentColor)
            setTypeface(Typeface.DEFAULT_BOLD)
            letterSpacing = 0.08f
            layoutParams  = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
            ).apply { marginStart = 10.dp() }
        }

        // Branded badge ("ScamShield") — right of title, left of close button
        val badgeView = TextView(context).apply {
            text     = context.getString(R.string.app_name)
            textSize = 9f
            setTextColor(COLOR_TEXT_SEC)
            gravity  = Gravity.CENTER
            setPadding(6.dp(), 3.dp(), 6.dp(), 3.dp())
            background = GradientDrawable().apply {
                setColor(COLOR_BG_SURFACE)
                setCornerRadius(4.dp().toFloat())
                setStroke(1.dp(), COLOR_BORDER)
            }
        }

        val closeBtn = TextView(context).apply {
            text          = "✕"
            textSize      = 17f
            setTextColor(COLOR_TEXT_SEC)
            gravity       = Gravity.CENTER
            minimumWidth  = 44.dp()
            minimumHeight = 44.dp()
            setPadding(8.dp(), 0, 8.dp(), 0)
            setOnClickListener { dismiss() }
        }

        header.addView(iconCircle)
        header.addView(titleView)
        header.addView(badgeView)
        header.addView(closeBtn)
        card.addView(header)

        // ─────────────────────────────────────────────────────────────────────────
        // Accent hairline divider
        // ─────────────────────────────────────────────────────────────────────────
        card.addView(View(context).apply {
            setBackgroundColor(accentAlpha50)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1,
            )
        })

        // ─────────────────────────────────────────────────────────────────────────
        // Body
        // ─────────────────────────────────────────────────────────────────────────
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 12.dp(), 16.dp(), 16.dp())
        }

        // ── Risk score row ────────────────────────────────────────────────────────
        val pct = (data.probability * 100).toInt()

        val riskRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        riskRow.addView(TextView(context).apply {
            text          = context.getString(R.string.overlay_risk_score)
            textSize      = 10f
            setTextColor(COLOR_TEXT_SEC)
            setTypeface(Typeface.DEFAULT_BOLD)
            letterSpacing = 0.15f
            layoutParams  = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
            )
        })

        riskRow.addView(TextView(context).apply {
            text     = "$pct%"
            textSize = 22f
            setTextColor(accentColor)
            setTypeface(Typeface.DEFAULT_BOLD)
        })

        body.addView(riskRow)

        // ── Animated risk fill bar ─────────────────────────────────────────────────
        // The fill view is sized to MATCH_PARENT, then scaleX is animated from 0 to
        // data.probability with pivotX=0 so the fill expands left-to-right.
        val barTrack = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 6.dp(),
            ).apply { topMargin = 8.dp() }
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
            // Collapse to zero width, scaling from the left edge
            pivotX = 0f
            scaleX = 0f
        }

        barTrack.addView(barFill)
        body.addView(barTrack)

        // Animate the fill once the card is laid out and measured
        barFill.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    barFill.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val targetScale = data.probability.coerceIn(0f, 1f)
                    val anim = ValueAnimator.ofFloat(0f, targetScale).apply {
                        duration     = ANIM_BAR_MS
                        startDelay   = ANIM_BAR_DELAY_MS
                        interpolator = DecelerateInterpolator()
                        addUpdateListener { barFill.scaleX = it.animatedValue as Float }
                    }
                    anim.start()
                    logD(TAG, "Risk bar animating to ${"%.0f".format(targetScale * 100)}%")
                }
            },
        )

        // ── Source app row ────────────────────────────────────────────────────────
        val sourceRow = LinearLayout(context).apply {
            orientation  = LinearLayout.HORIZONTAL
            gravity      = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 10.dp() }
        }

        sourceRow.addView(TextView(context).apply {
            text          = context.getString(R.string.overlay_from)
            textSize      = 10f
            setTextColor(COLOR_TEXT_SEC)
            setTypeface(Typeface.DEFAULT_BOLD)
            letterSpacing = 0.15f
            layoutParams  = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = 8.dp() }
        })

        sourceRow.addView(TextView(context).apply {
            text     = data.sourceAppName
            textSize = 13f
            setTextColor(COLOR_TEXT_PRI)
            setTypeface(Typeface.DEFAULT_BOLD)
        })

        body.addView(sourceRow)

        // ── Keywords section ──────────────────────────────────────────────────────
        if (data.keywords.isNotEmpty()) {
            body.addView(TextView(context).apply {
                text          = context.getString(R.string.overlay_flagged_keywords)
                textSize      = 10f
                setTextColor(COLOR_TEXT_SEC)
                setTypeface(Typeface.DEFAULT_BOLD)
                letterSpacing = 0.15f
                layoutParams  = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 10.dp() }
            })

            val chipsRow = LinearLayout(context).apply {
                orientation  = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 6.dp() }
            }

            // Show at most 4 chips; overflow count badge replaces the rest
            val visibleKeywords = data.keywords.take(4)
            val overflowCount   = data.keywords.size - visibleKeywords.size

            visibleKeywords.forEach { keyword ->
                // Truncate very long keywords so they fit on one row
                val label = if (keyword.length > 14) "${keyword.take(13)}…" else keyword
                chipsRow.addView(buildChip(context, density, label, accentColor))
            }

            if (overflowCount > 0) {
                chipsRow.addView(buildChip(context, density, "+$overflowCount", COLOR_TEXT_SEC))
            }

            body.addView(chipsRow)
        }

        // ── Dismiss button ─────────────────────────────────────────────────────────
        body.addView(TextView(context).apply {
            text          = context.getString(R.string.overlay_dismiss)
            textSize      = 12f
            setTextColor(COLOR_CYAN)
            setTypeface(Typeface.DEFAULT_BOLD)
            gravity       = Gravity.CENTER
            letterSpacing = 0.2f
            background    = GradientDrawable().apply {
                setColor(Color.argb(30, 0, 212, 255))    // cyan at ~12% opacity
                setCornerRadius(8.dp().toFloat())
                setStroke(1.dp(), COLOR_CYAN)
            }
            setPadding(0, 10.dp(), 0, 10.dp())
            layoutParams  = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 14.dp() }
            setOnClickListener { dismiss() }
        })

        card.addView(body)

        // ─────────────────────────────────────────────────────────────────────────
        // Outer wrapper — adds horizontal gutters so the card doesn't touch screen edges
        // ─────────────────────────────────────────────────────────────────────────
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
        }
    }

    /**
     * Creates a single keyword chip: rounded pill with semi-transparent accent fill,
     * 1dp accent border, and bold accent-coloured text.
     */
    private fun buildChip(context: Context, density: Float, text: String, accentColor: Int): TextView {
        fun Int.dp(): Int = (this * density).toInt()
        val chipAlpha20 = Color.argb(51, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        val chipAlpha60 = Color.argb(153, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))

        return TextView(context).apply {
            this.text     = text
            textSize      = 11f
            setTextColor(accentColor)
            setTypeface(Typeface.DEFAULT_BOLD)
            gravity       = Gravity.CENTER
            background    = GradientDrawable().apply {
                setColor(chipAlpha20)
                setCornerRadius(12.dp().toFloat())
                setStroke(1.dp(), chipAlpha60)
            }
            setPadding(8.dp(), 4.dp(), 8.dp(), 4.dp())
            layoutParams  = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = 6.dp() }
        }
    }
}
