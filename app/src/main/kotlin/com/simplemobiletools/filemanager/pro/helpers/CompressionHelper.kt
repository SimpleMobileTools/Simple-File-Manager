package com.simplemobiletools.filemanager.pro.helpers

import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.utils.IOUtils
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedList

object CompressionHelper {
    fun compress(
        activity: BaseSimpleActivity,
        sourcePaths: List<String>,
        targetPath: String,
        compressionFormat: CompressionFormat,
        password: String? = null
    ): Boolean {
        return when (compressionFormat) {
            CompressionFormat.ZIP -> compressToZip(activity, sourcePaths, targetPath, password)
            CompressionFormat.SEVEN_ZIP -> compressToSevenZip(activity, sourcePaths, targetPath)
            CompressionFormat.TAR_GZ,
            CompressionFormat.TAR_XZ -> compressToTarVariants(activity, sourcePaths, targetPath, compressionFormat)

            CompressionFormat.UNKNOWN -> false
        }
    }

    private fun compressToZip(
        activity: BaseSimpleActivity,
        sourcePaths: List<String>,
        targetPath: String,
        password: String? = null
    ): Boolean {
        val queue = LinkedList<String>()
        val fos = activity.getFileOutputStreamSync(targetPath, CompressionFormat.ZIP.mimeType) ?: return false

        val zout = password?.let { ZipOutputStream(fos, password.toCharArray()) } ?: ZipOutputStream(fos)
        var res: Closeable = fos

        fun zipEntry(name: String) = ZipParameters().also {
            it.fileNameInZip = name
            if (password != null) {
                it.isEncryptFiles = true
                it.encryptionMethod = EncryptionMethod.AES
            }
        }

        try {
            sourcePaths.forEach { currentPath ->
                var name: String
                var mainFilePath = currentPath
                val base = "${mainFilePath.getParentPath()}/"
                res = zout
                queue.push(mainFilePath)
                if (activity.getIsPathDirectory(mainFilePath)) {
                    name = "${mainFilePath.getFilenameFromPath()}/"
                    zout.putNextEntry(
                        ZipParameters().also {
                            it.fileNameInZip = name
                        }
                    )
                }

                while (!queue.isEmpty()) {
                    mainFilePath = queue.pop()
                    if (activity.getIsPathDirectory(mainFilePath)) {
                        if (activity.isRestrictedSAFOnlyRoot(mainFilePath)) {
                            activity.getAndroidSAFFileItems(mainFilePath, true) { files ->
                                for (file in files) {
                                    name = file.path.relativizeWith(base)
                                    if (activity.getIsPathDirectory(file.path)) {
                                        queue.push(file.path)
                                        name = "${name.trimEnd('/')}/"
                                        zout.putNextEntry(zipEntry(name))
                                    } else {
                                        zout.putNextEntry(zipEntry(name))
                                        activity.getFileInputStreamSync(file.path)!!.copyTo(zout)
                                        zout.closeEntry()
                                    }
                                }
                            }
                        } else {
                            val mainFile = File(mainFilePath)
                            for (file in mainFile.listFiles()) {
                                name = file.path.relativizeWith(base)
                                if (activity.getIsPathDirectory(file.absolutePath)) {
                                    queue.push(file.absolutePath)
                                    name = "${name.trimEnd('/')}/"
                                    zout.putNextEntry(zipEntry(name))
                                } else {
                                    zout.putNextEntry(zipEntry(name))
                                    activity.getFileInputStreamSync(file.path)!!.copyTo(zout)
                                    zout.closeEntry()
                                }
                            }
                        }

                    } else {
                        name = if (base == currentPath) currentPath.getFilenameFromPath() else mainFilePath.relativizeWith(base)
                        zout.putNextEntry(zipEntry(name))
                        activity.getFileInputStreamSync(mainFilePath)!!.copyTo(zout)
                        zout.closeEntry()
                    }
                }
            }
        } catch (exception: Exception) {
            activity.showErrorToast(exception)
            return false
        } finally {
            res.close()
        }
        return true
    }

    private fun compressToSevenZip(
        activity: BaseSimpleActivity,
        sourcePaths: List<String>,
        targetPath: String,
    ): Boolean {
        try {
            SevenZOutputFile(File(targetPath)).use { sevenZOutput ->
                sourcePaths.forEach { sourcePath ->
                    Files.walk(File(sourcePath).toPath()).forEach { path ->
                        val file = path.toFile()
                        val basePath = "${sourcePath.getParentPath()}/"

                        if (!activity.getIsPathDirectory(file.absolutePath)) {
                            FileInputStream(file).use { _ ->
                                val entryName = if (basePath == sourcePath) {
                                    sourcePath.getFilenameFromPath()
                                } else {
                                    path.toString().relativizeWith(basePath)
                                }

                                val sevenZArchiveEntry = sevenZOutput.createArchiveEntry(file, entryName)
                                sevenZOutput.putArchiveEntry(sevenZArchiveEntry)
                                sevenZOutput.write(Files.readAllBytes(file.toPath()))
                                sevenZOutput.closeArchiveEntry()
                            }
                        }
                    }
                }

                sevenZOutput.finish()
            }
        } catch (exception: IOException) {
            activity.showErrorToast(exception)
            return false
        }
        return true
    }

    private fun compressToTarVariants(
        activity: BaseSimpleActivity,
        sourcePaths: List<String>,
        outFilePath: String,
        format: CompressionFormat
    ): Boolean {
        if (!listOf(
                CompressionFormat.TAR_GZ,
                CompressionFormat.TAR_XZ
            ).contains(format)
        ) {
            return false
        }

        val fos = activity.getFileOutputStreamSync(outFilePath, format.mimeType)
        try {
            fos.use { fileOutputStream ->
                CompressorStreamFactory()
                    .createCompressorOutputStream(format.compressorStreamFactory, fileOutputStream).use { compressedOut ->
                        TarArchiveOutputStream(compressedOut).use { archive ->
                            archive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                            sourcePaths.forEach { sourcePath ->
                                val basePath = "${sourcePath.getParentPath()}/"
                                Files.walk(File(sourcePath).toPath()).forEach { path: Path ->
                                    val file = path.toFile()

                                    if (!activity.getIsPathDirectory(file.absolutePath)) {
                                        val entryName = if (basePath == sourcePath) {
                                            sourcePath.getFilenameFromPath()
                                        } else {
                                            path.toString().relativizeWith(basePath)
                                        }

                                        val tarArchiveEntry: ArchiveEntry = TarArchiveEntry(file, entryName)
                                        FileInputStream(file).use { fis ->
                                            archive.putArchiveEntry(tarArchiveEntry)
                                            IOUtils.copy(fis, archive)
                                            archive.closeArchiveEntry()
                                        }
                                    }
                                }
                            }

                            archive.finish()
                        }
                    }
            }
        } catch (exception: IOException) {
            activity.showErrorToast(exception)
            return false
        }
        return true
    }
}
