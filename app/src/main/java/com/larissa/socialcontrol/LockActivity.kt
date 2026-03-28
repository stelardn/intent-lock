package com.larissa.socialcontrol

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.larissa.socialcontrol.ui.theme.IntentLockTheme

class LockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val challengeSessionStore = ChallengeSessionStore(this)

        setContent {
            IntentLockTheme {
                var launchError by remember { mutableStateOf<String?>(null) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LockScreen(
                        modifier = Modifier.padding(innerPadding),
                        launchError = launchError,
                        onStartChallenge = {
                            val launchIntent = packageManager
                                .getLaunchIntentForPackage(AppConfig.CONTROL_PACKAGE)

                            if (launchIntent == null) {
                                launchError = "Could not find ${AppConfig.CONTROL_PACKAGE} on this device."
                                return@LockScreen
                            }

                            val session = ChallengeSession(
                                blockedPackage = AppConfig.BLOCKED_PACKAGE,
                                controlPackage = AppConfig.CONTROL_PACKAGE,
                                requiredSeconds = AppConfig.REQUIRED_SECONDS,
                                startedAtEpochMs = System.currentTimeMillis(),
                            )
                            challengeSessionStore.save(session)

                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(launchIntent)
                            finish()
                        },
                        onReturnToApp = {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        },
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LockScreen(
    modifier: Modifier = Modifier,
    launchError: String?,
    onStartChallenge: () -> Unit,
    onReturnToApp: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Access paused",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "The spike detected ${AppConfig.BLOCKED_PACKAGE} and redirected here.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Start the challenge to open ${AppConfig.CONTROL_PACKAGE}. After spending time there, come back here to see measured progress.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onStartChallenge) {
            Text("Start challenge")
        }
        if (launchError != null) {
            Text(
                text = launchError,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(onClick = onReturnToApp) {
            Text("Back to setup")
        }
        Button(onClick = onOpenAccessibilitySettings) {
            Text("Accessibility Settings")
        }
    }
}
