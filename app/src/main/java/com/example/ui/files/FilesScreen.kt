package com.example.ui.files

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// Phone files page (الملفات).
//
// Everything here works directly on the phone's own shared storage.
// Nothing is uploaded, synced or stored in Firebase or anywhere else.
// Full access (browse all folders, create, rename, move, delete) needs the
// system "All files access" grant on Android 11+; the screen asks for it.
// ---------------------------------------------------------------------------

private data class FileItem(
    val file: File,
    val isDir: Boolean,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long
)

private data class BaseFolder(val nameAr: String, val nameEn: String, val dirName: String)

private val baseFolders = listOf(
    BaseFolder("الصور", "Photos", Environment.DIRECTORY_PICTURES),
    BaseFolder("الأصوات", "Audio", Environment.DIRECTORY_MUSIC),
    BaseFolder("الملفات", "Files", Environment.DIRECTORY_DOCUMENTS),
    BaseFolder("الفيديوهات", "Videos", Environment.DIRECTORY_MOVIES)
)

private val imageExts = setOf(
    "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "avif", "tif", "tiff",
    "svg", "ico", "raw", "dng", "cr2", "nef", "arw"
)
private val videoExts = setOf(
    "mp4", "mkv", "avi", "3gp", "3g2", "webm", "mov", "m4v", "wmv", "flv", "mpg", "mpeg", "vob"
)
private val audioExts = setOf(
    "mp3", "wav", "m4a", "aac", "ogg", "oga", "flac", "amr", "opus", "wma", "mid", "midi", "aif", "aiff", "mka"
)
private val documentExts = setOf(
    "pdf", "doc", "docx", "dot", "dotx", "odt", "rtf", "txt", "md", "xls", "xlsx", "xlsm", "csv", "ods",
    "ppt", "pptx", "pps", "ppsx", "odp", "epub", "mobi", "djvu", "pages", "numbers", "key",
    "apk", "xapk", "apks", "aab", "zip", "rar", "7z", "tar", "gz", "json", "xml", "html", "htm",
    "vcf", "ics", "log", "tex"
)

/** Scan the whole storage for every file whose extension belongs to [cat]. */
private fun scanCategory(cat: BaseFolder): List<FileItem> {
    val root = Environment.getExternalStorageDirectory()
    val matcher: (String) -> Boolean = when (cat.nameEn) {
        "Photos" -> { n -> extOf(n) in imageExts }
        "Videos" -> { n -> extOf(n) in videoExts }
        "Audio" -> { n -> extOf(n) in audioExts }
        else -> { n -> extOf(n) in documentExts }
    }
    val out = ArrayList<FileItem>()
    val stack = java.util.ArrayDeque<File>()
    stack.add(root)
    while (stack.isNotEmpty()) {
        val d = stack.removeLast()
        val kids = d.listFiles() ?: continue
        for (f in kids) {
            if (f.name.startsWith(".")) continue
            if (f.isDirectory) {
                // Android/ holds other apps' private data and is not readable anyway.
                if (d.absolutePath == root.absolutePath && f.name == "Android") continue
                stack.add(f)
            } else if (matcher(f.name)) {
                out.add(FileItem(f, false, f.name, f.length(), f.lastModified()))
            }
        }
    }
    return out.sortedByDescending { it.lastModified }
}

private fun extOf(name: String): String = name.substringAfterLast('.', "").lowercase(Locale.US)
private fun isImage(name: String) = extOf(name) in imageExts
private fun isVideo(name: String) = extOf(name) in videoExts
private fun isAudio(name: String) = extOf(name) in audioExts

