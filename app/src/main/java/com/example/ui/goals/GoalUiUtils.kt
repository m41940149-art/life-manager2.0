package com.example.ui.goals

import com.example.model.GoalWeek
import com.example.util.GoalProgress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "12 أكتوبر" / "12 Oct" */
fun formatDayMonth(timestamp: Long, isArabic: Boolean): String {
    val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
    return SimpleDateFormat("d MMM", locale).format(Date(timestamp))
}

/** Date range of a week, e.g. "12 Oct – 18 Oct". */
fun formatWeekRange(week: GoalWeek, isArabic: Boolean): String {
    val last = GoalProgress.dayDate(week.startDate, 6)
    return "${formatDayMonth(week.startDate, isArabic)} – ${formatDayMonth(last, isArabic)}"
}
