package com.tim.hubitatdash.ui.hubitat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tim.hubitatdash.data.repository.HubitatEvent
import com.tim.hubitatdash.ui.util.shareText
import com.tim.hubitatdash.viewmodel.HubitatNotificationViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    .withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubitatNotificationScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: HubitatNotificationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()
    val serviceConnected by viewModel.serviceConnected.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hubitat Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to dashboard")
                    }
                },
                actions = {
                    if (events.isNotEmpty()) {
                        IconButton(onClick = {
                            shareText(context, "Hubitat Notifications Log", formatHubitatLog(events))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Export log")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                HubitatPermissionStatusCard(
                    permissionGranted = permissionGranted,
                    onOpenSettings = {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                    }
                )
            }
            item {
                HubitatServiceHealthCard(connected = serviceConnected)
            }

            item { HorizontalDivider() }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (events.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearEvents) {
                            Text("Clear")
                        }
                    }
                }
            }

            if (events.isEmpty()) {
                item {
                    Text(
                        text = "No notifications yet. All Hubitat app notifications will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(events) { event ->
                    HubitatEventCard(event = event)
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun HubitatPermissionStatusCard(permissionGranted: Boolean, onOpenSettings: () -> Unit) {
    val containerColor = if (permissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
    val contentColor = if (permissionGranted) Color(0xFF2E7D32) else Color(0xFFF57F17)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (permissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (permissionGranted) "Notification Access: Granted" else "Notification Access: Not Granted",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
                if (!permissionGranted) {
                    Text(
                        text = "Required to intercept Hubitat notifications in the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }
            }
            if (!permissionGranted) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
                ) {
                    Text("Open Settings")
                }
            }
        }
    }
}

@Composable
private fun HubitatServiceHealthCard(connected: Boolean) {
    val containerColor = if (connected) Color(0xFFE8F5E9) else Color(0xFFF3F3F3)
    val contentColor = if (connected) Color(0xFF2E7D32) else Color(0xFF757575)
    val dotColor = if (connected) Color(0xFF4CAF50) else Color(0xFF9E9E9E)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = null,
                tint = dotColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (connected) "Service: Connected" else "Service: Not Connected",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun HubitatEventCard(event: HubitatEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormatter.format(event.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
                if (event.title.isNotBlank()) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (event.notificationText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.notificationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = event.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val exportTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    .withZone(ZoneId.systemDefault())

private fun formatHubitatLog(events: List<HubitatEvent>): String = buildString {
    appendLine("=== Hubitat Notifications Log ===")
    appendLine("Exported: ${exportTimeFormatter.format(java.time.Instant.now())}")
    appendLine("${events.size} event(s)")
    appendLine()
    events.forEach { e ->
        append("[${timeFormatter.format(e.timestamp)}] [${e.packageName}] ")
        if (e.title.isNotBlank()) append("${e.title} | ")
        appendLine(e.notificationText.ifBlank { "(empty)" })
    }
}

