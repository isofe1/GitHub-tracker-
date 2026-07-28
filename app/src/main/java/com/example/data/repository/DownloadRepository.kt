package com.example.data.local

import kotlinx.coroutines.flow.Flow

class DownloadRepository(
    private val dao: DownloadDao,
    private val projectDao: TrackedProjectDao
) {
    val allDownloads: Flow<List<DownloadItem>> = dao.getAllDownloads()
    val allTrackedProjects: Flow<List<TrackedProject>> = projectDao.getAllTrackedProjects()

    suspend fun getDownloadById(id: Long): DownloadItem? = dao.getDownloadById(id)

    suspend fun getDownloadByUrl(url: String): DownloadItem? = dao.getDownloadByUrl(url)

    suspend fun insert(item: DownloadItem): Long = dao.insert(item)

    suspend fun update(item: DownloadItem) = dao.update(item)

    suspend fun delete(item: DownloadItem) = dao.delete(item)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()

    // Tracked Projects
    suspend fun getProjectByOwnerRepo(owner: String, repo: String): TrackedProject? = projectDao.getProjectByOwnerRepo(owner, repo)

    suspend fun insertProject(project: TrackedProject): Long = projectDao.insert(project)

    suspend fun updateProject(project: TrackedProject) = projectDao.update(project)

    suspend fun deleteProject(project: TrackedProject) = projectDao.delete(project)

    suspend fun deleteProjectById(id: Long) = projectDao.deleteById(id)
}
