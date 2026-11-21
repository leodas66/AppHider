package com.vishwa.apphider.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vishwa.apphider.R

@Composable
fun LockScreen(viewModel: MainViewModel, onBiometric: () -> Unit) {
    val pinSet by viewModel.pinSet.collectAsState()
    val biometricAvailable by remember { mutableStateOf(viewModel.biometricAvailable) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp))
        Text(stringResource(R.string.locked), style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = pin,
            onValueChange = { value ->
                if (value.length <= 6 && value.all(Char::isDigit)) {
                    pin = value
                    error = false
                }
            },
            label = { Text(stringResource(R.string.enter_pin)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = error,
        )

        Button(onClick = {
            if (pinSet) {
                if (!viewModel.unlock(pin)) error = true
            } else if (pin.length in 4..6) {
                viewModel.setPin(pin)
                pin = ""
            }
        }) {
            Text(stringResource(if (pinSet) R.string.unlock else R.string.set_pin))
        }

        if (pinSet && biometricAvailable) {
            TextButton(onClick = onBiometric) {
                Text(stringResource(R.string.use_biometric))
            }
        }

        if (error) {
            Text(stringResource(R.string.invalid_pin), color = MaterialTheme.colorScheme.error)
        }
    }
}
