package com.larissa.socialcontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.larissa.socialcontrol.ui.AppPickerDialog
import com.larissa.socialcontrol.ui.theme.IntentLockTheme
import java.util.Date
import kotlin.math.roundToInt

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
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background,
        ),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { HeroCard(uiState = uiState) }
            item {
                ReadinessCard(
                    readiness = uiState.readiness,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                )
            }
            item {
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
            }
            item {
                CurrentActivityCard(
                    uiState = uiState,
                    onRefresh = onRefresh,
                )
            }
        }
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
    val palette = heroPalette(uiState.heroStatus)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            palette.glow.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            palette.soft.copy(alpha = 0.24f),
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusChip(
                            label = "Acesso intencional",
                            containerColor = palette.soft,
                            contentColor = palette.strong,
                        )
                        Text(
                            text = "Controle o impulso antes de abrir o app.",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    StatusChip(
                        label = heroLabel(uiState, context),
                        containerColor = palette.strong,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text = heroDescription(uiState),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HeroHighlights(uiState = uiState)
            }
        }
    }
}

@Composable
private fun HeroHighlights(uiState: MainUiState) {
    val highlightItems = buildList {
        uiState.draft.blockedApp?.appName?.let { add("Bloqueado" to it) }
        uiState.draft.controlApp?.appName?.let { add("Controle" to it) }
        add("Desafio" to formatChallengeLabel(uiState.draft.requiredSecondsInput))
        add("Janela" to formatWindowLabel(uiState.draft.unlockWindowMinutesInput))
    }

    BoxWithConstraints {
        val useColumn = maxWidth < 420.dp

        if (useColumn) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                highlightItems.forEach { (label, value) ->
                    HeroHighlightCard(
                        modifier = Modifier.fillMaxWidth(),
                        label = label,
                        value = value,
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                highlightItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowItems.forEach { (label, value) ->
                            HeroHighlightCard(
                                modifier = Modifier.weight(1f),
                                label = label,
                                value = value,
                            )
                        }
                        if (rowItems.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroHighlightCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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

@Composable
private fun ReadinessCard(
    readiness: PermissionReadiness,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
) {
    AppSectionCard(
        title = "Sistema pronto para intervir",
        description = "As duas permissões abaixo precisam estar ativas para o bloqueio funcionar com consistência.",
    ) {
        BoxWithConstraints {
            val stackVertically = maxWidth < 460.dp

            if (stackVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PermissionTile(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Acessibilidade",
                        description = "Detecta quando o app bloqueado foi aberto.",
                        ready = readiness.accessibilityEnabled,
                    )
                    PermissionTile(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Usage Access",
                        description = "Mede o tempo cumprido no app de controle.",
                        ready = readiness.usageAccessEnabled,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PermissionTile(
                        modifier = Modifier.weight(1f),
                        title = "Acessibilidade",
                        description = "Detecta quando o app bloqueado foi aberto.",
                        ready = readiness.accessibilityEnabled,
                    )
                    PermissionTile(
                        modifier = Modifier.weight(1f),
                        title = "Usage Access",
                        description = "Mede o tempo cumprido no app de controle.",
                        ready = readiness.usageAccessEnabled,
                    )
                }
            }
        }
        BoxWithConstraints {
            val stackVertically = maxWidth < 460.dp

            if (stackVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenAccessibilitySettings,
                    ) {
                        Text("Abrir acessibilidade")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenUsageAccessSettings,
                    ) {
                        Text("Abrir Usage Access")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAccessibilitySettings,
                    ) {
                        Text("Abrir acessibilidade")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onOpenUsageAccessSettings,
                    ) {
                        Text("Abrir Usage Access")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionTile(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    ready: Boolean,
) {
    val containerColor = if (ready) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val contentColor = if (ready) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

    Surface(
        modifier = modifier,
        color = containerColor,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusChip(
                label = if (ready) "Ativo" else "Pendente",
                containerColor = contentColor.copy(alpha = 0.12f),
                contentColor = contentColor,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.85f),
            )
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
    AppSectionCard(
        title = "Monte sua regra",
        description = "Escolha um app que pede intenção antes de abrir e um app que serve como desafio para recuperar acesso.",
    ) {
        AppField(
            label = "App bloqueado",
            supportingText = "O app interceptado pelo IntentLock.",
            selection = uiState.draft.blockedApp,
            onSelect = onBlockedAppClick,
            onClear = onBlockedAppCleared,
        )
        AppField(
            label = "App de controle",
            supportingText = "O app onde a pessoa precisa permanecer antes de liberar crédito.",
            selection = uiState.draft.controlApp,
            onSelect = onControlAppClick,
            onClear = onControlAppCleared,
        )
        BoxWithConstraints {
            val stackVertically = maxWidth < 460.dp

            if (stackVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumericField(
                        value = uiState.draft.requiredSecondsInput,
                        onValueChange = onRequiredSecondsChanged,
                        label = "Duração do desafio",
                        supportingText = "Entre 10 e 300 segundos.",
                    )
                    NumericField(
                        value = uiState.draft.unlockWindowMinutesInput,
                        onValueChange = onUnlockWindowMinutesChanged,
                        label = "Janela de desbloqueio",
                        supportingText = "Entre 1 e 60 minutos.",
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumericField(
                        modifier = Modifier.weight(1f),
                        value = uiState.draft.requiredSecondsInput,
                        onValueChange = onRequiredSecondsChanged,
                        label = "Duração do desafio",
                        supportingText = "10 a 300 segundos",
                    )
                    NumericField(
                        modifier = Modifier.weight(1f),
                        value = uiState.draft.unlockWindowMinutesInput,
                        onValueChange = onUnlockWindowMinutesChanged,
                        label = "Janela de desbloqueio",
                        supportingText = "1 a 60 minutos",
                    )
                }
            }
        }
        ValidationPanel(
            draftValidation = uiState.draftValidation,
            savedRuleValidation = uiState.savedRuleValidation,
            hasSavedRule = uiState.savedRule != null,
        )
        BoxWithConstraints {
            val stackVertically = maxWidth < 460.dp

            if (stackVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSaveRule,
                        enabled = uiState.isSaveEnabled,
                    ) {
                        Text(if (uiState.savedRule == null) "Salvar regra" else "Atualizar regra")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClearRule,
                        enabled = uiState.canClearRule,
                    ) {
                        Text("Limpar")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onSaveRule,
                        enabled = uiState.isSaveEnabled,
                    ) {
                        Text(if (uiState.savedRule == null) "Salvar regra" else "Atualizar regra")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onClearRule,
                        enabled = uiState.canClearRule,
                    ) {
                        Text("Limpar")
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { Text(supportingText) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun ValidationPanel(
    draftValidation: RuleValidationResult,
    savedRuleValidation: RuleValidationResult,
    hasSavedRule: Boolean,
) {
    val messages = validationMessages(draftValidation).toMutableList()
    if (hasSavedRule && !savedRuleValidation.isValid) {
        messages += "A regra salva atual ficou inválida e não será usada até ser corrigida."
    }

    if (messages.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = "A configuração está coerente. Salve para aplicar a nova regra.",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        return
    }

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Ajustes necessários",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            messages.forEach { message ->
                Text(
                    text = "• $message",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
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

    AppSectionCard(
        title = "Estado atual",
        description = "Acompanhe o progresso do desafio, créditos ativos e o que acontece a seguir.",
        action = {
            FilledTonalButton(onClick = onRefresh) {
                Text("Atualizar")
            }
        },
    ) {
        when {
            uiState.session != null -> SessionPanel(
                session = uiState.session,
                progress = uiState.progress,
                savedRule = uiState.savedRule,
            )

            uiState.unlockGrant != null -> UnlockGrantPanel(
                unlockGrant = uiState.unlockGrant,
                savedRule = uiState.savedRule,
                context = context,
            )

            uiState.expiredCredit != null -> ExpiredCreditPanel(
                expiredCredit = uiState.expiredCredit,
                context = context,
            )

            else -> IdlePanel()
        }
    }
}

@Composable
private fun SessionPanel(
    session: ChallengeSession,
    progress: ChallengeProgress?,
    savedRule: InterventionRule?,
) {
    val title = savedRule?.controlAppName ?: session.controlPackage

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusChip(
                label = "Desafio em andamento",
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            )
            Text(
                text = "Permaneça em $title para acumular tempo e liberar novo acesso.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            when {
                progress == null -> {
                    Text(
                        text = "Aguardando leitura inicial de progresso.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }

                !progress.hasUsageAccess -> {
                    Text(
                        text = "Usage Access ausente. Sem essa permissão, o tempo do desafio não pode ser medido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }

                else -> {
                    val fraction = (progress.trackedSeconds.toFloat() / progress.requiredSeconds.toFloat())
                        .coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${progress.trackedSeconds}s de ${progress.requiredSeconds}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "${(fraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = if (session.completedAtEpochMs != null || progress.isComplete) {
                            "Desafio concluído. O crédito deve ser gerado automaticamente."
                        } else {
                            "Continue até completar o tempo mínimo."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlockGrantPanel(
    unlockGrant: UnlockGrant,
    savedRule: InterventionRule?,
    context: Context,
) {
    val remainingSeconds = ((unlockGrant.expiresAtEpochMs - System.currentTimeMillis()) / 1_000L)
        .coerceAtLeast(0L)
    val totalSeconds = ((savedRule?.unlockWindowMinutes ?: 1) * 60).toFloat()
    val fraction = (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    val formattedTime = DateFormat.getTimeFormat(context).format(Date(unlockGrant.expiresAtEpochMs))
    val blockedLabel = savedRule?.blockedAppName ?: unlockGrant.blockedPackage

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusChip(
                label = "Crédito ativo",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = "$blockedLabel está liberado até $formattedTime.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
            )
            Text(
                text = "Restam aproximadamente ${formatRemainingTime(remainingSeconds)} para abrir o app sem novo desafio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ExpiredCreditPanel(
    expiredCredit: ExpiredUnlockCredit,
    context: Context,
) {
    val formattedTime = DateFormat.getTimeFormat(context).format(Date(expiredCredit.expiredAtEpochMs))

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusChip(
                label = "Créditos encerrados",
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            )
            Text(
                text = "O último período de acesso terminou às $formattedTime.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = "Abra o app de controle novamente para cumprir outro desafio e recuperar acesso temporário.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun IdlePanel() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Nenhum desafio ou crédito ativo agora.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Quando o app bloqueado for interceptado, o IntentLock vai direcionar a pessoa para o fluxo de intenção antes de liberar o acesso.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppField(
    label: String,
    supportingText: String,
    selection: SelectedApp?,
    onSelect: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selection != null) {
                    StatusChip(
                        label = "Selecionado",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            if (selection == null) {
                Text(
                    text = "Nenhum app selecionado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = selection.appName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = selection.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!selection.isInstalled) {
                    Text(
                        text = "Este app não está mais instalado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            BoxWithConstraints {
                val stackVertically = maxWidth < 460.dp

                if (stackVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onSelect,
                        ) {
                            Text(if (selection == null) "Selecionar app" else "Trocar app")
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onClear,
                            enabled = selection != null,
                        ) {
                            Text("Limpar")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = onSelect) {
                            Text(if (selection == null) "Selecionar app" else "Trocar app")
                        }
                        OutlinedButton(
                            onClick = onClear,
                            enabled = selection != null,
                        ) {
                            Text("Limpar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSectionCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                action?.invoke()
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            content()
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private data class HeroPalette(
    val strong: Color,
    val soft: Color,
    val glow: Color,
)

@Composable
private fun heroPalette(status: HeroStatus): HeroPalette {
    return when (status) {
        HeroStatus.NOT_CONFIGURED -> HeroPalette(
            strong = MaterialTheme.colorScheme.secondary,
            soft = MaterialTheme.colorScheme.secondaryContainer,
            glow = MaterialTheme.colorScheme.secondary,
        )
        HeroStatus.CONFIGURED_MISSING_PERMISSIONS,
        HeroStatus.SAVED_RULE_INVALID,
        HeroStatus.CREDITS_EXHAUSTED,
        -> HeroPalette(
            strong = MaterialTheme.colorScheme.tertiary,
            soft = MaterialTheme.colorScheme.tertiaryContainer,
            glow = MaterialTheme.colorScheme.tertiary,
        )
        HeroStatus.CHALLENGE_IN_PROGRESS -> HeroPalette(
            strong = MaterialTheme.colorScheme.secondary,
            soft = MaterialTheme.colorScheme.secondaryContainer,
            glow = MaterialTheme.colorScheme.primary,
        )
        HeroStatus.UNLOCKED,
        HeroStatus.READY,
        -> HeroPalette(
            strong = MaterialTheme.colorScheme.primary,
            soft = MaterialTheme.colorScheme.primaryContainer,
            glow = MaterialTheme.colorScheme.primary,
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
            "Escolha um app bloqueado, um app de controle, o tempo mínimo do desafio e a janela de acesso temporário."
        HeroStatus.CONFIGURED_MISSING_PERMISSIONS ->
            "A regra está salva, mas o app ainda precisa das permissões corretas para observar uso e interceptar a abertura do app bloqueado."
        HeroStatus.READY ->
            "Tudo está pronto para interceptar ${uiState.savedRule?.blockedAppName ?: "o app bloqueado"} e pedir um momento de intenção antes do acesso."
        HeroStatus.CHALLENGE_IN_PROGRESS ->
            "Há um desafio rodando agora. O tempo passado no app de controle será convertido em novo crédito temporário."
        HeroStatus.UNLOCKED ->
            "O app bloqueado pode ser aberto normalmente enquanto ainda houver crédito disponível."
        HeroStatus.CREDITS_EXHAUSTED ->
            "O período de acesso terminou. Um novo desafio será necessário para recuperar crédito."
        HeroStatus.SAVED_RULE_INVALID ->
            "A regra salva não pode ser aplicada neste momento. Revise os apps escolhidos e salve novamente."
    }
}

private fun validationMessages(result: RuleValidationResult): List<String> {
    return buildList {
        if (result.hasIssue(RuleValidationIssue.BLOCKED_APP_REQUIRED)) add("Selecione o app bloqueado.")
        if (result.hasIssue(RuleValidationIssue.CONTROL_APP_REQUIRED)) add("Selecione o app de controle.")
        if (result.hasIssue(RuleValidationIssue.APPS_MUST_DIFFER)) add("O app bloqueado e o app de controle precisam ser diferentes.")
        if (result.hasIssue(RuleValidationIssue.REQUIRED_SECONDS_OUT_OF_RANGE)) add("A duração do desafio deve ficar entre 10 e 300 segundos.")
        if (result.hasIssue(RuleValidationIssue.UNLOCK_WINDOW_OUT_OF_RANGE)) add("A janela de desbloqueio deve ficar entre 1 e 60 minutos.")
        if (result.hasIssue(RuleValidationIssue.BLOCKED_APP_NOT_INSTALLED)) add("O app bloqueado selecionado não está mais instalado.")
        if (result.hasIssue(RuleValidationIssue.CONTROL_APP_NOT_INSTALLED)) add("O app de controle selecionado não está mais instalado.")
    }
}

private fun formatChallengeLabel(value: String): String {
    val seconds = value.toIntOrNull() ?: return "Defina o tempo"
    return "$seconds s"
}

private fun formatWindowLabel(value: String): String {
    val minutes = value.toIntOrNull() ?: return "Defina a janela"
    return if (minutes == 1) "1 min" else "$minutes min"
}

private fun formatRemainingTime(seconds: Long): String {
    if (seconds < 60) return "${seconds}s"
    val minutes = seconds / 60
    val remainder = seconds % 60
    return if (remainder == 0L) "$minutes min" else "$minutes min ${remainder}s"
}
