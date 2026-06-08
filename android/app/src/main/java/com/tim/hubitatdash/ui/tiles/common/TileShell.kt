package com.tim.hubitatdash.ui.tiles.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tim.hubitatdash.ui.theme.TileTokens

/**
 * Common tile interior: title row top-left (small muted caps), body below.
 * Matches the web card layout: title (12sp, gray-400) then content beneath.
 */
@Composable
fun TileShell(
    title: String,
    modifier: Modifier = Modifier,
    titleTrailing: @Composable (() -> Unit)? = null,
    body: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(TileTokens.TilePadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = TileTokens.TitleMuted,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            if (titleTrailing != null) {
                titleTrailing()
            }
        }
        body()
    }
}

