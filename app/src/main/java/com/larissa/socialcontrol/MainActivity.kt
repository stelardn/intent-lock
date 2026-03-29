package com.larissa.socialcontrol

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.larissa.socialcontrol.ui.AppIcon
import com.larissa.socialcontrol.ui.AppPickerDialog
import com.larissa.socialcontrol.ui.theme.IntentLockTheme
import java.util.Date

private enum class MainDestination {
    HOME,
    RULES,
    SYSTEM,
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IntentLockTheme {
                MainScreen(
                    uiState = viewModel.uiState,
                    onStartCreatingRule = viewModel::startCreatingRule,
                    onStartEditingRule = viewModel::startEditingRule,
                    onCancelEditing = viewModel::cancelEditing,
                    onBlockedAppSelected = viewModel::onBlockedAppSelected,
                    onBlockedAppCleared = viewModel::onBlockedAppCleared,
                    onControlAppSelected = viewModel::onControlAppSelected,
                    onControlAppCleared = viewModel::onControlAppCleared,
                    onRequiredSecondsChanged = viewModel::onRequiredSecondsChanged,
                    onUnlockWindowMinutesChanged = viewModel::onUnlockWindowMinutesChanged,
                    onRuleEnabledChanged = viewModel::onRuleEnabledChanged,
                    onSaveRule = viewModel::saveRule,
                    onDeleteRule = viewModel::deleteRule,
                    onToggleRuleEnabled = viewModel::toggleRuleEnabled,
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

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}

@Composable
private fun MainScreen(
    uiState: MainUiState,
    onStartCreatingRule: () -> Unit,
    onStartEditingRule: (String) -> Unit,
    onCancelEditing: () -> Unit,
    onBlockedAppSelected: (InstalledAppInfo) -> Unit,
    onBlockedAppCleared: () -> Unit,
    onControlAppSelected: (InstalledAppInfo) -> Unit,
    onControlAppCleared: () -> Unit,
    onRequiredSecondsChanged: (String) -> Unit,
    onUnlockWindowMinutesChanged: (String) -> Unit,
    onRuleEnabledChanged: (Boolean) -> Unit,
    onSaveRule: () -> Unit,
    onDeleteRule: (String) -> Unit,
    onToggleRuleEnabled: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
) {
    var destinationName by rememberSaveable { mutableStateOf(MainDestination.HOME.name) }
    var pickerTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val destination = remember(destinationName) { MainDestination.valueOf(destinationName) }

    val homeListState = rememberLazyListState()
    val rulesListState = rememberLazyListState()
    val editorListState = rememberLazyListState()
    val systemListState = rememberLazyListState()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
            MaterialTheme.colorScheme.background,
        ),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == destination,
                        onClick = { destinationName = item.name },
                        icon = {
                            when (item) {
                                MainDestination.HOME -> androidx.compose.material3.Icon(
                                    imageVector = Icons.Filled.Home,
                                    contentDescription = null,
                                )
                                MainDestination.RULES -> androidx.compose.material3.Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                )
                                MainDestination.SYSTEM -> androidx.compose.material3.Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                )
                            }
                        },
                        label = {
                            Text(
                                when (item) {
                                    MainDestination.HOME -> "Início"
                                    MainDestination.RULES -> "Regras"
                                    MainDestination.SYSTEM -> "Sistema"
                                },
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding),
        ) {
            when (destination) {
                MainDestination.HOME -> HomeScreen(
                    uiState = uiState,
                    listState = homeListState,
                    onCreateRule = {
                        destinationName = MainDestination.RULES.name
                        onStartCreatingRule()
                    },
                    onOpenRules = { destinationName = MainDestination.RULES.name },
                    onOpenSystem = { destinationName = MainDestination.SYSTEM.name },
                )

                MainDestination.RULES -> {
                    val editor = uiState.editor
                    if (editor == null) {
                        RulesScreen(
                            uiState = uiState,
                            listState = rulesListState,
                            onCreateRule = onStartCreatingRule,
                            onEditRule = onStartEditingRule,
                            onDeleteRule = onDeleteRule,
                            onToggleRuleEnabled = onToggleRuleEnabled,
                        )
                    } else {
                        RuleEditorScreen(
                            editor = editor,
                            listState = editorListState,
                            onBlockedAppClick = { pickerTarget = "blocked" },
                            onBlockedAppCleared = onBlockedAppCleared,
                            onControlAppClick = { pickerTarget = "control" },
                            onControlAppCleared = onControlAppCleared,
                            onRequiredSecondsChanged = onRequiredSecondsChanged,
                            onUnlockWindowMinutesChanged = onUnlockWindowMinutesChanged,
                            onRuleEnabledChanged = onRuleEnabledChanged,
                            onSaveRule = onSaveRule,
                            onCancel = onCancelEditing,
                            onDeleteRule = {
                                editor.editingRuleId?.let(onDeleteRule)
                            },
                        )
                    }
                }

                MainDestination.SYSTEM -> SystemScreen(
                    uiState = uiState,
                    listState = systemListState,
                    onRefresh = onRefresh,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                )
            }
        }
    }

    val editor = uiState.editor
    when {
        editor == null -> Unit
        pickerTarget == "blocked" -> AppPickerDialog(
            title = "Selecionar app bloqueado",
            apps = uiState.installedApps,
            selectedPackageName = editor.blockedApp?.packageName,
            disabledPackageName = editor.controlApp?.packageName,
            onDismissRequest = { pickerTarget = null },
            onSelected = {
                onBlockedAppSelected(it)
                pickerTarget = null
            },
        )

        pickerTarget == "control" -> AppPickerDialog(
            title = "Selecionar app de controle",
            apps = uiState.installedApps,
            selectedPackageName = editor.controlApp?.packageName,
            disabledPackageName = editor.blockedApp?.packageName,
            onDismissRequest = { pickerTarget = null },
            onSelected = {
                onControlAppSelected(it)
                pickerTarget = null
            },
        )
    }
}

