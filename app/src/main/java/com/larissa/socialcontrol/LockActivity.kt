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
import androidx.compose.material3.Card
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
    private val ruleStore by lazy { InterventionRuleStore(this) }
    private val sessionStore by lazy { ChallengeSessionStore(this) }
    private val unlockGrantStore by lazy { UnlockGrantStore(this) }
    private val installedAppRepository by lazy { InstalledAppRepository(this) }
    private val runtimeValidator by lazy { RuleRuntimeValidator(installedAppRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IntentLockTheme {
                var screenState by remember { mutableStateOf(loadInitialState()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LockScreen(
                        modifier = Modifier.padding(innerPadding),
                        screenState = screenState,
                        onStartChallenge = {
                            val readyState = screenState as? LockScreenState.Ready ?: return@LockScreen
                            val rule = readyState.rule
                            val launchIntent = installedAppRepository.getLaunchIntent(rule.controlPackage)

                            if (launchIntent == null) {
                                screenState = LockScreenState.Recovery(
                                    message = "Não foi possível abrir ${rule.controlAppName}. Revise a configuração na tela inicial.",
                                )
                                return@LockScreen
                            }

                            sessionStore.save(
                                ChallengeSession(
                                    ruleId = rule.ruleId,
                                    blockedPackage = rule.blockedPackage,
                                    controlPackage = rule.controlPackage,
                                    requiredSeconds = rule.requiredSeconds,
                                    startedAtEpochMs = System.currentTimeMillis(),
                                ),
                            )

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

    private fun loadInitialState(): LockScreenState {
        val rule = ruleStore.load()
            ?: return LockScreenState.Recovery(
                message = "Nenhuma regra válida foi encontrada. Volte para a tela inicial e configure o bloqueio.",
            )

        val validation = runtimeValidator.validateSavedRule(rule)
        if (!validation.isValid) {
            sessionStore.clear()
            unlockGrantStore.clear()
            return LockScreenState.Recovery(
                message = "A regra salva ficou inválida. Revise os aplicativos escolhidos e salve novamente.",
            )
        }

        return LockScreenState.Ready(rule)
    }
}

private sealed interface LockScreenState {
    data class Ready(val rule: InterventionRule) : LockScreenState

    data class Recovery(val message: String) : LockScreenState
}

@Composable
private fun LockScreen(
    modifier: Modifier = Modifier,
    screenState: LockScreenState,
    onStartChallenge: () -> Unit,
    onReturnToApp: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = when (screenState) {
                    is LockScreenState.Ready -> "Acesso pausado"
                    is LockScreenState.Recovery -> "Recuperação necessária"
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )

            when (screenState) {
                is LockScreenState.Ready -> {
                    Text(
                        text = "${screenState.rule.blockedAppName} foi interceptado.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Inicie o desafio para abrir ${screenState.rule.controlAppName} e permanecer lá por ${screenState.rule.requiredSeconds} segundos para ganhar créditos.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = onStartChallenge) {
                        Text("Iniciar desafio")
                    }
                }

                is LockScreenState.Recovery -> {
                    Text(
                        text = screenState.message,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Button(onClick = onReturnToApp) {
                Text("Voltar para configuração")
            }
            Button(onClick = onOpenAccessibilitySettings) {
                Text("Abrir acessibilidade")
            }
        }
    }
}
