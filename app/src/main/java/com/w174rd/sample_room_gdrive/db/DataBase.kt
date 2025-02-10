package com.w174rd.sample_room_gdrive.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.w174rd.sample_room_gdrive.model.Entity
import com.w174rd.sample_room_gdrive.utils.Attributes

@Database(entities = [Entity::class], version = Attributes.database.version)
abstract class DataBase: RoomDatabase() {
    abstract fun entityDao(): EntityDao
}