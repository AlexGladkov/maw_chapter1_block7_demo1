package tech.mobiledeveloper.c1b7p1_demo.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadApi {

    @Multipart
    @Headers("Accept: application/json")
    @POST("/api/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("message") message: String
    ): UploadResponse

    @Multipart
    @Headers("Accept: application/json")
    @POST("/api/uploadMany")
    suspend fun uploadMany(
        @Part files: List<MultipartBody.Part>
    ): List<UploadResponse>

    @Multipart
    @POST("api/upload/chunk")
    suspend fun uploadChunk(
        @Part("fileId") fileId: RequestBody,
        @Part("index") index: RequestBody,
        @Part("total") total: RequestBody,
        @Part chunk: MultipartBody.Part
    ): ChunkUploadResponse
}