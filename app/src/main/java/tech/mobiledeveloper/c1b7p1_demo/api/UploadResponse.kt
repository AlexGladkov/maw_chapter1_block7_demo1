package tech.mobiledeveloper.c1b7p1_demo.api

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val id: String,
    val fileName: String,
    val mediaType: String? = null,
    val sizeBytes: Long,
    val url: String
)