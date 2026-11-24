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


/** How the done items of a [Checklist] are rated. Chosen when the list is created. */
enum class ChecklistRatingMode {
    /** One-time completion list: ticking an item is all there is, no rating at all. */
    NONE,

    /** Every done item can get a score from 1 to 10. */
    SCORE,

    /** Done items get one of the list's own named boxes, e.g. مقبول / جيد / ممتاز. */
    CUSTOM
}

/**
 * One line in a [Checklist]. Once [isDone] is true the user can attach a [note]
 * and, depending on the list's [ChecklistRatingMode], a [rating]:
 * 1..10 for a score list, or the 1-based position of the chosen custom box
 * (0 = not rated). One-time lists never use it.
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
    val ratingMode: ChecklistRatingMode = ChecklistRatingMode.SCORE,
    /** Box names for [ChecklistRatingMode.CUSTOM], 1..10 entries. Item rating N picks labels[N-1]. */
    val ratingLabels: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val doneCount: Int get() = items.count { it.isDone }
}
