package com.vishwa.apphider.ui

import android.app.Application
import androidx.biometric.BiometricManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vishwa.apphider.AppHiderApplication
import com.vishwa.apphider.R
import com.vishwa.apphider.data.AppInfo
import com.vishwa.apphider.data.HiddenApp
import com.vishwa.apphider.root.ProtectedPackages
import com.vishwa.apphider.update.UpdateInfo
import com.vishwa.apphider.update.UpdateUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(private val application: Application) : AndroidViewModel(application) {

    private val container get() = (application as AppHiderApplication).container

    /** Apps the user wants hidden. Permanently unhidden apps are not part of this list. */
    val hiddenApps = container.hiddenAppDao.observeTracked().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())

    val apps: StateFlow<List<AppInfo>> = combine(_allApps, hiddenApps) { all, hidden ->
        val hiddenNames = hidden.map { it.packageName }.toSet()
        all.filterNot { it.packageName in hiddenNames }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val hiddenAppItems: StateFlow<List<AppInfo>> = hiddenApps
        .map { items ->
            items.map { item ->
                val info = container.packages.infoOrNull(item.packageName)
                AppInfo(
                    packageName = item.packageName,
                    label = info?.label ?: item.label,
                    protected = false,
                    icon = info?.icon,
                    unhidden = item.lastUnlockedAt != 0L,
                )
            }.sortedBy { it.label.lowercase() }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val locked: StateFlow<Boolean> = container.lock.locked

    private val _rootAvailable = MutableStateFlow(false)
    val rootAvailable: StateFlow<Boolean> = _rootAvailable

    private val _rootChecked = MutableStateFlow(false)
    val rootChecked: StateFlow<Boolean> = _rootChecked

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val pinSet = MutableStateFlow(container.pin.hasPin())
    val biometricAvailable = BiometricManager.from(application)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    private val _timeout = MutableStateFlow(container.settings.autoRehideMinutes)
    val timeout: StateFlow<Int> = _timeout

    private val _biometric = MutableStateFlow(container.settings.biometricEnabled)
    val biometric: StateFlow<Boolean> = _biometric

    private val _disguised = MutableStateFlow(container.disguise.isEnabled)
    val disguised: StateFlow<Boolean> = _disguised

    val currentVersion: String = "${container.updates.currentVersionName()} (${container.updates.currentVersionCode()})"

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    init {
        refresh()
        refreshRootState()
    }

    fun refreshRootState() {
        container.root.checkRoot { available ->
            _rootAvailable.value = available
            _rootChecked.value = true
        }
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
            val label = container.packages.labelOf(packageName)
            if (container.root.hide(packageName).isFailure) {
                fail(application.getString(R.string.error_hide_failed, label))
                return@launch
            }
            container.hiddenAppDao.upsert(
                HiddenApp(packageName, label, shouldBeHidden = true),
            )
            refresh()
        }
    }

    fun unhideAndLaunch(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val row = container.hiddenAppDao.get(packageName)
            if (container.root.unhide(packageName).isFailure) {
                fail(application.getString(R.string.error_unhide_failed, row?.label ?: packageName))
                return@launch
            }
            // Only mark the app as temporarily visible when it is still supposed to be hidden.
            // A permanently unhidden app must not be silently re-armed by tapping it.
            if (row != null && row.shouldBeHidden) {
                container.hiddenAppDao.upsert(row.copy(lastUnlockedAt = System.currentTimeMillis()))
            }
            container.rehide.syncState(startWatcher = true)
            val launched = withContext(Dispatchers.Main) { container.packages.launch(packageName) }
            if (!launched) {
                fail(application.getString(R.string.error_launch_failed, row?.label ?: packageName))
            }
        }
    }

    fun unhidePermanently(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val row = container.hiddenAppDao.get(packageName) ?: return@launch
            if (container.root.unhide(packageName).isFailure) {
                fail(application.getString(R.string.error_unhide_failed, row.label))
                return@launch
            }
            container.hiddenAppDao.upsert(row.copy(shouldBeHidden = false, lastUnlockedAt = 0L))
            container.rehide.syncState()
        }
    }

    fun removeFromList(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val row = container.hiddenAppDao.get(packageName) ?: return@launch
            // Never drop tracking for a package that is still hidden on the device: that would
            // leave an app the user cannot find or unhide from this list.
            if (container.root.isHidden(packageName) && container.root.unhide(packageName).isFailure) {
                fail(application.getString(R.string.error_remove_failed, row.label))
                return@launch
            }
            container.hiddenAppDao.delete(row)
            container.rehide.syncState()
        }
    }

    fun rehideAll() {
        viewModelScope.launch(Dispatchers.IO) {
            var failed = false
            container.hiddenAppDao.getAll()
                .filter { it.shouldBeHidden && it.lastUnlockedAt != 0L }
                .forEach { app ->
                    if (container.root.hide(app.packageName).isSuccess) {
                        container.hiddenAppDao.upsert(app.copy(lastUnlockedAt = 0L))
                    } else {
                        failed = true
                    }
                }
            if (failed) fail(application.getString(R.string.error_rehide_failed))
            container.rehide.syncState()
        }
    }

    fun unhideAll() {
        viewModelScope.launch(Dispatchers.IO) {
            var failed = false
            container.hiddenAppDao.getAll().forEach { app ->
                if (container.root.unhide(app.packageName).isSuccess) {
                    container.hiddenAppDao.upsert(app.copy(shouldBeHidden = false, lastUnlockedAt = 0L))
                } else {
                    failed = true
                }
            }
            if (failed) fail(application.getString(R.string.error_unhide_all_failed))
            container.rehide.syncState()
        }
    }

    fun clearError() {
        _error.value = null
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
        viewModelScope.launch(Dispatchers.IO) { container.rehide.syncState() }
    }

    fun setBiometric(enabled: Boolean) {
        container.settings.biometricEnabled = enabled
        _biometric.value = enabled
    }

    fun setDisguised(enabled: Boolean) {
        if (enabled && !container.pin.hasPin()) {
            fail(application.getString(R.string.disguise_pin_required))
            return
        }
        container.disguise.setEnabled(enabled)
        _disguised.value = enabled
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

    private fun fail(message: String) {
        _error.value = message
    }
}
