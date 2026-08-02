package com.app.metadataextractor.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.metadataextractor.data.*
import com.app.metadataextractor.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

// 1. Define the possible states of your User Interface
sealed class UiState {
    object Idle : UiState()
    data class Loading(val message: String) : UiState()
    data class Success(val metadata: List<MetadataItem>) : UiState()
    data class Error(val message: String) : UiState()
}

class MetadataViewModel(application: Application) : AndroidViewModel(application) {

    // 2. StateFlow exposes the state to your Jetpack Compose UI
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 3. Initialize dependencies (OkHttp, Inspector, Downloader)
    private val client = OkHttpClient()
    private val networkInspector = NetworkInspector(client)
    private val fileDownloader = FileDownloader(application, client)

    // Initialize Extractors
    private val pdfExtractor = PdfMetadataExtractor()
    private val wordExtractor = WordMetadataExtractor()
    private val imageExtractor = ImageMetadataExtractor()

    // 4. The main entry point triggered by the UI (Remote URLs)
    fun processUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading("Inspecting URL...")
            val inspectionResult = networkInspector.inspectUrl(url)

            when (inspectionResult) {
                is InspectionResult.Error -> {
                    _uiState.value = UiState.Error(inspectionResult.message)
                    return@launch
                }
                is InspectionResult.Unsupported -> {
                    _uiState.value = UiState.Error(inspectionResult.message)
                    return@launch
                }
                is InspectionResult.Success -> {
                    _uiState.value = UiState.Loading("Downloading secure copy...")
                    val downloadResult = fileDownloader.downloadFile(url, inspectionResult.type)

                    when (downloadResult) {
                        is DownloadResult.Error -> {
                            _uiState.value = UiState.Error(downloadResult.message)
                        }
                        is DownloadResult.Success -> {
                            _uiState.value = UiState.Loading("Extracting Metadata...")

                            // 1. Get the deep metadata from the file
                            val deepMetadata = extractMetadata(downloadResult.file, inspectionResult.type)

                            // 2. Extract the real name from the URL
                            val originalName = url.substringAfterLast('/').substringBefore('?')
                            val extension = downloadResult.file.extension

                            // 3. Get the basic properties
                            val basicProperties = getBasicFileProperties(downloadResult.file, originalName, extension)

                            // 4. Display the combined results
                            _uiState.value = UiState.Success(basicProperties + deepMetadata)
                        }
                    }
                }
            }
        }
    }

    // 5. The entry point for Local Files
    fun processLocalUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading("Reading local file...")

            try {
                val context = getApplication<Application>().applicationContext
                val contentResolver = context.contentResolver

                val mimeType = contentResolver.getType(uri)
                val documentType = when (mimeType) {
                    "application/pdf" -> DocumentType.PDF
                    "image/jpeg", "image/png" -> DocumentType.IMAGE
                    "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DocumentType.WORD
                    else -> {
                        _uiState.value = UiState.Error("Unsupported local file type: $mimeType")
                        return@launch
                    }
                }

                val extension = when (documentType) {
                    DocumentType.PDF -> ".pdf"
                    DocumentType.WORD -> ".docx"
                    DocumentType.IMAGE -> ".jpg"
                }
                val tempFile = File.createTempFile("local_target_", extension, context.cacheDir)

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: run {
                    _uiState.value = UiState.Error("Could not open file stream from system.")
                    return@launch
                }

                _uiState.value = UiState.Loading("Extracting Metadata...")

                // 1. Get deep metadata
                val deepMetadata = extractMetadata(tempFile, documentType)

                // 2. Ask Android for the real file name
                var originalName = "Unknown File"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            originalName = cursor.getString(nameIndex) ?: "Unknown File"
                        }
                    }
                }

                // 3. Get basic properties
                val basicProperties = getBasicFileProperties(tempFile, originalName, tempFile.extension)

                // 4. Display combined results
                _uiState.value = UiState.Success(basicProperties + deepMetadata)

            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to process local file: ${e.message}")
            }
        }
    }

    // 6. The Routing Engine
    private suspend fun extractMetadata(file: File, type: DocumentType): List<MetadataItem> {
        return when (type) {
            DocumentType.PDF -> pdfExtractor.extract(file)
            DocumentType.WORD -> wordExtractor.extract(file)
            DocumentType.IMAGE -> imageExtractor.extract(file)
        }
    }

    // Utility to reset the UI
    fun reset() {
        _uiState.value = UiState.Idle
    }

    // Helper 1: Formats raw bytes into KB or MB
    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.2f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.2f KB", kb)
            else -> "$bytes Bytes"
        }
    }

    // Helper 2: Generates the Basic Properties list
    private fun getBasicFileProperties(
        file: File,
        originalName: String,
        extension: String
    ): List<MetadataItem> {
        return listOf(
            MetadataItem("File Name", originalName),
            MetadataItem("File Extension", extension.uppercase(Locale.getDefault())),
            MetadataItem("File Size", formatFileSize(file.length()))
        )
    }
}