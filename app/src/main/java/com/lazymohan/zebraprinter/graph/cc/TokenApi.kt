package com.lazymohan.zebraprinter.graph.cc

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path

interface TokenApi {
    @FormUrlEncoded
    @POST("{tenantId}/oauth2/v2.0/token")
    suspend fun clientCredentialsToken(
        @Path("tenantId") tenantId: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("scope") scope: String = "https://graph.microsoft.com/.default"
    ): TokenResponse
}