private fun hasStorageAccess(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun listDirContents(dir: File?, category: BaseFolder? = null, showOthers: Boolean = true): List<FileItem> {
    val storageRoot = Environment.getExternalStorageDirectory()
    if (category != null) return scanCategory(category)
    if (dir == null) {
        // Root view: the four base folders first (always visible), then any
        // other folders and files sitting at the storage root.
        // Create the base folders when missing so they are real, browsable
        // targets (some phones lack Documents/Music/Movies by default).
        val baseDirs = baseFolders.map { File(storageRoot, it.dirName) }
        baseDirs.forEach { if (!it.exists()) it.mkdirs() }
        val basePaths = baseDirs.map { it.absolutePath }.toSet()
        val baseItems = baseDirs.map { f ->
            FileItem(f, true, f.name, 0L, if (f.exists()) f.lastModified() else 0L)
        }
        val others = (storageRoot.listFiles() ?: emptyArray())
            .filter { it.absolutePath !in basePaths && !it.name.startsWith(".") }
            .map { f ->
                FileItem(f, f.isDirectory, f.name, if (f.isFile) f.length() else 0L, f.lastModified())
            }
        return if (showOthers) baseItems + sortItems(others) else baseItems
    }
    val children = (dir.listFiles() ?: emptyArray())
        .filter { !it.name.startsWith(".") }
        .map { f ->
            FileItem(f, f.isDirectory, f.name, if (f.isFile) f.length() else 0L, f.lastModified())
        }
    return sortItems(children)
}

private fun sortItems(items: List<FileItem>): List<FileItem> =
    items.sortedWith(compareBy({ !it.isDir }, { it.name.lowercase(Locale.getDefault()) }))

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}

private fun formatDate(millis: Long, isArabic: Boolean): String {
    if (millis <= 0) return ""
    val locale = if (isArabic) Locale("ar") else Locale.US
    return SimpleDateFormat("yyyy/MM/dd", locale).format(Date(millis))
}

private fun displayName(item: FileItem, isArabic: Boolean): String {
    val match = baseFolders.firstOrNull {
        it.dirName == item.name &&
            item.file.parentFile?.absolutePath == Environment.getExternalStorageDirectory().absolutePath
    }
    return if (match != null) (if (isArabic) match.nameAr else match.nameEn) else item.name
}

