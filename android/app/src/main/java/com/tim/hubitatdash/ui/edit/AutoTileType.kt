package com.tim.hubitatdash.ui.edit

import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.TileType

fun autoTileType(device: DeviceState): TileType {
    val attrs = device.attributes
    val cmds = device.commands ?: emptyList()
    if ("hue" in attrs || "setColor" in cmds) return TileType.RGBW
    if ("level" in attrs && "setLevel" in cmds && "switch" in attrs) return TileType.DIMMER
    if ("lock" in attrs) return TileType.LOCK
    if ("contact" in attrs) return TileType.CONTACT
    if ("motion" in attrs) return TileType.MOTION
    if ("presence" in attrs) return TileType.PRESENCE
    if ("power" in attrs && "switch" !in attrs) return TileType.POWER_METER
    if ("temperature" in attrs && "switch" !in attrs) return TileType.TEMPERATURE
    if ("push" in cmds) return TileType.BUTTON
    if ("battery" in attrs) return TileType.BATTERY
    return TileType.SWITCH
}

fun availableTileTypes(device: DeviceState): List<TileType> {
    val attrs = device.attributes
    val cmds = device.commands ?: emptyList()
    val types = mutableListOf<TileType>()
    if ("hue" in attrs || "setColor" in cmds) types.add(TileType.RGBW)
    if ("level" in attrs && "setLevel" in cmds) types.add(TileType.DIMMER)
    if ("switch" in attrs) types.add(TileType.SWITCH)
    if ("lock" in attrs) types.add(TileType.LOCK)
    if ("contact" in attrs) types.add(TileType.CONTACT)
    if ("motion" in attrs) types.add(TileType.MOTION)
    if ("presence" in attrs) types.add(TileType.PRESENCE)
    if ("power" in attrs) types.add(TileType.POWER_METER)
    if ("temperature" in attrs) types.add(TileType.TEMPERATURE)
    if ("push" in cmds) types.add(TileType.BUTTON)
    if ("battery" in attrs) types.add(TileType.BATTERY)
    if (types.isEmpty()) types.add(TileType.SWITCH)
    return types.distinct()
}

