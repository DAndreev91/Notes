package com.example.test.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Note)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Note>)

    @Update
    suspend fun update(item: Note)

    @Delete
    suspend fun delete(item: Note)

    @Query("delete from notes")
    suspend fun deleteAll()

    @Query("select * from notes order by position")
    fun getNotes(): Flow<List<Note>>

    @Query("select * from notes where id = :itemId")
    fun getNote(itemId: Int): Flow<Note>

    @Query("select * from notes where id = :itemId")
    fun getNoteById(itemId: Int): Note

    // Обновляем позиции заметок при перемещении внутри списка с обновлением флагов и секции
    @Query("update notes " +
            "set position = case position " +
            "when :from then :to else position + (:from-:to)/abs(:from-:to) end," +
            "section = case position when :from then :newSection else section end," +
            "is_future = case when position = :from and :newSection = 'Planned' then 1 else 0 end," +
            "is_checked = case when position = :from and :newSection = 'Done' then 1 else 0 end " +
            "where position between :from and :to or position between :to and :from")
    suspend fun updateNotesPositions(from: Int, to: Int, newSection: String)

    // Сжимаем позиции после удаления записи
    @Query("update notes set position = position - 1 where position > :position")
    suspend fun compressPositionsAfterPosition(position: Int)

    // Расширяем обратно позиции после восстановления удалённой записи
    @Query("update notes set position = position + 1 where position >= :position")
    suspend fun stretchPositionsAfterPosition(position: Int)

    @Query("select * from notes_archive order by done_date desc")
    fun getArchiveNotes(): Flow<List<NoteArchive>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchiveAll(list: List<NoteArchive>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchive(item: NoteArchive)

    @Delete
    suspend fun deleteArchive(item: NoteArchive)

    @Query("select * from notes where section = 'Done' and date('now') - date(done_date) > :hoursBeforeArchive order by done_date")
    suspend fun getNotesNeededToArchive(hoursBeforeArchive: Double): List<Note>

    @Delete
    suspend fun deleteNotes(list: List<Note>)
}