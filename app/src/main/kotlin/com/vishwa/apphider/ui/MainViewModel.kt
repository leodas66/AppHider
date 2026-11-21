package com.vishwa.apphider.ui

import android.app.Application
import androidx.biometric.BiometricManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import com.vishwa.apphider.AppHiderApplication
import com.vishwa.apphider.data.AppInfo
import com.vishwa.apphider.data.HiddenApp
import com.vishwa.apphider.root.ProtectedPackages
import com.vishwa.apphider.update.UpdateInfo
import com.vishwa.apphider.update.UpdateUiState
import com.vishwa.apphider.work.RehideWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class MainViewModel(private val application: Application) : AndroidViewModel(application) {

    private val container get() = (application as AppHiderApplication).container

    val hiddenApps = container.hiddenAppDao.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())

    val apps: StateFlow<List<AppInfo>> = combine(_allApps, hiddenApps) { all, hidden ->
        val hiddenNames = hidden.filter { it.shouldBeHidden }.map { it.packageName }.toSet()
        all.filterNot { it.packageName in hiddenNames }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val hiddenAppItems: StateFlow<List<AppInfo>> = hiddenApps.map { items ->
        items.map { item ->
            AppInfo(
                packageName = item.packageName,
                label = item.label,
                protected = false,
                icon = container.packages.iconOf(item.packageName),
            )
        }.sortedBy { it.label.lowercase() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val locked: StateFlow<Boolean> = container.lock.locked
    val rootAvailable = MutableStateFlow(container.root.isRootAvailable())
    val pinSet = MutableStateFlow(container.pin.hasPin())
    val biometricAvailable = BiometricManager.from(application)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    private val _timeout = MutableStateFlow(container.settings.autoRehideMinutes)
    val timeout: StateFlow<Int> = _timeout

    private val _biometric = MutableStateFlow(container.settings.biometricEnabled)
    val biometric: StateFlow<Boolean> = _biometric

    val currentVersion: String = "${container.updates.currentVersionName()} (${container.updates.currentVersionCode()})"

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val ownPackage = application.packageName
            _allApps.value = container.root.listAll()
                .map { container.packages.appInfo(it, ProtectedPackages.isProtected(it, ownPackage)) }
                .sortedBy { it.label.lowercase() }
        }
    }

    fun hide(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (container.root.hide(packageName).isFailure) return@launch
            container.hiddenAppDao.upsert(
                HiddenApp(packageName, container.packages.labelOf(packageName), shouldBeHidden = true),
            )
            refresh()
        }
    }

    fun unhideAndLaunch(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (container.root.unhide(packageName).isFailure) return@launch
            container.hiddenAppDao.upsert(
                HiddenApp(
                    packageName = packageName,
                    label = container.packages.labelOf(packageName),
                    shouldBeHidden = true,
                    lastUnlockedAt = System.currentTimeMillis(),
                ),
            )
            scheduleRehide()
            container.notifications.show(hiddenApps.value.size)
            withContext(Dispatchers.Main) { container.packages.launch(packageName) }
        }
    }

    fun unhidePermanently(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            container.root.unhide(packageName)
            container.hiddenAppDao.upsert(
                HiddenApp(packageName, container.packages.labelOf(packageName), shouldBeHidden = false),
            )
            cancelRehide()
        }
    }

    fun removeFromList(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            container.hiddenAppDao.get(packageName)?.let { container.hiddenAppDao.delete(it) }
        }
    }

    fun rehideAll() {
        viewModelScope.launch(Dispatchers.IO) {
            container.hiddenAppDao.getAll()
                .filter { it.shouldBeHidden }
                .forEach { container.root.hide(it.packageName) }
            cancelRehide()
            container.notifications.dismiss()
        }
    }

    fun unhideAll() {
        viewModelScope.launch(Dispatchers.IO) {
            container.hiddenAppDao.getAll().forEach { app ->
                container.root.unhide(app.packageName)
                container.hiddenAppDao.upsert(app.copy(shouldBeHidden = false))
            }
            cancelRehide()
            container.notifications.dismiss()
        }
    }

    fun verifyPin(pin: String): Boolean = container.pin.verify(pin)

    fun unlock(pin: String): Boolean {
        val valid = container.pin.verify(pin)
        if (valid) container.lock.unlock()
        return valid
    }

    fun unlock() {
        container.lock.unlock()
    }

    fun lock() {
        container.lock.lock()
    }

    fun setPin(pin: String) {
        container.pin.setPin(pin)
        pinSet.value = true
        container.lock.unlock()
    }

    fun setTimeout(minutes: Int) {
        container.settings.autoRehideMinutes = minutes
        _timeout.value = minutes
    }

    fun setBiometric(enabled: Boolean) {
        container.settings.biometricEnabled = enabled
        _biometric.value = enabled
    }

    fun checkForUpdates() {
        if (_updateState.value is UpdateUiState.Checking ||
            _updateState.value is UpdateUiState.Downloading ||
            _updateState.value is UpdateUiState.Installing
        ) return

        _updateState.value = UpdateUiState.Checking
        viewModelScope.launch {
            try {
                val info = container.updates.check()
                _updateState.value = if (info.versionCode > container.updates.currentVersionCode()) {
                    UpdateUiState.Available(info)
                } else {
                    UpdateUiState.UpToDate
                }
            } catch (e: Exception) {
                _updateState.value = UpdateUiState.Error(e.message ?: "Update check failed")
            }
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        _updateState.value = UpdateUiState.Downloading(0)
        viewModelScope.launch {
            try {
                val apk = container.updates.download(info) { progress ->
                    _updateState.value = UpdateUiState.Downloading(progress)
                }
                _updateState.value = UpdateUiState.Ready(info, apk)
            } catch (e: Exception) {
                _updateState.value = UpdateUiState.Error(e.message ?: "Update download failed")
            }
        }
    }

    fun installUpdate(apk: File) {
        _updateState.value = UpdateUiState.Installing
        viewModelScope.launch(Dispatchers.IO) {
            val result = container.updateInstaller.install(apk)
            _updateState.value = if (result.isSuccess) {
                UpdateUiState.Idle
            } else {
                UpdateUiState.Error(
                    result.exceptionOrNull()?.message ?: "Update install failed",
                )
            }
        }
    }

    private fun scheduleRehide() {
        val request = OneTimeWorkRequestBuilder<RehideWorker>()
            .setInitialDelay(_timeout.value.toLong(), TimeUnit.MINUTES)
            .build()
        container.work.enqueueUniqueWork(REHIDE_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    private fun cancelRehide() {
        container.work.cancelUniqueWork(REHIDE_WORK)
    }

    private companion object {
        const val REHIDE_WORK = "rehide"
    }
}
