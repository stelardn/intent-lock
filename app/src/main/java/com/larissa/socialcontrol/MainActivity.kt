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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.larissa.socialcontrol.ui.theme.IntentLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IntentLockTheme {
                var sessionVersion by remember { mutableStateOf(0) }
                val sessionStore = remember(sessionVersion) {
                    ChallengeSessionStore(this@MainActivity)
                }
                val unlockGrantStore = remember(sessionVersion) {
                    UnlockGrantStore(this@MainActivity)
                }
                val tracker = remember { UsageStatsChallengeTracker(this@MainActivity) }
                val activeSession = sessionStore.load()
                val unlockGrant = unlockGrantStore.load()
                val progress = activeSession?.let { session ->
                    tracker.calculateProgress(session)
                }

                RefreshOnResume {
                    sessionVersion++
                }

                LaunchedEffect(activeSession?.startedAtEpochMs, progress?.isComplete) {
                    if (activeSession != null &&
                        progress?.isComplete == true &&
                        activeSession.completedAtEpochMs == null
                    ) {
                        val completedAt = System.currentTimeMillis()
                        sessionStore.markCompleted(completedAt)
                        unlockGrantStore.save(
                            UnlockGrant(
                                blockedPackage = activeSession.blockedPackage,
                                expiresAtEpochMs = completedAt + AppConfig.UNLOCK_WINDOW_MS,
                            ),
                        )
                        sessionVersion++
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SpikeSetupScreen(
                        modifier = Modifier.padding(innerPadding),
                        activeSession = activeSession,
                        unlockGrant = unlockGrant?.takeIf {
                            it.expiresAtEpochMs > System.currentTimeMillis()
                        },
                        progress = progress,
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onOpenUsageAccessSettings = {
                            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                        onRefreshProgress = {
                            sessionVersion++
                        },
                        onClearSession = {
                            sessionStore.clear()
                            unlockGrantStore.clear()
                            sessionVersion++
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RefreshOnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun SpikeSetupScreen(
    modifier: Modifier = Modifier,
    activeSession: ChallengeSession?,
    unlockGrant: UnlockGrant?,
    progress: ChallengeProgress?,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onRefreshProgress: () -> Unit,
    onClearSession: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Intent Lock",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "This build watches for ${AppConfig.BLOCKED_PACKAGE}, launches ${AppConfig.CONTROL_PACKAGE}, tracks challenge time, and grants a temporary unlock on completion.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Enable Accessibility and Usage Access so the app can detect the blocked app and measure time spent in the control app.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onOpenAccessibilitySettings) {
            Text("Open Accessibility Settings")
        }
        Button(onClick = onOpenUsageAccessSettings) {
            Text("Open Usage Access Settings")
        }
        Text(
            text = "After enabling the service, open Instagram, tap Start challenge, spend about ${AppConfig.REQUIRED_SECONDS} seconds in Duolingo, then return here and retry Instagram.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (activeSession != null) {
            Text(
                text = "Active session: ${activeSession.controlPackage} for ${activeSession.requiredSeconds}s.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (progress != null && !progress.hasUsageAccess) {
                Text(
                    text = "Usage Access is still missing, so no challenge time can be measured yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (progress != null && progress.hasUsageAccess) {
                Text(
                    text = "Tracked time: ${progress.trackedSeconds}s / ${progress.requiredSeconds}s.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (activeSession.completedAtEpochMs != null || progress.isComplete) {
                        "Challenge completed."
                    } else {
                        "Challenge still in progress."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (unlockGrant != null) {
                val remainingSeconds = ((unlockGrant.expiresAtEpochMs - System.currentTimeMillis()) / 1_000L)
                    .coerceAtLeast(0L)
                Text(
                    text = "Instagram unlocked for about ${remainingSeconds}s more.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(onClick = onRefreshProgress) {
                Text("Refresh challenge progress")
            }
            Button(onClick = onClearSession) {
                Text("Clear active session")
            }
        }
    }
}
