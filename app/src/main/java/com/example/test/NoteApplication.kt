package com.example.test

import android.app.Application
import com.example.test.data.NoteRoomDatabase

class NoteApplication: Application() {
    val database: NoteRoomDatabase by lazy { NoteRoomDatabase.getDatabase(this) }
}