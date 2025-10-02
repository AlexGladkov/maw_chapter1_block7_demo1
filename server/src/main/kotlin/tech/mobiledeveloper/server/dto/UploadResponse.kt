package tech.mobiledeveloper.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val id: String,
    val fileName: String,
    val mediaType: String? = null,
    val sizeBytes: Long,
    val url: String
)