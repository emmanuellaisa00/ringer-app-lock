package com.example.ringer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM locked_apps")
    fun getAllLockedApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName")
    suspend fun getAppByPackage(packageName: String): AppInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfo)

    @Delete
    suspend fun deleteApp(app: AppInfo)

    @Query("DELETE FROM locked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("UPDATE locked_apps SET isLocked = :isLocked WHERE packageName = :packageName")
    suspend fun updateLockStatus(packageName: String, isLocked: Boolean)
}