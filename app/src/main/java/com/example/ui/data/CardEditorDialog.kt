package com.example.ui.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.model.Folder
import com.example.model.TextCard
import com.example.util.PasswordUtils
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private data class EditorSnapshot(
    val title: String,
    val content: String,
    val tags: List<String>,
    val folderId: String?,
    val isFavorite: Boolean,
    val isProtected: Boolean,
    val passwordHash: String?
)

private fun parseTags(input: String): List<String> =
    input.split(",", "،").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }

/**
 * Full-screen note editor (Google Keep style) with autosave.
 * - New card: created on the first non-empty input, never saved empty.
 * - Existing card: saved ~0.7s after the last change, when leaving the screen,
 *   and when the app goes to background.
 * - Protected cards ask for the password before the editor opens
 *   (for both viewing and editing).
 */
@Composable
fun CardEditorDialog(
    card: TextCard?,
    initialFolderId: String?,
    folders: List<Folder>,
    isArabic: Boolean,
    onSave: (card: TextCard, isNew: Boolean) -> Unit,
    onDelete: (cardId: String) -> Unit,
    onDeleteWithUndo: (card: TextCard) -> Unit,
    onClose: () -> Unit
) {
    val base = remember { card ?: TextCard(title = "", content = "", folderId = initialFolderId) }

    var title by remember { mutableStateOf(base.title) }
    var content by remember { mutableStateOf(base.content) }
    var tagsInput by remember { mutableStateOf(base.tags.joinToString(", ")) }
    var folderId by remember { mutableStateOf(base.folderId) }
    var isFavorite by remember { mutableStateOf(base.isFavorite) }
    var isProtected by remember { mutableStateOf(base.isPasswordProtected) }
    var passwordHash by remember { mutableStateOf(base.passwordHash) }

    var isUnlocked by remember { mutableStateOf(!base.isPasswordProtected) }
    var persisted by remember { mutableStateOf(card != null) }
    var deleted by remember { mutableStateOf(false) }
    var lastEdited by remember { mutableStateOf(if (card != null) base.updatedAt else 0L) }

    var showMove by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    fun snapshot() = EditorSnapshot(
        title = title.trim(),
        content = content.trim(),
        tags = parseTags(tagsInput),
        folderId = folderId,
        isFavorite = isFavorite,
        isProtected = isProtected,
        passwordHash = passwordHash
    )

    var lastSaved by remember {
        mutableStateOf<EditorSnapshot?>(
            if (card != null) EditorSnapshot(
                base.title.trim(), base.content.trim(), base.tags, base.folderId,
                base.isFavorite, base.isPasswordProtected, base.passwordHash
            ) else null
        )
    }

    val flush: () -> Unit = flush@{
        if (deleted || !isUnlocked) return@flush
        val snap = snapshot()
        if (snap == lastSaved) return@flush
        if (!persisted && snap.title.isBlank() && snap.content.isBlank()) return@flush

        val now = System.currentTimeMillis()
        onSave(
            base.copy(
                title = snap.title,
                content = snap.content,
                tags = snap.tags,
                folderId = snap.folderId,
                isFavorite = snap.isFavorite,
                isPasswordProtected = snap.isProtected,
                passwordHash = snap.passwordHash,
                updatedAt = now
            ),
            !persisted
        )
        persisted = true
        lastSaved = snap
        lastEdited = now
    }
    val currentFlush by rememberUpdatedState(flush)

    val close: () -> Unit = {
        val isBlank = title.isBlank() && content.isBlank()
        if (isUnlocked && card == null && persisted && isBlank && !deleted) {
            // A note that was cleared completely is discarded, like Keep does
            deleted = true
            onDelete(base.id)
        } else {
            currentFlush()
        }
        onClose()
    }

    // Delete from the menu: no confirmation, the list screen offers "Undo" instead
    val requestDelete: () -> Unit = {
        currentFlush()
        deleted = true
        val snap = snapshot()
        onDeleteWithUndo(
            base.copy(
                title = snap.title,
                content = snap.content,
                tags = snap.tags,
                folderId = snap.folderId,
                isFavorite = snap.isFavorite,
                isPasswordProtected = snap.isProtected,
                passwordHash = snap.passwordHash
            )
        )
        onClose()
    }

    // Debounced autosave
    LaunchedEffect(title, content, tagsInput, folderId, isFavorite, isProtected, passwordHash) {
        delay(700)
        currentFlush()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { currentFlush() }
    BackHandler { close() }

    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (card == null) {
            try { titleFocus.requestFocus() } catch (_: Exception) { }
        }
    }

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // ---------- Top bar ----------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = close,
                        modifier = Modifier.testTag("editor_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    if (isUnlocked) {
                        IconButton(
                            onClick = { isFavorite = !isFavorite },
                            modifier = Modifier.testTag("editor_favorite_btn")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (isArabic) "مفضلة" else "Favorite",
                                tint = if (isFavorite) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showLockDialog = true },
                            modifier = Modifier.testTag("editor_lock_btn")
                        ) {
                            Icon(
                                imageVector = if (isProtected) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isArabic) "حماية بكلمة مرور" else "Password protection",
                                tint = if (isProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.testTag("editor_menu_btn")
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = if (isArabic) "المزيد" else "More",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isArabic) "نقل إلى مجلد" else "Move to folder") },
                                    leadingIcon = {
                                        Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        showMove = true
                                    }
                                )
                                if (persisted) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isArabic) "حذف البطاقة" else "Delete card",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            requestDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isUnlocked) {
                    UnlockContent(
                        isArabic = isArabic,
                        passwordHash = base.passwordHash,
                        onUnlocked = { isUnlocked = true },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // ---------- Body ----------
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = {
                                Text(
                                    if (isArabic) "العنوان" else "Title",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            },
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 3,
                            colors = transparentFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(titleFocus)
                                .testTag("text_card_title_input")
                        )

                        TextField(
                            value = content,
                            onValueChange = { content = it },
                            placeholder = {
                                Text(
                                    if (isArabic) "اكتب ملاحظتك هنا..." else "Note",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            colors = transparentFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 260.dp)
                                .testTag("text_card_content_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TextField(
                                value = tagsInput,
                                onValueChange = { tagsInput = it },
                                placeholder = {
                                    Text(
                                        if (isArabic) "وسوم مفصولة بفاصلة" else "Tags, comma-separated",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = transparentFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("text_card_tags_input")
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // ---------- Bottom bar ----------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { showMove = true },
                            label = {
                                Text(
                                    folders.find { it.id == folderId }?.name
                                        ?: if (isArabic) "عام" else "General"
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.testTag("editor_folder_chip")
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (lastEdited > 0L) {
                            val time = remember(lastEdited) {
                                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(lastEdited))
                            }
                            Text(
                                text = if (isArabic) "آخر حفظ $time" else "Saved $time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMove) {
        MoveCardDialog(
            card = base.copy(
                title = title.ifBlank { if (isArabic) "بدون عنوان" else "Untitled" },
                folderId = folderId
            ),
            folders = folders,
            isArabic = isArabic,
            onDismiss = { showMove = false },
            onMove = { folderId = it }
        )
    }

    if (showLockDialog) {
        LockManageDialog(
            isProtected = isProtected,
            isArabic = isArabic,
            onDismiss = { showLockDialog = false },
            onSetPassword = { password ->
                passwordHash = PasswordUtils.hashPassword(password)
                isProtected = true
                showLockDialog = false
            },
            onRemove = {
                passwordHash = null
                isProtected = false
                showLockDialog = false
            }
        )
    }
}

@Composable
private fun transparentFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent
)

@Composable
private fun UnlockContent(
    isArabic: Boolean,
    passwordHash: String?,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isArabic) "هذه البطاقة محمية" else "This card is protected",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isArabic) "أدخل كلمة المرور لعرض المحتوى وتعديله" else "Enter the password to view and edit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = {
                passwordInput = it
                if (passwordError) passwordError = false
            },
            label = { Text(if (isArabic) "كلمة المرور" else "Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = passwordError,
            supportingText = if (passwordError) {
                { Text(if (isArabic) "كلمة المرور غير صحيحة" else "Incorrect password") }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("unlock_password_input")
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (PasswordUtils.verifyPassword(passwordInput, passwordHash)) onUnlocked()
                else passwordError = true
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("unlock_card_btn")
        ) {
            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isArabic) "فتح القفل" else "Unlock")
        }
    }
}

@Composable
private fun LockManageDialog(
    isProtected: Boolean,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSetPassword: (String) -> Unit,
    onRemove: () -> Unit
) {
    // mode 0: card already protected (change / remove), mode 1: enter a new password
    var mode by remember { mutableStateOf(if (isProtected) 0 else 1) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isArabic) "الحماية بكلمة مرور" else "Password protection",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (mode == 0) {
                Text(if (isArabic) "هذه البطاقة محمية حالياً. يمكنك تغيير كلمة المرور أو إزالة الحماية." else "This card is protected. You can change the password or remove protection.")
            } else {
                Column {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("set_password_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it; error = null },
                        label = { Text(if (isArabic) "تأكيد كلمة المرور" else "Confirm password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = error != null,
                        supportingText = if (error != null) {
                            { Text(error.orEmpty()) }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_password_input")
                    )
                }
            }
        },
        confirmButton = {
            if (mode == 0) {
                TextButton(onClick = { mode = 1 }) {
                    Text(if (isArabic) "تغيير كلمة المرور" else "Change password")
                }
            } else {
                Button(
                    onClick = {
                        when {
                            password.isBlank() ->
                                error = if (isArabic) "يرجى إدخال كلمة المرور" else "Please enter a password"
                            password != confirm ->
                                error = if (isArabic) "كلمتا المرور غير متطابقتين" else "Passwords do not match"
                            else -> onSetPassword(password)
                        }
                    },
                    modifier = Modifier.testTag("save_password_btn")
                ) {
                    Text(if (isArabic) "حفظ" else "Save")
                }
            }
        },
        dismissButton = {
            if (mode == 0) {
                TextButton(onClick = onRemove) {
                    Text(
                        if (isArabic) "إزالة الحماية" else "Remove protection",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
