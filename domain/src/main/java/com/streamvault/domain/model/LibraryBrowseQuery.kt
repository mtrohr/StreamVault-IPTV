package com.streamvault.domain.model

data class LibraryBrowseQuery(
    val providerId: Long,
    val categoryId: Long? = null,
    val offset: Int = 0,
    val limit: Int = 40
)

data class PagedResult<T>(
    val items: List<T>,
    val totalCount: Int,
    val offset: Int,
    val limit: Int
)
