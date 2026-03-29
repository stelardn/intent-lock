package com.larissa.socialcontrol.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.larissa.socialcontrol.InstalledAppInfo

@Composable
fun AppPickerDialog(
    title: String,
    apps: List<InstalledAppInfo>,
    selectedPackageName: String?,
    disabledPackageName: String?,
    onDismissRequest: () -> Unit,
    onSelected: (InstalledAppInfo) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(query, apps) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            apps
        } else {
            apps.filter { app ->
                app.label.contains(normalizedQuery, ignoreCase = true) ||
                    app.packageName.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar aplicativo") },
                    singleLine = true,
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName },
                    ) { app ->
                        val isDisabled = app.packageName == disabledPackageName
                        val isSelected = app.packageName == selectedPackageName
                        AppPickerRow(
                            app = app,
                            isSelected = isSelected,
                            isDisabled = isDisabled,
                            onClick = {
                                if (!isDisabled) {
                                    onSelected(app)
                                }
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    app: InstalledAppInfo,
    isSelected: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit,
) {
    val rowAlpha = if (isDisabled) 0.45f else 1f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clickable(enabled = !isDisabled, onClick = onClick),
        tonalElevation = if (isSelected) 3.dp else 0.dp,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(
                context = LocalContext.current,
                packageName = app.packageName,
                fallbackLabel = app.label,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (isDisabled) {
                    Text(
                        text = "Já selecionado no outro campo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (isSelected) {
                    Text(
                        text = "Selecionado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIcon(
    context: Context,
    packageName: String,
    fallbackLabel: String,
) {
    val imageBitmap = remember(packageName) {
        runCatching {
            context.packageManager
                .getApplicationIcon(packageName)
                .toBitmap(96, 96)
                .asImageBitmap()
        }.getOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
        )
        return
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = fallbackLabel.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
