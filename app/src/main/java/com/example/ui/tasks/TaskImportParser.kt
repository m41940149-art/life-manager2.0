package com.example.ui.tasks

import com.example.model.Task
import com.example.model.TaskPriority
import com.example.util.DateTimeUtils
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

/** One task read from pasted JSON, ready to be saved. */
data class ImportRow(val task: Task, val isDuplicate: Boolean, val isPast: Boolean)

/** Result of reading pasted text. [fatal] is set when nothing could be read at all. */
data class ImportParseResult(
    val rows: List<ImportRow> = emptyList(),
    val errors: List<String> = emptyList(),
    val fatal: String? = null
) {
    val toAdd: List<ImportRow> get() = rows.filter { !it.isDuplicate }
    val duplicateCount: Int get() = rows.count { it.isDuplicate }
    val pastCount: Int get() = rows.count { !it.isDuplicate && it.isPast }
}

object TaskImportParser {

    private val categoriesAr = listOf("عمل", "شخصي", "صحة", "تطوير", "مالية", "عام")
    private val categoriesEn = listOf("Work", "Personal", "Health", "Growth", "Finance", "General")

    fun normalizeDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            sb.append(
                when (c) {
                    in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
                    in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    /** Keeps only the JSON part: drops ``` fences and any text before the first bracket or after the last. */
    private fun extractJson(text: String): String? {
        val firstObj = text.indexOf('{')
        val firstArr = text.indexOf('[')
        val start = listOf(firstObj, firstArr).filter { it >= 0 }.minOrNull() ?: return null
        val end = maxOf(text.lastIndexOf('}'), text.lastIndexOf(']'))
        if (end <= start) return null
        return text.substring(start, end + 1)
    }

    private fun readRoot(json: String): Any? {
        return try {
            if (json.trimStart().startsWith("[")) JSONArray(json) else JSONObject(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun dayKey(title: String, dueDate: Long): String =
        title.trim().lowercase() + "|" + DateTimeUtils.getStartOfDay(dueDate)

    fun parse(raw: String, isArabic: Boolean, existing: List<Task>): ImportParseResult {
        val json0 = extractJson(raw)
            ?: return ImportParseResult(fatal = if (isArabic) "لم أجد JSON في النص الملصوق." else "No JSON found in the pasted text.")
        var root = readRoot(json0)
        if (root == null) {
            val fixed = json0.replace('\u201C', '"').replace('\u201D', '"')
            root = readRoot(fixed)
        }
        if (root == null) {
            return ImportParseResult(fatal = if (isArabic) "تعذّرت قراءة الـ JSON. تأكد أنه منسوخ كاملاً." else "Could not read the JSON. Make sure it was copied completely.")
        }
        val array: JSONArray? = when (root) {
            is JSONArray -> root
            is JSONObject -> root.optJSONArray("tasks")
            else -> null
        }
        if (array == null) {
            return ImportParseResult(fatal = if (isArabic) "لم أجد قائمة \"tasks\" في الـ JSON." else "No \"tasks\" list found in the JSON.")
        }
        if (array.length() == 0) {
            return ImportParseResult(fatal = if (isArabic) "قائمة المهام فارغة." else "The task list is empty.")
        }

        val seen = HashSet<String>()
        existing.forEach { seen.add(dayKey(it.title, it.dueDate)) }
        val today = DateTimeUtils.getTodayStartOfDay()
        val rows = ArrayList<ImportRow>()
        val errors = ArrayList<String>()

        for (i in 0 until array.length()) {
            val n = i + 1
            val obj = array.optJSONObject(i)
            if (obj == null) {
                errors.add(if (isArabic) "المهمة $n: ليست كائنًا صحيحًا" else "Task $n: not a valid object")
                continue
            }
            val title = normalizeDigits(obj.optString("title", "")).trim()
            if (title.isEmpty()) {
                errors.add(if (isArabic) "المهمة $n: العنوان ناقص" else "Task $n: missing title")
                continue
            }
            val dueDate = parseDate(obj.optString("date", ""))
            if (dueDate == null) {
                errors.add(if (isArabic) "المهمة $n ($title): التاريخ ناقص أو غير صالح" else "Task $n ($title): missing or invalid date")
                continue
            }
            val timeRaw = obj.optString("time", "").trim()
            var dueTime: String? = null
            if (timeRaw.isNotEmpty()) {
                dueTime = formatTime(timeRaw, isArabic)
                if (dueTime == null) {
                    errors.add(if (isArabic) "المهمة $n ($title): الوقت غير صالح ($timeRaw)" else "Task $n ($title): invalid time ($timeRaw)")
                    continue
                }
            }
            val notes = obj.optString("notes", obj.optString("description", "")).trim()
            val task = Task(
                title = title,
                description = notes,
                dueDate = dueDate,
                dueTime = dueTime,
                priority = parsePriority(obj.optString("priority", "")),
                category = parseCategory(obj.optString("category", ""), isArabic)
            )
            val key = dayKey(title, dueDate)
            val duplicate = !seen.add(key)
            rows.add(ImportRow(task, duplicate, dueDate < today))
        }
        if (rows.isEmpty()) {
            return ImportParseResult(errors = errors, fatal = if (isArabic) "لا توجد مهام صالحة للإضافة." else "No valid tasks to add.")
        }
        return ImportParseResult(rows = rows, errors = errors)
    }

    /** "2026-12-01" (also with / or .) to the start of that day in the device time zone. */
    fun parseDate(raw: String): Long? {
        val m = Regex("(\\d{4})\\D(\\d{1,2})\\D(\\d{1,2})").find(normalizeDigits(raw).trim()) ?: return null
        val y = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        val d = m.groupValues[3].toInt()
        if (mo !in 1..12 || d !in 1..31) return null
        val cal = Calendar.getInstance()
        cal.isLenient = false
        cal.clear()
        cal.set(y, mo - 1, d, 0, 0, 0)
        return try {
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    /** "09:30", "9:30 PM", "09:30 ص" to the same text the task form writes, e.g. "09:30 ص" / "09:30 AM". */
    fun formatTime(raw: String, isArabic: Boolean): String? {
        val clean = normalizeDigits(raw).trim()
        if (!Regex("\\d{1,2}:\\d{2}").containsMatchIn(clean)) return null
        val hm = DateTimeUtils.parseHourMinute(clean) ?: return null
        val h = hm.first
        val min = hm.second
        val h12 = if (h % 12 == 0) 12 else h % 12
        val suffix = if (h >= 12) (if (isArabic) "م" else "PM") else (if (isArabic) "ص" else "AM")
        return String.format(java.util.Locale.ENGLISH, "%02d:%02d %s", h12, min, suffix)
    }

    private fun parsePriority(raw: String): TaskPriority {
        val v = raw.trim().lowercase()
        return when {
            v.startsWith("high") || v.startsWith("urgent") || v.startsWith("عال") || v.startsWith("مرتفع") || v.startsWith("عاجل") -> TaskPriority.HIGH
            v.startsWith("low") || v.startsWith("منخفض") || v.startsWith("قليل") -> TaskPriority.LOW
            else -> TaskPriority.MEDIUM
        }
    }

    private fun parseCategory(raw: String, isArabic: Boolean): String {
        val v = raw.trim()
        if (v.isEmpty()) return if (isArabic) "عام" else "General"
        val ar = categoriesAr.indexOf(v)
        val en = categoriesEn.indexOfFirst { it.equals(v, ignoreCase = true) }
        val idx = if (ar >= 0) ar else en
        if (idx < 0) return v
        return if (isArabic) categoriesAr[idx] else categoriesEn[idx]
    }

    /** The prompt the user copies and gives to any AI together with his plan. */
    fun buildPrompt(isArabic: Boolean): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
        val today = fmt.format(java.util.Date(DateTimeUtils.getTodayStartOfDay()))
        val tomorrow = fmt.format(java.util.Date(DateTimeUtils.getTomorrowStartOfDay()))
        return if (isArabic) {
            """أنت مساعد تخطيط. سأعطيك خطتي، وأريد منك تحويلها إلى مهام يومية بصيغة JSON فقط، بدون أي شرح وبدون علامات ```.
الصيغة المطلوبة بالضبط:
{"version":1,"tasks":[{"title":"...","date":"YYYY-MM-DD","time":"HH:mm","priority":"low|medium|high","category":"...","notes":"..."}]}
القواعد:
- title و date إجباريان. الباقي اختياري (احذف الحقل إن لم يلزم).
- date تاريخ يوم محدد مطلق (مثل 2026-12-01)، ممنوع كلمات مثل "غدا" أو "اليوم 5".
- time بنظام 24 ساعة HH:mm فقط.
- priority واحدة من: low, medium, high.
- category واحدة من: عمل، شخصي، صحة، تطوير، مالية، عام.
- عنوان قصير لكل مهمة (أقل من 60 حرفا)، والتفاصيل في notes.
تاريخ اليوم عندي: $today
ابدأ من تاريخ: $tomorrow (غيّره إن أردت)، وعدد الأيام: [اكتب العدد]
خطتي: [الصق خطتك هنا]"""
        } else {
            """You are a planning assistant. I will give you my plan. Turn it into daily tasks as JSON only, with no explanation and no ``` fences.
Exact format:
{"version":1,"tasks":[{"title":"...","date":"YYYY-MM-DD","time":"HH:mm","priority":"low|medium|high","category":"...","notes":"..."}]}
Rules:
- title and date are required. The rest is optional (leave the field out if not needed).
- date is an absolute calendar day (like 2026-12-01). Never words like "tomorrow" or "day 5".
- time is 24-hour HH:mm only.
- priority is one of: low, medium, high.
- category is one of: Work, Personal, Health, Growth, Finance, General.
- Keep each title short (under 60 characters) and put details in notes.
Today's date for me: $today
Start from: $tomorrow (change it if you like), number of days: [write the number]
My plan: [paste your plan here]"""
        }
    }
}
