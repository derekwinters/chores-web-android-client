package com.derekwinters.chores.network

import com.derekwinters.chores.auth.SessionManager
import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites every request's scheme/host/port to the user-entered, persisted server URL
 * ([SessionManager.serverUrl]) rather than rebuilding the Retrofit instance per URL change —
 * see docs/adr/0002-network-auth-architecture.md.
 *
 * Retrofit is constructed with a fixed placeholder base URL purely to satisfy its API; the
 * scheme/host/port it produces are always overwritten here before the request is dispatched.
 * The request's path, query, method and body are left untouched.
 */
class ServerUrlInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val serverUrl = sessionManager.serverUrl?.let(::normalize)?.toHttpUrlOrNull()
            ?: return chain.proceed(request)

        val rewrittenUrl = request.url.newBuilder()
            .scheme(serverUrl.scheme)
            .host(serverUrl.host)
            .port(serverUrl.port)
            .build()

        return chain.proceed(request.newBuilder().url(rewrittenUrl).build())
    }

    /** Users may enter a server URL without a scheme (e.g. "192.168.1.20:8000"); default to http. */
    private fun normalize(url: String): String =
        if (url.startsWith("http://") || url.startsWith("https://")) url else "http://$url"
}
