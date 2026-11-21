package com.vishwa.apphider.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.vishwa.apphider.data.AppInfo
import com.vishwa.apphider.data.HiddenApp

@Composable
fun HiddenScreen(viewModel: MainViewModel) {
    val hidden by viewModel.hiddenApps.collectAsState()
    var menuApp by remember { mutableStateOf<HiddenApp?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (hidden.isEmpty()) {
            Column(Modifier.align(Alignment.Center)) {
                Text(stringResource(R.string.no_hidden))
            }
        } else {
            AppGrid(
                items = hidden.map { AppInfo(it.packageName, it.label, protected = false) },
                dimmed = true,
                onClick = { viewModel.unhideAndLaunch(it.packageName) },
                onLongClick = { app -> menuApp = hidden.firstOrNull { it.packageName == app.packageName } },
            )
        }

        FloatingActionButton(
            onClick = { viewModel.rehideAll() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Default.VisibilityOff, contentDescription = stringResource(R.string.panic_hide))
        }
    }

    val current = menuApp
    if (current != null) {
        AlertDialog(
            onDismissRequest = { menuApp = null },
            title = { Text(current.label) },
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
