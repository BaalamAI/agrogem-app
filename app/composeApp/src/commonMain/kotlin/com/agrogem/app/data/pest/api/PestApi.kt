package com.agrogem.app.data.pest.api

interface PestApi {
    suspend fun getUploadUrl(): PestUploadUrlResponse
    suspend fun uploadImage(signedUrl: String, imageBytes: ByteArray)
    suspend fun identify(objectPath: String): PestIdentifyResponse
}
