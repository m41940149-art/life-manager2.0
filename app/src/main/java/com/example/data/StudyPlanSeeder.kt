package com.example.data

import android.content.Context
import com.example.model.Task
import com.example.model.TaskPriority
import java.util.Calendar

/**
 * One-time seed of the user's 28-day study plan (23 Nov - 20 Dec 2026)
 * into the Tasks tab.
 *
 * Days that have both new study material and review become two separate
 * tasks on that day; days with only one kind stay a single task
 * (48 tasks in total: day 1 has no review, days 22-28 have no new material).
 *
 * The plan is inserted once per install, guarded by a SharedPreferences flag,
 * so opening the app repeatedly never duplicates the tasks and tasks the user
 * deletes stay deleted. Seed tasks use fixed IDs, so even a retried seed
 * overwrites the same rows instead of creating copies.
 * Existing user tasks are never touched.
 */
object StudyPlanSeeder {

    private const val PREFS_NAME = "study_plan_seed_prefs"
    private const val KEY_SEEDED = "study_plan_2026_seeded_v1"

    private data class SeedEntry(
        val idSuffix: String,
        val month: Int, // 1-based
        val dayOfMonth: Int,
        val title: String,
        val description: String
    )

    private fun newTask(day: Int, month: Int, dayOfMonth: Int, material: String, pages: Int) =
        SeedEntry("y$day-new", month, dayOfMonth, "ي$day: $material", "جديد $pages" + "ص")

    private fun reviewTask(day: Int, month: Int, dayOfMonth: Int, review: String) =
        SeedEntry("y$day-review", month, dayOfMonth, "ي$day: مراجعة", "مراجعة: $review")

    private fun singleTask(day: Int, month: Int, dayOfMonth: Int, title: String, description: String) =
        SeedEntry("y$day", month, dayOfMonth, "ي$day: $title", description)

