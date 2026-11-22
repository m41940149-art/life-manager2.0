package com.example.model

/** Whether a habit belongs in the automatic local-time view. Legacy habits stay visible. */
fun isHabitVisibleInPeriod(time: String?, hourOfDay: Int, showAllPeriods: Boolean): Boolean {
    require(hourOfDay in 0..23)
    val period = if (hourOfDay < 16) HabitTimeGroup.MORNING else HabitTimeGroup.AFTERNOON_EVENING
    val group = habitTimeGroup(time)
    return showAllPeriods || group == HabitTimeGroup.UNSCHEDULED || group == period
}

/** Uses local scheduled performance time, never the time a check-in was recorded. */
enum class HabitTimeGroup { MORNING, AFTERNOON_EVENING, UNSCHEDULED }

fun habitTimeGroup(time: String?): HabitTimeGroup {
    val match = Regex("^(\\d{2}):(\\d{2})$").matchEntire(time ?: "")
        ?: return HabitTimeGroup.UNSCHEDULED
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    if (hour !in 0..23 || minute !in 0..59) return HabitTimeGroup.UNSCHEDULED
    return if (hour < 16) HabitTimeGroup.MORNING else HabitTimeGroup.AFTERNOON_EVENING
}
