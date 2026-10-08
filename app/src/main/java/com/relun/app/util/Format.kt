package com.relun.app.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

fun calculateAge(dob: LocalDate, today: LocalDate = LocalDate.now()): Int = Period.between(dob, today).years

/** "Very close by", "< 1 km away", "3.2 km away", "12 km away". */
fun formatDistance(km: Double?): String? = when {
    km == null -> null
    km < 0.5 -> "Very close by"
    km < 1 -> "< 1 km away"
    km < 10 -> String.format(Locale.getDefault(), "%.1f km away", km).replace(".0 km", " km")
    else -> "${km.roundToInt()} km away"
}

fun formatLastActive(online: Boolean, lastActive: Instant?): String {
    if (online) return "Online now"
    if (lastActive == null) return "Recently active"
    val minutes = Duration.between(lastActive, Instant.now()).toMinutes()
    return when {
        minutes < 60 -> "Active ${minutes.coerceAtLeast(1)}m ago"
        minutes < 60 * 24 -> "Active ${minutes / 60}h ago"
        minutes < 60 * 24 * 7 -> "Active ${minutes / (60 * 24)}d ago"
        else -> "Active a while ago"
    }
}

/** Conversation list timestamps: "now", "5m", "3h", "2d", or a date. */
fun formatShortAgo(at: Instant): String {
    val minutes = Duration.between(at, Instant.now()).toMinutes()
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 60 * 24 -> "${minutes / 60}h"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d"
        else -> DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()).format(at.atZone(ZoneId.systemDefault()))
    }
}

fun formatAgo(at: Instant?): String {
    if (at == null) return ""
    val minutes = Duration.between(at, Instant.now()).toMinutes()
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("H:mm", Locale.getDefault())
private val dateTimeFormat = DateTimeFormatter.ofPattern("EEE d MMM · H:mm", Locale.getDefault())

fun formatClock(at: Instant): String = timeFormat.format(at.atZone(ZoneId.systemDefault()))

/** "Sat 12 Oct · 11:00" */
fun formatWhen(at: Instant): String = dateTimeFormat.format(at.atZone(ZoneId.systemDefault()))

/** Day separators in chat: "Today", "Yesterday", "Monday", "12 Oct". */
fun formatDay(at: Instant): String {
    val date = at.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7 -> date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
        else -> DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()).format(date)
    }
}

fun formatHeight(cm: Double?): String? = cm?.let {
    if (it < 3) {
        // Stored in feet by some older clients.
        val inches = (it * 12).roundToInt()
        "${inches / 12}'${inches % 12}\""
    } else {
        "${it.roundToInt()} cm"
    }
}

fun formatCoins(n: Int): String = String.format(Locale.getDefault(), "%,d", n)
