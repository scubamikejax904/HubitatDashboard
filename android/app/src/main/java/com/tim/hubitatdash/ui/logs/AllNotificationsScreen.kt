package com.tim.hubitatdash.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tim.hubitatdash.data.repository.AllNotificationEvent
import com.tim.hubitatdash.ui.util.shareText
import com.tim.hubitatdash.viewmodel.AllNotificationsViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val RING_PACKAGE = "com.ringapp"
private val HIGHLIGHT_PACKAGES = setOf(RING_PACKAGE, "app.status", "app.error")

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    .withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllNotificationsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AllNotificationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val events by viewModel.events.collectAsStateWithLifecycle()
    var ringOnly by remember { mutableStateOf(false) }

    val displayed = if (ringOnly)
        events.filter { it.packageName == RING_PACKAGE || it.packageName.startsWith("app.") }
    else events

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to dashboard")
                    }
                },
                actions = {
                    if (events.isNotEmpty()) {
                        IconButton(onClick = {
                            shareText(context, "All Notifications Log", formatAllLog(events))
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Raw Log (max 200)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Every notification received by the listener, unfiltered.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (events.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearEvents) {
                            Text("Clear")
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !ringOnly,
                        onClick = { ringOnly = false },
                        label = { Text("All (${events.size})") }
                    )
                    FilterChip(
                        selected = ringOnly,
                        onClick = { ringOnly = true },
                        label = { Text("Ring + Status") }
                    )
                }
            }

            if (displayed.isEmpty()) {
                item {
                    Text(
                        text = if (ringOnly) "No Ring or status entries yet."
                               else "No notifications received yet. All notifications from any app will appear here once the listener service is active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(displayed) { event ->
                    AllNotificationCard(event = event)
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun AllNotificationCard(event: AllNotificationEvent) {
    val isHighlighted = event.packageName in HIGHLIGHT_PACKAGES
    val containerColor = when (event.packageName) {
        "app.error" -> MaterialTheme.colorScheme.errorContainer
        "app.status" -> MaterialTheme.colorScheme.primaryContainer
        RING_PACKAGE -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                    text = event.packageName.substringAfterLast('.'),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (event.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (event.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (event.title.isBlank() && event.text.isBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "(empty)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val exportTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    .withZone(ZoneId.systemDefault())

private fun formatAllLog(events: List<AllNotificationEvent>): String = buildString {
    appendLine("=== All Notifications Log ===")
    appendLine("Exported: ${exportTimeFormatter.format(java.time.Instant.now())}")
    appendLine("${events.size} event(s)")
    appendLine()
    events.forEach { e ->
        appendLine("[${timeFormatter.format(e.timestamp)}] ${e.packageName}")
        if (e.title.isNotBlank()) appendLine("  Title: ${e.title}")
        if (e.text.isNotBlank()) appendLine("  Text:  ${e.text}")
        if (e.title.isBlank() && e.text.isBlank()) appendLine("  (empty)")
    }
}

