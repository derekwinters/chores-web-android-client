package com.derekwinters.chores.network.di

import com.derekwinters.chores.auth.AuthApi
import com.derekwinters.chores.chores.ChoresApi
import com.derekwinters.chores.chores.PeopleApi
import com.derekwinters.chores.network.AuthInterceptor
import com.derekwinters.chores.network.ServerUrlInterceptor
import com.derekwinters.chores.network.UnauthorizedInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Provides the OkHttp/Retrofit/API-service singletons. The Retrofit instance is built once with a
 * placeholder base URL and never rebuilt — [ServerUrlInterceptor] rewrites each request's
 * scheme/host/port from the persisted, user-entered server URL instead. See
 * docs/adr/0002-network-auth-architecture.md.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Never dispatched to directly — every request is rewritten by [ServerUrlInterceptor]. */
    private const val PLACEHOLDER_BASE_URL = "http://localhost/v1/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        serverUrlInterceptor: ServerUrlInterceptor,
        authInterceptor: AuthInterceptor,
        unauthorizedInterceptor: UnauthorizedInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(serverUrlInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(unauthorizedInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideChoresApi(retrofit: Retrofit): ChoresApi = retrofit.create(ChoresApi::class.java)

    @Provides
    @Singleton
    fun providePeopleApi(retrofit: Retrofit): PeopleApi = retrofit.create(PeopleApi::class.java)
}
