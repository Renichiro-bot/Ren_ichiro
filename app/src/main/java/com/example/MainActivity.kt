package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.theme.VellumShapes
import com.example.core.designsystem.theme.VellumSpacing
import com.example.core.designsystem.theme.VellumTheme
import com.example.core.preferences.ThemeViewModel
import com.example.navigation.VellumNavHost

import android.content.Intent
import com.example.core.util.IncomingIntentTarget
import com.example.core.util.VellumIntentParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels {
        ThemeViewModel.provideFactory(this)
    }

    private val _incomingTarget = MutableStateFlow<IncomingIntentTarget>(IncomingIntentTarget.None)
    val incomingTarget: StateFlow<IncomingIntentTarget> = _incomingTarget.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        val app = application as VellumApplication
        val noteRepository = app.noteRepository
        val appLockManager = app.appLockManager

        setContent {
            val themeConfig by themeViewModel.themeConfig.collectAsState()
            val isAppLockEnabled by appLockManager.isAppLockEnabledStream.collectAsState(initial = false)
            val isSessionUnlocked by appLockManager.isSessionUnlocked.collectAsState()
            val target by incomingTarget.collectAsState()

            val unlockLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    appLockManager.markSessionUnlocked()
                }
            }

            VellumTheme(themeConfig = themeConfig) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isAppLockEnabled && !isSessionUnlocked) {
                        AppLockGateScreen(
                            onUnlockClick = {
                                val unlockIntent = appLockManager.createUnlockIntent()
                                if (unlockIntent != null) {
                                    unlockLauncher.launch(unlockIntent)
                                } else {
                                    // Device has no secure lock set
                                    appLockManager.markSessionUnlocked()
                                }
                            }
                        )
                    } else {
                        VellumNavHost(
                            themeViewModel = themeViewModel,
                            repository = noteRepository,
                            incomingTarget = target,
                            onConsumeIncomingTarget = { consumeIncomingTarget() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val target = VellumIntentParser.parseIntent(intent)
        if (target !is IncomingIntentTarget.None) {
            _incomingTarget.value = target
        }
    }

    fun consumeIncomingTarget() {
        _incomingTarget.value = IncomingIntentTarget.None
    }
}

@Composable
private fun AppLockGateScreen(
    onUnlockClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(VellumSpacing.xl)
            .testTag("app_lock_gate_screen")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, VellumShapes.cornerFull)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(VellumSpacing.l))

            Text(
                text = "Vellum is Locked",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(VellumSpacing.s))

            Text(
                text = "Device screen lock protection is enabled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(VellumSpacing.xl))

            Button(
                onClick = onUnlockClick,
                shape = VellumShapes.cornerM,
                modifier = Modifier.testTag("unlock_app_button")
            ) {
                Text("Unlock with Device Security")
            }
        }
    }
}
