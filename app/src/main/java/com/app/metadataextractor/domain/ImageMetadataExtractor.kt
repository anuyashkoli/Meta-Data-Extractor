package com.app.metadataextractor.domain

import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ImageMetadataExtractor : MetadataExtractor {

    override suspend fun extract(file: File): List<MetadataItem> = withContext(Dispatchers.IO) {
        val metadataList = mutableListOf<MetadataItem>()

        try {
            val exif = ExifInterface(file)

            // Extract standard tags
            exif.getAttribute(ExifInterface.TAG_DATETIME)?.let {
                metadataList.add(MetadataItem("Date/Time", it))
            }
            exif.getAttribute(ExifInterface.TAG_MAKE)?.let {
                metadataList.add(MetadataItem("Camera Make", it))
            }
            exif.getAttribute(ExifInterface.TAG_MODEL)?.let {
                metadataList.add(MetadataItem("Camera Model", it))
            }
            exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.let {
                metadataList.add(MetadataItem("Software/Editor", it))
            }
            exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.let { width ->
                exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.let { height ->
                    metadataList.add(MetadataItem("Dimensions", "${width}x${height} px"))
                }
            }

            // Extract GPS Location data if available
            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) {
                metadataList.add(MetadataItem("GPS Latitude", latLong[0].toString()))
                metadataList.add(MetadataItem("GPS Longitude", latLong[1].toString()))
            }

        } catch (e: Exception) {
            metadataList.add(MetadataItem("Error", "Failed to parse EXIF data: ${e.message}"))
        }

        return@withContext metadataList
    }
}