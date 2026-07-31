package com.app.metadataextractor.ui

import android.app.Application
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
    private val imageExtractor = ImageMetadataExtractor() // We'll assume you created this based on the earlier code!

    // 4. The main entry point triggered by the UI
    fun processUrl(url: String) {
        // viewModelScope ensures this runs safely and cancels if the ViewModel is destroyed
        viewModelScope.launch {
            
            // Phase A: Inspect
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
                    // Phase B: Download
                    _uiState.value = UiState.Loading("Downloading secure copy...")
                    val downloadResult = fileDownloader.downloadFile(url, inspectionResult.type)

                    when (downloadResult) {
                        is DownloadResult.Error -> {
                            _uiState.value = UiState.Error(downloadResult.message)
                        }
                        is DownloadResult.Success -> {
                            // Phase C: Extract
                            _uiState.value = UiState.Loading("Extracting Metadata...")
                            val metadata = extractMetadata(downloadResult.file, inspectionResult.type)
                            
                            // Phase D: Display
                            _uiState.value = UiState.Success(metadata)
                        }
                    }
                }
            }
        }
    }

    // 5. The Routing Engine
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
}