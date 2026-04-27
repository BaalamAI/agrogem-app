package com.agrogem.app.data.pest.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PestUploadUrlResponse(
    @SerialName("object_path")
    val objectPath: String,
    @SerialName("signed_url")
    val signedUrl: String,
    @SerialName("content_type")
    val contentType: String,
    @SerialName("expires_in_seconds")
    val expiresInSeconds: Int,
)

@Serializable
data class PestIdentifyResponse(
    @SerialName("top_match")
    val topMatch: TopMatch? = null,
    val votes: Map<String, Int> = emptyMap(),
)

@Serializable
data class TopMatch(
    @SerialName("pest_name")
    val pestName: String,
    val similarity: Double,
    val confidence: String,
)
