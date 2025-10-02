package tech.mobiledeveloper.c1b7p1_demo.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel
) {
    val state by viewModel.state.collectAsState()

    val singlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { viewModel.onPickSingle(it) }
    )

    val multiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
        onResult = { viewModel.onPickMultiple(it) }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Multipart Upload Demo") }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    singlePicker.launch(arrayOf("image/*", "video/*"))
                }) {
                    Text("Select 1 File")
                }
                OutlinedButton(onClick = {
                    multiPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }) {
                    Text("Select Multiple Files")
                }

                if (state.files.isNotEmpty()) {
                    TextButton(onClick = {
                        viewModel.clearSelection()
                    }) {
                        Text("Clean")
                    }
                }
            }

            if (state.files.isEmpty()) {
                Text("Files not selected")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.files, key = { f -> f.uri }) { file ->
                        FileItem(file)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.uploadSequential() },
                enabled = state.files.isNotEmpty() && !state.isUploading,
                modifier = Modifier.height(40.dp).fillMaxWidth()
            ) {
                Text(if (state.isUploading) "Uploading..." else "Upload sequence")
            }

            Button(
                onClick = { viewModel.uploadChunk() },
                enabled = state.files.isNotEmpty() && !state.isUploading,
                modifier = Modifier.height(40.dp).fillMaxWidth()
            ) {
                Text(if (state.isUploading) "Uploading..." else "Upload by chunks")
            }

            if (state.files.size > 1) {
                OutlinedButton(
                    onClick = { viewModel.uploadInstance() },
                    enabled = !state.isUploading,
                    modifier = Modifier.height(40.dp).fillMaxWidth()
                ) {
                    Text(if (state.isUploading) "Uploading..." else "Upload all")
                }
            }
        }
    }
}

@Composable
fun FileItem(file: SelectedFile) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilePreview(uri = file.uri, mime = file.mime)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(file.mime, style = MaterialTheme.typography.bodySmall)
                    if (file.size > 0) {
                        Text("${file.size} bytes", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            when (file.status) {
                UploadStatus.Idle -> Text("Prepare for loading")
                UploadStatus.Uploading -> {
                    LinearProgressIndicator(
                        progress = (file.progress / 100f).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                UploadStatus.Done -> {
                    Text("Done")
                    file.resultUrl?.let { Text("URL: $it") }
                }
                UploadStatus.Failed -> {
                    Text("Error: ${file.error ?: "unknown"}")
                }
            }
        }
    }
}

@Composable
private fun FilePreview(uri: Uri, mime: String) {
    val isImage = mime.startsWith("image/")
    if (isImage) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
    } else {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("FILE")
        }
    }
}























