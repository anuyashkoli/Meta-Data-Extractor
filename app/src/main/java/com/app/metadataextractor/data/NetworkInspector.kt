package com.app.metadataextractor.data

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkInspector(private val client: OkHttpClient) {

    // Map MIME types to your project's specific document requirements
    private val supportedMimeTypes = mapOf(
        "application/pdf" to DocumentType.PDF,
        "image/jpeg" to DocumentType.IMAGE,
        "image/png" to DocumentType.IMAGE,
        "application/msword" to DocumentType.WORD,
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to DocumentType.WORD
    )

    suspend fun inspectUrl(url: String): InspectionResult = withContext(Dispatchers.IO) {
        try {
            // 1. Construct the HEAD request to avoid downloading the body
            val request = Request.Builder()
                .url(url)
                .head() 
                .build()

            // 2. Execute the network call
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext InspectionResult.Error("Server error: ${response.code}")
                }

                // 3. Extract the Content-Type header (ignoring charset info like ; charset=utf-8)
                val contentType = response.header("Content-Type")?.substringBefore(";")?.trim()
                
                // Optional: Grab the size to prevent downloading massive files
                val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L

                // 4. Validate against our supported types
                val documentType = supportedMimeTypes[contentType]
                
                return@withContext if (documentType != null) {
                    InspectionResult.Success(url, documentType, contentLength)
                } else {
                    InspectionResult.Unsupported("Unsupported file type: $contentType")
                }
            }
        } catch (e: Exception) {
            return@withContext InspectionResult.Error("Network error: ${e.message}")
        }
    }
}

// State management for the inspection result
enum class DocumentType { PDF, WORD, IMAGE }

sealed class InspectionResult {
    data class Success(val url: String, val type: DocumentType, val sizeInBytes: Long) : InspectionResult()
    data class Unsupported(val message: String) : InspectionResult()
    data class Error(val message: String) : InspectionResult()
}