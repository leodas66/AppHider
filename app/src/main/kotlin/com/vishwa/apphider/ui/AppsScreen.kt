package com.vishwa.apphider.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vishwa.apphider.R
import com.vishwa.apphider.data.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: MainViewModel) {
    val apps by viewModel.apps.collectAsState()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AppInfo?>(null) }
    var confirm by remember { mutableStateOf(false) }

    val filtered = apps.filter {
        it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.search_apps)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        )
        AppGrid(
            items = filtered,
            onClick = { if (!it.protected) selected = it },
            onLongClick = { if (!it.protected) selected = it },
        )
    }

    val current = selected
    if (current != null) {
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(Modifier.padding(16.dp)) {
                Text(current.label, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Text(current.packageName, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { confirm = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(stringResource(R.string.hide))
                }
            }
        }
    }

    if (confirm && current != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.hide_confirm_title)) },
            text = { Text(stringResource(R.string.hide_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.hide(current.packageName)
                    confirm = false
                    selected = null
                }) {
                    Text(stringResource(R.string.hide))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
