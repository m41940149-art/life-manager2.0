package com.example.model

import java.util.UUID

data class Folder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val colorHex: Long = 0xFF006C5F,
    val iconName: String = "folder",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TextCard(
    val id: String = UUID.randomUUID().toString(),
    val folderId: String? = null,
    val title: String,
    val content: String,
    val isFavorite: Boolean = false,
    val isPasswordProtected: Boolean = false,
    val passwordHash: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


/**
 * One line in a [Checklist]. Once [isDone] is true the user can attach a [note]
 * and a [rating] out of 10 (1..10, 0 = not rated) to it - e.g. a movie you watched.
 */
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false,
    val note: String = "",
    val rating: Int = 0, // 0 = not rated, otherwise 1..10
    val completedAt: Long? = null
)

/** Third kind of entry in the Data tab (next to folders and text cards). */
data class Checklist(
    val id: String = UUID.randomUUID().toString(),
    val folderId: String? = null,
    val title: String,
    val items: List<ChecklistItem> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val doneCount: Int get() = items.count { it.isDone }
}
