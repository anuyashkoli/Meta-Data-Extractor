package com.app.metadataextractor.ui

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Info

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
    // Separate metadata into basic and advanced lists
    val basicItems = remember(metadata) { metadata.filter { !it.isAdvanced } }
    val advancedItems = remember(metadata) { metadata.filter { it.isAdvanced } }

    // Check if any deep/hidden metadata exists beyond the 3 basic filesystem properties
    val hasDeepMetadata = remember(metadata) {
        metadata.any { item ->
            item.key != "File Name" && item.key != "File Extension" && item.key != "File Size"
        }
    }

    var isAdvancedExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text("Scan Another File / URL")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Render Basic Filesystem Items (Name, Extension, Size, + any basic metadata)
            items(basicItems) { item ->
                MetadataCard(item)
            }

            // 2. Empty / Sanitized State Notice
            if (!hasDeepMetadata) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Information",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "No Hidden Metadata Detected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "This file appears to be clean. Either no EXIF or document metadata was recorded, or the file was sanitized prior to release.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // 3. Render Advanced / Raw Section (if any advanced items exist)
            if (advancedItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAdvancedExpanded = !isAdvancedExpanded },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Advanced / Raw OSINT Data (${advancedItems.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Advanced Data"
                            )
                        }
                    }
                }

                if (isAdvancedExpanded) {
                    items(advancedItems) { item ->
                        MetadataCard(item = item, isAdvanced = true)
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataCard(item: MetadataItem, isAdvanced: Boolean = false) {
    // 1. Grab the context (for starting the intent and showing Toasts)
    val context = LocalContext.current

    // 2. Grab the compose clipboard manager (Fixed reference)
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdvanced)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        // Use a Row so the text is on the left and the buttons are on the right
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Column takes up the remaining space (weight = 1f)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.key,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isAdvanced) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Buttons Row
            Row {
                // Copy Button
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(item.value))
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy to Clipboard",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Search Button
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(SearchManager.QUERY, item.value)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) { // Fixed: Using underscore for unused parameter
                        Toast.makeText(context, "No search app available", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Online",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
