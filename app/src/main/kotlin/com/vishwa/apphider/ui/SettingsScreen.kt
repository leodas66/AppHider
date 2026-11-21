package com.vishwa.apphider.ui

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vishwa.apphider.R

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val timeout by viewModel.timeout.collectAsState()
    val biometric by viewModel.biometric.collectAsState()
    val hasRoot by viewModel.rootAvailable.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(stringResource(R.string.rehide_timeout), style = MaterialTheme.typography.titleMedium)
        listOf(1, 5, 15).forEach { minutes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = timeout == minutes, onClick = { viewModel.setTimeout(minutes) })
                Text("$minutes min")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(checked = biometric, onCheckedChange = { viewModel.setBiometric(it) })
            Text(stringResource(R.string.biometric), modifier = Modifier.padding(start = 8.dp))
        }

        Button(
            onClick = { viewModel.unhideAll() },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.unhide_all))
        }

        Text(
            text = stringResource(if (hasRoot) R.string.root_available else R.string.root_unavailable),
            modifier = Modifier.padding(top = 16.dp),
        )
        Text("Android ${Build.VERSION.RELEASE}")
    }
}
