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
