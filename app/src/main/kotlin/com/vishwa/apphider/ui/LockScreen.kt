package com.vishwa.apphider.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vishwa.apphider.R

@Composable
fun LockScreen(viewModel: MainViewModel, onBiometric: () -> Unit) {
    val pinSet by viewModel.pinSet.collectAsState()
    val biometricEnabled by viewModel.biometric.collectAsState()
    val biometricAvailable = viewModel.biometricAvailable
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val submit = {
        if (pinSet) {
            if (!viewModel.unlock(pin)) error = true
        } else if (pin.length in 4..6) {
            viewModel.setPin(pin)
            pin = ""
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(if (pinSet) R.string.locked else R.string.set_pin),
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(if (pinSet) R.string.locked_body else R.string.set_pin_body),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = pin,
                onValueChange = { value ->
                    if (value.length <= 6 && value.all(Char::isDigit)) {
                        pin = value
                        error = false
                    }
                },
                label = { Text(stringResource(R.string.enter_pin)) },
                supportingText = {
                    Text(stringResource(if (error) R.string.invalid_pin else R.string.pin_hint))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = error,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )

            Button(
                onClick = { submit() },
                enabled = if (pinSet) pin.isNotBlank() else pin.length in 4..6,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text(stringResource(if (pinSet) R.string.unlock else R.string.set_pin))
            }

            if (pinSet && biometricEnabled && biometricAvailable) {
                TextButton(onClick = onBiometric, modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.use_biometric))
                }
            }
        }
    }
}
