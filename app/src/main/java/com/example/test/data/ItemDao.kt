package com.example.test.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Note)

    @Update
    suspend fun update(item: Note)

    @Delete
    suspend fun delete(item: Note)

    @Query("select * from notes order by id")
    fun getNotes(): Flow<List<Note>>

    @Query("select * from notes where id = :itemId")
    fun getNote(itemId: Int): Flow<Note>
}