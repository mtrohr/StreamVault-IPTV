package com.streamvault.app.ui.components.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.streamvault.app.ui.design.AppColors
import com.streamvault.app.ui.design.FocusSpec

data class VodClassicCategoryOption(
    val key: String,
    val label: String,
    val count: Int,
    val isSelected: Boolean,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null
)

@Composable
fun VodClassicSplitLayout(
    railTitle: String,
    railSearchValue: String,
    onRailSearchValueChange: (String) -> Unit,
    railSearchPlaceholder: String,
    categories: List<VodClassicCategoryOption>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CategoryRailPanel(
            title = railTitle,
            searchValue = railSearchValue,
            onSearchValueChange = onRailSearchValueChange,
            searchPlaceholder = railSearchPlaceholder,
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(categories, key = { it.key }) { category ->
                Surface(
                    onClick = category.onClick,
                    onLongClick = category.onLongClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (category.isSelected) AppColors.Brand.copy(alpha = 0.22f) else AppColors.SurfaceElevated,
                        focusedContainerColor = AppColors.SurfaceEmphasis,
                        contentColor = if (category.isSelected) AppColors.BrandStrong else AppColors.TextPrimary,
                        focusedContentColor = AppColors.TextPrimary
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(
                            border = BorderStroke(FocusSpec.BorderWidth, AppColors.Focus),
                            shape = RoundedCornerShape(14.dp)
                        )
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = FocusSpec.FocusedScale)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (category.isSelected) AppColors.BrandStrong else AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = category.count.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(AppColors.Canvas, RoundedCornerShape(28.dp))
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            content()
        }
    }
}

@Composable
fun VodClassicContentHeader(
    title: String,
    subtitle: String,
    actions: List<VodActionChip>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        VodActionChipRow(
            actions = actions,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 900.dp)
        )
    }
}
