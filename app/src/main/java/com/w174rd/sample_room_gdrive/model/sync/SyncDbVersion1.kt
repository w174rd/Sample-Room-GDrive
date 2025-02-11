package com.w174rd.sample_room_gdrive.model.sync

import com.google.gson.annotations.SerializedName

data class DBVersino1 (
    @SerializedName("db_version")
    val dbVersion: Int,
    @SerializedName("entity_data")
    val entityData: List<EntityData>
)

data class EntityData(
    val id: Long? = null,
    val value: String? = null
)
