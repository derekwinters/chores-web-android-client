package com.derekwinters.chores.network

import com.derekwinters.chores.auth.SessionManager
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Global session handling: on any 401 response, clears the persisted token. The root composable
 * observes [SessionManager.authState] and reactively navigates back to the Login screen — mirroring
 * chores-web's `frontend/src/api/client.js` behavior of clearing the token on session expiry.
 *
 * The 401 response itself is still returned unmodified so the caller can surface
 * [com.derekwinters.chores.common.StatusCodeFallback]'s message.
 */
class UnauthorizedInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            sessionManager.clearToken()
        }
        return response
    }
}
