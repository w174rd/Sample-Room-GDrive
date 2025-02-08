package com.w174rd.sample_room_gdrive.model

import com.google.gson.annotations.SerializedName

class Meta() {
    @SerializedName("error")
    var error: Int? = null
    @SerializedName("code")
    var code: Int? = null
    @SerializedName("message")
    var message: String? = null

    constructor(error: Int?, code: Int?, message: String?) : this() {
        this.error = error
        this.code = code
        this.message = message
    }
}