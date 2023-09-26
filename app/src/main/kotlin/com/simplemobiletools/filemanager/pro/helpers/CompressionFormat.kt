package com.simplemobiletools.filemanager.pro.helpers

import org.apache.commons.compress.compressors.CompressorStreamFactory

enum class CompressionFormat(
    val extension: String,
    val mimeType: String,
    val compressorStreamFactory: String,
    val canReadEncryptedArchive: Boolean,
    val canCreateEncryptedArchive: Boolean
) {
    ZIP(".zip", "application/zip", "", true, true),
    SEVEN_ZIP(".7z", "application/x-7z-compressed", "", true, false),
    TAR_GZ(".tar.gz", "application/gzip", CompressorStreamFactory.GZIP, false, false),

    //    TAR_SZ(".tar.sz", "application/x-snappy-framed", CompressorStreamFactory.SNAPPY_FRAMED, false, false), // FIXME: ask for enabling it. it's not so common
    TAR_XZ(".tar.xz", "application/x-xz", CompressorStreamFactory.XZ, false, false);

    companion object {
        fun fromExtension(extension: String): CompressionFormat {
            return when (extension.lowercase()) {
                ZIP.extension -> ZIP
                SEVEN_ZIP.extension -> SEVEN_ZIP
                TAR_GZ.extension -> TAR_GZ
//                TAR_SZ.extension -> TAR_SZ
                TAR_XZ.extension -> TAR_XZ
                else -> ZIP
            }
        }
    }
}
