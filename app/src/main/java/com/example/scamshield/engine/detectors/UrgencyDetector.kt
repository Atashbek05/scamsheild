package com.example.scamshield.engine.detectors

/**
 * Flags time-pressure language that scammers use to short-circuit deliberation.
 */
object UrgencyDetector {

    private val PATTERNS = listOf(
        "act now"             to "Urgency trigger",
        "act immediately"     to "Urgency trigger",
        "limited time"        to "Time pressure",
        "time is running out" to "Time pressure",
        "expires soon"        to "Expiry pressure",
        "expires in"          to "Expiry pressure",
        "expires today"       to "Expiry pressure",
        "last chance"         to "Final deadline",
        "today only"          to "Today-only offer",
        "ends today"          to "Today-only offer",
        "while supplies last" to "Scarcity bait",
        "only a few left"     to "Scarcity bait",
        "24 hours"            to "24-hour deadline",
        "48 hours"            to "48-hour deadline",
        "next 10 minutes"     to "Sub-hour deadline",
        "respond immediately" to "Immediate response demand",
        "respond now"         to "Immediate response demand",
        "don't delay"         to "Delay warning",
        "don't wait"          to "Delay warning",
        "urgent"              to "Urgency keyword",
        "asap"                to "Urgency keyword",
        "hurry"                to "Urgency keyword",
        "immediately"         to "Urgency keyword",
        "deadline"            to "Deadline reference",
        "final notice"        to "Final-notice threat",
        "final warning"       to "Final-warning threat",
    )

    fun detect(lowerText: String): List<String> {
        val labels = LinkedHashSet<String>()
        for ((needle, label) in PATTERNS) {
            if (lowerText.contains(needle)) labels.add(label)
        }
        return labels.toList()
    }
}
