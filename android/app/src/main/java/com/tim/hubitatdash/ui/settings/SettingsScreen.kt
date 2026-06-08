package com.tim.hubitatdash.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tim.hubitatdash.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSaveSuccess: () -> Unit,
    onRingListenerClick: () -> Unit = {},
    onGpsTrackerClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showToken by remember { mutableStateOf(false) }
    var showHubPassword by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // File picker for export — creates a new JSON file
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = viewModel.exportConfig()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }.onFailure {
                // snackbar will already show from viewModel if needed
            }
        }
    }

    // File picker for import — opens an existing JSON file
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: error("Could not read file")
                showImportConfirmDialog = json
            }.onFailure {
                // no-op
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onSaveSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Local Hub IP
            OutlinedTextField(
                value = uiState.localHubIp,
                onValueChange = { viewModel.onLocalHubIpChange(it) },
                label = { Text("Local Hub IP") },
                placeholder = { Text("192.168.1.42") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = uiState.localHubIpError != null,
                supportingText = uiState.localHubIpError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            // Maker App ID
            OutlinedTextField(
                value = uiState.makerAppId,
                onValueChange = { viewModel.onMakerAppIdChange(it) },
                label = { Text("Maker App ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Access Token
            OutlinedTextField(
                value = uiState.makerToken,
                onValueChange = { viewModel.onMakerTokenChange(it) },
                label = { Text("Access Token") },
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(
                            imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showToken) "Hide token" else "Show token"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Cloud Hub ID
            OutlinedTextField(
                value = uiState.cloudHubId,
                onValueChange = { viewModel.onCloudHubIdChange(it) },
                label = { Text("Cloud Hub ID") },
                placeholder = { Text("606cf154-46af-4877-adb4-680b40e940c0") },
                supportingText = { Text("Found in Hubitat → Apps → Maker API → Cloud Access") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            // Connection Mode
            Text("Connection Mode", style = MaterialTheme.typography.labelMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Local", "Cloud", "Auto").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = uiState.connectionModeIndex == index,
                        onClick = { viewModel.onConnectionModeChange(index) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.testConnection() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Text("Test Connection")
                }
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Text("Save")
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Dashboard Config", style = MaterialTheme.typography.labelLarge)
            Text(
                "Export your group and tile configuration to a JSON file, " +
                "or import a file previously exported from the web app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Hub credentials for file manager (only needed when hub security is enabled)
            Text("Hub File Manager Credentials", style = MaterialTheme.typography.labelMedium)
            Text(
                "Required only if Hub Security is enabled on your Hubitat hub.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = uiState.hubUsername,
                onValueChange = { viewModel.onHubUsernameChange(it) },
                label = { Text("Hub Username") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.hubPassword,
                onValueChange = { viewModel.onHubPasswordChange(it) },
                label = { Text("Hub Password") },
                visualTransformation = if (showHubPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showHubPassword = !showHubPassword }) {
                        Icon(
                            imageVector = if (showHubPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showHubPassword) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("hubitat-config.json") },
                    modifier = Modifier.weight(1f)
                ) { Text("Export Config") }
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.weight(1f)
                ) { Text("Import Config") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.pushConfigToHub() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) { Text("Push to Hub") }
                Button(
                    onClick = { viewModel.pullConfigFromHub() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) { Text("Pull from Hub") }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            OutlinedButton(
                onClick = onRingListenerClick,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ring Listener →") }

            Spacer(modifier = Modifier.size(12.dp))

            OutlinedButton(
                onClick = onGpsTrackerClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("GPS Tracker Settings →")
            }

            Spacer(modifier = Modifier.size(24.dp))
        }
    }

    // Confirmation dialog before overwriting config on import
    showImportConfirmDialog?.let { pendingJson ->
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = null },
            title = { Text("Replace Config?") },
            text = { Text("This will replace all group and tile configuration with the imported data. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirmDialog = null
                    viewModel.importConfig(pendingJson)
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = null }) { Text("Cancel") }
            }
        )
    }

    // Confirmation dialog before overwriting config on hub pull
    if (uiState.pendingHubImportData != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelHubPull() },
            title = { Text("Replace Config?") },
            text = { Text("This will replace all group and tile configuration with the data pulled from the hub. Continue?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmHubPull() }) { Text("Pull") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelHubPull() }) { Text("Cancel") }
            }
        )
    }
}

