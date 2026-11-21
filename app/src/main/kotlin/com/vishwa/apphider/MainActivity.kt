package com.vishwa.apphider

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import com.vishwa.apphider.ui.CalculatorScreen
import com.vishwa.apphider.ui.MainScreen
import com.vishwa.apphider.ui.MainViewModel
import com.vishwa.apphider.ui.theme.AppHiderTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : FragmentActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var biometricPrompt: BiometricPrompt
    private val calculatorMode = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        updateMode(intent)
        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlock()
                }
            },
        )

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) viewModel.lock()
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS,
            )
        }

        setContent {
            AppHiderTheme {
                val calculator by calculatorMode.collectAsState()
                if (calculator) {
                    CalculatorScreen(onUnlock = { candidate ->
                        val valid = viewModel.verifyPin(candidate)
                        if (valid) {
                            viewModel.unlock()
                            calculatorMode.value = false
                        }
                        valid
                    })
                } else {
                    MainScreen(viewModel = viewModel, onBiometric = { showBiometric() })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateMode(intent)
    }

    private fun updateMode(intent: Intent?) {
        if (intent?.action == ACTION_SECRET_UNLOCK) {
            // Secret-code entry always leads to the real PIN screen, never to the cover app.
            viewModel.lock()
            calculatorMode.value = false
        } else {
            calculatorMode.value = viewModel.disguised.value
        }
    }

    private fun showBiometric() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.use_biometric))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    companion object {
        const val ACTION_SECRET_UNLOCK = "com.vishwa.apphider.SECRET_UNLOCK"
        private const val REQUEST_NOTIFICATIONS = 1001
    }
}
