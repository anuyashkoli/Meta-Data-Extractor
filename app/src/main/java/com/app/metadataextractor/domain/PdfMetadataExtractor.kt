package com.app.metadataextractor.domain

import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class PdfMetadataExtractor : MetadataExtractor {

    override suspend fun extract(file: File): List<MetadataItem> = withContext(Dispatchers.IO) {
        val metadataList = mutableListOf<MetadataItem>()

        try {
            // 1. Load the PDF document using PdfBox. The 'use' block ensures it closes automatically to prevent memory leaks.
            PDDocument.load(file).use { document ->
                
                // 2. Access the document's internal metadata dictionary
                val info = document.documentInformation

                // 3. Formatter for making calendar dates human-readable
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                // 4. Extract standard OSINT metadata points. 
                // Using takeIf { it.isNotBlank() } ensures we don't display empty fields in the UI.
                info.title?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Title", it)) }
                info.author?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Author", it)) }
                info.subject?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Subject", it)) }
                info.creator?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Creator Tool", it)) }
                info.producer?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Producer", it)) }
                info.keywords?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Keywords", it)) }

                info.creationDate?.time?.let { date ->
                    metadataList.add(MetadataItem("Creation Date", dateFormat.format(date)))
                }
                info.modificationDate?.time?.let { date ->
                    metadataList.add(MetadataItem("Modification Date", dateFormat.format(date)))
                }

                // 5. Extract structural data
                metadataList.add(MetadataItem("Page Count", document.numberOfPages.toString()))
                metadataList.add(MetadataItem("PDF Version", document.version.toString()))
                metadataList.add(MetadataItem("Encrypted", document.isEncrypted.toString()))
            }
        } catch (e: Exception) {
            metadataList.add(MetadataItem("Error", "Failed to parse PDF data: ${e.message}"))
        }

        return@withContext metadataList
    }
}