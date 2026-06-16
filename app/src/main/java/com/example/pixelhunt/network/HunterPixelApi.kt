package com.example.pixelhunt.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class ProcessResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("image")
    val image: String?,
    
    @SerializedName("is_correct")
    val is_correct: Boolean? = null,
    
    @SerializedName("detected_object")
    val detected_object: String? = null,
    
    @SerializedName("message")
    val message: String? = null
)

interface HunterPixelApi {
    @Multipart
    @POST("process")
    suspend fun processImage(
        @Part image: MultipartBody.Part,
        @Part("x") x: RequestBody,
        @Part("y") y: RequestBody,
        @Part("target_object") targetObject: RequestBody
    ): ProcessResponse
}