    private val SEED_ENTRIES = listOf(
        singleTask(1, 11, 23, "قانون ف1–4", "جديد 69ص\nمراجعة: لا شيء"),

        newTask(2, 11, 24, "قانون ف5–7", 54),
        reviewTask(2, 11, 24, "ي1 (69ص)"),

        newTask(3, 11, 25, "قانون ف8–10", 61),
        reviewTask(3, 11, 25, "ي2 (54ص)"),

        newTask(4, 11, 26, "قانون ف11–12 + بحري ف1–2", 51),
        reviewTask(4, 11, 26, "ي3 (61ص) + ي1 (69ص) = 130ص"),

        newTask(5, 11, 27, "بحري ف3–4", 47),
        reviewTask(5, 11, 27, "ي4 (51ص) + ي2 (54ص) = 105ص"),

        newTask(6, 11, 28, "نقود وبنوك ج1 ف1–3", 60),
        reviewTask(6, 11, 28, "ي5 (47ص) + ي3 (61ص) = 108ص"),

        newTask(7, 11, 29, "نقود وبنوك ج1 ف4–6", 68),
        reviewTask(7, 11, 29, "ي6 (60ص) + ي4 (51ص) = 111ص"),

        newTask(8, 11, 30, "نقود وبنوك ج1 ف7–8", 53),
        reviewTask(8, 11, 30, "ي7 (68ص) + ي5 (47ص) + ي1 (69ص) = 184ص"),

        newTask(9, 12, 1, "نقود وبنوك ج2 ف1–3", 58),
        reviewTask(9, 12, 1, "ي8 (53ص) + ي6 (60ص) + ي2 (54ص) = 167ص"),

        newTask(10, 12, 2, "نقود وبنوك ج2 ف4–6", 73),
        reviewTask(10, 12, 2, "ي9 (58ص) + ي7 (68ص) + ي3 (61ص) = 187ص"),

        newTask(11, 12, 3, "نقود وبنوك ج2 ف7–8", 36),
        reviewTask(11, 12, 3, "ي10 (73ص) + ي8 (53ص) + ي4 (51ص) = 177ص"),

        newTask(12, 12, 4, "تسويق ف1–3", 43),
        reviewTask(12, 12, 4, "ي11 (36ص) + ي9 (58ص) + ي5 (47ص) = 141ص"),

        newTask(13, 12, 5, "تسويق ف4–5", 60),
        reviewTask(13, 12, 5, "ي12 (43ص) + ي10 (73ص) + ي6 (60ص) = 176ص"),

        newTask(14, 12, 6, "تسويق ف6–7", 68),
        reviewTask(14, 12, 6, "ي13 (60ص) + ي11 (36ص) + ي7 (68ص) = 164ص"),

        newTask(15, 12, 7, "تسويق ف8–9", 58),
        reviewTask(15, 12, 7, "ي14 (68ص) + ي12 (43ص) + ي8 (53ص) = 164ص"),

        newTask(16, 12, 8, "تسويق ف10", 51),
        reviewTask(16, 12, 8, "ي15 (58ص) + ي13 (60ص) + ي9 (58ص) = 176ص"),

        newTask(17, 12, 9, "تسويق ف11–12", 57),
        reviewTask(17, 12, 9, "ي16 (51ص) + ي14 (68ص) + ي10 (73ص) = 192ص"),

        newTask(18, 12, 10, "إدارة عامة ف1–3", 62),
        reviewTask(18, 12, 10, "ي17 (57ص) + ي15 (58ص) + ي11 (36ص) = 151ص"),

        newTask(19, 12, 11, "إدارة عامة ف4–6", 54),
        reviewTask(19, 12, 11, "ي18 (62ص) + ي16 (51ص) + ي12 (43ص) = 156ص"),

        newTask(20, 12, 12, "إدارة عامة ف7–8", 51),
        reviewTask(20, 12, 12, "ي19 (54ص) + ي17 (57ص) + ي13 (60ص) = 171ص"),

        newTask(21, 12, 13, "إدارة عامة ف9", 47),
        reviewTask(21, 12, 13, "ي20 (51ص) + ي18 (62ص) + ي14 (68ص) = 181ص"),

        singleTask(22, 12, 14, "مراجعة شاملة: القانون كامل (282ص)", "لا جديد (تعويض أي تأخير)"),
        singleTask(23, 12, 15, "مراجعة شاملة: النقود والبنوك كامل (348ص) + ملزمتها إن صدرت", "لا جديد"),
        singleTask(24, 12, 16, "مراجعة شاملة: التسويق كامل (337ص)", "لا جديد"),
        singleTask(25, 12, 17, "مراجعة شاملة: الإدارة العامة (214ص)", "لا جديد"),
        singleTask(26, 12, 18, "الملازم والأسئلة: القانون + النقود والبنوك", "ملازم وأسئلة"),
        singleTask(27, 12, 19, "الملازم والأسئلة: التسويق + الإدارة", "ملازم وأسئلة"),
        singleTask(28, 12, 20, "اختبار ذاتي شامل", "اكتب عناوين كل مادة من ذاكرتك ثم صحّح")
    )

    /**
     * Builds the 48 study-plan tasks with fixed IDs, each dated at local
     * start-of-day on its scheduled date. Pure and unit-testable.
     */
    fun buildStudyPlanTasks(): List<Task> = SEED_ENTRIES.map { entry ->
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(2026, entry.month - 1, entry.dayOfMonth, 0, 0, 0)
        Task(
            id = "study-plan-2026-${entry.idSuffix}",
            title = entry.title,
            description = entry.description,
            dueDate = cal.timeInMillis,
            dueTime = null,
            priority = TaskPriority.MEDIUM,
            category = "مذاكرة"
        )
    }

    /**
     * Inserts the study plan once per install. Runs before any user interaction
     * with the seeded rows; afterwards the flag keeps user edits and deletions
     * final. The tasks sync to Firestore like any locally created task.
     */
    suspend fun seedIfNeeded(context: Context, repository: TaskRepository) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return
        buildStudyPlanTasks().forEach { repository.addTask(it) }
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }
}
