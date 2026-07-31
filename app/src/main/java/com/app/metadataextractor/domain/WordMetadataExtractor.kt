package com.app.metadataextractor.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream

class WordMetadataExtractor : MetadataExtractor {

    override suspend fun extract(file: File): List<MetadataItem> = withContext(Dispatchers.IO) {
        val metadataList = mutableListOf<MetadataItem>()

        try {
            FileInputStream(file).use { fis ->
                XWPFDocument(fis).use { document ->
                    val coreProps = document.properties.coreProperties
                    val extendedProps = document.properties.extendedProperties
                    val customProps = document.properties.customProperties

                    // --- 1. BASIC METADATA ---
                    coreProps.title?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Title", it)) }
                    coreProps.creator?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Creator", it)) }
                    coreProps.lastModifiedByUser?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Last Modified By", it)) }
                    coreProps.created?.let { metadataList.add(MetadataItem("Creation Date", it.toString())) }
                    coreProps.modified?.let { metadataList.add(MetadataItem("Modification Date", it.toString())) }

                    // --- 2. ADVANCED METADATA ---
                    coreProps.revision?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Revision Number", it, isAdvanced = true)) }
                    coreProps.subject?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Subject", it, isAdvanced = true)) }
                    coreProps.keywords?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Keywords", it, isAdvanced = true)) }

                    extendedProps.application?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Software Application", it, isAdvanced = true)) }
                    extendedProps.company?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Company", it, isAdvanced = true)) }
                    extendedProps.template?.takeIf { it.isNotBlank() }?.let { metadataList.add(MetadataItem("Base Template", it, isAdvanced = true)) }

                    if (extendedProps.pages > 0) metadataList.add(MetadataItem("Page Count", extendedProps.pages.toString(), isAdvanced = true))
                    if (extendedProps.words > 0) metadataList.add(MetadataItem("Word Count", extendedProps.words.toString(), isAdvanced = true))
                    if (extendedProps.paragraphs > 0) metadataList.add(MetadataItem("Paragraph Count", extendedProps.paragraphs.toString(), isAdvanced = true))

                    // Raw Custom Properties
                    try {
                        val propertyList = customProps.underlyingProperties.propertyList
                        for (prop in propertyList) {
                            val name = prop.name
                            val value = prop.getLpwstr() ?: prop.toString()
                            metadataList.add(MetadataItem("Custom Property: $name", value, isAdvanced = true))
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            metadataList.add(MetadataItem("Error", "Failed to parse Word data: ${e.message}"))
        }

        return@withContext metadataList
    }
}