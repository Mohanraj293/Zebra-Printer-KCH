package com.lazymohan.zebraprinter.graph.cc

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("expires_in")
    val expiresIn: Long,

    @SerializedName("ext_expires_in")
    val extExpiresIn: Long?,

    @SerializedName("access_token")
    val accessToken: String
)
