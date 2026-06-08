package com.tim.hubitatdash.ui.tracker

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tim.hubitatdash.viewmodel.LocationTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationTrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: LocationTrackerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh permissions on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissions(context)
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshPermissions(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Tracker") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // === Master toggle ===
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("GPS Tracking", fontWeight = FontWeight.Bold)
                        Text(
                            "Log location to Google Sheet every ${uiState.interval} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.enabled,
                        onCheckedChange = { viewModel.setEnabled(it) },
                        enabled = uiState.hasFineLocation && uiState.appsScriptUrl.isNotBlank()
                    )
                }
            }

            // === Permission status ===
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permissions", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    PermissionRow(
                        label = "Fine location",
                        granted = uiState.hasFineLocation,
                        onClick = {
                            fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Spacer(Modifier.height(4.dp))
                        PermissionRow(
                            label = "Background location",
                            granted = uiState.hasBackgroundLocation,
                            onClick = {
                                if (uiState.hasFineLocation) {
                                    backgroundLocationLauncher.launch(
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    )
                                }
                            }
                        )
                        if (!uiState.hasBackgroundLocation) {
                            Text(
                                "Required for tracking with screen off. " +
                                "You may be taken to system settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // === Configuration ===
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configuration", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.appsScriptUrl,
                        onValueChange = { viewModel.setAppsScriptUrl(it) },
                        label = { Text("Apps Script URL") },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.deviceName,
                        onValueChange = { viewModel.setDeviceName(it) },
                        label = { Text("Device name") },
                        placeholder = { Text(Build.MODEL) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    // Interval selector
                    var expanded by remember { mutableStateOf(false) }
                    val intervals = listOf(5, 10, 15, 30, 60)

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = "${uiState.interval} minutes",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Interval") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            intervals.forEach { mins ->
                                DropdownMenuItem(
                                    text = { Text("$mins minutes") },
                                    onClick = {
                                        viewModel.setInterval(mins)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = "%.1f".format(uiState.minDistanceMiles),
                        onValueChange = { value ->
                            value.toFloatOrNull()?.let { viewModel.setMinDistanceMiles(it) }
                        },
                        label = { Text("Min distance to trigger update") },
                        placeholder = { Text("1.0") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        suffix = { Text("miles") }
                    )
                }
            }

            // === Test ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Test started…", Toast.LENGTH_SHORT).show()
                        viewModel.testNow(context)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isTesting
                        && uiState.appsScriptUrl.isNotBlank()
                        && uiState.hasFineLocation
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text(if (uiState.isTesting) "Sending…" else "Test Now")
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                            "https://script.google.com/"
                        ))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apps Script Console")
                }
            }

            // === Info ===
            if (uiState.enabled && !uiState.hasBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = null)
                        Text(
                            "  Background location not granted. Tracking will stop when the " +
                            "phone is locked or the app is closed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (granted) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Granted",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.padding(end = 8.dp)
            )
        } else {
            Icon(
                Icons.Filled.Warning,
                contentDescription = "Not granted",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(
            "$label: ${if (granted) "Granted" else "Not granted"}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        if (!granted) {
            OutlinedButton(onClick = onClick) {
                Text("Grant")
            }
        }
    }
}

