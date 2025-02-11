package com.w174rd.sample_room_gdrive.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.w174rd.sample_room_gdrive.model.Entity

@Dao
interface EntityDao {
    @Query("SELECT * FROM entity")
    fun getAll(): List<Entity>

    @Insert
    fun insert(data: Entity)

    @Insert
    fun insertAll(data: List<Entity>)

    @Update
    fun update(data: Entity)

    @Delete
    fun delete(data: Entity)
}