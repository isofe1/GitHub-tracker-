package com.example.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.api.GitHubApiClient
import com.example.data.local.AppDatabase
import com.example.util.NotificationHelper
import java.util.concurrent.TimeUnit

class ReleaseCheckWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val db = AppDatabase.getInstance(appContext)
            val dao = db.trackedProjectDao()
            val projects = dao.getAllTrackedProjectsList()

            for (project in projects) {
                val fetchResult = GitHubApiClient.fetchLatestRelease(project.owner, project.repo)
                fetchResult.onSuccess { releaseData ->
                    val isNewTag = project.latestTagName != null && project.latestTagName != releaseData.tagName
                    val updated = project.copy(
                        latestTagName = releaseData.tagName,
                        latestReleaseName = releaseData.releaseName,
                        latestReleaseUrl = releaseData.htmlUrl,
                        latestAssetsJson = GitHubApiClient.serializeAssets(releaseData.assets),
                        lastUpdated = System.currentTimeMillis(),
                        lastError = null
                    )
                    dao.update(updated)

                    if (isNewTag) {
                        NotificationHelper.sendNewReleaseNotification(
                            context = appContext,
                            owner = project.owner,
                            repo = project.repo,
                            tagName = releaseData.tagName,
                            releaseUrl = releaseData.htmlUrl
                        )
                    }
                }.onFailure { err ->
                    val updated = project.copy(lastError = err.localizedMessage)
                    dao.update(updated)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "GitHubReleaseCheckWorker"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ReleaseCheckWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
