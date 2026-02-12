package com.streamvault.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.streamvault.app.ui.theme.LocalSpacing
import com.streamvault.app.ui.theme.OnBackground

@Composable
fun <T> CategoryRow(
    title: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    val spacing = LocalSpacing.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = OnBackground,
            modifier = Modifier.padding(
                start = spacing.xl,
                top = spacing.lg,
                bottom = spacing.sm
            )
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            items(
                items = items,
                key = { it.hashCode() }
            ) { item ->
                itemContent(item)
            }
        }
    }
}
