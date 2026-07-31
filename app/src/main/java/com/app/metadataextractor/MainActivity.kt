package com.app.metadataextractor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.app.metadataextractor.ui.MetadataScreen
import com.app.metadataextractor.ui.MetadataViewModel
import com.app.metadataextractor.ui.theme.MetaDataExtractorTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {

    // Instantiate the ViewModel scoped to this Activity
    private val viewModel: MetadataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize PDFBox-Android
        PDFBoxResourceLoader.init(applicationContext)

        // Check if the app was opened via a shared link
        handleIntent(intent)

        setContent {
            MetaDataExtractorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass the ViewModel to your Compose Screen
                    MetadataScreen(viewModel = viewModel)
                }
            }
        }
    }

    // This catches intents if the app is already open in the background
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

            // Check if the shared text contains a URL
            if (sharedText != null && (sharedText.startsWith("http://") || sharedText.startsWith("https://"))) {
                viewModel.processUrl(sharedText)
            }
        }
    }
}