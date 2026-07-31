package com.app.metadataextractor.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FileDownloader(
    private val context: Context,
    private val client: OkHttpClient
) {

    suspend fun downloadFile(url: String, documentType: DocumentType): DownloadResult = withContext(Dispatchers.IO) {
        try {
            // 1. Construct the GET request to fetch the actual file body
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            // 2. Execute the network call
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadResult.Error("Failed to download: HTTP ${response.code}")
                }

                val body = response.body ?: return@withContext DownloadResult.Error("Empty response body")

                // 3. Determine the correct extension based on the inspected type
                val extension = when (documentType) {
                    DocumentType.PDF -> ".pdf"
                    DocumentType.WORD -> ".docx"
                    DocumentType.IMAGE -> ".jpg" 
                }

                // 4. Create a secure, temporary file in the app's internal cache
                val tempFile = File.createTempFile("osint_target_", extension, context.cacheDir)

                // 5. Stream the bytes directly from the network to the local file
                body.byteStream().use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                return@withContext DownloadResult.Success(tempFile)
            }
        } catch (e: Exception) {
            return@withContext DownloadResult.Error("Download failed: ${e.message}")
        }
    }
}

// State management for the download result
sealed class DownloadResult {
    data class Success(val file: File) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}