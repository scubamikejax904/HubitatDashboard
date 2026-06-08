package com.tim.hubitatdash.ui.tiles.common

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tim.hubitatdash.ui.theme.TileTokens

/**
 * Displays a large color-coded value next to an icon. Used by sensor tiles
 * like temperature, battery, and power. Matches the web
 * `<Thermometer /> 71.6 °F` layout.
 */
@Composable
fun TileValue(
    icon: ImageVector,
    value: String,
    unit: String? = null,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
        if (unit != null) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

/**
 * A small colored status chip used by read-only sensor tiles
 * (motion, contact, presence, ring). Mirrors the web's small labeled
 * badge style.
 */
@Composable
fun TileStatusChip(
    text: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val bg = color.copy(alpha = if (dark) 0.24f else 0.16f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(TileTokens.PillRadius))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

