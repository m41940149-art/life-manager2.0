package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.TextCard

@Entity(
    tableName = "text_cards",
    indices = [
        Index("folderId"),
        Index("isFavorite")
    ]
)
data class TextCardEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val folderId: String? = null,
    val isFavorite: Boolean = false,
    val isPasswordProtected: Boolean = false,
    val passwordHash: String? = null,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun TextCardEntity.toDomain(): TextCard = TextCard(
    id = id,
    folderId = folderId,
    title = title,
    content = content,
    isFavorite = isFavorite,
    isPasswordProtected = isPasswordProtected,
    passwordHash = passwordHash,
    tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TextCard.toEntity(updatedAt: Long = this.updatedAt): TextCardEntity = TextCardEntity(
    id = id,
    folderId = folderId,
    title = title,
    content = content,
    isFavorite = isFavorite,
    isPasswordProtected = isPasswordProtected,
    passwordHash = passwordHash,
    tags = tags.joinToString(","),
    createdAt = createdAt,
    updatedAt = updatedAt
)
