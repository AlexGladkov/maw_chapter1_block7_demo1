package tech.mobiledeveloper.server.controller

import jakarta.validation.constraints.NotBlank
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.unit.DataSize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import tech.mobiledeveloper.server.dto.UploadResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.use

@Validated
@RestController
@RequestMapping("/api")
class UploadController(
    @Value("\${app.upload-dir}") private val uploadDir: String
) {

    private val allowedImage = setOf("image/jpeg", "image/png", "image/webp", "image/heic")
    private val allowedVideo = setOf("video/mp4", "video/quicktime", "video/webm")
    private val maxImageSize = DataSize.ofMegabytes(30).toBytes()
    private val maxVideoSize = DataSize.ofMegabytes(512).toBytes()

    init {
        Path.of(uploadDir).createDirectories()
    }

    @GetMapping("/health")
    fun health(): Map<String, Any> = mapOf("status" to "ok", "time" to Instant.now().toString())

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestPart("file") file: MultipartFile,
        @RequestParam(required = false) @NotBlank message: String?
    ): ResponseEntity<UploadResponse> {
        validate(file)
        val id = UUID.randomUUID().toString()
        val ext = guessExtension(file.contentType)
        val stored = Path.of(uploadDir, "$id$ext")
        file.inputStream.use { Files.copy(it, stored) }

        val url = "/$id$ext" // раздаётся как статика
        val resp = UploadResponse(
            id = id,
            fileName = file.originalFilename ?: "unknown",
            mediaType = file.contentType,
            sizeBytes = file.size,
            url = url
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(resp)
    }

    @PostMapping("/uploadMany", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadMany(
        @RequestPart("files") files: List<MultipartFile>
    ): ResponseEntity<List<UploadResponse>> {
        val responses = files.map { f ->
            validate(f)
            val id = UUID.randomUUID().toString()
            val ext = guessExtension(f.contentType)
            val stored = Path.of(uploadDir, "$id$ext")
            f.inputStream.use { Files.copy(it, stored) }
            UploadResponse(
                id = id,
                fileName = f.originalFilename ?: "unknown",
                mediaType = f.contentType,
                sizeBytes = f.size,
                url = "/$id$ext"
            )
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(responses)
    }

    /**
     * Чанк-загрузка для больших файлов.
     * Клиент шлёт: fileId (общий id файла), index (0..total-1), total (кол-во чанков) и сам чанк.
     * Когда все чанки получены — склеиваем в uploads/<fileId>.bin
     */
    @PostMapping("/upload/chunk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadChunk(
        @RequestParam fileId: String,
        @RequestParam index: Int,
        @RequestParam total: Int,
        @RequestPart("chunk") chunk: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        require(fileId.isNotBlank()) { "fileId is blank" }
        require(total > 0 && index in 0 until total) { "Bad chunk index/total" }

        val tmpDir = Path.of(uploadDir, "chunks", fileId).apply { createDirectories() }
        val chunkPath = tmpDir.resolve(index.toString())
        chunk.inputStream.use { Files.copy(it, chunkPath) }

        val done = Files.list(tmpDir).use { it.count().toInt() } == total
        if (done) {
            val outPath = Path.of(uploadDir, "$fileId.mp4")
            Files.deleteIfExists(outPath)
            Files.newOutputStream(outPath).use { out ->
                for (i in 0 until total) {
                    Files.newInputStream(tmpDir.resolve(i.toString())).use { it.transferTo(out) }
                }
            }
            // Чистим чанки
            tmpDir.toFile().deleteRecursively()

            return ResponseEntity.ok(
                mapOf(
                    "completed" to true,
                    "url" to "/${outPath.fileName}",
                    "finishedAt" to Instant.now().toString()
                )
            )
        }

        return ResponseEntity.accepted().body(
            mapOf(
                "completed" to false,
                "receivedIndex" to index
            )
        )
    }

    // --- helpers ---

    private fun validate(file: MultipartFile) {
        val ct = file.contentType ?: ""
        val allowed = allowedImage + allowedVideo
        require(ct in allowed) { "Unsupported content type: $ct" }

        val size = file.size
        if (ct in allowedImage) require(size <= maxImageSize) { "Image too large" }
        if (ct in allowedVideo) require(size <= maxVideoSize) { "Video too large" }
    }

    private fun guessExtension(contentType: String?): String = when (contentType) {
        "image/jpeg" -> ".jpg"
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        "image/heic" -> ".heic"
        "video/mp4" -> ".mp4"
        "video/quicktime" -> ".mov"
        "video/webm" -> ".webm"
        else -> ""
    }
}