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
            PDDocument.load(file).use { document ->
                val info = document.documentInformation
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                // --- 1. BASIC / HIGH-VALUE OSINT METADATA ---
                info.title?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Title", it)) }
                info.author?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Author", it)) }
                info.creator?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Creator Tool", it)) }
                info.producer?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Producer", it)) }
                info.creationDate?.time?.let { metadataList.add(MetadataItem("Creation Date", dateFormat.format(it))) }
                info.modificationDate?.time?.let { metadataList.add(MetadataItem("Modification Date", dateFormat.format(it))) }
                metadataList.add(MetadataItem("Page Count", document.numberOfPages.toString()))

                // --- 2. ADVANCED / RAW COS DICTIONARY DUMP ---
                val cosDict = info.cosObject
                val standardKeys = setOf("Title", "Author", "Subject", "Creator", "Producer", "CreationDate", "ModDate", "Keywords")

                for (cosName in cosDict.keySet()) {
                    val keyName = cosName.name
                    // Skip keys we already handled in basic section
                    if (!standardKeys.contains(keyName)) {
                        val rawValue = cosDict.getString(cosName) ?: cosDict.getItem(cosName)?.toString()
                        rawValue?.takeIf { it.isNotBlank() }?.let {
                            metadataList.add(MetadataItem("Raw PDF Key: /$keyName", it, isAdvanced = true))
                        }
                    }
                }

                // Structural info as Advanced
                metadataList.add(MetadataItem("PDF Version", document.version.toString(), isAdvanced = true))
                metadataList.add(MetadataItem("Is Encrypted", document.isEncrypted.toString(), isAdvanced = true))
            }
        } catch (e: Exception) {
            metadataList.add(MetadataItem("Error", "Failed to parse PDF data: ${e.message}"))
        }

        return@withContext metadataList
    }
}