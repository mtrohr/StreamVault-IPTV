package com.streamvault.domain.usecase

import com.streamvault.domain.model.Category
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCustomCategories @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return favoriteRepository.getGroups().map { groups ->
            val categories = groups.map { group ->
                Category(
                    id = -group.id, // Use negative ID for virtual groups to avoid collision with provider IDs
                    name = group.name,
                    type = ContentType.LIVE,
                    isVirtual = true
                )
            }.toMutableList()

            // prepend "Favorites" as a special virtual category
            categories.add(0, Category(
                id = -999L, // Special ID for "All Favorites"
                name = "★ Favorites",
                type = ContentType.LIVE,
                isVirtual = true
            ))

            categories
        }
    }
}
