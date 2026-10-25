package com.example.ui.data

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.model.Checklist
import com.example.model.ChecklistItem
import com.example.model.Folder
import com.example.model.TextCard
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private val StarColor = Color(0xFFEAB308)

private data class ChecklistSnapshot(
    val title: String,
    val items: List<ChecklistItem>,
    val folderId: String?,
    val isFavorite: Boolean
)

/**
 * Full-screen checklist editor with autosave (same behaviour as [CardEditorDialog]).
 *
 * - Tap the box next to an item to mark it done. A done item opens a details area under it
 *   where you can write a note and give it a 1..10 rating
 *   (watched a movie -> "great ending", 4 stars).
 * - Type in the last row and press "done" on the keyboard to add items one after another.
 * - A new list is created on the first non-empty input and never saved empty.
 */
@Composable
fun ChecklistEditorDialog(
    checklist: Checklist?,
    initialFolderId: String?,
    folders: List<Folder>,
    isArabic: Boolean,
    onSave: (checklist: Checklist, isNew: Boolean) -> Unit,
    onDelete: (checklistId: String) -> Unit,
    onDeleteWithUndo: (checklist: Checklist) -> Unit,
    onClose: () -> Unit
) {
    val base = remember { checklist ?: Checklist(title = "", folderId = initialFolderId) }

    var title by remember { mutableStateOf(base.title) }
    val items = remember { mutableStateListOf<ChecklistItem>().apply { addAll(base.items) } }
    var folderId by remember { mutableStateOf(base.folderId) }
    var isFavorite by remember { mutableStateOf(base.isFavorite) }
    var newItemText by remember { mutableStateOf("") }
    // Done items whose notes/rating area is open
    var expandedIds by remember { mutableStateOf(setOf<String>()) }

    var persisted by remember { mutableStateOf(checklist != null) }
    var deleted by remember { mutableStateOf(false) }
    var lastEdited by remember { mutableStateOf(if (checklist != null) base.updatedAt else 0L) }

    var showMove by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    fun normalizedItems(): List<ChecklistItem> =
        items.filter { it.text.isNotBlank() }
            .map { it.copy(text = it.text.trim(), note = it.note.trim()) }

    fun snapshot() = ChecklistSnapshot(
        title = title.trim(),
        items = normalizedItems(),
        folderId = folderId,
        isFavorite = isFavorite
    )

    var lastSaved by remember {
        mutableStateOf<ChecklistSnapshot?>(
            if (checklist != null) ChecklistSnapshot(
                base.title.trim(),
                base.items.filter { it.text.isNotBlank() }
                    .map { it.copy(text = it.text.trim(), note = it.note.trim()) },
                base.folderId,
                base.isFavorite
            ) else null
        )
    }

    fun addItem() {
        val text = newItemText.trim()
        if (text.isEmpty()) return
        items.add(ChecklistItem(text = text))
        newItemText = ""
    }

    fun updateItem(id: String, transform: (ChecklistItem) -> ChecklistItem) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) items[index] = transform(items[index])
    }

    fun toggleDone(id: String) {
        val current = items.firstOrNull { it.id == id } ?: return
        val nowDone = !current.isDone
        updateItem(id) {
            it.copy(
                isDone = nowDone,
                completedAt = if (nowDone) System.currentTimeMillis() else null
            )
        }
        // Ticking an item opens its notes + rating area right away
        expandedIds = if (nowDone) expandedIds + id else expandedIds - id
    }

    // Plain-text version of the whole list: [x] done / [ ] not done yet, with rating and note
    fun copyAll() {
        val snap = snapshot()
        val text = buildString {
            if (snap.title.isNotBlank()) appendLine(snap.title)
            (snap.items.filter { !it.isDone } + snap.items.filter { it.isDone }).forEach { item ->
                append(if (item.isDone) "[x] " else "[ ] ")
                append(item.text)
                if (item.isDone && item.rating > 0) append(" - ${item.rating}/10")
                appendLine()
                if (item.isDone && item.note.isNotBlank()) {
                    item.note.lines().forEach { appendLine("    $it") }
                }
            }
        }.trimEnd()
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, if (isArabic) "تم نسخ القائمة" else "List copied", Toast.LENGTH_SHORT).show()
    }

    fun build(snap: ChecklistSnapshot, now: Long) = base.copy(
        title = snap.title,
        items = snap.items,
        folderId = snap.folderId,
        isFavorite = snap.isFavorite,
        updatedAt = now
    )

    val flush: () -> Unit = flush@{
        if (deleted) return@flush
        val snap = snapshot()
        if (snap == lastSaved) return@flush
        if (!persisted && snap.title.isBlank() && snap.items.isEmpty()) return@flush

        val now = System.currentTimeMillis()
        onSave(build(snap, now), !persisted)
        persisted = true
        lastSaved = snap
        lastEdited = now
    }
    val currentFlush by rememberUpdatedState(flush)

    val close: () -> Unit = {
        // Keep an item that was typed but not confirmed with the keyboard
        addItem()
        val snap = snapshot()
        if (checklist == null && persisted && snap.title.isBlank() && snap.items.isEmpty() && !deleted) {
            // A list that was cleared completely is discarded, like cards
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
        onDeleteWithUndo(build(snapshot(), base.updatedAt))
        onClose()
    }

    // Debounced autosave
    LaunchedEffect(snapshot()) {
        delay(700)
        currentFlush()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { currentFlush() }
    BackHandler { close() }

    val scrollState = rememberScrollState()

    val titleFocus = remember { FocusRequester() }
    val addFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (checklist == null) {
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
                        modifier = Modifier.testTag("checklist_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier.testTag("checklist_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isArabic) "مفضلة" else "Favorite",
                            tint = if (isFavorite) StarColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("checklist_menu_btn")
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
                                text = { Text(if (isArabic) "نسخ القائمة كاملة" else "Copy whole list") },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    menuExpanded = false
                                    copyAll()
                                },
                                modifier = Modifier.testTag("checklist_copy_btn")
                            )
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
                                            if (isArabic) "حذف القائمة" else "Delete list",
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

                // ---------- Body ----------
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = {
                            Text(
                                if (isArabic) "عنوان القائمة (أفلام، كتب، تسوق...)" else "List title (movies, books, shopping...)",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try { addFocus.requestFocus() } catch (_: Exception) { }
                        }),
                        colors = plainFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocus)
                            .testTag("checklist_title_input")
                    )

                    val total = items.count { it.text.isNotBlank() }
                    if (total > 0) {
                        val done = items.count { it.isDone && it.text.isNotBlank() }
                        Text(
                            text = if (isArabic) "تم $done  •  متبقي ${total - done}" else "$done done  •  ${total - done} left",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    val undoneItems = items.filter { !it.isDone }
                    val doneItems = items.filter { it.isDone }
                    val itemContent: @Composable (ChecklistItem) -> Unit = { item ->
                        key(item.id) {
                            ChecklistItemRow(
                                item = item,
                                expanded = item.id in expandedIds,
                                isArabic = isArabic,
                                onToggleDone = { toggleDone(item.id) },
                                onTextChange = { t -> updateItem(item.id) { it.copy(text = t) } },
                                onNoteChange = { n -> updateItem(item.id) { it.copy(note = n) } },
                                onRatingChange = { r -> updateItem(item.id) { it.copy(rating = r) } },
                                onToggleExpanded = {
                                    expandedIds = if (item.id in expandedIds) expandedIds - item.id else expandedIds + item.id
                                },
                                onDelete = {
                                    items.removeAll { it.id == item.id }
                                    expandedIds = expandedIds - item.id
                                }
                            )
                        }
                    }

                    // Not done yet: on top, followed by the add-item row
                    undoneItems.forEach { itemContent(it) }

                    // Add-item row: "done" on the keyboard adds the item and keeps the keyboard open
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        TextField(
                            value = newItemText,
                            onValueChange = { newItemText = it },
                            placeholder = {
                                Text(
                                    if (isArabic) "إضافة عنصر" else "Add item",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addItem() }),
                            colors = plainFieldColors(),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(addFocus)
                                .testTag("checklist_add_item_input")
                        )
                    }
                    // Done: below a small divider
                    if (doneItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "مكتمل (${doneItems.size})" else "Done (${doneItems.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        doneItems.forEach { itemContent(it) }
                    }
                    Spacer(modifier = Modifier.height(120.dp))
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
                        modifier = Modifier.testTag("checklist_folder_chip")
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

    if (showMove) {
        MoveCardDialog(
            card = TextCard(
                id = base.id,
                title = title.ifBlank { if (isArabic) "بدون عنوان" else "Untitled" },
                content = "",
                folderId = folderId
            ),
            folders = folders,
            isArabic = isArabic,
            onDismiss = { showMove = false },
            onMove = { folderId = it }
        )
    }
}

