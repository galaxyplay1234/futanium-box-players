package com.futaniumbox.players.model

import com.google.gson.annotations.SerializedName

data class Channel(

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("link")
    val link: String? = null,

    @SerializedName("captureM3u8")
    val captureM3u8: Boolean = false

)