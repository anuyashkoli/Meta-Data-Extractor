package com.app.metadataextractor.domain

import java.io.File

// Standard key-value pair for displaying in UI
data class MetadataItem(
    val key: String,
    val value: String,
    val isAdvanced: Boolean = false
)

// Standard interface every file extractor will implement
interface MetadataExtractor {
    suspend fun extract(file: File): List<MetadataItem>
}