@Composable
private fun HomeScreen(
    uiState: MainUiState,
    listState: LazyListState,
    onCreateRule: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenSystem: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DashboardHeroCard(
                uiState = uiState,
                onCreateRule = onCreateRule,
                onOpenRules = onOpenRules,
                onOpenSystem = onOpenSystem,
            )
        }

        if (uiState.dashboard.activeCredits.isNotEmpty()) {
            item { CreditsCard(credits = uiState.dashboard.activeCredits) }
        }

        if (uiState.dashboard.protectedApps.isNotEmpty()) {
            item {
                ProtectedAppsCard(
                    apps = uiState.dashboard.protectedApps,
                    onOpenRules = onOpenRules,
                )
            }
        }

        if (uiState.dashboard.activeChallenges.isNotEmpty()) {
            item { ChallengeCard(challenges = uiState.dashboard.activeChallenges) }
        }

        if (uiState.dashboard.alerts.isNotEmpty()) {
            item { AlertsCard(alerts = uiState.dashboard.alerts) }
        }
    }
}

@Composable
private fun RulesScreen(
    uiState: MainUiState,
    listState: LazyListState,
    onCreateRule: () -> Unit,
    onEditRule: (String) -> Unit,
    onDeleteRule: (String) -> Unit,
    onToggleRuleEnabled: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeaderCard(
                title = "Regras",
                description = if (uiState.rules.isEmpty()) {
                    "Você ainda não criou nenhuma regra."
                } else {
                    "Defina quais apps exigem intenção antes do uso."
                },
            )
        }
        item {
            PrimaryActionCard(
                title = "Nova regra",
                description = "Adicione um app bloqueado e um app de controle.",
                actionLabel = "Criar regra",
                onAction = onCreateRule,
            )
        }

        if (uiState.rules.isEmpty()) {
            item { EmptyRulesCard(onCreateRule = onCreateRule) }
        } else {
            items(uiState.rules, key = { it.ruleId }) { rule ->
                RuleSummaryCard(
                    rule = rule,
                    onEditRule = { onEditRule(rule.ruleId) },
                    onDeleteRule = { onDeleteRule(rule.ruleId) },
                    onToggleRuleEnabled = { onToggleRuleEnabled(rule.ruleId) },
                )
            }
        }
    }
}

