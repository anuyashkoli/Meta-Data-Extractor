package com.app.metadataextractor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.metadataextractor.domain.MetadataItem

@Composable
fun MetadataScreen(viewModel: MetadataViewModel) {
    // Observe the ViewModel's state. When this changes, Compose redraws the UI.
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

        // State Machine: Show different UI based on the current state
        when (val state = uiState) {
            is UiState.Idle -> IdleView(onAnalyzeClick = { url -> viewModel.processUrl(url) })
            is UiState.Loading -> LoadingView(message = state.message)
            is UiState.Error -> ErrorView(message = state.message, onRetry = { viewModel.reset() })
            is UiState.Success -> SuccessView(metadata = state.metadata, onReset = { viewModel.reset() })
        }
    }
}

@Composable
fun IdleView(onAnalyzeClick: (String) -> Unit) {
    var urlInput by remember { mutableStateOf("") }

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
            Text("Analyze File")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Or use the Android Share menu from your browser to send a URL directly to this app.",
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