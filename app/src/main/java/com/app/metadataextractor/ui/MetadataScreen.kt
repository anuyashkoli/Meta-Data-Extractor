package com.app.metadataextractor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.metadataextractor.domain.MetadataItem

@Composable
fun MetadataScreen(viewModel: MetadataViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OSINT Metadata Extractor",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp, top = 32.dp)
        )

        when (val state = uiState) {
            is UiState.Idle -> IdleView(
                onAnalyzeClick = { url -> viewModel.processUrl(url) },
                onLocalFileSelected = { uri -> viewModel.processLocalUri(uri) }
            )
            is UiState.Loading -> LoadingView(message = state.message)
            is UiState.Error -> ErrorView(message = state.message, onRetry = { viewModel.reset() })
            is UiState.Success -> SuccessView(metadata = state.metadata, onReset = { viewModel.reset() })
        }
    }
}

@Composable
fun IdleView(
    onAnalyzeClick: (String) -> Unit,
    onLocalFileSelected: (Uri) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }

    // This creates the Intent to open the Android System File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        // If the user selected a file and didn't back out, pass the URI up
        if (uri != null) {
            onLocalFileSelected(uri)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Paste Public Document URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { if (urlInput.isNotBlank()) onAnalyzeClick(urlInput) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Analyze Remote URL")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // A visual divider between remote and local options
        HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Launch the file picker, strictly filtering for our OSINT target types
                filePickerLauncher.launch(arrayOf(
                    "application/pdf",
                    "image/jpeg",
                    "image/png",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Select Local File")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Or use the Android Share menu to send a URL directly to this app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LoadingView(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Extraction Failed", 
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Go Back")
        }
    }
}

@Composable
fun SuccessView(metadata: List<MetadataItem>, onReset: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text("Scan Another URL")
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn is Android's modern, highly efficient scrollable list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(metadata) { item ->
                MetadataCard(item)
            }
        }
    }
}

@Composable
fun MetadataCard(item: MetadataItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.key,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}