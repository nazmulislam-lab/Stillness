package com.example.ui.util

import java.util.Locale

fun formatDurationMillis(millis: Long): String {
    val totalMinutes = (millis / 60_000L).toInt().coerceAtLeast(0)
    return formatDurationMinutes(totalMinutes)
}

fun formatDurationMinutes(totalMinutes: Int): String {
    val clamped = totalMinutes.coerceAtLeast(0)
    val hours = clamped / 60
    val minutes = clamped % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

fun formatHour24(hour24: Int): String {
    val normalized = ((hour24 % 24) + 24) % 24
    val suffix = if (normalized < 12) "AM" else "PM"
    val hour12 = when {
        normalized == 0 -> 12
        normalized > 12 -> normalized - 12
        else -> normalized
    }
    return String.format(Locale.getDefault(), "%d %s", hour12, suffix)
}

fun dayOfWeekLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    else -> "Sunday"
}
