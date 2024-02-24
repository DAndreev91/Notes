package com.example.test.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_archive")
data class NoteArchive (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") var desc: String,
    @ColumnInfo(name = "done_date", defaultValue = "CURRENT_TIMESTAMP") var doneDate: String,
    @ColumnInfo(name = "is_section") val isSection: Boolean,
    @ColumnInfo("position") val pos: Int,
    @ColumnInfo("section") val section: String
)