@Composable
private fun RuleEditorScreen(
    editor: RuleEditorUiState,
    listState: LazyListState,
    onBlockedAppClick: () -> Unit,
    onBlockedAppCleared: () -> Unit,
    onControlAppClick: () -> Unit,
    onControlAppCleared: () -> Unit,
    onRequiredSecondsChanged: (String) -> Unit,
    onUnlockWindowMinutesChanged: (String) -> Unit,
    onRuleEnabledChanged: (Boolean) -> Unit,
    onSaveRule: () -> Unit,
    onCancel: () -> Unit,
    onDeleteRule: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeaderCard(
                title = if (editor.isCreating) "Nova regra" else "Editar regra",
                description = "Configure um app protegido e o desafio necessário para recuperar acesso.",
            )
        }
        item {
            AppSectionCard(
                title = "Configuração da regra",
                description = "Cada app bloqueado pode existir em apenas uma regra.",
            ) {
                AppField(
                    label = "App bloqueado",
                    supportingText = "App que será interceptado pelo IntentLock.",
                    selection = editor.blockedApp,
                    onSelect = onBlockedAppClick,
                    onClear = onBlockedAppCleared,
                )
                AppField(
                    label = "App de controle",
                    supportingText = "App usado para cumprir o desafio.",
                    selection = editor.controlApp,
                    onSelect = onControlAppClick,
                    onClear = onControlAppCleared,
                )
                BoxWithConstraints {
                    val stackVertically = maxWidth < 460.dp

                    if (stackVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumericField(
                                value = editor.requiredSecondsInput,
                                onValueChange = onRequiredSecondsChanged,
                                label = "Duração do desafio",
                                supportingText = "Entre 10 e 300 segundos.",
                            )
                            NumericField(
                                value = editor.unlockWindowMinutesInput,
                                onValueChange = onUnlockWindowMinutesChanged,
                                label = "Janela de desbloqueio",
                                supportingText = "Entre 1 e 60 minutos.",
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumericField(
                                modifier = Modifier.weight(1f),
                                value = editor.requiredSecondsInput,
                                onValueChange = onRequiredSecondsChanged,
                                label = "Duração do desafio",
                                supportingText = "10 a 300 segundos",
                            )
                            NumericField(
                                modifier = Modifier.weight(1f),
                                value = editor.unlockWindowMinutesInput,
                                onValueChange = onUnlockWindowMinutesChanged,
                                label = "Janela de desbloqueio",
                                supportingText = "1 a 60 minutos",
                            )
                        }
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Regra ativa",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Quando desativada, a regra continua salva mas deixa de proteger o app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = editor.isEnabled,
                            onCheckedChange = onRuleEnabledChanged,
                        )
                    }
                }
                ValidationPanel(validation = editor.validation)
                EditorActions(
                    editor = editor,
                    onSaveRule = onSaveRule,
                    onCancel = onCancel,
                    onDeleteRule = onDeleteRule,
                )
            }
        }
    }
}

@Composable
private fun SystemScreen(
    uiState: MainUiState,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeaderCard(
                title = "Permissões",
                description = systemSubtitle(uiState.readiness),
            )
        }
        item {
            PrimaryActionCard(
                title = "Atualizar status",
                description = "Releia o estado atual das permissões do Android.",
                actionLabel = "Atualizar",
                onAction = onRefresh,
            )
        }
        item {
            PermissionCard(
                title = "Acessibilidade",
                description = "Detecta quando um app protegido foi aberto.",
                enabled = uiState.readiness.accessibilityEnabled,
                actionLabel = "Abrir ajustes",
                onAction = onOpenAccessibilitySettings,
            )
        }
        item {
            PermissionCard(
                title = "Dados de uso",
                description = "Mede o tempo no app de controle.",
                enabled = uiState.readiness.usageAccessEnabled,
                actionLabel = "Abrir ajustes",
                onAction = onOpenUsageAccessSettings,
            )
        }
    }
}

