package com.tim.hubitatdash.ui.ring

import android.content.Intent
import android.provider.Settings
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
import com.tim.hubitatdash.data.repository.RingEvent
import com.tim.hubitatdash.ui.util.shareText
import com.tim.hubitatdash.viewmodel.RingListenerViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    .withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingListenerScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RingListenerViewModel = hiltViewModel()
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
                title = { Text("Ring Listener") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to dashboard")
                    }
                },
                actions = {
                    if (events.isNotEmpty()) {
                        IconButton(onClick = {
                            shareText(context, "Ring Listener Log", formatRingLog(events))
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
                PermissionStatusCard(
                    permissionGranted = permissionGranted,
                    onOpenSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
            item {
                ServiceHealthCard(connected = serviceConnected)
            }

            item { HorizontalDivider() }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Events",
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
                        text = "No events yet. All Ring notifications will appear here — forwarded ones show the hub response, others show why they were skipped.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(events) { event ->
                    RingEventCard(event = event)
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PermissionStatusCard(permissionGranted: Boolean, onOpenSettings: () -> Unit) {
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
                        text = "Required to intercept Ring notifications in the background.",
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
private fun ServiceHealthCard(connected: Boolean) {
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
private fun RingEventCard(event: RingEvent) {
    val successColor = Color(0xFF2E7D32)
    val errorColor = MaterialTheme.colorScheme.error

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
                Text(
                    text = if (event.success) "✓ HTTP ${event.httpCode}"
                           else if (event.url.isBlank()) "— skipped"
                           else "✗ ${event.error ?: "Failed"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        event.success    -> successColor
                        event.url.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
                        else             -> errorColor
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.notificationText.ifBlank { "(empty)" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val exportTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    .withZone(ZoneId.systemDefault())

private fun formatRingLog(events: List<RingEvent>): String = buildString {
    appendLine("=== Ring Listener Log ===")
    appendLine("Exported: ${exportTimeFormatter.format(java.time.Instant.now())}")
    appendLine("${events.size} event(s)")
    appendLine()
    events.forEach { e ->
        append("[${timeFormatter.format(e.timestamp)}] ")
        appendLine(e.notificationText.ifBlank { "(empty)" })
        when {
            e.success -> appendLine("  → HTTP ${e.httpCode} (forwarded)")
            e.url.isBlank() -> appendLine("  → ${e.error ?: "skipped"}")
            else -> appendLine("  → ERROR: ${e.error ?: "failed"} (HTTP ${e.httpCode ?: "—"})")
        }
    }
}

