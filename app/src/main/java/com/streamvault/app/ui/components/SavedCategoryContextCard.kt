package com.streamvault.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.streamvault.app.R
import com.streamvault.app.ui.theme.OnSurface
import com.streamvault.app.ui.theme.OnSurfaceDim
import com.streamvault.app.ui.theme.Primary
import com.streamvault.app.ui.theme.SurfaceElevated

@Composable
fun SavedCategoryContextCard(
    categoryName: String,
    itemCount: Int,
    onManageClick: () -> Unit,
    onBrowseAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = SurfaceDefaults.colors(
            containerColor = SurfaceElevated
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.library_saved_active_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary
                )
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.library_saved_active_summary, itemCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceDim,
                    maxLines = 1
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onManageClick,
                    colors = ButtonDefaults.colors(
                        containerColor = Primary.copy(alpha = 0.92f)
                    )
                ) {
                    Text(stringResource(R.string.library_saved_manage_action))
                }
                Button(
                    onClick = onBrowseAllClick,
                    colors = ButtonDefaults.colors(
                        containerColor = SurfaceElevated,
                        contentColor = OnSurface
                    )
                ) {
                    Text(stringResource(R.string.library_browse_all))
                }
            }
        }
    }
}
