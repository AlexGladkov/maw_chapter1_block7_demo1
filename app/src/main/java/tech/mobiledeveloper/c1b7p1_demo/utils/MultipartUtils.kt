package tech.mobiledeveloper.c1b7p1_demo.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

fun queryDisplayName(resolver: ContentResolver, uri: Uri): String {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { column ->
        val index = column.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (column.moveToFirst() && index >= 0) return column.getString(index)
    }

    return "upload"
}

fun queryLength(resolver: ContentResolver, uri: Uri): Long {
    resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor -> return descriptor.length }
    return -1L
}

fun buildMultipartData(
    formTitle: String,
    context: Context,
    uri: Uri,
    onProgress: (Long, Long) -> Unit
): MultipartBody.Part {
    val resolver = context.contentResolver
    val name = queryDisplayName(resolver, uri)
    val type = resolver.getType(uri) ?: "application/octet-stream"

    val body = ProgressRequestBody(
        context = context,
        uri = uri,
        contentType = type.toMediaTypeOrNull(),
        progress = onProgress
    )

    return MultipartBody.Part.createFormData(formTitle, name, body)
}

class ProgressRequestBody(
    private val context: Context,
    private val uri: Uri,
    private val contentType: MediaType?,
    private val progress: (bytesWritten: Long, total: Long) -> Unit
): RequestBody() {
    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            return descriptor.length
        }

        return -1
    }

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        var written = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
            val src = input.source()
            val buf = 8L * 1024L
            while(true) {
                val read = src.read(sink.buffer, buf)
                if (read == -1L) break
                written += read
                sink.flush()
                progress(written, total)
            }
        }
    }

}