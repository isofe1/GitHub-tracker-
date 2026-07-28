package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedProjectDao {
    @Query("SELECT * FROM tracked_projects ORDER BY addedAt DESC")
    fun getAllTrackedProjects(): Flow<List<TrackedProject>>

    @Query("SELECT * FROM tracked_projects")
    suspend fun getAllTrackedProjectsList(): List<TrackedProject>

    @Query("SELECT * FROM tracked_projects WHERE owner = :owner AND repo = :repo LIMIT 1")
    suspend fun getProjectByOwnerRepo(owner: String, repo: String): TrackedProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: TrackedProject): Long

    @Update
    suspend fun update(project: TrackedProject)

    @Delete
    suspend fun delete(project: TrackedProject)

    @Query("DELETE FROM tracked_projects WHERE id = :id")
    suspend fun deleteById(id: Long)
}
