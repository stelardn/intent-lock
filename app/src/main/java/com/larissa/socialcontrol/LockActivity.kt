package com.larissa.socialcontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

        val ruleId = intent.getStringExtra(EXTRA_RULE_ID)

        setContent {
            IntentLockTheme {
                var screenState by remember(ruleId) { mutableStateOf(loadInitialState(ruleId)) }

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
                                    message = "Não foi possível abrir ${rule.controlAppName}. Revise a regra em Regras.",
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

    private fun loadInitialState(ruleId: String?): LockScreenState {
        if (ruleId.isNullOrBlank()) {
            return LockScreenState.Recovery(
                message = "Não foi possível identificar a regra que deveria bloquear este app. Volte ao IntentLock e tente novamente.",
            )
        }

        val rule = ruleStore.load(ruleId)
            ?: return LockScreenState.Recovery(
                message = "A regra selecionada não foi encontrada. Revise sua lista em Regras.",
            )

        if (!rule.isEnabled) {
            sessionStore.clearForRule(rule.ruleId)
            unlockGrantStore.clearForRule(rule.ruleId)
            return LockScreenState.Recovery(
                message = "Essa regra está inativa no momento. Reative-a em Regras para voltar a proteger ${rule.blockedAppName}.",
            )
        }

        val validation = runtimeValidator.validateSavedRule(rule, ruleStore.loadAll())
        if (!validation.isValid) {
            sessionStore.clearForRule(rule.ruleId)
            unlockGrantStore.clearForRule(rule.ruleId)
            return LockScreenState.Recovery(
                message = "A regra salva ficou inválida. Revise os apps escolhidos e salve novamente.",
            )
        }

        return LockScreenState.Ready(rule)
    }

    companion object {
        private const val EXTRA_RULE_ID = "extra_rule_id"

        fun createIntent(context: Context, ruleId: String): Intent {
            return Intent(context, LockActivity::class.java).apply {
                putExtra(EXTRA_RULE_ID, ruleId)
            }
        }
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
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f),
            MaterialTheme.colorScheme.background,
        ),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(20.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = "Momento de intenção",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = when (screenState) {
                            is LockScreenState.Ready -> "Acesso pausado antes da abertura"
                            is LockScreenState.Recovery -> "Recuperação necessária"
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    when (screenState) {
                        is LockScreenState.Ready -> ReadyLockState(
                            rule = screenState.rule,
                            onStartChallenge = onStartChallenge,
                        )

                        is LockScreenState.Recovery -> RecoveryLockState(message = screenState.message)
                    }
                }
            }

            BoxWithConstraints {
                val stackVertically = maxWidth < 460.dp

                if (stackVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onReturnToApp,
                        ) {
                            Text("Voltar ao app")
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onOpenAccessibilitySettings,
                        ) {
                            Text("Abrir acessibilidade")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = onReturnToApp,
                        ) {
                            Text("Voltar ao app")
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onOpenAccessibilitySettings,
                        ) {
                            Text("Abrir acessibilidade")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyLockState(
    rule: InterventionRule,
    onStartChallenge: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "${rule.blockedAppName} foi interceptado. Antes de liberar acesso, cumpra um curto desafio no app definido como controle.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BoxWithConstraints {
            val stackVertically = maxWidth < 460.dp

            if (stackVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LockFact(label = "App bloqueado", value = rule.blockedAppName)
                    LockFact(label = "App de controle", value = rule.controlAppName)
                    LockFact(label = "Tempo mínimo", value = "${rule.requiredSeconds} segundos")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LockFact(modifier = Modifier.weight(1f), label = "App bloqueado", value = rule.blockedAppName)
                    LockFact(modifier = Modifier.weight(1f), label = "App de controle", value = rule.controlAppName)
                    LockFact(modifier = Modifier.weight(1f), label = "Tempo mínimo", value = "${rule.requiredSeconds}s")
                }
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStartChallenge,
        ) {
            Text("Iniciar desafio")
        }
    }
}

@Composable
private fun RecoveryLockState(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "A intervenção não pode continuar agora.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun LockFact(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
