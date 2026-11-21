package com.vishwa.apphider.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vishwa.apphider.R
import com.vishwa.apphider.update.UpdateUiState

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val timeout by viewModel.timeout.collectAsState()
    val biometric by viewModel.biometric.collectAsState()
    val disguised by viewModel.disguised.collectAsState()
    val hasRoot by viewModel.rootAvailable.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshRootState() }

    var confirmUnhideAll by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    val wrongPinText = stringResource(R.string.wrong_current_pin)
    val invalidPinText = stringResource(R.string.pin_invalid_length)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(stringResource(R.string.section_behavior))
        Text(
            text = stringResource(R.string.rehide_timeout),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.timeout_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf(1, 5, 15).forEach { minutes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = timeout == minutes, onClick = { viewModel.setTimeout(minutes) })
                Text(stringResource(R.string.minutes, minutes))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = timeout <= 0, onClick = { viewModel.setTimeout(0) })
            Text(stringResource(R.string.timeout_on_screen_off))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = biometric && viewModel.biometricAvailable,
                onCheckedChange = { viewModel.setBiometric(it) },
                enabled = viewModel.biometricAvailable,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.biometric))
                if (!viewModel.biometricAvailable) {
                    Text(
                        text = stringResource(R.string.biometric_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(checked = disguised, onCheckedChange = { viewModel.setDisguised(it) })
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.disguise))
                Text(
                    text = stringResource(R.string.disguise_body, "*#*#2774437#*#*"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        SectionTitle(stringResource(R.string.section_security))
        OutlinedButton(
            onClick = {
                currentPin = ""
                newPin = ""
                pinError = null
                showPinDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.change_pin))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        SectionTitle(stringResource(R.string.section_hidden_apps))
        Button(
            onClick = { confirmUnhideAll = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.unhide_all))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        SectionTitle(stringResource(R.string.section_updates))
        Text(
            text = stringResource(R.string.current_version, viewModel.currentVersion),
            style = MaterialTheme.typography.bodyMedium,
        )

        when (val state = updateState) {
            UpdateUiState.Idle -> Unit
            UpdateUiState.Checking -> Text(
                text = stringResource(R.string.update_checking),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            UpdateUiState.UpToDate -> Text(
                text = stringResource(R.string.update_up_to_date),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            is UpdateUiState.Available -> {
                Text(
                    text = stringResource(R.string.update_available, state.info.versionName),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = state.info.releaseNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { viewModel.downloadUpdate(state.info) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.download_update))
                }
            }
            is UpdateUiState.Downloading -> {
                val label = if (state.progress >= 0) {
                    stringResource(R.string.downloading_percent, state.progress)
                } else {
                    stringResource(R.string.downloading)
                }
                Text(text = label, modifier = Modifier.padding(top = 8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            is UpdateUiState.Ready -> {
                Text(
                    text = stringResource(R.string.update_ready, state.info.versionName),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = { viewModel.installUpdate(state.apk) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = hasRoot,
                ) {
                    Text(stringResource(R.string.install_update))
                }
                if (!hasRoot) {
                    Text(
                        text = stringResource(R.string.update_root_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            UpdateUiState.Installing -> Text(
                text = stringResource(R.string.installing_update),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            is UpdateUiState.Error -> Text(
                text = state.message,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = { viewModel.checkForUpdates() },
            enabled = updateState !is UpdateUiState.Checking &&
                updateState !is UpdateUiState.Downloading &&
                updateState !is UpdateUiState.Installing,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.check_for_updates))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        SectionTitle(stringResource(R.string.section_device))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatusRow(
                available = hasRoot,
                label = stringResource(if (hasRoot) R.string.root_available else R.string.root_unavailable),
            )
            TextButton(onClick = { viewModel.refreshRootState() }) {
                Text(stringResource(R.string.refresh_root_status))
            }
        }
        Text(
            text = stringResource(R.string.android_version, Build.VERSION.RELEASE),
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirmUnhideAll) {
        AlertDialog(
            onDismissRequest = { confirmUnhideAll = false },
            title = { Text(stringResource(R.string.unhide_all_title)) },
            text = { Text(stringResource(R.string.unhide_all_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unhideAll()
                    confirmUnhideAll = false
                }) {
                    Text(stringResource(R.string.unhide_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnhideAll = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(stringResource(R.string.change_pin)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { currentPin = it.filter(Char::isDigit).take(6) },
                        label = { Text(stringResource(R.string.current_pin)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            newPin = it.filter(Char::isDigit).take(6)
                            pinError = null
                        },
                        label = { Text(stringResource(R.string.new_pin)) },
                        supportingText = { Text(stringResource(R.string.pin_hint)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError != null,
                    )
                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        !viewModel.verifyPin(currentPin) -> pinError = wrongPinText
                        newPin.length !in 4..6 -> pinError = invalidPinText
                        else -> {
                            viewModel.setPin(newPin)
                            showPinDialog = false
                        }
                    }
                }) {
                    Text(stringResource(R.string.change_pin))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun StatusRow(available: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (available) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
