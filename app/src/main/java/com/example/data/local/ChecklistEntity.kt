package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.Checklist
import com.example.model.ChecklistItem
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "checklists",
    indices = [
        Index("folderId"),
        Index("isFavorite")
    ]
)
data class ChecklistEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val folderId: String? = null,
    val isFavorite: Boolean = false,
    /** JSON array of [ChecklistItem]. */
    val itemsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

internal fun List<ChecklistItem>.toItemsJson(): String {
    val array = JSONArray()
    forEach { item ->
        array.put(
            JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("done", item.isDone)
                put("note", item.note)
                put("rating", item.rating)
                put("completedAt", item.completedAt ?: JSONObject.NULL)
            }
        )
    }
    return array.toString()
}

internal fun parseItemsJson(json: String): List<ChecklistItem> {
    if (json.isBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            ChecklistItem(
                id = o.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                text = o.optString("text"),
                isDone = o.optBoolean("done", false),
                note = o.optString("note"),
                rating = o.optInt("rating", 0).coerceIn(0, 10),
                completedAt = if (o.isNull("completedAt")) null else o.optLong("completedAt")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun ChecklistEntity.toDomain(): Checklist = Checklist(
    id = id,
    folderId = folderId,
    title = title,
    items = parseItemsJson(itemsJson),
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Checklist.toEntity(updatedAt: Long = this.updatedAt): ChecklistEntity = ChecklistEntity(
    id = id,
    title = title,
    folderId = folderId,
    isFavorite = isFavorite,
    itemsJson = items.toItemsJson(),
    createdAt = createdAt,
    updatedAt = updatedAt
)
