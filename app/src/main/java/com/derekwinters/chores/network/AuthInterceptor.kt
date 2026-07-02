package com.derekwinters.chores.network

import com.derekwinters.chores.auth.SessionManager
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer <token>` to every request when a token is persisted.
 * See docs/adr/0002-network-auth-architecture.md.
 */
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = sessionManager.token ?: return chain.proceed(request)

        val authenticated = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticated)
    }
}
