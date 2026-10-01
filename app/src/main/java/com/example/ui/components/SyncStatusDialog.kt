package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppContainer
import com.example.sync.SyncInfo
import com.example.sync.SyncState
import com.example.sync.SyncWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncStatusDialog(
    syncInfo: SyncInfo,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isSyncing = syncInfo.state == SyncState.SYNCING

    val statusTitle = when (syncInfo.state) {
        SyncState.IDLE -> if (isArabic) "جاهز للمزامنة" else "Ready to Sync"
        SyncState.SYNCING -> if (isArabic) "جارٍ المزامنة..." else "Syncing..."
        SyncState.SYNCED -> if (isArabic) "تمت المزامنة بنجاح" else "Synced"
        SyncState.OFFLINE -> if (isArabic) "غير متصل بالإنترنت" else "Offline"
        SyncState.NOT_CONFIGURED -> if (isArabic) "الوضع المحلي (أوفلاين أولاً)" else "Local Mode (Offline-First)"
        SyncState.SIGNED_OUT -> if (isArabic) "لم يتم تسجيل الدخول" else "Signed Out"
        SyncState.ERROR -> if (isArabic) "حدث خطأ أثناء المزامنة" else "Sync Error"
    }

    val statusIcon = when (syncInfo.state) {
        SyncState.SYNCING -> Icons.Default.Sync
        SyncState.SYNCED -> Icons.Default.CloudDone
        SyncState.OFFLINE -> Icons.Default.CloudOff
        SyncState.NOT_CONFIGURED -> Icons.Default.CloudOff
        SyncState.SIGNED_OUT -> Icons.Default.CloudOff
        SyncState.ERROR -> Icons.Default.ErrorOutline
        SyncState.IDLE -> Icons.Default.CloudDone
    }

    val statusColor = when (syncInfo.state) {
        SyncState.SYNCED -> Color(0xFF1B5E20)
        SyncState.SYNCING -> MaterialTheme.colorScheme.primary
        SyncState.OFFLINE -> Color(0xFFE65100)
        SyncState.NOT_CONFIGURED -> MaterialTheme.colorScheme.secondary
        SyncState.SIGNED_OUT -> MaterialTheme.colorScheme.secondary
        SyncState.ERROR -> MaterialTheme.colorScheme.error
        SyncState.IDLE -> MaterialTheme.colorScheme.primary
    }

    val lastSyncFormatted = if (syncInfo.lastSyncTime > 0) {
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault())
        sdf.format(Date(syncInfo.lastSyncTime))
    } else {
        if (isArabic) "لم تتم المزامنة بعد" else "Never"
    }

    val rotation = if (isSyncing) {
        val infiniteTransition = rememberInfiniteTransition(label = "spin_sync")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing)
            ),
            label = "spin_angle"
        )
        angle
    } else {
        0f
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(rotation)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isArabic) "مزامنة البيانات (Firebase)" else "Firebase Synchronization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Info block
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (!syncInfo.isConfigured) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (!syncInfo.isConfigured) {
                                    if (isArabic) {
                                        "بياناتك (المهام، العادات، البيانات، الاستبيانات) محفوظة بالكامل ومحمية في قاعدة بيانات الجهاز المحلية (Room). لتفعيل المزامنة السحابية عبر الأجهزة، يتطلب ذلك إضافة ملف google-services.json إلى مجلد app."
                                    } else {
                                        "All your data (Tasks, Habits, Personal Data, Surveys) is securely stored in your local Room database. To enable cross-device cloud sync with Cloud Firestore, provide google-services.json in the app/ folder."
                                    }
                                } else {
                                    if (isArabic) {
                                        "المزامنة مفعلة مع Cloud Firestore بنظام (أوفلاين أولاً). كل العمليات تسجل محلياً في Room أولاً ثم تُرفع تلقائياً عند توفر اتصال."
                                    } else {
                                        "Connected with Cloud Firestore (Offline-First). All changes write to local Room first and sync to the cloud in the background."
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail Items
                Text(
                    text = if (isArabic) "آخر مزامنة ناجحة:" else "Last Synced:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = lastSyncFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isArabic) "معرف التثبيت (جهاز أحادي المستخدم):" else "Installation ID (Single-User):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = syncInfo.installationId,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (syncInfo.errorMessage != null && syncInfo.state == SyncState.ERROR) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncInfo.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        AppContainer.syncManager.sync()
                    }
                },
                enabled = !isSyncing,
                modifier = Modifier.testTag("sync_now_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "مزامنة الآن" else "Sync Now"
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_sync_dialog_btn")
            ) {
                Text(text = if (isArabic) "إغلاق" else "Close")
            }
        }
    )
}
