package com.lazymohan.zebraprinter.graph

import com.lazymohan.zebraprinter.graph.cc.TokenApi
import com.lazymohan.zebraprinter.graph.cc.TokenRepository
import com.lazymohan.zebraprinter.graph.drive.GraphDriveApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object GraphNetworkModule {

    // -------- OkHttp --------

    /** Plain client (no auth) for AAD token endpoint */
    @Provides @Singleton @Named("PlainOkHttp")
    fun providePlainOkHttp(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    /** Graph client that adds Authorization: Bearer <token> using TokenRepository */
    @Provides @Singleton @Named("GraphOkHttp")
    fun provideGraphOkHttp(tokenRepo: TokenRepository): OkHttpClient {
        val auth = Interceptor { chain ->
            val accessToken = kotlinx.coroutines.runBlocking { tokenRepo.getAccessToken() }
            val req = chain.request().newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
            chain.proceed(req)
        }
        return OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    // -------- Retrofit --------

    /** Retrofit for token calls (login.microsoftonline.com) */
    @Provides @Singleton @Named("TokenRetrofit")
    fun provideTokenRetrofit(@Named("PlainOkHttp") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://login.microsoftonline.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** Retrofit for Graph calls (graph.microsoft.com) */
    @Provides @Singleton @Named("GraphRetrofit")
    fun provideGraphRetrofit(@Named("GraphOkHttp") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://graph.microsoft.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // -------- APIs / Repos --------

    @Provides @Singleton
    fun provideTokenApi(@Named("TokenRetrofit") retrofit: Retrofit): TokenApi =
        retrofit.create(TokenApi::class.java)

    @Provides @Singleton
    fun provideTokenRepo(api: TokenApi): TokenRepository = TokenRepository(api)

    @Provides @Singleton
    fun provideDriveApi(@Named("GraphRetrofit") retrofit: Retrofit): GraphDriveApi =
        retrofit.create(GraphDriveApi::class.java)
}
