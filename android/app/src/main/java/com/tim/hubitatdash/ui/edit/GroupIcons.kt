package com.tim.hubitatdash.ui.edit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Doorbell
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

fun iconForName(name: String): ImageVector = when (name) {
    "home" -> Icons.Default.Home
    "air" -> Icons.Default.Air
    "security" -> Icons.Default.Security
    "bedtime" -> Icons.Default.Bedtime
    "directions_walk" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "fence" -> Icons.Default.Fence
    "emergency" -> Icons.Default.Emergency
    "videocam" -> Icons.Default.Videocam
    "sensor_door" -> Icons.Default.DoorFront
    "schedule" -> Icons.Default.Schedule
    "bolt" -> Icons.Default.Bolt
    "settings_applications" -> Icons.Default.SettingsApplications
    "settings" -> Icons.Default.Settings
    "star" -> Icons.Default.Star
    "doorbell" -> Icons.Default.Doorbell
    "lightbulb" -> Icons.Default.Lightbulb
    "lock" -> Icons.Default.Lock
    "water_drop" -> Icons.Default.WaterDrop
    "electric_bolt" -> Icons.Default.ElectricBolt
    "thermostat" -> Icons.Default.Thermostat
    "people" -> Icons.Default.People
    "badge" -> Icons.Default.Badge
    "category" -> Icons.Default.Category
    "garage" -> Icons.Default.Garage
    "yard" -> Icons.Default.Yard
    else -> Icons.Default.Category
}

