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
            // 1. Open the file stream and pass it to Apache POI's XWPFDocument
            FileInputStream(file).use { fis ->
                XWPFDocument(fis).use { document ->
                    
                    // 2. Access the Core and Extended properties of the OOXML document
                    val coreProps = document.properties.coreProperties
                    val extendedProps = document.properties.extendedProperties

                    // 3. Extract Core OSINT Metadata
                    coreProps.title?.takeIf { it.isNotBlank() }?.let { 
                        metadataList.add(MetadataItem("Title", it)) 
                    }
                    coreProps.creator?.takeIf { it.isNotBlank() }?.let { 
                        metadataList.add(MetadataItem("Creator", it)) 
                    }
                    coreProps.lastModifiedByUser?.takeIf { it.isNotBlank() }?.let { 
                        metadataList.add(MetadataItem("Last Modified By", it)) 
                    }
                    coreProps.revision?.takeIf { it.isNotBlank() }?.let { 
                        metadataList.add(MetadataItem("Revision Number", it)) 
                    }
                    
                    // Dates in Apache POI are returned as java.util.Date objects
                    coreProps.created?.let { 
                        metadataList.add(MetadataItem("Creation Date", it.toString())) 
                    }
                    coreProps.modified?.let { 
                        metadataList.add(MetadataItem("Modification Date", it.toString())) 
                    }

                    // 4. Extract Extended Software Metadata
                    extendedProps.application?.takeIf { it.isNotBlank() }?.let { 
                        metadataList.add(MetadataItem("Software Application", it)) 
                    }
                    extendedProps.company?.takeIf { it.isNotBlank() }?.let { 
                        metadataList.add(MetadataItem("Company", it)) 
                    }
                    
                    // Pages count is returned as an integer. We only add it if it's greater than 0.
                    val pages = extendedProps.pages
                    if (pages > 0) {
                        metadataList.add(MetadataItem("Page Count", pages.toString()))
                    }
                }
            }
        } catch (e: Exception) {
            metadataList.add(MetadataItem("Error", "Failed to parse Word data: ${e.message}"))
        }

        return@withContext metadataList
    }
}