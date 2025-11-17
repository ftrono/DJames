package com.ftrono.DJames.be.utils

import android.util.Log
import com.ftrono.DJames.be.models.HttpResponse
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class HttpClient() {
    //OkHTTP: build & get HTTP client:
    fun getClient(timeout: Long): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .callTimeout(timeout, TimeUnit.SECONDS)
            .build()
    }

    //OkHTTP: make HTTP request:
    suspend fun makeRequest(client: OkHttpClient, request: Request): HttpResponse =
        suspendCoroutine { continuation ->
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(
                        HttpResponse(
                            code = response.code,
                            body = response.body!!.string()
                        )
                    )
                }

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(
                        HttpResponse(
                            code = -1,  // -1 to indicate failure
                            body = ""
                        )
                    )
                    Log.d("TAG", "RESPONSE ERROR: ", e)
                }
            })
        }
}