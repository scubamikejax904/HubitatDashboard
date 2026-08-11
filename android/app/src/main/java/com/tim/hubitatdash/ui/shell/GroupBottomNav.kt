package com.tim.hubitatdash.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(val groupId: String, val label: String, val icon: ImageVector)

val bottomNavItems = listOf<BottomNavItem>()

@Composable
fun GroupBottomNav(
    currentGroupId: String,
    onGroupSelected: (String) -> Unit,
    onMoreClick: () -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentGroupId == item.groupId,
                onClick = { onGroupSelected(item.groupId) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
        NavigationBarItem(
            selected = false,
            onClick = onMoreClick,
            icon = { Icon(Icons.Default.Menu, contentDescription = "More") },
            label = { Text("More") }
        )
    }
}

