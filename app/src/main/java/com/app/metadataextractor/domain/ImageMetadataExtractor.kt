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

            // --- 1. BASIC METADATA ---
            exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { metadataList.add(MetadataItem("Date/Time", it)) }
            exif.getAttribute(ExifInterface.TAG_MAKE)?.let { metadataList.add(MetadataItem("Camera Make", it)) }
            exif.getAttribute(ExifInterface.TAG_MODEL)?.let { metadataList.add(MetadataItem("Camera Model", it)) }

            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) {
                metadataList.add(MetadataItem("GPS Latitude", latLong[0].toString()))
                metadataList.add(MetadataItem("GPS Longitude", latLong[1].toString()))
            }

            // --- 2. ADVANCED / RAW EXIF TAGS ---
            val advancedTags = mapOf(
                "Software / Editor" to ExifInterface.TAG_SOFTWARE,
                "Lens Model" to ExifInterface.TAG_LENS_MODEL,
                "Lens Make" to ExifInterface.TAG_LENS_MAKE,
                "ISO Speed" to ExifInterface.TAG_ISO_SPEED_RATINGS,
                "F-Number (Aperture)" to ExifInterface.TAG_F_NUMBER,
                "Exposure Time" to ExifInterface.TAG_EXPOSURE_TIME,
                "Focal Length" to ExifInterface.TAG_FOCAL_LENGTH,
                "White Balance" to ExifInterface.TAG_WHITE_BALANCE,
                "Flash Mode" to ExifInterface.TAG_FLASH,
                "Artist / Owner" to ExifInterface.TAG_ARTIST,
                "Copyright" to ExifInterface.TAG_COPYRIGHT,
                "User Comment" to ExifInterface.TAG_USER_COMMENT,
                "Digital Zoom Ratio" to ExifInterface.TAG_DIGITAL_ZOOM_RATIO
            )

            for ((label, tag) in advancedTags) {
                exif.getAttribute(tag)?.takeIf { it.isNotBlank() }?.let {
                    metadataList.add(MetadataItem(label, it, isAdvanced = true))
                }
            }

        } catch (e: Exception) {
            metadataList.add(MetadataItem("Error", "Failed to parse EXIF data: ${e.message}"))
        }

        return@withContext metadataList
    }
}