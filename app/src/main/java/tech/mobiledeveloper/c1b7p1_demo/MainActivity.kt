package tech.mobiledeveloper.c1b7p1_demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import tech.mobiledeveloper.c1b7p1_demo.ui.theme.C1B7P1_DemoTheme
import tech.mobiledeveloper.c1b7p1_demo.upload.UploadScreen
import tech.mobiledeveloper.c1b7p1_demo.upload.UploadViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            C1B7P1_DemoTheme {
                val vm = UploadViewModel(application)
                UploadScreen(viewModel = vm)
            }
        }
    }
}