package com.vishwa.apphider.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.vishwa.apphider.R
import com.vishwa.apphider.data.HiddenApp

@Composable
fun HiddenScreen(viewModel: MainViewModel, onOpenApps: () -> Unit) {
    val hidden by viewModel.hiddenApps.collectAsState()
    val items by viewModel.hiddenAppItems.collectAsState()
    var menuApp by remember { mutableStateOf<HiddenApp?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.no_hidden),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.no_hidden_body),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenApps, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.go_to_apps))
                }
            }
        } else {
            AppGrid(
                items = items,
                hidden = true,
                onClick = { viewModel.unhideAndLaunch(it.packageName) },
                onLongClick = { app -> menuApp = hidden.firstOrNull { it.packageName == app.packageName } },
                contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 96.dp),
            )
        }

        if (hidden.any { it.shouldBeHidden }) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.rehideAll() },
                icon = {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null)
                },
                text = { Text(stringResource(R.string.panic_hide)) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }

    val current = menuApp
    if (current != null) {
        AlertDialog(
            onDismissRequest = { menuApp = null },
            title = { Text(current.label) },
            text = { Text(stringResource(R.string.hidden_options_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unhidePermanently(current.packageName)
                    menuApp = null
                }) {
                    Text(stringResource(R.string.unhide_permanently))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.removeFromList(current.packageName)
                    menuApp = null
                }) {
                    Text(stringResource(R.string.remove_from_list))
                }
            },
        )
    }
}
