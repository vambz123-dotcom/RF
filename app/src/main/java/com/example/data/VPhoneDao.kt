package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VPhoneDao {
    @Query("SELECT * FROM v_phones ORDER BY createdAt DESC")
    fun getAllPhones(): Flow<List<VPhone>>

    @Query("SELECT * FROM v_phones WHERE id = :id")
    fun getPhoneById(id: Int): Flow<VPhone?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhone(phone: VPhone): Long

    @Update
    suspend fun updatePhone(phone: VPhone)

    @Delete
    suspend fun deletePhone(phone: VPhone)

    @Query("DELETE FROM v_phones WHERE id = :id")
    suspend fun deletePhoneById(id: Int)
}
