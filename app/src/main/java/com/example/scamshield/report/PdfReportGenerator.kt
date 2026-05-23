package com.example.scamshield.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.StatFs
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.scamshield.data.DetectedThreat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a cyber-security themed PDF report from a list of detected threats.
 *
 * Usage:
 *   val file = PdfReportGenerator.generate(context, threats, ReportFilter.HIGH_RISK, totalScanned)
 *
 * Architecture:
 *   - Public API: [generate] — validates input, delegates to [ReportRenderer], saves file.
 *   - [ReportRenderer] holds all drawing state (paints, y-cursor, page management)
 *     so the public object stays stateless and thread-safe.
 *   - PDF coordinates use points (1 pt = 1/72 inch). A4 = 595 × 842 pt.
 *   - All drawing uses Android's built-in [android.graphics.pdf.PdfDocument] (API 19+).
 *   - Multi-line text uses [StaticLayout.Builder] (API 23+; minSdk is 24).
 */
object PdfReportGenerator {

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates a PDF report and returns the saved [File].
     *
     * Runs on [Dispatchers.IO]. Safe to call from any coroutine context.
     *
     * @param context       Application context — used for [getExternalFilesDir].
     * @param threats       Full threat list from ThreatStore (not yet filtered).
     * @param filter        Which subset of threats to include.
     * @param totalScanned  Lifetime scanned-message count for the executive summary.
     */
    suspend fun generate(
        context: Context,
        threats: List<DetectedThreat>,
        filter: ReportFilter,
        totalScanned: Int,
    ): File = withContext(Dispatchers.IO) {
        val filtered = filter.applyTo(threats)

        val dir = File(context.getExternalFilesDir(null), "Reports").also { it.mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "ScamShield_Report_$timestamp.pdf")

        val document = PdfDocument()
        try {
            ReportRenderer(document, filtered, totalScanned, filter).render()
            file.outputStream().use { document.writeTo(it) }
        } finally {
            document.close()
        }

        file
    }
}

// ── Private rendering engine ──────────────────────────────────────────────────

/**
 * Stateful PDF renderer. One instance per [generate] call — not thread-safe on
 * its own, but [PdfReportGenerator.generate] already runs inside a single
 * [Dispatchers.IO] coroutine so no concurrent access occurs.
 *
 * Page layout constants (A4 at 72 DPI):
 *   PAGE_W = 595 pt, PAGE_H = 842 pt, MARGIN = 40 pt, CONTENT_W = 515 pt
 */