@Composable
fun FilesScreen(
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        permissionTick++
        onPauseOrDispose { }
    }
    val granted = remember(permissionTick) { hasStorageAccess(context) }

    if (!granted) {
        StoragePermissionGate(
            isArabic = isArabic,
            modifier = modifier,
            onPermissionResult = { permissionTick++ }
        )
        return
    }

    var currentDir by remember { mutableStateOf<File?>(null) }
    var category by remember { mutableStateOf<BaseFolder?>(null) }
    var showOthers by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var gridMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    val items by produceState<List<FileItem>?>(initialValue = null, currentDir, category, showOthers, refreshTick) {
        value = null
        value = withContext(Dispatchers.IO) { listDirContents(currentDir, category, showOthers) }
    }

    fun refresh() { refreshTick++ }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    fun openFile(item: FileItem) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                item.file
            )
            val mime = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extOf(item.name)) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(if (isArabic) "لا يوجد تطبيق لفتح هذا الملف" else "No app can open this file")
        } catch (e: Exception) {
            toast(if (isArabic) "تعذر فتح الملف" else "Could not open file")
        }
    }

    val selectionMode = selected.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        if (selectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selected = emptySet() }) {
                    Icon(Icons.Filled.Close, contentDescription = if (isArabic) "إلغاء" else "Cancel")
                }
                Text(
                    text = if (isArabic) "${selected.size} محدد" else "${selected.size} selected",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                if (selected.size == 1) {
                    val item = items?.firstOrNull { it.file.absolutePath == selected.first() }
                    IconButton(onClick = { if (item != null) renameTarget = item }) {
                        Icon(
                            Icons.Filled.DriveFileRenameOutline,
                            contentDescription = if (isArabic) "إعادة تسمية" else "Rename"
                        )
                    }
                }
                IconButton(onClick = { showMoveDialog = true }) {
                    Icon(
                        Icons.Filled.DriveFileMove,
                        contentDescription = if (isArabic) "نقل" else "Move"
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = if (isArabic) "حذف" else "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentDir != null || category != null) {
                    IconButton(onClick = {
                        if (category != null) {
                            category = null
                        } else {
                            val parent = currentDir?.parentFile
                            val root = Environment.getExternalStorageDirectory()
                            currentDir = if (parent == null || parent.absolutePath == root.absolutePath) null else parent
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Up"
                        )
                    }
                }
                Text(
                    text = when {
                        category != null -> category?.let { if (isArabic) it.nameAr else it.nameEn } ?: ""
                        currentDir == null -> if (isArabic) "الملفات" else "Files"
                        else -> currentDir?.name ?: ""
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                IconButton(onClick = { showNewFolderDialog = true }) {
                    Icon(
                        Icons.Filled.CreateNewFolder,
                        contentDescription = if (isArabic) "مجلد جديد" else "New folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { gridMode = !gridMode }) {
                    Icon(
                        if (gridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = if (isArabic) "تغيير العرض" else "Toggle view"
                    )
                }
                IconButton(onClick = { refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = if (isArabic) "تحديث" else "Refresh")
                }
            }
        }

        // Content
        val list = items
        if (list == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (list.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) "هذا المجلد فارغ" else "This folder is empty",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (!gridMode) {
            val atRoot = currentDir == null && category == null
            val head = if (atRoot) list.take(baseFolders.size) else list
            val tail = if (atRoot) list.drop(baseFolders.size) else emptyList()
            val rowContent: @Composable (FileItem) -> Unit = { item ->
                    FileRow(
                        item = item,
                        isArabic = isArabic,
                        selected = item.file.absolutePath in selected,
                        selectionMode = selectionMode,
                        onClick = {
                            when {
                                selectionMode -> {
                                    val p = item.file.absolutePath
                                    selected = if (p in selected) selected - p else selected + p
                                }
                                item.isDir -> {
                                    val baseMatch = if (currentDir == null && category == null)
                                        baseFolders.firstOrNull { it.dirName == item.name } else null
                                    if (baseMatch != null) {
                                        category = baseMatch
                                    } else {
                                        if (!item.file.exists()) item.file.mkdirs()
                                        currentDir = item.file
                                    }
                                }
                                else -> openFile(item)
                            }
                        },
                        onLongClick = {
                            val p = item.file.absolutePath
                            selected = if (p in selected) selected - p else selected + p
                        }
                    )
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                listItems(head, key = { it.file.absolutePath }) { item -> rowContent(item) }
                if (atRoot) {
                    item(key = "toggle-others") { OthersToggle(isArabic, showOthers) { showOthers = !showOthers } }
                    listItems(tail, key = { it.file.absolutePath }) { item -> rowContent(item) }
                }
            }
        } else {
            val atRoot = currentDir == null && category == null
            val head = if (atRoot) list.take(baseFolders.size) else list
            val tail = if (atRoot) list.drop(baseFolders.size) else emptyList()
            val cardContent: @Composable (FileItem) -> Unit = { item ->
                    FileCard(
                        item = item,
                        isArabic = isArabic,
                        selected = item.file.absolutePath in selected,
                        selectionMode = selectionMode,
                        onClick = {
                            when {
                                selectionMode -> {
                                    val p = item.file.absolutePath
                                    selected = if (p in selected) selected - p else selected + p
                                }
                                item.isDir -> {
                                    val baseMatch = if (currentDir == null && category == null)
                                        baseFolders.firstOrNull { it.dirName == item.name } else null
                                    if (baseMatch != null) {
                                        category = baseMatch
                                    } else {
                                        if (!item.file.exists()) item.file.mkdirs()
                                        currentDir = item.file
                                    }
                                }
                                else -> openFile(item)
                            }
                        },
                        onLongClick = {
                            val p = item.file.absolutePath
                            selected = if (p in selected) selected - p else selected + p
                        }
                    )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gridItems(head, key = { it.file.absolutePath }) { item -> cardContent(item) }
                if (atRoot) {
                    item(key = "toggle-others", span = { GridItemSpan(maxLineSpan) }) {
                        OthersToggle(isArabic, showOthers) { showOthers = !showOthers }
                    }
                    gridItems(tail, key = { it.file.absolutePath }) { item -> cardContent(item) }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            isArabic = isArabic,
            onDismiss = { showNewFolderDialog = false },
            onCreate = { name ->
                val parent = currentDir ?: Environment.getExternalStorageDirectory()
                val target = File(parent, name)
                if (target.exists()) {
                    toast(if (isArabic) "يوجد مجلد بهذا الاسم بالفعل" else "A folder with this name already exists")
                } else if (target.mkdirs()) {
                    toast(if (isArabic) "تم إنشاء المجلد" else "Folder created")
                    refresh()
                } else {
                    toast(if (isArabic) "تعذر إنشاء المجلد" else "Could not create folder")
                }
                showNewFolderDialog = false
            }
        )
    }

    renameTarget?.let { target ->
        RenameDialog(
            isArabic = isArabic,
            currentName = target.name,
            onDismiss = { renameTarget = null },
            onRename = { newName ->
                val dest = File(target.file.parentFile, newName)
                if (dest.exists()) {
                    toast(if (isArabic) "يوجد ملف بهذا الاسم بالفعل" else "Name already in use")
                } else if (target.file.renameTo(dest)) {
                    toast(if (isArabic) "تمت إعادة التسمية" else "Renamed")
                    selected = emptySet()
                    refresh()
                } else {
                    toast(if (isArabic) "تعذرت إعادة التسمية" else "Rename failed")
                }
                renameTarget = null
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (isArabic) "حذف العناصر المحددة؟" else "Delete selected items?") },
            text = {
                Text(
                    if (isArabic)
                        "سيتم حذف ${selected.size} عنصر من هاتفك نهائيًا، ولا يمكن التراجع."
                    else
                        "${selected.size} item(s) will be permanently deleted from your phone. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    var ok = 0
                    selected.forEach { path ->
                        if (File(path).deleteRecursively()) ok++
                    }
                    toast(
                        if (isArabic) "تم حذف $ok عنصر" else "Deleted $ok item(s)"
                    )
                    selected = emptySet()
                    showDeleteConfirm = false
                    refresh()
                }) {
                    Text(if (isArabic) "حذف" else "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    if (showMoveDialog) {
        MoveDialog(
            isArabic = isArabic,
            blockedPaths = selected,
            onDismiss = { showMoveDialog = false },
            onPick = { destDir ->
                var ok = 0
                var failed = 0
                selected.forEach { path ->
                    val src = File(path)
                    val dest = File(destDir, src.name)
                    val insideItself = src.isDirectory &&
                        (destDir.absolutePath == src.absolutePath ||
                            destDir.absolutePath.startsWith(src.absolutePath + File.separator))
                    when {
                        insideItself || dest.exists() -> failed++
                        src.renameTo(dest) -> ok++
                        else -> failed++
                    }
                }
                toast(
                    if (isArabic)
                        if (failed == 0) "تم نقل $ok عنصر" else "تم نقل $ok وتعذر نقل $failed"
                    else
                        if (failed == 0) "Moved $ok item(s)" else "Moved $ok, $failed failed"
                )
                selected = emptySet()
                showMoveDialog = false
                refresh()
            }
        )
    }
}

@Composable
private fun StoragePermissionGate(
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    onPermissionResult: () -> Unit
) {
    val context = LocalContext.current
    val legacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { onPermissionResult() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isArabic) "الوصول إلى ملفات الهاتف" else "Access to phone files",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isArabic)
                "لعرض ملفات هاتفك وتنظيمها (إنشاء مجلدات وإعادة تسمية ونقل وحذف)، يحتاج التطبيق إذن «الوصول إلى كل الملفات». كل شيء يبقى على هاتفك، ولا يُرفع أي ملف إلى الإنترنت."
            else
                "To show and organize your phone files (create folders, rename, move, delete), the app needs \"All files access\". Everything stays on your phone; nothing is uploaded.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + context.packageName)
                    )
                    context.startActivity(intent)
                } catch (e: Exception) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                legacyLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }) {
            Text(if (isArabic) "منح الإذن" else "Grant access")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    item: FileItem,
    isArabic: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = bg)
        ) {
            ItemThumb(item = item, modifier = Modifier.size(44.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName(item, isArabic),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    if (!item.isDir) append(formatSize(item.sizeBytes))
                    if (item.lastModified > 0) {
                        if (isNotEmpty()) append(" · ")
                        append(formatDate(item.lastModified, isArabic))
                    }
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selectionMode) {
            Icon(
                imageVector = if (selected) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileCard(
    item: FileItem,
    isArabic: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ItemThumb(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = displayName(item, isArabic),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ItemThumb(item: FileItem, modifier: Modifier = Modifier) {
    when {
        item.isDir -> ThumbIcon(Icons.Filled.Folder, modifier)
        isImage(item.name) -> AsyncImage(
            model = item.file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
        isVideo(item.name) -> VideoThumb(item = item, modifier = modifier)
        isAudio(item.name) -> ThumbIcon(Icons.Filled.MusicNote, modifier)
        extOf(item.name) == "pdf" -> ThumbIcon(Icons.Filled.PictureAsPdf, modifier)
        else -> ThumbIcon(Icons.AutoMirrored.Filled.InsertDriveFile, modifier)
    }
}

@Composable
private fun ThumbIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun VideoThumb(item: FileItem, modifier: Modifier = Modifier) {
    val thumb by produceState<ImageBitmap?>(initialValue = null, item.file) {
        value = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ThumbnailUtils.createVideoThumbnail(
                        item.file,
                        android.util.Size(256, 256),
                        null
                    )?.asImageBitmap()
                } else {
                    val b = android.media.MediaMetadataRetriever().run {
                        try {
                            setDataSource(item.file.absolutePath)
                            frameAtTime
                        } catch (e: Exception) {
                            null
                        } finally {
                            try { release() } catch (_: Exception) {}
                        }
                    }
                    b?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    val bitmap = thumb
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        ThumbIcon(Icons.Filled.Movie, modifier)
    }
}

@Composable
private fun NewFolderDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "مجلد جديد" else "New folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(if (isArabic) "اسم المجلد" else "Folder name") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text(if (isArabic) "إنشاء" else "Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
private fun RenameDialog(
    isArabic: Boolean,
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "إعادة التسمية" else "Rename") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(if (isArabic) "الاسم الجديد" else "New name") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && name.trim() != currentName) onRename(name.trim()) },
                enabled = name.isNotBlank() && name.trim() != currentName
            ) { Text(if (isArabic) "حفظ" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoveDialog(
    isArabic: Boolean,
    blockedPaths: Set<String>,
    onDismiss: () -> Unit,
    onPick: (File) -> Unit
) {
    val storageRoot = Environment.getExternalStorageDirectory()
    var pickerDir by remember { mutableStateOf(storageRoot) }
    val subdirs by produceState<List<File>>(initialValue = emptyList(), pickerDir) {
        value = withContext(Dispatchers.IO) {
            // At the root, make sure the four base folders exist so they can
            // be picked as move destinations, then list real folders.
            if (pickerDir.absolutePath == storageRoot.absolutePath) {
                baseFolders.forEach { val f = File(storageRoot, it.dirName); if (!f.exists()) f.mkdirs() }
            }
            (pickerDir.listFiles() ?: emptyArray())
                .filter { it.isDirectory && !it.name.startsWith(".") && it.absolutePath !in blockedPaths }
                .sortedWith(
                    compareBy(
                        { baseFolders.indexOfFirst { bf -> bf.dirName == it.name && it.parentFile?.absolutePath == storageRoot.absolutePath }.let { i -> if (i < 0) Int.MAX_VALUE else i } },
                        { it.name.lowercase(Locale.getDefault()) }
                    )
                )
        }
    }
    val isRoot = pickerDir.absolutePath == storageRoot.absolutePath

    fun pickerLabel(dir: File): String {
        val match = baseFolders.firstOrNull {
            it.dirName == dir.name && dir.parentFile?.absolutePath == storageRoot.absolutePath
        }
        return if (match != null) (if (isArabic) match.nameAr else match.nameEn) else dir.name
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isRoot) {
                    IconButton(onClick = {
                        pickerDir.parentFile?.let { pickerDir = it }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Up"
                        )
                    }
                }
                Text(
                    text = (if (isArabic) "نقل إلى: " else "Move to: ") +
                        (if (isRoot) (if (isArabic) "الرئيسية" else "Storage root") else pickerLabel(pickerDir)),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            if (subdirs.isEmpty()) {
                Text(
                    if (isArabic) "لا توجد مجلدات فرعية هنا" else "No subfolders here",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    listItems(subdirs, key = { it.absolutePath }) { dir ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(onClick = { pickerDir = dir }, onLongClick = {})
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                pickerLabel(dir),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onPick(pickerDir) }) {
                Text(if (isArabic) "نقل هنا" else "Move here")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OthersToggle(isArabic: Boolean, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onToggle, onLongClick = {})
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (isArabic) "عرض أو إخفاء باقي الملفات" else "Show or hide other files",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (expanded)
                (if (isArabic) "إخفاء باقي الملفات" else "Hide other files")
            else
                (if (isArabic) "عرض باقي الملفات" else "Show other files"),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
