package com.streamvault.domain.model

data class Favorite(
    val id: Long = 0,
    val contentId: Long,
    val contentType: ContentType,
    val position: Int = 0,
    val groupId: Long? = null,
    val addedAt: Long = System.currentTimeMillis()
)

data class VirtualGroup(
    val id: Long = 0,
    val name: String,
    val iconEmoji: String? = null,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
