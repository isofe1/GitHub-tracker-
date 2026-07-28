package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_projects")
data class TrackedProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectUrl: String,
    val owner: String,
    val repo: String,
    val addedAt: Long = System.currentTimeMillis(),
    val latestTagName: String? = null,
    val latestReleaseName: String? = null,
    val latestReleaseUrl: String? = null,
    val latestAssetsJson: String? = null,
    val lastUpdated: Long = 0,
    val lastError: String? = null
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val downloadCount: Int = 0
)
