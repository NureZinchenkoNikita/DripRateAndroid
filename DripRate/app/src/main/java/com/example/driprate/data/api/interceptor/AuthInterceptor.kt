package com.example.driprate.data.api.interceptor

import com.example.driprate.data.api.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        
        // Skip auth endpoints
        if (path.contains("/api/Auth/login") || path.contains("/api/Auth/register")) {
            return chain.proceed(request)
        }

        val token = TokenManager.token
        if (token.isNullOrBlank()) {
            return chain.proceed(request)
        }

        // Final safety check for quotes to prevent 401 loops
        val cleanToken = token.replace("\"", "").trim()
        
        val authenticatedRequest = request.newBuilder()
            .header("Authorization", "Bearer $cleanToken")
            .build()
            
        return chain.proceed(authenticatedRequest)
    }
}
