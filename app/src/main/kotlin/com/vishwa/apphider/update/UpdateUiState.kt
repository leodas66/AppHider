package com.vishwa.apphider.update

import java.io.File

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val progress: Int) : UpdateUiState
    data class Ready(val info: UpdateInfo, val apk: File) : UpdateUiState
    data object Installing : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}
