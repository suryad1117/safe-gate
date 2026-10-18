package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE fileType = :fileType ORDER BY createdAt DESC")
    fun getVaultItemsByType(fileType: String): Flow<List<VaultItemEntity>>

    @Query("SELECT COUNT(*) FROM vault_items WHERE fileType = :fileType")
    fun getCountByType(fileType: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItems(items: List<VaultItemEntity>)

    @Delete
    suspend fun deleteVaultItem(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItemById(id: Long)
}
