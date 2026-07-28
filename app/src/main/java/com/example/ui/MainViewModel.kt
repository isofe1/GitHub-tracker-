package com.example.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GitHubApiClient
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadItem
import com.example.data.local.DownloadRepository
import com.example.data.local.DownloadStatus
import com.example.data.local.TrackedProject
import com.example.service.DownloadEngine
import com.example.util.GitHubUrlParser
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class MainUiState(
    val selectedTab: Int = 0, // 0 = Projects, 1 = Direct Link, 2 = Downloads History
    val urlInput: String = "",
    val customFileName: String = "",
    val isSystemDownloadManager: Boolean = false,
    val selectedFilter: FilterType = FilterType.ALL,
    val searchQuery: String = "",
    val activeDownload: DownloadItem? = null,
    val snackbarMessage: String? = null,
    val isRefreshingProjects: Boolean = false,
    val showAddProjectDialog: Boolean = false,
    val projectToEdit: TrackedProject? = null
)

enum class FilterType {
    ALL,
    APKS,
    ARCHIVES,
    COMPLETED
}

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DownloadRepository
    val downloadEngine: DownloadEngine

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    val trackedProjects: StateFlow<List<TrackedProject>>

    init {
        val database = AppDatabase.getInstance(application)
        repository = DownloadRepository(database.downloadDao(), database.trackedProjectDao())
        downloadEngine = DownloadEngine(application, repository)

        trackedProjects = repository.allTrackedProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed default project on first app open and auto-refresh tracked projects
        viewModelScope.launch {
            val list = repository.allTrackedProjects.first()
            if (list.isEmpty()) {
                addTrackedProject("https://github.com/j-hc/revanced-magisk-module/releases", isDefault = true)
            } else {
                refreshAllTrackedProjects(isSilent = true)
            }
        }
    }

    fun setSelectedTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    // Observe all downloads from Room
    val allDownloads: StateFlow<List<DownloadItem>> = combine(
        repository.allDownloads,
        _uiState
    ) { downloads, state ->
        var list = downloads
        
        // Search Filter
        if (state.searchQuery.isNotBlank()) {
            list = list.filter {
                it.fileName.contains(state.searchQuery, ignoreCase = true) ||
                it.url.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // Category Filter
        when (state.selectedFilter) {
            FilterType.ALL -> list
            FilterType.APKS -> list.filter { it.fileName.endsWith(".apk", ignoreCase = true) }
            FilterType.ARCHIVES -> list.filter {
                it.fileName.endsWith(".zip", ignoreCase = true) ||
                it.fileName.endsWith(".tar", ignoreCase = true) ||
                it.fileName.endsWith(".gz", ignoreCase = true) ||
                it.fileName.endsWith(".7z", ignoreCase = true) ||
                it.fileName.endsWith(".rar", ignoreCase = true)
            }
            FilterType.COMPLETED -> list.filter { it.status == DownloadStatus.COMPLETED }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeDownloadState: StateFlow<DownloadItem?> = downloadEngine.currentActiveDownload

    fun setShowAddProjectDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddProjectDialog = show)
    }

    fun setProjectToEdit(project: TrackedProject?) {
        _uiState.value = _uiState.value.copy(projectToEdit = project)
    }

    fun updateProjectUrl(project: TrackedProject, newUrl: String) {
        val parsed = GitHubApiClient.extractOwnerAndRepo(newUrl)
        if (parsed == null) {
            emitToast("Invalid URL. Enter a GitHub repo link like github.com/owner/repo")
            return
        }

        val (newOwner, newRepo) = parsed
        viewModelScope.launch {
            val updated = project.copy(
                projectUrl = "https://github.com/$newOwner/$newRepo",
                owner = newOwner,
                repo = newRepo,
                latestTagName = null,
                latestReleaseName = null,
                latestReleaseUrl = null,
                latestAssetsJson = null,
                lastError = null
            )
            repository.updateProject(updated)
            setProjectToEdit(null)
            emitToast("Updated project to $newOwner/$newRepo. Checking releases...")
            refreshTrackedProject(updated)
        }
    }

    fun addTrackedProject(url: String, isDefault: Boolean = false) {
        val parsed = GitHubApiClient.extractOwnerAndRepo(url)
        if (parsed == null) {
            if (!isDefault) emitToast("Invalid GitHub URL. Please enter owner/repo format")
            return
        }

        val (owner, repo) = parsed

        viewModelScope.launch {
            val existing = repository.getProjectByOwnerRepo(owner, repo)
            val projectToSave = existing ?: TrackedProject(
                projectUrl = "https://github.com/$owner/$repo",
                owner = owner,
                repo = repo
            )

            val id = if (existing == null) {
                repository.insertProject(projectToSave)
            } else {
                existing.id
            }

            val updatedProject = projectToSave.copy(id = id)
            refreshTrackedProject(updatedProject)
            if (!isDefault) {
                setShowAddProjectDialog(false)
            }
        }
    }

    fun refreshTrackedProject(project: TrackedProject) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshingProjects = true)
            val result = GitHubApiClient.fetchLatestRelease(project.owner, project.repo)

            result.onSuccess { releaseData ->
                val isNewTag = project.latestTagName != null && project.latestTagName != releaseData.tagName
                val updated = project.copy(
                    latestTagName = releaseData.tagName,
                    latestReleaseName = releaseData.releaseName,
                    latestReleaseUrl = releaseData.htmlUrl,
                    latestAssetsJson = GitHubApiClient.serializeAssets(releaseData.assets),
                    lastUpdated = System.currentTimeMillis(),
                    lastError = null
                )
                repository.updateProject(updated)

                if (isNewTag) {
                    NotificationHelper.sendNewReleaseNotification(
                        context = getApplication(),
                        owner = project.owner,
                        repo = project.repo,
                        tagName = releaseData.tagName,
                        releaseUrl = releaseData.htmlUrl
                    )
                    emitToast("🚀 New release found: ${releaseData.tagName} for ${project.owner}/${project.repo}")
                } else {
                    emitToast("Refreshed ${project.owner}/${project.repo}")
                }
            }.onFailure { err ->
                val updated = project.copy(
                    lastError = err.localizedMessage ?: "Fetch failed"
                )
                repository.updateProject(updated)
                emitToast("Error updating ${project.owner}/${project.repo}: ${err.localizedMessage}")
            }
            _uiState.value = _uiState.value.copy(isRefreshingProjects = false)
        }
    }

    fun refreshAllTrackedProjects(isSilent: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshingProjects = true)
            val list = repository.allTrackedProjects.first()
            var newReleasesCount = 0
            for (project in list) {
                val result = GitHubApiClient.fetchLatestRelease(project.owner, project.repo)
                result.onSuccess { releaseData ->
                    val isNewTag = project.latestTagName != null && project.latestTagName != releaseData.tagName
                    val updated = project.copy(
                        latestTagName = releaseData.tagName,
                        latestReleaseName = releaseData.releaseName,
                        latestReleaseUrl = releaseData.htmlUrl,
                        latestAssetsJson = GitHubApiClient.serializeAssets(releaseData.assets),
                        lastUpdated = System.currentTimeMillis(),
                        lastError = null
                    )
                    repository.updateProject(updated)

                    if (isNewTag) {
                        newReleasesCount++
                        NotificationHelper.sendNewReleaseNotification(
                            context = getApplication(),
                            owner = project.owner,
                            repo = project.repo,
                            tagName = releaseData.tagName,
                            releaseUrl = releaseData.htmlUrl
                        )
                    }
                }.onFailure { err ->
                    val updated = project.copy(lastError = err.localizedMessage)
                    repository.updateProject(updated)
                }
            }
            _uiState.value = _uiState.value.copy(isRefreshingProjects = false)
            if (!isSilent) {
                if (newReleasesCount > 0) {
                    emitToast("Found $newReleasesCount new releases!")
                } else {
                    emitToast("All tracked projects are up to date")
                }
            }
        }
    }

    fun deleteTrackedProject(project: TrackedProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
            emitToast("Removed project ${project.owner}/${project.repo}")
        }
    }

    fun downloadAsset(downloadUrl: String, fileName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedTab = 2) // Auto switch to Downloads tab
            emitToast("Starting download: $fileName")
            val result = downloadEngine.downloadFile(
                rawUrl = downloadUrl,
                customFileName = fileName,
                useSystemDownloadManager = _uiState.value.isSystemDownloadManager
            )

            result.onSuccess {
                emitToast("Downloading: ${it.fileName}")
            }.onFailure { e ->
                emitToast("Error starting download: ${e.localizedMessage}")
            }
        }
    }

    fun onUrlInputChanged(newUrl: String) {
        val parsedInfo = if (newUrl.isNotBlank()) GitHubUrlParser.parseAndNormalizeUrl(newUrl) else null
        _uiState.value = _uiState.value.copy(
            urlInput = newUrl,
            customFileName = parsedInfo?.fileName ?: _uiState.value.customFileName
        )
    }

    fun onCustomFileNameChanged(newName: String) {
        _uiState.value = _uiState.value.copy(customFileName = newName)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setFilterType(filterType: FilterType) {
        _uiState.value = _uiState.value.copy(selectedFilter = filterType)
    }

    fun toggleSystemDownloadManager(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isSystemDownloadManager = enabled)
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
            if (pastedText.isNotBlank()) {
                onUrlInputChanged(pastedText)
                emitToast("Pasted URL from clipboard")
            } else {
                emitToast("Clipboard is empty")
            }
        } else {
            emitToast("No text in clipboard")
        }
    }

    fun handleSharedIntentUrl(url: String) {
        if (url.isNotBlank()) {
            onUrlInputChanged(url)
            emitToast("Received shared link")
        }
    }

    fun startDownload() {
        val url = _uiState.value.urlInput.trim()
        if (url.isBlank()) {
            emitToast("Please enter a valid download URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedTab = 2) // Auto switch to Downloads tab
            emitToast("Starting download...")
            val customName = _uiState.value.customFileName.trim().takeIf { it.isNotBlank() }
            val result = downloadEngine.downloadFile(
                rawUrl = url,
                customFileName = customName,
                useSystemDownloadManager = _uiState.value.isSystemDownloadManager
            )

            result.onSuccess {
                emitToast("Download started: ${it.fileName}")
            }.onFailure { e ->
                emitToast("Failed to start download: ${e.localizedMessage}")
            }
        }
    }

    fun cancelDownload(id: Long) {
        downloadEngine.cancelDownload(id)
        emitToast("Download cancelled")
    }

    fun reDownload(item: DownloadItem) {
        _uiState.value = _uiState.value.copy(urlInput = item.url, customFileName = item.fileName)
        startDownload()
    }

    fun openFile(item: DownloadItem) {
        val error = downloadEngine.openDownloadedFile(item)
        if (error != null) {
            emitToast(error)
        }
    }

    fun shareFile(item: DownloadItem) {
        val error = downloadEngine.shareDownloadedFile(item)
        if (error != null) {
            emitToast(error)
        }
    }

    fun deleteDownload(item: DownloadItem, deleteFileFromStorage: Boolean = true) {
        viewModelScope.launch {
            if (deleteFileFromStorage) {
                try {
                    val file = File(item.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
            repository.delete(item)
            emitToast("Removed file from history")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            emitToast("Cleared download history")
        }
    }

    fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    private fun emitToast(msg: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = msg)
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowToast(msg))
        }
    }
}
