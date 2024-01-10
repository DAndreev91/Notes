package com.example.test.data

import androidx.room.Database
import androidx.room.Entity
import androidx.room.RoomDatabase

@Database(entities = [Note::class], version = 1)
abstract class NoteRoomDatabase: RoomDatabase()  {

}