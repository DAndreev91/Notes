package com.example.test.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") var desc: String,
    @ColumnInfo(name = "is_checked") var isChecked: Boolean,
    @ColumnInfo(name = "is_future") var isFuture: Boolean,
    @ColumnInfo(name = "done_date", defaultValue = "CURRENT_TIMESTAMP") var doneDate: String,
    @ColumnInfo(name = "is_section") var isSection: Boolean,
    @ColumnInfo("position") var pos: Int,
    @ColumnInfo("section") var section: String
)
