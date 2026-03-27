package com.example.studywise.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Converts a date string to a human-readable format.
 * @param dateString The input date string (ISO 8601 format recommended, e.g., "2026-03-26T14:30:00").
 * @param showRelative If true, returns "today", "yesterday", or "x days ago" if applicable; otherwise, returns "dd/MM/yyyy".
 * @return The formatted date string.
 */
fun formatDateHumanReadable(dateString: String, showRelative: Boolean = true): String {
    val inputFormatter = DateTimeFormatter.ISO_DATE_TIME
    val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return try {
        // Parse as OffsetDateTime if possible, else LocalDateTime (assume UTC)
        val zonedDateTime = try {
            java.time.OffsetDateTime.parse(dateString, inputFormatter).atZoneSameInstant(java.time.ZoneId.systemDefault())
        } catch (e: Exception) {
            java.time.LocalDateTime.parse(dateString, inputFormatter).atZone(java.time.ZoneOffset.UTC).withZoneSameInstant(java.time.ZoneId.systemDefault())
        }
        val now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
        val date = zonedDateTime.toLocalDate()
        val today = now.toLocalDate()
        val daysBetween = ChronoUnit.DAYS.between(date, today).toInt()
        val monthsBetween = ChronoUnit.MONTHS.between(date.withDayOfMonth(1), today.withDayOfMonth(1)).toInt()
        val yearsBetween = ChronoUnit.YEARS.between(date.withDayOfYear(1), today.withDayOfYear(1)).toInt()
        if (showRelative) {
            when {
                daysBetween == 0 -> {
                    val hoursBetween = ChronoUnit.HOURS.between(zonedDateTime, now).toInt()
                    if (hoursBetween == 0) {
                        val minutesBetween = ChronoUnit.MINUTES.between(zonedDateTime, now).toInt()
                        when {
                            minutesBetween == 0 -> "just now"
                            minutesBetween == 1 -> "1 minute ago"
                            minutesBetween > 1 -> "$minutesBetween minutes ago"
                            else -> "today"
                        }
                    } else when {
                        hoursBetween == 1 -> "1 hour ago"
                        hoursBetween > 1 -> "$hoursBetween hours ago"
                        else -> "today"
                    }
                }
                daysBetween == 1 -> "yesterday"
                daysBetween in 2..6 -> "$daysBetween days ago"
                monthsBetween in 1..11 && yearsBetween == 0 -> "$monthsBetween month${if (monthsBetween > 1) "s" else ""} ago"
                yearsBetween >= 1 -> "$yearsBetween year${if (yearsBetween > 1) "s" else ""} ago"
                else -> date.format(outputFormatter)
            }
        } else {
            date.format(outputFormatter)
        }
    } catch (e: Exception) {
        // Fallback: return the original string if parsing fails
        dateString
    }
}