package com.larissa.socialcontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.larissa.socialcontrol.ui.AppPickerDialog
import com.larissa.socialcontrol.ui.theme.IntentLockTheme
import java.util.Date

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IntentLockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        uiState = viewModel.uiState,
                        onBlockedAppSelected = viewModel::onBlockedAppSelected,
                        onBlockedAppCleared = viewModel::onBlockedAppCleared,
                        onControlAppSelected = viewModel::onControlAppSelected,
                        onControlAppCleared = viewModel::onControlAppCleared,
                        onRequiredSecondsChanged = viewModel::onRequiredSecondsChanged,
                        onUnlockWindowMinutesChanged = viewModel::onUnlockWindowMinutesChanged,
                        onSaveRule = viewModel::saveRule,
                        onClearRule = viewModel::clearRule,
                        onRefresh = viewModel::refresh,
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onOpenUsageAccessSettings = {
                            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}

@Composable
private fun MainScreen(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    onBlockedAppSelected: (InstalledAppInfo) -> Unit,
    onBlockedAppCleared: () -> Unit,
    onControlAppSelected: (InstalledAppInfo) -> Unit,
    onControlAppCleared: () -> Unit,
    onRequiredSecondsChanged: (String) -> Unit,
    onUnlockWindowMinutesChanged: (String) -> Unit,
    onSaveRule: () -> Unit,
    onClearRule: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
) {
    var pickerTarget by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeroCard(uiState)
        ReadinessCard(
            readiness = uiState.readiness,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onOpenUsageAccessSettings = onOpenUsageAccessSettings,
        )
        RuleConfigurationCard(
            uiState = uiState,
            onBlockedAppClick = { pickerTarget = "blocked" },
            onBlockedAppCleared = onBlockedAppCleared,
            onControlAppClick = { pickerTarget = "control" },
            onControlAppCleared = onControlAppCleared,
            onRequiredSecondsChanged = onRequiredSecondsChanged,
            onUnlockWindowMinutesChanged = onUnlockWindowMinutesChanged,
            onSaveRule = onSaveRule,
            onClearRule = onClearRule,
        )
        CurrentActivityCard(
            uiState = uiState,
            onRefresh = onRefresh,
        )
    }

    when (pickerTarget) {
        "blocked" -> AppPickerDialog(
            title = "Selecionar app bloqueado",
            apps = uiState.installedApps,
            selectedPackageName = uiState.draft.blockedApp?.packageName,
            disabledPackageName = uiState.draft.controlApp?.packageName,
            onDismissRequest = { pickerTarget = null },
            onSelected = {
                onBlockedAppSelected(it)
                pickerTarget = null
            },
        )

        "control" -> AppPickerDialog(
            title = "Selecionar app de controle",
            apps = uiState.installedApps,
            selectedPackageName = uiState.draft.controlApp?.packageName,
            disabledPackageName = uiState.draft.blockedApp?.packageName,
            onDismissRequest = { pickerTarget = null },
            onSelected = {
                onControlAppSelected(it)
                pickerTarget = null
            },
        )

        null -> Unit
    }
}

@Composable
private fun HeroCard(uiState: MainUiState) {
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Intent Lock",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            StatusChip(heroLabel(uiState, context))
            Text(
                text = heroDescription(uiState),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ReadinessCard(
    readiness: PermissionReadiness,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Readiness",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            ReadinessRow("Acessibilidade", readiness.accessibilityEnabled)
            ReadinessRow("Usage Access", readiness.usageAccessEnabled)
            Button(onClick = onOpenAccessibilitySettings) {
                Text("Abrir acessibilidade")
            }
            Button(onClick = onOpenUsageAccessSettings) {
                Text("Abrir Usage Access")
            }
        }
    }
}

@Composable
private fun RuleConfigurationCard(
    uiState: MainUiState,
    onBlockedAppClick: () -> Unit,
    onBlockedAppCleared: () -> Unit,
    onControlAppClick: () -> Unit,
    onControlAppCleared: () -> Unit,
    onRequiredSecondsChanged: (String) -> Unit,
    onUnlockWindowMinutesChanged: (String) -> Unit,
    onSaveRule: () -> Unit,
    onClearRule: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Sua regra",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            AppField(
                label = "App bloqueado",
                selection = uiState.draft.blockedApp,
                onSelect = onBlockedAppClick,
                onClear = onBlockedAppCleared,
            )
            AppField(
                label = "App de controle",
                selection = uiState.draft.controlApp,
                onSelect = onControlAppClick,
                onClear = onControlAppCleared,
            )
            OutlinedTextField(
                value = uiState.draft.requiredSecondsInput,
                onValueChange = onRequiredSecondsChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Duração do desafio (10-300s)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.draft.unlockWindowMinutesInput,
                onValueChange = onUnlockWindowMinutesChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Janela de desbloqueio (1-60 min)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            validationMessages(uiState.draftValidation).forEach { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (uiState.savedRule != null && !uiState.savedRuleValidation.isValid) {
                Text(
                    text = "A regra salva atual está inválida e não será usada até ser corrigida.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onSaveRule,
                enabled = uiState.isSaveEnabled,
            ) {
                Text("Salvar regra")
            }
            TextButton(
                onClick = onClearRule,
                enabled = uiState.canClearRule,
            ) {
                Text("Limpar regra")
            }
        }
    }
}

