package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    /**
     * Returns the epoch milliseconds normalized to 00:00:00.000 (start of day)
     * for the given timestamp in the default timezone.
     */
    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Start of today (00:00:00)
     */
    fun getTodayStartOfDay(): Long {
        return getStartOfDay(System.currentTimeMillis())
    }

    /**
     * Start of tomorrow (00:00:00)
     */
    fun getTomorrowStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = getTodayStartOfDay()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /**
     * Start of yesterday (00:00:00)
     */
    fun getYesterdayStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = getTodayStartOfDay()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return cal.timeInMillis
    }

    /**
     * Start of day after tomorrow
     */
    fun getDayAfterTomorrowStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = getTodayStartOfDay()
        cal.add(Calendar.DAY_OF_YEAR, 2)
        return cal.timeInMillis
    }

    /**
     * True if the timestamp falls on the current calendar day
     */
    fun isToday(timestamp: Long): Boolean {
        return getStartOfDay(timestamp) == getTodayStartOfDay()
    }

    /**
     * True if the timestamp falls on tomorrow
     */
    fun isTomorrow(timestamp: Long): Boolean {
        return getStartOfDay(timestamp) == getTomorrowStartOfDay()
    }

    /**
     * True if the timestamp falls on yesterday
     */
    fun isYesterday(timestamp: Long): Boolean {
        return getStartOfDay(timestamp) == getYesterdayStartOfDay()
    }

    /**
     * True if the timestamp falls before today
     */
    fun isPast(timestamp: Long): Boolean {
        return getStartOfDay(timestamp) < getTodayStartOfDay()
    }

    /**
     * True if the timestamp falls after tomorrow
     */
    fun isUpcoming(timestamp: Long): Boolean {
        return getStartOfDay(timestamp) > getTomorrowStartOfDay()
    }

    /**
     * True if the timestamp is in a different calendar year than today
     */
    fun isDifferentYear(timestamp: Long): Boolean {
        val calCurrent = Calendar.getInstance()
        val calTarget = Calendar.getInstance()
        calTarget.timeInMillis = timestamp
        return calCurrent.get(Calendar.YEAR) != calTarget.get(Calendar.YEAR)
    }

    /**
     * Add days to today's start of day
     */
    fun getRelativeDayStart(daysOffset: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = getTodayStartOfDay()
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        return cal.timeInMillis
    }

    /**
     * Formats date nicely for tasks with full support for today, tomorrow,
     * yesterday, different months, and different years.
     */
    fun formatTaskDate(timestamp: Long, isArabic: Boolean): String {
        val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
        return when {
            isToday(timestamp) -> {
                val dayFormat = SimpleDateFormat("d MMMM", locale)
                val formatted = dayFormat.format(Date(timestamp))
                if (isArabic) "اليوم ($formatted)" else "Today ($formatted)"
            }
            isTomorrow(timestamp) -> {
                val dayFormat = SimpleDateFormat("d MMMM", locale)
                val formatted = dayFormat.format(Date(timestamp))
                if (isArabic) "غداً ($formatted)" else "Tomorrow ($formatted)"
            }
            isYesterday(timestamp) -> {
                val dayFormat = SimpleDateFormat("d MMMM", locale)
                val formatted = dayFormat.format(Date(timestamp))
                if (isArabic) "أمس ($formatted)" else "Yesterday ($formatted)"
            }
            isPast(timestamp) -> {
                val fullFormat = SimpleDateFormat(if (isDifferentYear(timestamp)) "d MMMM yyyy" else "d MMMM", locale)
                val formatted = fullFormat.format(Date(timestamp))
                if (isArabic) "متأخر ($formatted)" else "Overdue ($formatted)"
            }
            isDifferentYear(timestamp) -> {
                val fullFormat = SimpleDateFormat("EEE، d MMMM yyyy", locale)
                fullFormat.format(Date(timestamp))
            }
            else -> {
                val fullFormat = SimpleDateFormat("EEE، d MMMM", locale)
                fullFormat.format(Date(timestamp))
            }
        }
    }

    /**
     * Format time for display (e.g. 09:30 AM / 09:30 ص)
     */
    fun formatTimeDisplay(hour: Int, minute: Int, isArabic: Boolean): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
        val sdf = SimpleDateFormat("hh:mm a", locale)
        return sdf.format(cal.time)
    }

    /**
     * Short date format for chips and headers (e.g., "27 سبتمبر 2026" / "Sep 27, 2026")
     */
    fun formatShortDate(timestamp: Long, isArabic: Boolean): String {
        val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
        val sdf = SimpleDateFormat("d MMMM yyyy", locale)
        return sdf.format(Date(timestamp))
    }

    /**
     * Parses time string such as "08:30", "8:30 AM", "08:30 ص", "20:00" into (hour, minute).
     */
    fun parseHourMinute(timeString: String?): Pair<Int, Int>? {
        if (timeString.isNullOrBlank()) return null
        return try {
            val clean = timeString.trim()
            val isPM = clean.contains("م", ignoreCase = true) || clean.contains("PM", ignoreCase = true)
            val isAM = clean.contains("ص", ignoreCase = true) || clean.contains("AM", ignoreCase = true)
            val digits = clean.replace(Regex("[^0-9:]"), "")
            val parts = digits.split(":")
            if (parts.size >= 2) {
                var hour = parts[0].toInt()
                val minute = parts[1].toInt()
                if (isPM && hour < 12) hour += 12
                if (isAM && hour == 12) hour = 0
                Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
