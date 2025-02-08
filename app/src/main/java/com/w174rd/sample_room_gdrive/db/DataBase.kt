package com.w174rd.sample_room_gdrive.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.w174rd.sample_room_gdrive.model.Entity

@Database(entities = [Entity::class], version = 1)
abstract class DataBase: RoomDatabase() {
    abstract fun entityDao(): EntityDao
}