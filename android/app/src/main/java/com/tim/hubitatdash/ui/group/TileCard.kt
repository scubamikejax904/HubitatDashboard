package com.tim.hubitatdash.ui.group

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.HsmMode
import com.tim.hubitatdash.data.model.HubMode
import com.tim.hubitatdash.data.model.HubVariable
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.data.model.TileType
import com.tim.hubitatdash.ui.tiles.BatteryTile
import com.tim.hubitatdash.ui.tiles.ButtonTile
import com.tim.hubitatdash.ui.tiles.ConnectorTile
import com.tim.hubitatdash.ui.tiles.ContactTile
import com.tim.hubitatdash.ui.tiles.DimmerTile
import com.tim.hubitatdash.ui.tiles.HsmTile
import com.tim.hubitatdash.ui.tiles.HubVariableTile
import com.tim.hubitatdash.ui.tiles.LockTile
import com.tim.hubitatdash.ui.tiles.ModeTile
import com.tim.hubitatdash.ui.tiles.MotionTile
import com.tim.hubitatdash.ui.tiles.PowerMeterTile
import com.tim.hubitatdash.ui.tiles.PresenceTile
import com.tim.hubitatdash.ui.tiles.RGBWTile
import com.tim.hubitatdash.ui.tiles.RingDetectionTile
import com.tim.hubitatdash.ui.tiles.SwitchTile
import com.tim.hubitatdash.ui.tiles.TemperatureTile

// Maps tile type + device state → accent Color and whether to highlight the card border.
// Colors match the web dashboard palette (see TileTokens).
private fun tileStateInfo(
    tile: TileConfig,
    device: DeviceState?,
    hsmStatus: HsmMode
): Pair<Color, Boolean> {
    val attrs = device?.attributes
    return when (tile.tileType) {
        TileType.SWITCH, TileType.CONNECTOR -> {
            val isOn = attrs?.get("switch") == "on"
            if (isOn) Pair(TileTokens.GreenBorder, true) else Pair(TileTokens.OffBorder, true)
        }
        TileType.DIMMER -> {
            val isOn = attrs?.get("switch") == "on"
            if (isOn) Pair(TileTokens.AmberActive, true) else Pair(TileTokens.OffBorder, true)
        }
        TileType.RGBW -> {
            val isOn = attrs?.get("switch") == "on"
            if (isOn) Pair(TileTokens.PurpleRgb, true) else Pair(TileTokens.OffBorder, true)
        }
        TileType.MOTION -> {
            val isActive = attrs?.get("motion") == "active"
            Pair(TileTokens.AmberActive, isActive)
        }
        TileType.RING_DETECTION -> {
            val isActive = attrs?.get("motion") == "active"
            Pair(TileTokens.RedAlert, isActive)
        }
        TileType.CONTACT -> {
            val isOpen = attrs?.get("contact") == "open"
            if (device == null) Pair(Color.Transparent, false)
            else Pair(if (isOpen) TileTokens.OrangeHot else TileTokens.GreenBorder, isOpen)
        }
        TileType.LOCK -> {
            val isLocked = attrs?.get("lock") == "locked"
            if (device == null) Pair(Color.Transparent, false)
            else Pair(if (isLocked) TileTokens.GreenBorder else TileTokens.RedAlert, true)
        }
        TileType.PRESENCE -> {
            val isPresent = attrs?.get("presence") == "present"
            Pair(TileTokens.GreenBorder, isPresent)
        }
        TileType.HSM -> when (hsmStatus) {
            HsmMode.ARMED_AWAY  -> Pair(TileTokens.RedAlert, true)
            HsmMode.ARMED_HOME  -> Pair(TileTokens.OrangeHot, true)
            HsmMode.ARMED_NIGHT -> Pair(TileTokens.BlueCold, true)
            HsmMode.DISARMED, HsmMode.ALL_DISARMED -> Pair(TileTokens.GreenBorder, true)
            HsmMode.UNKNOWN     -> Pair(Color.Transparent, false)
        }
        TileType.MODE -> Pair(TileTokens.BlueBorder, true)
        TileType.POWER_METER -> {
            val power = attrs?.get("power")?.toFloatOrNull() ?: 0f
            val switchOn = attrs?.get("switch") == "on"
            Pair(TileTokens.AmberActive, power > 0f || switchOn)
        }
        TileType.BATTERY -> {
            val level = attrs?.get("battery")?.toFloatOrNull()?.toInt() ?: -1
            when {
                level < 0  -> Pair(Color.Transparent, false)
                level < 20 -> Pair(TileTokens.RedAlert, true)
                level < 50 -> Pair(TileTokens.OrangeHot, true)
                else       -> Pair(TileTokens.GreenBorder, true)
            }
        }
        TileType.TEMPERATURE -> {
            val t = attrs?.get("temperature")?.toFloatOrNull()
            when {
                t == null -> Pair(Color.Transparent, false)
                t < 65f   -> Pair(TileTokens.BlueBorder, true)
                t <= 80f  -> Pair(TileTokens.GreenBorder, true)
                else      -> Pair(TileTokens.OrangeBorder, true)
            }
        }
        else -> Pair(Color.Transparent, false)
    }
}

@Composable
fun TileCard(
    tile: TileConfig,
    device: DeviceState?,
    onCommand: (deviceId: String, command: String, value: String?) -> Unit,
    hubVariables: List<HubVariable> = emptyList(),
    hsmStatus: HsmMode = HsmMode.UNKNOWN,
    modes: List<HubMode> = emptyList(),
    onSetHsmMode: (mode: String) -> Unit = { _ -> },
    onSetMode: (modeId: String) -> Unit = { _ -> },
    onSetVariable: (name: String, value: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val (stateColor, isHighlighted) = tileStateInfo(tile, device, hsmStatus)

    Card(
        modifier = modifier.defaultMinSize(minHeight = TileTokens.TileMinHeight),
        shape = RoundedCornerShape(TileTokens.TileCornerRadius),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHighlighted) 2.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(),
        border = if (isHighlighted) {
            BorderStroke(2.dp, stateColor)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopStart
        ) {
            when (tile.tileType) {
                TileType.SWITCH -> SwitchTile(tile, device, onCommand)
                TileType.CONNECTOR -> ConnectorTile(tile, device, onCommand)
                TileType.DIMMER -> DimmerTile(tile, device, onCommand)
                TileType.RGBW -> RGBWTile(tile, device, onCommand)
                TileType.CONTACT -> ContactTile(tile, device)
                TileType.MOTION -> MotionTile(tile, device)
                TileType.TEMPERATURE -> TemperatureTile(tile, device)
                TileType.POWER_METER -> PowerMeterTile(tile, device, onCommand)
                TileType.BUTTON -> ButtonTile(tile, device, onCommand)
                TileType.LOCK -> LockTile(tile, device, onCommand)
                TileType.HUB_VARIABLE -> HubVariableTile(tile, hubVariables, onSetVariable)
                TileType.HSM -> HsmTile(tile, hsmStatus, onSetHsmMode)
                TileType.MODE -> ModeTile(tile, modes, onSetMode)
                TileType.RING_DETECTION -> RingDetectionTile(tile, device, hubVariables)
                TileType.PRESENCE -> PresenceTile(tile, device)
                TileType.BATTERY -> BatteryTile(tile, device)
                // Web-only types — not rendered on Android (tiles are filtered from groupAdditions before reaching here)
                TileType.SUN_TIMES, TileType.MULTI_DEVICE -> {}
            }
        }
    }
}

