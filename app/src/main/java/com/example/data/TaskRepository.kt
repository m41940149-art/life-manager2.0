package com.example.data

import com.example.model.Task
import com.example.model.TaskPriority
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface TaskRepository {
    fun getTasks(): Flow<List<Task>>
    suspend fun addTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: String)
    suspend fun toggleTaskCompletion(taskId: String)
}

class InMemoryTaskRepository : TaskRepository {
    private val today = DateTimeUtils.getTodayStartOfDay()
    private val tomorrow = DateTimeUtils.getTomorrowStartOfDay()
    private val upcoming = DateTimeUtils.getRelativeDayStart(3)

    private val _tasks = MutableStateFlow<List<Task>>(
        listOf(
            Task(
                id = "task-1",
                title = "مراجعة تقرير خطة العمل الشهرية",
                description = "تحليل مؤشرات الإنجاز وتحديد أولويات الربع القادم",
                dueDate = today,
                dueTime = "09:30 ص",
                isCompleted = false,
                priority = TaskPriority.HIGH,
                category = "عمل"
            ),
            Task(
                id = "task-2",
                title = "جلسة قراءة وتلخيص ٣٠ دقيقة",
                description = "كتاب العمل العميق - تلخيص الأفكار الرئيسية",
                dueDate = today,
                dueTime = "05:00 م",
                isCompleted = true,
                completedAt = System.currentTimeMillis() - 3600000,
                priority = TaskPriority.MEDIUM,
                category = "تطوير"
            ),
            Task(
                id = "task-3",
                title = "جلسة تدريب رياضي وتمارين سويدية",
                description = "تمارين الكارديو والإطالة لمدة ٤٥ دقيقة",
                dueDate = today,
                dueTime = "07:30 م",
                isCompleted = false,
                priority = TaskPriority.MEDIUM,
                category = "صحة"
            ),
            Task(
                id = "task-4",
                title = "تنظيم مجلدات الملاحظات والبيانات الشخصية",
                description = "فرز بطاقات البيانات الحساسة وتحديث كلمات المرور",
                dueDate = tomorrow,
                dueTime = "11:00 ص",
                isCompleted = false,
                priority = TaskPriority.LOW,
                category = "تنظيم"
            ),
            Task(
                id = "task-5",
                title = "تجهيز استبيان قياس الإنتاجية والرضا",
                description = "إعداد أسئلة الاستبيان ومشاركتها للمراجعة الدورية",
                dueDate = upcoming,
                dueTime = "02:00 م",
                isCompleted = false,
                priority = TaskPriority.HIGH,
                category = "أهداف"
            )
        )
    )

    override fun getTasks(): Flow<List<Task>> = _tasks.asStateFlow()

    override suspend fun addTask(task: Task) {
        _tasks.update { listOf(task) + it }
    }

    override suspend fun updateTask(task: Task) {
        _tasks.update { list ->
            list.map { if (it.id == task.id) task else it }
        }
    }

    override suspend fun deleteTask(taskId: String) {
        _tasks.update { list ->
            list.filterNot { it.id == taskId }
        }
    }

    override suspend fun toggleTaskCompletion(taskId: String) {
        _tasks.update { list ->
            list.map { task ->
                if (task.id == taskId) {
                    val willBeCompleted = !task.isCompleted
                    task.copy(
                        isCompleted = willBeCompleted,
                        completedAt = if (willBeCompleted) System.currentTimeMillis() else null
                    )
                } else task
            }
        }
    }
}
