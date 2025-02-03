package com.example.sample.api

//import retrofit2.http.Field
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface FlaskApiService {
    @Multipart
    @POST("upload") // Flaskサーバーのエンドポイント
    fun uploadFile(@Part file: MultipartBody.Part): Call<ClumsyResult>

    @POST("/mail")
    fun sendMail(@Body requestBody: MailRequest): Call<MailResult>

    @POST("/family")
    fun sendFamily(@Body requestBody: MailRequest): Call<MailResult>
}

data class ClumsyResult(
    val mfcc: List<Float>
)

data class MailRequest(
    val name: String,
    val mail: String?
)
data class MailResult(
    val message: String
)
