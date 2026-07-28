package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.GitHubApiClient
import com.example.data.local.DownloadItem
import com.example.data.local.DownloadStatus
import com.example.data.local.TrackedProject
import com.example.ui.theme.GitHubBlue
import com.example.ui.theme.GitHubBlueLight
import com.example.ui.theme.GitHubGreen
import com.example.ui.theme.GitHubGreenLight
import com.example.ui.theme.GitHubPurple
import com.example.ui.theme.GitHubRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    val activeDownload by viewModel.activeDownloadState.collectAsStateWithLifecycle()
    val trackedProjects by viewModel.trackedProjects.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showMenu by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<DownloadItem?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    bannerMessage = event.message
                }
            }
        }
    }

    LaunchedEffect(bannerMessage) {
        if (bannerMessage != null) {
            kotlinx.coroutines.delay(2800)
            bannerMessage = null
        }
    }

    if (uiState.showAddProjectDialog) {
        AddProjectDialog(
            onDismiss = { viewModel.setShowAddProjectDialog(false) },
            onConfirm = { url -> viewModel.addTrackedProject(url) }
        )
    }

    uiState.projectToEdit?.let { project ->
        EditProjectDialog(
            project = project,
            onDismiss = { viewModel.setProjectToEdit(null) },
            onConfirm = { newUrl -> viewModel.updateProjectUrl(project, newUrl) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(GitHubBlue, GitHubPurple)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "App Logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "GitHub Release Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Clean Release & APK Manager",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("top_app_bar_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu Options"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Track New Repository") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = GitHubPurple
                                )
                            },
                            onClick = {
                                showMenu = false
                                viewModel.setShowAddProjectDialog(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Use System Download Manager") },
                            trailingIcon = {
                                Switch(
                                    checked = uiState.isSystemDownloadManager,
                                    onCheckedChange = { viewModel.toggleSystemDownloadManager(it) }
                                )
                            },
                            onClick = { viewModel.toggleSystemDownloadManager(!uiState.isSystemDownloadManager) }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Download History") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = GitHubRed
                                )
                            },
                            onClick = {
                                showMenu = false
                                showClearAllDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Projects"
                        )
                    },
                    label = { Text("Projects", fontSize = 12.sp, fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Direct Link"
                        )
                    },
                    label = { Text("Direct Link", fontSize = 12.sp, fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = {
                        if (activeDownload != null && activeDownload?.status == DownloadStatus.DOWNLOADING) {
                            BadgedBox(
                                badge = { Badge { Text("1") } }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Downloads"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Downloads"
                            )
                        }
                    },
                    label = { Text("Downloads", fontSize = 12.sp, fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> TrackedProjectsTabContent(
                    projects = trackedProjects,
                    isRefreshing = uiState.isRefreshingProjects,
                    onAddProjectClick = { viewModel.setShowAddProjectDialog(true) },
                    onRefreshProject = { viewModel.refreshTrackedProject(it) },
                    onRefreshAll = { viewModel.refreshAllTrackedProjects(isSilent = false) },
                    onEditProject = { viewModel.setProjectToEdit(it) },
                    onDeleteProject = { viewModel.deleteTrackedProject(it) },
                    onDownloadAsset = { url, name -> viewModel.downloadAsset(url, name) }
                )
                1 -> DirectDownloadTabContent(
                    urlInput = uiState.urlInput,
                    customFileName = uiState.customFileName,
                    onUrlChange = { viewModel.onUrlInputChanged(it) },
                    onFileNameChange = { viewModel.onCustomFileNameChanged(it) },
                    onPasteClick = { viewModel.pasteFromClipboard() },
                    onDownloadClick = {
                        keyboardController?.hide()
                        viewModel.startDownload()
                    }
                )
                2 -> DownloadsHistoryTabContent(
                    downloads = downloads,
                    activeDownload = activeDownload,
                    searchQuery = uiState.searchQuery,
                    selectedFilter = uiState.selectedFilter,
                    onSearchChange = { viewModel.onSearchQueryChanged(it) },
                    onFilterChange = { viewModel.setFilterType(it) },
                    onCancelActive = { id -> viewModel.cancelDownload(id) },
                    onOpenFile = { viewModel.openFile(it) },
                    onShareFile = { viewModel.shareFile(it) },
                    onReDownload = { viewModel.reDownload(it) },
                    onDeleteFile = { itemToDelete = it },
                    onClearAll = { showClearAllDialog = true }
                )
            }

            ModernFloatingAlertBanner(
                message = bannerMessage,
                onDismiss = { bannerMessage = null },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete File") },
            text = { Text("Do you want to delete \"${itemToDelete?.fileName}\" from storage and download history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { viewModel.deleteDownload(it, deleteFileFromStorage = true) }
                        itemToDelete = null
                    }
                ) {
                    Text("Delete", color = GitHubRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to clear all download history records?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Clear All", color = GitHubRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DownloadInputCard(
    urlInput: String,
    customFileName: String,
    onUrlChange: (String) -> Unit,
    onFileNameChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    var showAdvanced by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_input_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Direct Download Link",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(
                    onClick = onPasteClick,
                    modifier = Modifier.testTag("paste_clipboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        modifier = Modifier.size(16.dp),
                        tint = GitHubBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Paste",
                        fontSize = 13.sp,
                        color = GitHubBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("github_url_input"),
                placeholder = {
                    Text(
                        text = "https://github.com/owner/repo/releases/download/...",
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = GitHubBlue
                    )
                },
                trailingIcon = {
                    if (urlInput.isNotEmpty()) {
                        IconButton(onClick = { onUrlChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear URL"
                            )
                        }
                    }
                },
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDownloadClick() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GitHubBlue,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            AnimatedVisibility(visible = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Target File Name (Optional)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customFileName,
                        onValueChange = onFileNameChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_filename_input"),
                        singleLine = true,
                        placeholder = { Text("custom_filename.apk") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showAdvanced = !showAdvanced }
                ) {
                    Text(
                        text = if (showAdvanced) "Hide options" else "Rename file manually",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onDownloadClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_download_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GitHubGreen
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Download File",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ActiveDownloadCard(
    item: DownloadItem,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, GitHubBlueLight, RoundedCornerShape(18.dp))
            .testTag("active_download_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = GitHubBlue.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    FileIconBadge(extension = item.fileExtension)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Downloading...",
                            fontSize = 11.sp,
                            color = GitHubBlueLight
                        )
                    }
                }

                IconButton(
                    onClick = onCancelClick,
                    modifier = Modifier.testTag("cancel_active_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = GitHubRed
                    )
                }
            }

            LinearProgressIndicator(
                progress = { item.progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = GitHubBlueLight,
                trackColor = GitHubBlue.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(item.progressPercentage * 100).toInt()}% (${item.formattedDownloadedSize} / ${item.formattedSize})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (item.formattedSpeed.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = GitHubGreenLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.formattedSpeed,
                            fontSize = 12.sp,
                            color = GitHubGreenLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(
    item: DownloadItem,
    onOpenClick: () -> Unit,
    onShareClick: () -> Unit,
    onRedownloadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd • hh:mm a", Locale.ENGLISH) }
    val formattedDate = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileIconBadge(extension = item.fileExtension)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.formattedSize,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "  •  $formattedDate",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                StatusBadge(status = item.status)
            }

            if (item.status == DownloadStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                Text(
                    text = "Error: ${item.errorMessage}",
                    fontSize = 11.sp,
                    color = GitHubRed
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.status == DownloadStatus.COMPLETED) {
                    Button(
                        onClick = onOpenClick,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("open_file_button_${item.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = GitHubBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.fileExtension == "APK") "Install / Open" else "Open File",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = onShareClick,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("share_file_button_${item.id}"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(
                    onClick = onRedownloadClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("redownload_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Redownload",
                        tint = GitHubBlueLight,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = GitHubRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FileIconBadge(extension: String) {
    val (bgColor, icon) = when (extension) {
        "APK" -> Pair(GitHubGreen, Icons.Default.Android)
        "ZIP", "RAR", "7Z", "TAR", "GZ" -> Pair(GitHubPurple, Icons.Default.FolderZip)
        else -> Pair(GitHubBlue, Icons.Default.InsertDriveFile)
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = bgColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun StatusBadge(status: DownloadStatus) {
    val (label, color) = when (status) {
        DownloadStatus.COMPLETED -> Pair("Completed", GitHubGreen)
        DownloadStatus.DOWNLOADING -> Pair("Downloading", GitHubBlueLight)
        DownloadStatus.FAILED -> Pair("Failed", GitHubRed)
        DownloadStatus.PAUSED -> Pair("Paused", Color.Gray)
        DownloadStatus.IDLE -> Pair("Idle", Color.Gray)
    }

    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun EmptyHistoryCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No downloads in history yet",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Track GitHub repositories or paste a direct release link to begin downloading.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ModernFloatingAlertBanner(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (message != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GitHubGreen.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = GitHubGreenLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackedProjectsTabContent(
    projects: List<TrackedProject>,
    isRefreshing: Boolean,
    onAddProjectClick: () -> Unit,
    onRefreshProject: (TrackedProject) -> Unit,
    onRefreshAll: () -> Unit,
    onEditProject: (TrackedProject) -> Unit,
    onDeleteProject: (TrackedProject) -> Unit,
    onDownloadAsset: (downloadUrl: String, fileName: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tracked Repositories (${projects.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Auto-checking releases & direct APK downloads",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onRefreshAll,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh All",
                            tint = GitHubBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Button(
                        onClick = onAddProjectClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GitHubGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Track Repo", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }

            if (isRefreshing) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = GitHubBlue
                )
            }
        }

        if (projects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = GitHubPurple,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No tracked projects yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add GitHub repo links (e.g. github.com/owner/repo) to get notified on new releases and download APK assets easily.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onAddProjectClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GitHubPurple)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Track First Repository")
                        }
                    }
                }
            }
        } else {
            items(projects, key = { it.id }) { project ->
                TrackedProjectItemCard(
                    project = project,
                    onRefresh = { onRefreshProject(project) },
                    onEdit = { onEditProject(project) },
                    onDelete = { onDeleteProject(project) },
                    onDownloadAsset = onDownloadAsset
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DirectDownloadTabContent(
    urlInput: String,
    customFileName: String,
    onUrlChange: (String) -> Unit,
    onFileNameChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Text(
                    text = "Direct File Downloader",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Paste any direct file or GitHub release URL to download instantly",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            DownloadInputCard(
                urlInput = urlInput,
                customFileName = customFileName,
                onUrlChange = onUrlChange,
                onFileNameChange = onFileNameChange,
                onPasteClick = onPasteClick,
                onDownloadClick = onDownloadClick
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = GitHubBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Pro Tip",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "You can download direct URLs here or head to the 'Projects' tab to track repositories for automatic update alerts.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsHistoryTabContent(
    downloads: List<DownloadItem>,
    activeDownload: DownloadItem?,
    searchQuery: String,
    selectedFilter: FilterType,
    onSearchChange: (String) -> Unit,
    onFilterChange: (FilterType) -> Unit,
    onCancelActive: (Long) -> Unit,
    onOpenFile: (DownloadItem) -> Unit,
    onShareFile: (DownloadItem) -> Unit,
    onReDownload: (DownloadItem) -> Unit,
    onDeleteFile: (DownloadItem) -> Unit,
    onClearAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Downloads History (${downloads.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Manage downloaded files, install APKs & share",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (downloads.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", color = GitHubRed, fontSize = 12.sp)
                    }
                }
            }
        }

        if (activeDownload != null && activeDownload.status == DownloadStatus.DOWNLOADING) {
            item {
                ActiveDownloadCard(
                    item = activeDownload,
                    onCancelClick = { onCancelActive(activeDownload.id) }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search downloaded files...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_history_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FilterType.entries.toTypedArray()) { filter ->
                        val label = when (filter) {
                            FilterType.ALL -> "All"
                            FilterType.APKS -> "APKs"
                            FilterType.ARCHIVES -> "Archives"
                            FilterType.COMPLETED -> "Completed"
                        }
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GitHubBlue.copy(alpha = 0.2f),
                                selectedLabelColor = GitHubBlueLight
                            )
                        )
                    }
                }
            }
        }

        if (downloads.isEmpty()) {
            item {
                EmptyHistoryCard()
            }
        } else {
            items(downloads, key = { it.id }) { item ->
                DownloadItemCard(
                    item = item,
                    onOpenClick = { onOpenFile(item) },
                    onShareClick = { onShareFile(item) },
                    onRedownloadClick = { onReDownload(item) },
                    onDeleteClick = { onDeleteFile(item) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TrackedProjectItemCard(
    project: TrackedProject,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDownloadAsset: (downloadUrl: String, fileName: String) -> Unit
) {
    val assets = remember(project.latestAssetsJson) {
        GitHubApiClient.deserializeAssets(project.latestAssetsJson)
    }

    var isExpanded by remember { mutableStateOf(false) }

    val displayedAssets = remember(assets, isExpanded) {
        if (isExpanded || assets.size <= 2) assets else assets.take(2)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GitHubBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = GitHubBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${project.owner} / ${project.repo}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!project.latestTagName.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = GitHubGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = project.latestTagName,
                                        color = GitHubGreenLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (!project.latestReleaseName.isNullOrBlank() && project.latestReleaseName != project.latestTagName) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = project.latestReleaseName,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = GitHubBlueLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit URL",
                            tint = GitHubPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = GitHubRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (!project.lastError.isNullOrBlank()) {
                Text(
                    text = "Failed to fetch: ${project.lastError}",
                    fontSize = 11.sp,
                    color = GitHubRed
                )
            } else if (assets.isEmpty()) {
                Text(
                    text = "Checking for latest release assets...",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Release Assets (${assets.size}):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    displayedAssets.forEach { asset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (asset.name.endsWith(".apk", ignoreCase = true)) Icons.Default.Android else Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (asset.name.endsWith(".apk", ignoreCase = true)) GitHubGreenLight else GitHubBlueLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = asset.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (asset.size > 0) {
                                        Text(
                                            text = formatFileSize(asset.size),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { onDownloadAsset(asset.downloadUrl, asset.name) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GitHubGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (assets.size > 2) {
                        Surface(
                            onClick = { isExpanded = !isExpanded },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 6.dp, horizontal = 12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = GitHubBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isExpanded) "Show less" else "Show ${assets.size - 2} more assets",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GitHubBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String) -> Unit
) {
    var projectUrlInput by remember { mutableStateOf("https://github.com/") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = GitHubPurple
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Track Repository", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter a GitHub repository URL (e.g., github.com/owner/repo) to auto-check for new releases:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = projectUrlInput,
                    onValueChange = { projectUrlInput = it },
                    placeholder = { Text("https://github.com/owner/repo") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_project_url_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(projectUrlInput) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GitHubGreen)
            ) {
                Text("Track", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditProjectDialog(
    project: TrackedProject,
    onDismiss: () -> Unit,
    onConfirm: (newUrl: String) -> Unit
) {
    var projectUrlInput by remember(project) { mutableStateOf(project.projectUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = GitHubPurple
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Repository URL")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Update current repository URL (${project.owner}/${project.repo}):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = projectUrlInput,
                    onValueChange = { projectUrlInput = it },
                    placeholder = { Text("https://github.com/owner/repo") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_project_url_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(projectUrlInput) },
                colors = ButtonDefaults.buttonColors(containerColor = GitHubPurple)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.ENGLISH, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.ENGLISH, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.ENGLISH, "%.0f KB", kb)
        else -> "$bytes B"
    }
}