@Composable
private fun CurrentActivityCard(
    uiState: MainUiState,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Atividade atual",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (uiState.session == null && uiState.unlockGrant == null) {
                Text(
                    text = if (uiState.expiredCredit != null) {
                        "Os créditos acabaram. Conclua um novo desafio para liberar outro período de acesso."
                    } else {
                        "Nenhum desafio ou desbloqueio ativo no momento."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            uiState.session?.let { session ->
                Text(
                    text = "Desafio ativo para ${uiState.savedRule?.controlAppName ?: session.controlPackage}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val progress = uiState.progress
                if (progress == null) {
                    Text(
                        text = "Aguardando leitura de progresso.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else if (!progress.hasUsageAccess) {
                    Text(
                        text = "Usage Access ausente. O tempo do desafio não pode ser medido.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = "Progresso: ${progress.trackedSeconds}s / ${progress.requiredSeconds}s.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (session.completedAtEpochMs != null || progress.isComplete) {
                            "Desafio concluído."
                        } else {
                            "Desafio em andamento."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            uiState.unlockGrant?.let { unlockGrant ->
                val formattedTime = DateFormat.getTimeFormat(context).format(Date(unlockGrant.expiresAtEpochMs))
                val remainingSeconds = ((unlockGrant.expiresAtEpochMs - System.currentTimeMillis()) / 1_000L)
                    .coerceAtLeast(0L)
                Text(
                    text = "Créditos ativos até $formattedTime.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Restam aproximadamente ${remainingSeconds}s de crédito para ${uiState.savedRule?.blockedAppName ?: unlockGrant.blockedPackage}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            uiState.expiredCredit?.let { expiredCredit ->
                val formattedTime = DateFormat.getTimeFormat(context).format(Date(expiredCredit.expiredAtEpochMs))
                Text(
                    text = "Os créditos expiraram às $formattedTime.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(onClick = onRefresh) {
                Text("Atualizar estado")
            }
        }
    }
}

@Composable
private fun AppField(
    label: String,
    selection: SelectedApp?,
    onSelect: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = selection?.let { "${it.appName}\n${it.packageName}" } ?: "Nenhum app selecionado",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (selection != null && !selection.isInstalled) {
                Text(
                    text = "Este app não está mais instalado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSelect) {
                    Text("Selecionar")
                }
                TextButton(
                    onClick = onClear,
                    enabled = selection != null,
                ) {
                    Text("Limpar")
                }
            }
        }
    }
}

@Composable
private fun ReadinessRow(
    label: String,
    ready: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        StatusChip(if (ready) "Pronto" else "Necessário")
    }
}

@Composable
private fun StatusChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun heroLabel(
    uiState: MainUiState,
    context: Context,
): String {
    return when (uiState.heroStatus) {
        HeroStatus.NOT_CONFIGURED -> "Não configurado"
        HeroStatus.CONFIGURED_MISSING_PERMISSIONS -> "Faltam permissões"
        HeroStatus.READY -> "Pronto"
        HeroStatus.CHALLENGE_IN_PROGRESS -> "Desafio em andamento"
        HeroStatus.UNLOCKED -> {
            val unlockGrant = uiState.unlockGrant ?: return "Créditos ativos"
            "Créditos até ${DateFormat.getTimeFormat(context).format(Date(unlockGrant.expiresAtEpochMs))}"
        }
        HeroStatus.CREDITS_EXHAUSTED -> "Créditos esgotados"
        HeroStatus.SAVED_RULE_INVALID -> "Regra inválida"
    }
}

private fun heroDescription(uiState: MainUiState): String {
    return when (uiState.heroStatus) {
        HeroStatus.NOT_CONFIGURED ->
            "Escolha um app bloqueado, um app de controle, a duração do desafio e a janela de desbloqueio."
        HeroStatus.CONFIGURED_MISSING_PERMISSIONS ->
            "A regra foi salva, mas o app ainda precisa de Acessibilidade e/ou Usage Access para funcionar por completo."
        HeroStatus.READY ->
            "A regra salva está pronta para interceptar ${uiState.savedRule?.blockedAppName ?: "o app bloqueado"}."
        HeroStatus.CHALLENGE_IN_PROGRESS ->
            "Um desafio está ativo. O tempo no app de controle será usado para liberar novos créditos temporários."
        HeroStatus.UNLOCKED ->
            "O app bloqueado pode ser aberto normalmente enquanto ainda houver créditos ativos."
        HeroStatus.CREDITS_EXHAUSTED ->
            "A janela de desbloqueio terminou e os créditos acabaram. Conclua um novo desafio para ganhar mais tempo."
        HeroStatus.SAVED_RULE_INVALID ->
            "A regra salva não pode ser aplicada. Revise os aplicativos escolhidos e salve novamente."
    }
}

private fun validationMessages(result: RuleValidationResult): List<String> {
    return buildList {
        if (result.hasIssue(RuleValidationIssue.BLOCKED_APP_REQUIRED)) {
            add("Selecione o app bloqueado.")
        }
        if (result.hasIssue(RuleValidationIssue.CONTROL_APP_REQUIRED)) {
            add("Selecione o app de controle.")
        }
        if (result.hasIssue(RuleValidationIssue.APPS_MUST_DIFFER)) {
            add("O app bloqueado e o app de controle precisam ser diferentes.")
        }
        if (result.hasIssue(RuleValidationIssue.REQUIRED_SECONDS_OUT_OF_RANGE)) {
            add("A duração do desafio deve ficar entre 10 e 300 segundos.")
        }
        if (result.hasIssue(RuleValidationIssue.UNLOCK_WINDOW_OUT_OF_RANGE)) {
            add("A janela de desbloqueio deve ficar entre 1 e 60 minutos.")
        }
        if (result.hasIssue(RuleValidationIssue.BLOCKED_APP_NOT_INSTALLED)) {
            add("O app bloqueado selecionado não está mais instalado.")
        }
        if (result.hasIssue(RuleValidationIssue.CONTROL_APP_NOT_INSTALLED)) {
            add("O app de controle selecionado não está mais instalado.")
        }
    }
}
