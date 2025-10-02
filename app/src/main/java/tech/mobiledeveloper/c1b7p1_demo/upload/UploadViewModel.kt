package tech.mobiledeveloper.c1b7p1_demo.upload

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import tech.mobiledeveloper.c1b7p1_demo.api.ChunkUploadResponse
import tech.mobiledeveloper.c1b7p1_demo.api.UploadResponse
import tech.mobiledeveloper.c1b7p1_demo.network.Client
import tech.mobiledeveloper.c1b7p1_demo.utils.buildMultipartData
import tech.mobiledeveloper.c1b7p1_demo.utils.queryDisplayName
import tech.mobiledeveloper.c1b7p1_demo.utils.queryLength
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.UUID

enum class UploadStatus { Idle, Uploading, Done, Failed }

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val mime: String,
    val size: Long,
    val progress: Int = 0,
    val status: UploadStatus = UploadStatus.Idle,
    val resultUrl: String? = null,
    val error: String? = null
)

data class UploadViewState(
    val files: List<SelectedFile> = emptyList(),
    val isUploading: Boolean = false,
    val lastMessage: String? = null
)

class UploadViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(UploadViewState())
    val state: StateFlow<UploadViewState> = _state

    fun onPickSingle(uri: Uri?) {
        if (uri == null) return
        val contentResolver = getApplication<Application>().contentResolver
        val file = SelectedFile(
            uri = uri,
            name = queryDisplayName(contentResolver, uri),
            mime = contentResolver.getType(uri) ?: "application/octet-stream",
            size = queryLength(contentResolver, uri)
        )

        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {

        }

        _state.value = _state.value.copy(files = listOf(file), lastMessage = null)
    }

    fun onPickMultiple(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val contentResolver = getApplication<Application>().contentResolver
        val list = uris.map { uri ->
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {

            }

            SelectedFile(
                uri = uri,
                name = queryDisplayName(contentResolver, uri),
                mime = contentResolver.getType(uri) ?: "application/octet-stream",
                size = queryLength(contentResolver, uri)
            )
        }

        _state.value = _state.value.copy(files = list, lastMessage = null)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(files = emptyList(), lastMessage = null)
    }

    fun uploadInstance() {
        val files = _state.value.files
        if (files.isEmpty()) return

        _state.value = _state.value.copy(isUploading = true, lastMessage = "Uploading...")
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val uploadApi = Client.request()

            val updated = files.toMutableList()
            for ((index, f) in files.withIndex()) {
                updated[index] = f.copy(
                    progress = 0,
                    status = UploadStatus.Uploading,
                    error = null,
                    resultUrl = null
                )

                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(files = updated.toList())
                }
            }

            try {
                val parts = files.mapIndexed { index, f ->
                    buildMultipartData("files", context, f.uri) { written, total ->
                        if (total > 0) {
                            val progress = (written * 100 / total).toInt().coerceIn(0, 100)
                            updated[index] = updated[index].copy(progress = progress)
                            _state.value = _state.value.copy(files = updated.toList())
                        }
                    }
                }

                val response: List<UploadResponse> = uploadApi.uploadMany(parts)
                for ((index, f) in files.withIndex()) {
                    updated[index] = f.copy(
                        progress = 100,
                        status = UploadStatus.Done,
                        error = null,
                        resultUrl = response[index].url
                    )

                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(files = updated.toList())
                    }
                }
            } catch (e: kotlin.Exception) {
                for ((index, f) in files.withIndex()) {
                    updated[index] = f.copy(
                        progress = 0,
                        status = UploadStatus.Failed,
                        error = e.message,
                        resultUrl = null
                    )

                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(files = updated.toList())
                    }
                }
            }
        }
    }

    fun uploadSequential() {
        val files = _state.value.files
        if (files.isEmpty()) return

        _state.value = _state.value.copy(isUploading = true, lastMessage = "Uploading...")

        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val uploadApi = Client.request()

            val updated = files.toMutableList()
            for ((index, f) in files.withIndex()) {
                updated[index] = f.copy(
                    progress = 0,
                    status = UploadStatus.Uploading,
                    error = null,
                    resultUrl = null
                )

                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(files = updated.toList())
                }

                try {
                    val part = buildMultipartData("file", context, f.uri) { written, total ->
                        if (total > 0) {
                            val progress = (written * 100 / total).toInt().coerceIn(0, 100)
                            updated[index] = updated[index].copy(progress = progress)
                            _state.value = _state.value.copy(files = updated.toList())
                        }
                    }
                    val response: UploadResponse = uploadApi.upload(part, "from compose client")
                    updated[index] = updated[index].copy(
                        progress = 100,
                        status = UploadStatus.Done,
                        resultUrl = response.url,
                        error = null
                    )
                } catch (e: Exception) {
                    Log.e("TAG", "Error upload: ${e.message}")
                    updated[index] = updated[index].copy(
                        progress = 0,
                        status = UploadStatus.Failed,
                        resultUrl = null,
                        error = e.message
                    )
                }

                _state.value = _state.value.copy(files = updated.toList())
            }
        }

        _state.value = _state.value.copy(isUploading = false, lastMessage = "Done")
    }

    fun uploadChunk(chunkSizeBytes: Int = 2 * 1024) {
        val files = _state.value.files
        if (files.isEmpty()) return
        val file = files.first()

        val context = getApplication<Application>()
        val size = queryLength(context.contentResolver, file.uri)
        val total = chunkCount(size, chunkSizeBytes)
        if (total == 0) return

        val fileId = UUID.randomUUID().toString()

        _state.value = _state.value.copy(
            isUploading = true,
            lastMessage = "Chunks..",
            files = listOf(
                files.toMutableList().first().copy(
                    status = UploadStatus.Uploading,
                    progress = 0,
                    error = null,
                    resultUrl = null
                )
            )
        )

        viewModelScope.launch(Dispatchers.IO) {
            val uploadApi = Client.request()
            var uploadedBytes = 0L
            var finalUrl: String? = null
            var error: String? = null

            try {
                for (i in 0 until total) {
                    val offset = i.toLong() * chunkSizeBytes
                    Log.e("TAG", "Chunk $i started with offset $offset")
                    val (temp, read) = readChunkToTempFile(context, file.uri, offset, chunkSizeBytes)
                    if (read <= 0) {
                        temp.delete()
                        throw IllegalStateException("Empty chunk at index $i")
                    }

                    var part = MultipartBody.Part.createFormData(
                        name = "chunk",
                        filename = "chunk_$i.bin",
                        body = temp.asRequestBody("application/octet-stream".toMediaType())
                    )

                    val response: ChunkUploadResponse = uploadApi.uploadChunk(
                        fileId = textPart(fileId),
                        index = textPart(i.toString()),
                        total = textPart(total.toString()),
                        chunk = part
                    )

                    uploadedBytes += read
                    val progress = (uploadedBytes * 100 / size).toInt().coerceIn(0, 100)
                    val list = _state.value.files.toMutableList()
                    list[0] = list[0].copy(progress = progress)
                    _state.value = _state.value.copy(files = list)

                    temp.delete()

                    if (response.completed) {
                        finalUrl = response.url
                        break
                    }
                }
            } catch (e: Exception) {
                error = e.message
            }

            val list = _state.value.files.toMutableList()
            list[0] = list[0].copy(
                status = if (error == null) UploadStatus.Done else UploadStatus.Failed,
                progress = if (error == null) 100 else 0,
                resultUrl = finalUrl,
                error = error
            )

            _state.value = _state.value.copy(files = list, isUploading = false, lastMessage = if (error == null) "Done" else "Failed $error")
        }
    }

    private fun readChunkToTempFile(
        context: Context,
        uri: Uri,
        offset: Long,
        maxLength: Int
    ): Pair<File, Int> {
        val temp = File.createTempFile("chunk_", ".bin", context.cacheDir)
        var readTotal = 0
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                channel.position(offset)
                FileOutputStream(temp).channel.use { out ->
                    val bufSize = 128 * 1024
                    val buffer = ByteBuffer.allocate(bufSize)
                    while (readTotal < maxLength) {
                        buffer.clear()
                        val need = minOf(bufSize, maxLength - readTotal)
                        buffer.limit(need)
                        val n = channel.read(buffer)
                        if (n <= 0) break
                        buffer.flip()
                        out.write(buffer)
                        readTotal += n
                    }
                }
            }
        }

        return temp to readTotal
    }

    private fun textPart(value: String): RequestBody =
        value.toRequestBody("text/plain".toMediaType())

    private fun chunkCount(size: Long, chunkSizeBytes: Int): Int =
        if (size <= 0) 0 else ((size + chunkSizeBytes - 1) / chunkSizeBytes).toInt()
}