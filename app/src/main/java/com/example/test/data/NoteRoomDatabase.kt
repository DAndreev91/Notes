package com.example.test.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Note::class], version = 2)
abstract class NoteRoomDatabase: RoomDatabase()  {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteRoomDatabase? = null
        fun getDatabase(context: Context): NoteRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    NoteRoomDatabase::class.java,
                    "note_database"
                ).fallbackToDestructiveMigration()
                //.createFromAsset("notes.db")
                .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}