/** One checklist line, plus the notes / rating area that opens under it once it is done. */
@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    expanded: Boolean,
    isArabic: Boolean,
    onToggleDone: () -> Unit,
    onTextChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onToggleExpanded: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (item.isDone) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else Color.Transparent
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleDone,
                modifier = Modifier.testTag("checklist_item_check_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.isDone) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                    contentDescription = if (isArabic) "إنجاز" else "Done",
                    tint = if (item.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            TextField(
                value = item.text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                ),
                colors = plainFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("checklist_item_input_${item.id}")
            )
            if (item.isDone) {
                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.testTag("checklist_item_expand_${item.id}")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isArabic) "الملاحظات والتقييم" else "Notes and rating",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("checklist_item_delete_${item.id}")
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = if (isArabic) "حذف العنصر" else "Remove item",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (item.isDone) {
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, end = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = if (item.rating > 0) {
                            if (isArabic) "التقييم: ${item.rating} من 10" else "Rating: ${item.rating} / 10"
                        } else {
                            if (isArabic) "التقييم (من 10)" else "Rating (out of 10)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    for (rowStart in intArrayOf(1, 6)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            for (n in rowStart..rowStart + 4) {
                                val selected = item.rating == n
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { onRatingChange(if (selected) 0 else n) }
                                        .testTag("checklist_item_rating_${item.id}_$n"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$n",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    TextField(
                        value = item.note,
                        onValueChange = onNoteChange,
                        placeholder = {
                            Text(
                                if (isArabic) "اكتب ملاحظاتك..." else "Write your notes...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        minLines = 2,
                        maxLines = 8,
                        shape = RoundedCornerShape(12.dp),
                        colors = noteFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checklist_item_note_${item.id}")
                    )
                }
            } else if (item.rating > 0 || item.note.isNotBlank()) {
                // Collapsed summary: stars + first line of the note. Tap to open.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleExpanded)
                        .padding(start = 56.dp, end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.rating > 0) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = StarColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${item.rating}/10",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (item.note.isNotBlank()) {
                        Text(
                            text = item.note.lineSequence().first(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun plainFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent
)

@Composable
private fun noteFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent
)
