package tech.mobiledeveloper.c1b7p1_demo.api

import kotlinx.serialization.Serializable

@Serializable
data class ChunkUploadResponse(
    val completed: Boolean,
    val url: String? = null,
    val finishedAt: String? = null,
    val receivedIndex: Int? = null
)