package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppContainer
import com.example.sync.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    isArabic: Boolean,
    isDarkTheme: Boolean,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syncInfo by AppContainer.syncPreferences.syncInfo.collectAsState()
    var showSyncDialog by remember { mutableStateOf(false) }

    if (showSyncDialog) {
        SyncStatusDialog(
            syncInfo = syncInfo,
            isArabic = isArabic,
            onDismiss = { showSyncDialog = false }
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Logo / Symbol Accent
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LM",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sync status indicator chip
                val syncIcon = when (syncInfo.state) {
                    SyncState.SYNCING -> Icons.Default.Sync
                    SyncState.SYNCED -> Icons.Default.CloudDone
                    SyncState.OFFLINE -> Icons.Default.CloudOff
                    SyncState.NOT_CONFIGURED -> Icons.Default.CloudOff
                    SyncState.SIGNED_OUT -> Icons.Default.CloudOff
                    SyncState.ERROR -> Icons.Default.ErrorOutline
                    SyncState.IDLE -> Icons.Default.CloudDone
                }
                val syncLabel = when (syncInfo.state) {
                    SyncState.SYNCING -> if (isArabic) "مزامنة" else "Syncing"
                    SyncState.SYNCED -> if (isArabic) "متزامن" else "Synced"
                    SyncState.OFFLINE -> if (isArabic) "أوفلاين" else "Offline"
                    SyncState.NOT_CONFIGURED -> if (isArabic) "محلي" else "Local"
                    SyncState.SIGNED_OUT -> if (isArabic) "غير مسجّل" else "Signed out"
                    SyncState.ERROR -> if (isArabic) "خطأ" else "Error"
                    SyncState.IDLE -> if (isArabic) "جاهز" else "Ready"
                }

                AssistChip(
                    onClick = { showSyncDialog = true },
                    label = {
                        Text(
                            text = syncLabel,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = syncIcon,
                            contentDescription = "Sync Status",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (syncInfo.state) {
                            SyncState.SYNCED -> Color(0xFF1B5E20).copy(alpha = 0.12f)
                            SyncState.SYNCING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            SyncState.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier.testTag("sync_status_btn")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Language toggle pill
                AssistChip(
                    onClick = onToggleLanguage,
                    label = {
                        Text(
                            text = if (isArabic) "EN" else "عربي",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Switch Language",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("toggle_language_btn")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Theme toggle icon
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("toggle_theme_btn")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
