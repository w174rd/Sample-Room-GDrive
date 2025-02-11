package com.w174rd.sample_room_gdrive.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Entity (
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Long? = System.currentTimeMillis(),
    @ColumnInfo(name = "name") val name: String? = null
)