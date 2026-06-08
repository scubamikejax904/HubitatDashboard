package com.tim.hubitatdash.ui.tiles.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tim.hubitatdash.ui.theme.TileTokens

/**
 * The web-style on/off (or "toggle") pill used by switch-like tiles.
 * Green background + white text when on, neutral gray when off.
 * Tapping the pill (or the whole tile) toggles the device; the pill is the
 * primary visual affordance.
 */
@Composable
fun TilePill(
    label: String,
    isOn: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pending: Boolean = false,
    onColor: Color = TileTokens.GreenOn
) {
    val dark = isSystemInDarkTheme()
    val bg by animateColorAsState(
        targetValue = when {
            isOn -> onColor
            dark -> TileTokens.PillOffBgDark
            else -> TileTokens.PillOffBgLight
        },
        label = "pillBg"
    )
    val fg = when {
        isOn -> Color.White
        dark -> TileTokens.PillOffTextDark
        else -> TileTokens.PillOffTextLight
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = TileTokens.PillHeight)
            .clip(RoundedCornerShape(TileTokens.PillRadius))
            .background(bg)
            .clickable(enabled = !pending, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pending) {
            CircularProgressIndicator(
                modifier = Modifier.size(TileTokens.IconSize),
                strokeWidth = 2.dp,
                color = fg
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(TileTokens.IconSize)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1
        )
    }
}

/** Skeleton placeholder shown while a tile's device state hasn't loaded yet. */
@Composable
fun TilePillSkeleton(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = TileTokens.PillHeight)
            .height(TileTokens.PillHeight)
            .clip(RoundedCornerShape(TileTokens.PillRadius))
            .background(if (dark) TileTokens.SkeletonDark else TileTokens.SkeletonLight)
    )
}