@Composable
private fun DashboardHeroCard(
    uiState: MainUiState,
    onCreateRule: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenSystem: () -> Unit,
) {
    val palette = heroPalette(uiState.heroStatus)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            palette.soft.copy(alpha = 0.32f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            palette.glow.copy(alpha = 0.18f),
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.large,
                )
                {
                    Text(
                        text = "IntentLock",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.strong,
                    )
                }
                Text(
                    text = "Intenção antes da abertura.",
                    style = screenHeaderTextStyle(),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = heroDescription(uiState),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    color = palette.strong.copy(alpha = 0.14f),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = heroLabel(uiState),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.strong,
                    )
                }
                HeroActions(
                    uiState = uiState,
                    onCreateRule = onCreateRule,
                    onOpenRules = onOpenRules,
                    onOpenSystem = onOpenSystem,
                )
            }
        }
    }
}

@Composable
private fun CreditsCard(credits: List<ActiveCreditUiState>) {
    val context = LocalContext.current

    AppSectionCard(
        title = "Créditos ativos",
        description = if (credits.size == 1) {
            "Há um app liberado no momento."
        } else {
            "${credits.size} apps estão com crédito ativo."
        },
    ) {
        if (credits.size == 1) {
            val credit = credits.first()
            val remainingSeconds = ((credit.expiresAtEpochMs - System.currentTimeMillis()) / 1_000L)
                .coerceAtLeast(0L)
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${credit.blockedAppName} liberado por mais ${formatRemainingTime(remainingSeconds)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Expira as ${DateFormat.getTimeFormat(context).format(Date(credit.expiresAtEpochMs))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${credits.size} apps com crédito ativo",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = credits.take(3).joinToString(" • ") { it.blockedAppName } +
                            credits.drop(3).takeIf { it.isNotEmpty() }?.let { " • +${it.size}" }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProtectedAppsCard(
    apps: List<ProtectedAppUiState>,
    onOpenRules: () -> Unit,
) {
    AppSectionCard(
        title = "Apps protegidos",
        description = "Apps com regra válida no momento.",
        modifier = Modifier.clickable(onClick = onOpenRules),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            apps.take(6).forEach { app ->
                SmallAppChip(app = app)
            }
            if (apps.size > 6) {
                SmallChip(label = "+${apps.size - 6}")
            }
        }
        Text(
            text = "Toque para ver ou editar as regras.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SmallAppChip(app: ProtectedAppUiState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                packageName = app.packageName,
                fallbackLabel = app.appName,
                size = 20.dp,
            )
            Text(
                text = app.appName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChallengeCard(challenges: List<ActiveChallengeUiState>) {
    AppSectionCard(
        title = "Desafio em andamento",
        description = if (challenges.size == 1) {
            "Há uma sessão ativa agora."
        } else {
            "${challenges.size} desafios estão em andamento."
        },
    ) {
        challenges.take(3).forEach { challenge ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "${challenge.controlAppName} para liberar ${challenge.blockedAppName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    if (challenge.hasUsageAccess) {
                        val progress = (
                            challenge.trackedSeconds.toFloat() /
                                challenge.requiredSeconds.toFloat()
                            ).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                        )
                        Text(
                            text = "${challenge.trackedSeconds}s de ${challenge.requiredSeconds}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    } else {
                        Text(
                            text = "Ative Dados de uso para medir o tempo do desafio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
        if (challenges.size > 3) {
            Text(
                text = "+${challenges.size - 3} desafio(s) ainda em andamento.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlertsCard(alerts: List<String>) {
    AppSectionCard(
        title = "Atenção",
        description = "Algumas configurações precisam de revisão.",
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                alerts.forEach { message ->
                    Text(
                        text = "• $message",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRulesCard(onCreateRule: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Você ainda não criou nenhuma regra",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Adicione um app bloqueado e um app de controle para começar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onCreateRule) {
                Text("Nova regra")
            }
        }
    }
}

@Composable
private fun PrimaryActionCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            val stackVertically = maxWidth < 560.dp

            if (stackVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onAction,
                    ) {
                        Text(actionLabel)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(onClick = onAction) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleSummaryCard(
    rule: RuleCardUiState,
    onEditRule: () -> Unit,
    onDeleteRule: () -> Unit,
    onToggleRuleEnabled: () -> Unit,
) {
    AppSectionCard(
        title = rule.blockedAppName,
        description = rule.blockedPackage,
        leadingContent = {
            AppIcon(
                packageName = rule.blockedPackage,
                fallbackLabel = rule.blockedAppName,
                size = 52.dp,
            )
        },
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppIcon(
                    packageName = rule.controlPackage,
                    fallbackLabel = rule.controlAppName,
                    size = 40.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "App de controle",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = rule.controlAppName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = rule.controlPackage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SmallChip(label = "Desafio: ${rule.requiredSeconds}s")
            SmallChip(label = "Janela: ${formatWindowLabel(rule.unlockWindowMinutes.toString())}")
        }
        StatusChip(
            label = ruleStatusLabel(rule.status),
            containerColor = statusContainerColor(rule.status),
            contentColor = statusContentColor(rule.status),
        )
        val messages = validationMessages(rule.validation)
        if (messages.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    messages.take(2).forEach { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
        BoxWithConstraints {
            val stackVertically = maxWidth < 520.dp

            if (stackVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onEditRule,
                    ) {
                        Text("Editar")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onToggleRuleEnabled,
                    ) {
                        Text(if (rule.isEnabled) "Desativar" else "Ativar")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDeleteRule,
                    ) {
                        Text("Excluir")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onEditRule,
                    ) {
                        Text("Editar")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onToggleRuleEnabled,
                    ) {
                        Text(if (rule.isEnabled) "Desativar" else "Ativar")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDeleteRule,
                    ) {
                        Text("Excluir")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    enabled: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    AppSectionCard(
        title = title,
        description = description,
        action = {
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel)
            }
        },
    ) {
        StatusChip(
            label = if (enabled) "Ativa" else "Pendente",
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun HeroActions(
    uiState: MainUiState,
    onCreateRule: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenSystem: () -> Unit,
) {
    BoxWithConstraints {
        val stackVertically = maxWidth < 460.dp
        val primaryAction = primaryHeroAction(uiState, onCreateRule, onOpenRules, onOpenSystem)

        if (stackVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = primaryAction,
                ) {
                    Text(primaryHeroActionLabel(uiState))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = primaryAction,
                ) {
                    Text(primaryHeroActionLabel(uiState))
                }
            }
        }
    }
}

private fun primaryHeroAction(
    uiState: MainUiState,
    onCreateRule: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenSystem: () -> Unit,
): () -> Unit {
    return when (uiState.heroStatus) {
        HeroStatus.NOT_CONFIGURED -> onCreateRule
        HeroStatus.MISSING_PERMISSIONS -> onOpenSystem
        HeroStatus.READY,
        HeroStatus.CHALLENGE_IN_PROGRESS,
        HeroStatus.CREDITS_ACTIVE,
        HeroStatus.RULES_WITH_PROBLEM,
        -> onOpenRules
    }
}

private fun primaryHeroActionLabel(uiState: MainUiState): String {
    return when (uiState.heroStatus) {
        HeroStatus.NOT_CONFIGURED -> "Nova regra"
        HeroStatus.MISSING_PERMISSIONS -> "Ver sistema"
        HeroStatus.READY,
        HeroStatus.CHALLENGE_IN_PROGRESS,
        HeroStatus.CREDITS_ACTIVE,
        HeroStatus.RULES_WITH_PROBLEM,
        -> "Ver regras"
    }
}

@Composable
private fun EditorActions(
    editor: RuleEditorUiState,
    onSaveRule: () -> Unit,
    onCancel: () -> Unit,
    onDeleteRule: () -> Unit,
) {
    BoxWithConstraints {
        val stackVertically = maxWidth < 520.dp

        if (stackVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSaveRule,
                    enabled = editor.isSaveEnabled,
                ) {
                    Text("Salvar regra")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCancel,
                ) {
                    Text("Cancelar")
                }
                if (!editor.isCreating) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDeleteRule,
                    ) {
                        Text("Excluir")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onSaveRule,
                    enabled = editor.isSaveEnabled,
                ) {
                    Text("Salvar regra")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onCancel,
                ) {
                    Text("Cancelar")
                }
                if (!editor.isCreating) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDeleteRule,
                    ) {
                        Text("Excluir")
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
private fun ValidationPanel(validation: RuleValidationResult) {
    val messages = validationMessages(validation)
    if (messages.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = "A configuração está coerente. Salve para aplicar a regra.",
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
                    SmallChip(label = "Selecionado")
                }
            }

            if (selection == null) {
                Text(
                    text = "Nenhum app selecionado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppIcon(
                        packageName = selection.packageName,
                        fallbackLabel = selection.appName,
                        size = 48.dp,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
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
                    }
                }
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
private fun ScreenHeaderCard(
    title: String,
    description: String,
    action: (@Composable () -> Unit)? = null,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val stackHeader = action != null && maxWidth < 560.dp

        if (stackHeader) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        style = screenHeaderTextStyle(),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                action?.invoke()
            }
        } else {
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
                        style = screenHeaderTextStyle(),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                action?.invoke()
            }
        }
    }
}

@Composable
private fun AppSectionCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BoxWithConstraints {
                val stackHeader = action != null && maxWidth < 560.dp

                if (stackHeader) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SectionHeaderText(
                            title = title,
                            description = description,
                            leadingContent = leadingContent,
                        )
                        action?.invoke()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                        ) {
                            SectionHeaderText(
                                title = title,
                                description = description,
                                leadingContent = leadingContent,
                            )
                        }
                        action?.invoke()
                    }
                }
            }
            if (showDivider) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            }
            content()
        }
    }
}

@Composable
private fun SectionHeaderText(
    title: String,
    description: String,
    leadingContent: (@Composable () -> Unit)?,
) {
    if (leadingContent == null) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = sectionHeaderTextStyle(),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            leadingContent()
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = sectionHeaderTextStyle(),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun screenHeaderTextStyle() =
    MaterialTheme.typography.headlineLarge.copy(
        fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
    )

@Composable
private fun sectionHeaderTextStyle() =
    MaterialTheme.typography.headlineSmall.copy(
        fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
    )

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

@Composable
private fun SmallChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        HeroStatus.MISSING_PERMISSIONS,
        HeroStatus.RULES_WITH_PROBLEM,
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
        HeroStatus.CREDITS_ACTIVE,
        HeroStatus.READY,
        -> HeroPalette(
            strong = MaterialTheme.colorScheme.primary,
            soft = MaterialTheme.colorScheme.primaryContainer,
            glow = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun heroLabel(uiState: MainUiState): String {
    return when (uiState.heroStatus) {
        HeroStatus.NOT_CONFIGURED -> "Nenhuma regra"
        HeroStatus.MISSING_PERMISSIONS -> "Faltam permissões"
        HeroStatus.READY -> "Pronto"
        HeroStatus.CHALLENGE_IN_PROGRESS -> "Desafio em andamento"
        HeroStatus.CREDITS_ACTIVE -> "Créditos ativos"
        HeroStatus.RULES_WITH_PROBLEM -> "Regras com problema"
    }
}

private fun heroDescription(uiState: MainUiState): String {
    return when (uiState.heroStatus) {
        HeroStatus.NOT_CONFIGURED -> "Crie regras para transformar a abertura de apps em uma decisão mais consciente."
        HeroStatus.MISSING_PERMISSIONS -> "O fluxo já está desenhado, mas o Android ainda precisa liberar as permissões do IntentLock."
        HeroStatus.READY -> "Seus apps protegidos agora pedem uma pausa breve antes do uso."
        HeroStatus.CHALLENGE_IN_PROGRESS -> "Há um desafio ativo agora. Continue no app de controle para recuperar acesso com intenção."
        HeroStatus.CREDITS_ACTIVE -> "Há acesso temporário liberado neste momento sem perder a lógica de intenção configurada."
        HeroStatus.RULES_WITH_PROBLEM -> "Algumas regras precisam de revisão para que a proteção continue clara e confiável."
    }
}

private fun systemSubtitle(readiness: PermissionReadiness): String {
    val pendingCount = listOf(readiness.accessibilityEnabled, readiness.usageAccessEnabled).count { !it }
    return when (pendingCount) {
        0 -> "Tudo pronto para o IntentLock funcionar."
        1 -> "1 permissão pendente."
        else -> "$pendingCount permissões pendentes."
    }
}

private fun validationMessages(result: RuleValidationResult): List<String> {
    return buildList {
        if (result.hasIssue(RuleValidationIssue.BLOCKED_APP_REQUIRED)) add("Selecione o app bloqueado.")
        if (result.hasIssue(RuleValidationIssue.CONTROL_APP_REQUIRED)) add("Selecione o app de controle.")
        if (result.hasIssue(RuleValidationIssue.BLOCKED_APP_ALREADY_USED)) {
            add("Este app já está protegido por outra regra.")
            add("Edite a regra existente ou escolha outro app bloqueado.")
        }
        if (result.hasIssue(RuleValidationIssue.APPS_MUST_DIFFER)) add("Os dois apps precisam ser diferentes.")
        if (result.hasIssue(RuleValidationIssue.REQUIRED_SECONDS_OUT_OF_RANGE)) add("A duração do desafio deve ficar entre 10 e 300 segundos.")
        if (result.hasIssue(RuleValidationIssue.UNLOCK_WINDOW_OUT_OF_RANGE)) add("A janela de desbloqueio deve ficar entre 1 e 60 minutos.")
        if (result.hasIssue(RuleValidationIssue.BLOCKED_APP_NOT_INSTALLED)) add("Este app bloqueado não está mais instalado.")
        if (result.hasIssue(RuleValidationIssue.CONTROL_APP_NOT_INSTALLED)) add("Este app de controle não está mais instalado.")
    }
}

private fun ruleStatusLabel(status: RuleCardStatus): String {
    return when (status) {
        RuleCardStatus.ACTIVE -> "Ativa"
        RuleCardStatus.INACTIVE -> "Inativa"
        RuleCardStatus.INVALID -> "Inválida"
        RuleCardStatus.PERMISSIONS_INCOMPLETE -> "Permissões incompletas"
    }
}

@Composable
private fun statusContainerColor(status: RuleCardStatus): Color {
    return when (status) {
        RuleCardStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
        RuleCardStatus.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
        RuleCardStatus.INVALID -> MaterialTheme.colorScheme.errorContainer
        RuleCardStatus.PERMISSIONS_INCOMPLETE -> MaterialTheme.colorScheme.tertiaryContainer
    }
}

@Composable
private fun statusContentColor(status: RuleCardStatus): Color {
    return when (status) {
        RuleCardStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
        RuleCardStatus.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
        RuleCardStatus.INVALID -> MaterialTheme.colorScheme.onErrorContainer
        RuleCardStatus.PERMISSIONS_INCOMPLETE -> MaterialTheme.colorScheme.onTertiaryContainer
    }
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