private class ReportRenderer(
    private val document: PdfDocument,
    private val threats: List<DetectedThreat>,
    private val totalScanned: Int,
    private val filter: ReportFilter,
) {

    // ── Page geometry ─────────────────────────────────────────────────────────

    private val PAGE_W = 595
    private val PAGE_H = 842
    private val MARGIN = 40f
    private val CONTENT_W = (PAGE_W - MARGIN * 2).toInt()

    // Bottom boundary — leave space for the footer (28 pt high + 12 pt gap).
    private val CONTENT_BOTTOM = PAGE_H - MARGIN - 40f

    // ── Cyber color palette ───────────────────────────────────────────────────

    private val CLR_BG         = Color.parseColor("#0A0E1A")   // dark navy — page background
    private val CLR_SURFACE    = Color.parseColor("#141828")   // card background
    private val CLR_BORDER     = Color.parseColor("#1E2A45")   // subtle card border
    private val CLR_CYAN       = Color.parseColor("#00D4FF")   // primary accent / headers
    private val CLR_GREEN      = Color.parseColor("#00FF88")   // safe / low risk
    private val CLR_RED        = Color.parseColor("#FF3B5C")   // high risk / threat
    private val CLR_AMBER      = Color.parseColor("#FFB800")   // medium risk
    private val CLR_ORANGE     = Color.parseColor("#FF6B35")   // social engineering
    private val CLR_TEXT       = Color.parseColor("#E8EAF6")   // primary body text
    private val CLR_MUTED      = Color.parseColor("#6B7A9F")   // secondary / caption text

    // ── Reusable paints ───────────────────────────────────────────────────────

    private val bgPaint     = Paint().apply { color = CLR_BG;      style = Paint.Style.FILL }
    private val surfacePaint= Paint().apply { color = CLR_SURFACE; style = Paint.Style.FILL }
    private val borderPaint = Paint().apply {
        color = CLR_BORDER
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val cyanAccent  = Paint().apply { color = CLR_CYAN; style = Paint.Style.FILL }

    // TextPaint for StaticLayout (multi-line wrapping)
    private fun bodyTp(size: Float = 9f, color: Int = CLR_TEXT, bold: Boolean = false): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize   = size
            this.color = color
            typeface   = if (bold) Typeface.DEFAULT_BOLD else Typeface.MONOSPACE
        }

    // Single-line paint helper
    private fun textPaint(size: Float, color: Int, bold: Boolean = false): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize   = size
            this.color = color
            typeface   = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    // ── Mutable state (per-render) ────────────────────────────────────────────

    private var pageIndex   = 1                          // current 1-based page number
    private var totalPages  = 1                          // updated once we know total pages needed
    private var y           = 0f                         // current vertical cursor on the active page
    private var canvas      = null as Canvas?            // active page canvas
    private var activePage  = null as PdfDocument.Page?  // page returned by startPage()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US)
    private val generatedAt = dateFormat.format(Date())

    // ── Entry point ───────────────────────────────────────────────────────────

    fun render() {
        // Estimate pages: 1 cover + summary + ~1 per threat card (2–3 per page usually)
        totalPages = maxOf(1, 1 + (threats.size + 1) / 2)

        newPage()
        drawPageBackground()
        drawHeader()
        drawExecutiveSummary()

        if (threats.isEmpty()) {
            drawNoThreats()
        } else {
            drawThreatsSection()
        }

        drawFooter()
        finishPage()
    }

    // ── Page management ───────────────────────────────────────────────────────

    private fun newPage() {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageIndex).create()
        activePage = document.startPage(pageInfo)
        canvas = activePage!!.canvas
        y = MARGIN
    }

    private fun finishPage() {
        val page = activePage ?: return
        document.finishPage(page)
        activePage = null
    }

    /** Advances to a new page, draws background + footer slot. */
    private fun nextPage() {
        drawFooter()
        finishPage()
        pageIndex++
        newPage()
        drawPageBackground()
        y = MARGIN + 8f
    }

    /** Fills entire page with the dark navy background. */
    private fun drawPageBackground() {
        canvas?.drawRect(0f, 0f, PAGE_W.toFloat(), PAGE_H.toFloat(), bgPaint)
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private fun drawHeader() {
        val c = canvas ?: return

        // Top cyan accent bar
        c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 3f, cyanAccent)
        y += 10f

        // App name
        c.drawText(
            "SCAMSHIELD SECURITY REPORT",
            MARGIN, y + 16f,
            textPaint(16f, CLR_CYAN, bold = true),
        )
        y += 22f

        // Subtitle row
        c.drawText("AI-Powered Threat Intelligence", MARGIN, y + 10f, textPaint(8f, CLR_MUTED))
        c.drawText(
            "Generated: $generatedAt",
            PAGE_W - MARGIN - 160f, y + 10f,
            textPaint(7f, CLR_MUTED),
        )
        y += 18f

        // Filter badge
        val badge = "  ${filter.label.uppercase()}  "
        val badgePaint = textPaint(7f, CLR_CYAN, bold = true)
        val badgeW = badgePaint.measureText(badge) + 8f
        val badgeRect = RectF(MARGIN, y + 2f, MARGIN + badgeW, y + 14f)
        c.drawRoundRect(badgeRect, 4f, 4f,
            Paint().apply { color = Color.parseColor("#0D2033"); style = Paint.Style.FILL })
        borderPaint.color = CLR_CYAN
        c.drawRoundRect(badgeRect, 4f, 4f, borderPaint)
        borderPaint.color = CLR_BORDER
        c.drawText(badge, MARGIN + 4f, y + 11f, badgePaint)
        y += 22f

        // Divider
        c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 1f,
            Paint().apply { color = CLR_BORDER; style = Paint.Style.FILL })
        y += 12f
    }

    // ── Executive Summary ─────────────────────────────────────────────────────

    private fun drawExecutiveSummary() {
        val c = canvas ?: return

        c.drawText("EXECUTIVE SUMMARY", MARGIN, y + 10f, textPaint(10f, CLR_CYAN, bold = true))
        y += 18f

        val highRisk  = threats.count { it.probability >= 0.70f }
        val medRisk   = threats.count { it.probability in 0.40f..0.69f }
        val lowRisk   = threats.count { it.probability < 0.40f }
        val detRate   = if (totalScanned > 0) threats.size * 100 / totalScanned else 0

        // Stat boxes — 4 columns
        val boxW = (CONTENT_W - 9f) / 4f
        val stats = listOf(
            Triple("MESSAGES\nSCANNED", totalScanned.toString(), CLR_CYAN),
            Triple("THREATS\nDETECTED", threats.size.toString(), CLR_RED),
            Triple("HIGH RISK\n(>70%)", highRisk.toString(), CLR_RED),
            Triple("DETECTION\nRATE", "$detRate%", CLR_AMBER),
        )
        stats.forEachIndexed { i, (label, value, color) ->
            val x = MARGIN + i * (boxW + 3f)
            drawStatBox(c, x, y, boxW, 44f, label, value, color)
        }
        y += 52f

        // Risk distribution bar
        c.drawText("RISK DISTRIBUTION", MARGIN, y + 9f, textPaint(8f, CLR_MUTED, bold = true))
        y += 14f

        if (threats.isNotEmpty()) {
            val barH   = 10f
            val total  = threats.size.toFloat()
            val highW  = CONTENT_W * (highRisk / total)
            val medW   = CONTENT_W * (medRisk / total)
            val lowW   = CONTENT_W * (lowRisk / total)

            var bx = MARGIN
            if (highW > 0) {
                c.drawRect(bx, y, bx + highW, y + barH,
                    Paint().apply { color = CLR_RED; style = Paint.Style.FILL })
                bx += highW
            }
            if (medW > 0) {
                c.drawRect(bx, y, bx + medW, y + barH,
                    Paint().apply { color = CLR_AMBER; style = Paint.Style.FILL })
                bx += medW
            }
            if (lowW > 0) {
                c.drawRect(bx, y, bx + lowW, y + barH,
                    Paint().apply { color = CLR_GREEN; style = Paint.Style.FILL })
            }
            y += barH + 6f

            // Legend
            val legend = listOf(
                Triple("HIGH", highRisk, CLR_RED),
                Triple("MEDIUM", medRisk, CLR_AMBER),
                Triple("LOW", lowRisk, CLR_GREEN),
            )
            var lx = MARGIN
            legend.forEach { (lbl, count, col) ->
                c.drawRect(lx, y + 1f, lx + 8f, y + 9f,
                    Paint().apply { color = col; style = Paint.Style.FILL })
                c.drawText("$lbl  $count", lx + 11f, y + 9f, textPaint(7f, CLR_MUTED))
                lx += 70f
            }
            y += 18f
        }

        // Divider
        c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 1f,
            Paint().apply { color = CLR_BORDER; style = Paint.Style.FILL })
        y += 14f
    }

    private fun drawStatBox(
        c: Canvas, x: Float, y: Float, w: Float, h: Float,
        label: String, value: String, accentColor: Int,
    ) {
        val rect = RectF(x, y, x + w, y + h)
        c.drawRoundRect(rect, 4f, 4f, surfacePaint)

        val bp = Paint().apply { color = accentColor; style = Paint.Style.STROKE; strokeWidth = 1.2f }
        c.drawRoundRect(rect, 4f, 4f, bp)

        // Top accent line
        c.drawRect(x, y, x + w, y + 2f, Paint().apply { color = accentColor; style = Paint.Style.FILL })

        c.drawText(value, x + w / 2f - textPaint(14f, accentColor, bold = true).measureText(value) / 2f,
            y + 22f, textPaint(14f, accentColor, bold = true))

        // Multi-line label using StaticLayout
        val lp = bodyTp(6f, CLR_MUTED)
        val sl = StaticLayout.Builder.obtain(label, 0, label.length, lp, w.toInt() - 4)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .build()
        c.save()
        c.translate(x + 2f, y + 28f)
        sl.draw(c)
        c.restore()
    }

    // ── Threat cards ──────────────────────────────────────────────────────────

    private fun drawNoThreats() {
        val c = canvas ?: return
        c.drawText(
            "No threats found for the selected filter.",
            MARGIN, y + 14f,
            textPaint(10f, CLR_MUTED),
        )
        y += 24f
    }

    private fun drawThreatsSection() {
        val c = canvas ?: return
        c.drawText(
            "DETECTED THREATS  (${threats.size})",
            MARGIN, y + 10f,
            textPaint(10f, CLR_CYAN, bold = true),
        )
        y += 18f

        threats.forEachIndexed { index, threat ->
            drawThreatCard(threat, index + 1)
        }
    }

    /**
     * Draws one threat card. If the card does not fit on the current page,
     * advances to a new page first.
     *
     * A card always occupies at least [MIN_CARD_H] points before any dynamic content.
     */
    private fun drawThreatCard(threat: DetectedThreat, number: Int) {
        val MIN_CARD_H = 90f

        // Check if we need a new page before we start drawing.
        if (y + MIN_CARD_H > CONTENT_BOTTOM) {
            nextPage()
        }

        val c = canvas ?: return
        val riskColor = riskColor(threat.probability)
        val cardX = MARGIN
        val cardY = y

        // Measure dynamic content to know full card height before drawing.
        val keywordsText = if (threat.keywords.isNotEmpty())
            "KEYWORDS: ${threat.keywords.joinToString(" · ")}" else null
        val explanationReason = threat.explanation?.overallReason

        val textAreaW = CONTENT_W - 16

        // Estimate text heights (approximate — StaticLayout will be used for exact rendering)
        val previewLines = estimateLines(threat.messagePreview, textAreaW, 8f)
        val keywordLines = if (keywordsText != null) estimateLines(keywordsText, textAreaW, 7f) else 0
        val explanationLines = if (explanationReason != null) estimateLines(explanationReason, textAreaW, 7.5f) else 0

        val cardH = 16f +                    // top padding
            12f +                            // header row (threat number + probability)
            8f +                             // timestamp row
            (previewLines * 11f) + 6f +      // preview text
            (if (keywordsText != null) keywordLines * 10f + 8f else 0f) +
            (if (explanationReason != null) explanationLines * 10f + 10f else 0f) +
            16f                              // bottom padding

        // Page break if card does not fit
        if (y + cardH > CONTENT_BOTTOM) {
            nextPage()
        }

        val c2 = canvas ?: return

        // Card background
        val cardRect = RectF(cardX, y, cardX + CONTENT_W, y + cardH)
        c2.drawRoundRect(cardRect, 6f, 6f, surfacePaint)
        // Left accent bar
        c2.drawRect(cardX, y + 6f, cardX + 3f, y + cardH - 6f,
            Paint().apply { color = riskColor; style = Paint.Style.FILL })
        // Card border
        val cbp = Paint().apply { color = riskColor; style = Paint.Style.STROKE; strokeWidth = 0.8f }
        c2.drawRoundRect(cardRect, 6f, 6f, cbp)

        var cy = y + 14f

        // ── Header row ────────────────────────────────────────────────────────
        val numLabel = "#$number"
        c2.drawText(numLabel, cardX + 10f, cy, textPaint(8f, CLR_MUTED, bold = true))

        val probPct = "${(threat.probability * 100).toInt()}% SCAM"
        val probP   = textPaint(9f, riskColor, bold = true)
        c2.drawText(probPct, PAGE_W - MARGIN - probP.measureText(probPct) - 6f, cy, probP)

        val riskLabel = riskLabel(threat.probability)
        val rlP = textPaint(7f, riskColor)
        val rlX = PAGE_W - MARGIN - probP.measureText(probPct) - 6f - rlP.measureText(riskLabel) - 8f
        c2.drawText(riskLabel, rlX, cy, rlP)

        cy += 12f

        // ── Source + timestamp ────────────────────────────────────────────────
        val srcLabel = threat.sourcePackage.substringAfterLast('.').uppercase()
        val ts = dateFormat.format(Date(threat.timestamp))
        c2.drawText("$srcLabel  ·  $ts", cardX + 10f, cy, textPaint(7f, CLR_MUTED))
        cy += 10f

        // ── Message preview ───────────────────────────────────────────────────
        val previewTp = bodyTp(8f, CLR_TEXT)
        val previewSl = StaticLayout.Builder
            .obtain(threat.messagePreview, 0, threat.messagePreview.length, previewTp, textAreaW)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1f)
            .setMaxLines(6)
            .build()
        c2.save()
        c2.translate(cardX + 10f, cy)
        previewSl.draw(c2)
        c2.restore()
        cy += previewSl.height + 6f

        // ── Suspicious keywords ───────────────────────────────────────────────
        if (keywordsText != null) {
            val ktp = bodyTp(7f, CLR_AMBER)
            val ksl = StaticLayout.Builder
                .obtain(keywordsText, 0, keywordsText.length, ktp, textAreaW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1f, 1f)
                .build()
            c2.save()
            c2.translate(cardX + 10f, cy)
            ksl.draw(c2)
            c2.restore()
            cy += ksl.height + 8f
        }

        // ── Explanation ───────────────────────────────────────────────────────
        if (explanationReason != null) {
            c2.drawText("AI ANALYSIS:", cardX + 10f, cy, textPaint(7f, CLR_CYAN, bold = true))
            cy += 10f
            val etp = bodyTp(7.5f, CLR_MUTED)
            val esl = StaticLayout.Builder
                .obtain(explanationReason, 0, explanationReason.length, etp, textAreaW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.5f, 1f)
                .build()
            c2.save()
            c2.translate(cardX + 10f, cy)
            esl.draw(c2)
            c2.restore()
            cy += esl.height + 8f
        }

        y += cardH + 8f
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private fun drawFooter() {
        val c = canvas ?: return
        val fy = PAGE_H - MARGIN + 4f

        c.drawRect(MARGIN, fy - 14f, PAGE_W - MARGIN, fy - 13f,
            Paint().apply { color = CLR_BORDER; style = Paint.Style.FILL })

        c.drawText("ScamShield · AI-Powered Mobile Security", MARGIN, fy, textPaint(7f, CLR_MUTED))

        val pageStr = "Page $pageIndex"
        val pp = textPaint(7f, CLR_MUTED)
        c.drawText(pageStr, PAGE_W - MARGIN - pp.measureText(pageStr), fy, pp)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Approximate line count for a string rendered at [textSizeSp] in [availableW] px. */
    private fun estimateLines(text: String, availableW: Int, textSizeSp: Float): Int {
        val charsPerLine = (availableW / (textSizeSp * 0.55f)).toInt().coerceAtLeast(1)
        return (text.length / charsPerLine) + 1
    }

    private fun riskColor(probability: Float): Int = when {
        probability >= 0.70f -> CLR_RED
        probability >= 0.40f -> CLR_AMBER
        else                 -> CLR_GREEN
    }

    private fun riskLabel(probability: Float): String = when {
        probability >= 0.70f -> "HIGH RISK"
        probability >= 0.40f -> "MEDIUM RISK"
        else                 -> "LOW RISK"
    }
}
