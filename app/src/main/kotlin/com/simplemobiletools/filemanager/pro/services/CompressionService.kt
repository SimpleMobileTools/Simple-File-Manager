package com.simplemobiletools.filemanager.pro.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.simplemobiletools.commons.compose.extensions.getActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.CONFLICT_OVERWRITE
import com.simplemobiletools.commons.helpers.CONFLICT_SKIP
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.SplashActivity
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.LocalFileHeader
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.File
import java.util.Collections
import java.util.LinkedList
import java.util.zip.ZipFile

class CompressionService : Service() {
    companion object {
        private var NOTIFICATION_ID = 10000

        private const val ACTION_ID = "com.simplemobiletools.filemanager.pro.action"
        const val ACTION_COMPRESS = "$ACTION_ID.ACTION_COMPRESS"
        const val ACTION_DECOMPRESS = "$ACTION_ID.ACTION_DECOMPRESS"
        const val ACTION_CANCEL = "$ACTION_ID.ACTION_CANCEL"

        const val EXTRA_URI = "uri"
        const val EXTRA_PATHS = "paths"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_DESTINATION = "destination"
        private const val EXTRA_JOB_ID = "job_id"

        private val cancellations = Collections.synchronizedSet(mutableSetOf<Int>())
        private val running = Collections.synchronizedSet(mutableSetOf<Int>())
    }

    private var notificationId = NOTIFICATION_ID++

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent.action) {
            ACTION_COMPRESS -> {
                intent.apply {
                    val paths = getStringArrayListExtra(EXTRA_PATHS)!!
                    val password = getStringExtra(EXTRA_PASSWORD)
                    val destination = getStringExtra(EXTRA_DESTINATION)!!
                    val jobId = notificationId++
                    startServiceJob(jobId, destination.getFilenameFromPath(), getString(R.string.compressing))
                    ensureBackgroundThread {
                        toast(R.string.compressing)
                        if (compressPaths(jobId, paths, destination, password)) {
                            toast(R.string.compression_successful)
                        } else {
                            toast(R.string.compressing_failed)
                        }
                        finishServiceJob(jobId)
                    }
                }
            }
            ACTION_DECOMPRESS -> {
                intent.apply {
                    val uri = getParcelableExtra<Uri>(EXTRA_URI)!!
                    val password = getStringExtra(EXTRA_PASSWORD)
                    val destination = getStringExtra(EXTRA_DESTINATION)!!
                    val realPath = getRealPathFromURI(uri)
                    val title = realPath?.getFilenameFromPath() ?: Uri.decode(uri.toString().getFilenameFromPath())
                    val jobId = notificationId++
                    startServiceJob(jobId, title, getString(R.string.decompressing))
                    ensureBackgroundThread {
                        decompressTo(jobId, title, uri, password, destination)
                        finishServiceJob(jobId)
                    }
                }
            }
            ACTION_CANCEL -> {
                val jobId = intent.getIntExtra(EXTRA_JOB_ID, -1)
                if (jobId != -1) {
                    cancellations.add(jobId)
                    notificationManager.cancel(jobId)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun decompressTo(jobId: Int, fileName: String, uri: Uri, password: String?, destination: String) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val zipInputStream = ZipInputStream(BufferedInputStream(inputStream!!))
            if (password != null) {
                zipInputStream.setPassword(password.toCharArray())
            }
            val buffer = ByteArray(1024)
            var progress = 0
            var progressMax = 0

            try {
                progressMax = ZipFile(uri.toFile()).size()
            } catch (e: Exception) {
                e.printStackTrace()
                // ignored
            }

            zipInputStream.use {
                while (true) {
                    if (cancellations.contains(jobId)) {
                        cancellations.remove(jobId)
                        return@use
                    }

                    progress++
                    notificationManager.notify(jobId, showNotification(jobId, fileName, getString(R.string.decompressing), progress, progressMax))

                    val realPath = getRealPathFromURI(uri)
                    val title = realPath?.getFilenameFromPath() ?: Uri.decode(uri.toString().getFilenameFromPath())
                    val entry = zipInputStream.nextEntry ?: break
                    val filename = title.toString().substringBeforeLast(".")
                    val parent = "$destination/$filename"
                    val newPath = "$parent/${entry.fileName.trimEnd('/')}"

                    if (!getDoesFilePathExist(parent)) {
                        if (!createDirectorySync(parent)) {
                            continue
                        }
                    }

                    if (entry.isDirectory) {
                        continue
                    }

                    val isVulnerableForZipPathTraversal = !File(newPath).canonicalPath.startsWith(parent)
                    if (isVulnerableForZipPathTraversal) {
                        continue
                    }

                    val fos = getFileOutputStreamSync(newPath, newPath.getMimeType())
                    var count: Int
                    while (true) {
                        if (cancellations.contains(jobId)) {
                            cancellations.remove(jobId)
                            fos?.close()
                            return@use
                        }
                        count = zipInputStream.read(buffer)
                        if (count == -1) {
                            break
                        }

                        fos!!.write(buffer, 0, count)
                    }
                    fos!!.close()
                }

                toast(R.string.decompression_successful)
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    @SuppressLint("NewApi")
    private fun compressPaths(jobId: Int, sourcePaths: List<String>, targetPath: String, password: String? = null): Boolean {
        val queue = LinkedList<String>()
        val fos = getFileOutputStreamSync(targetPath, "application/zip") ?: return false

        val zout = password?.let { ZipOutputStream(fos, password.toCharArray()) } ?: ZipOutputStream(fos)
        var res: Closeable = fos

        fun zipEntry(name: String) = ZipParameters().also {
            it.fileNameInZip = name
            if (password != null) {
                it.isEncryptFiles = true
                it.encryptionMethod = EncryptionMethod.AES
            }
        }

        var progress = 0
        var progressMax = sourcePaths.count()

        try {
            sourcePaths.forEach { currentPath ->
                if (cancellations.contains(jobId)) {
                    cancellations.remove(jobId)
                    res.close()
                    return@forEach
                }

                progress++
                notificationManager.notify(jobId, showNotification(jobId, targetPath.getFilenameFromPath(), getString(R.string.compressing), progress, progressMax))

                var name: String
                var mainFilePath = currentPath
                val base = "${mainFilePath.getParentPath()}/"
                res = zout
                queue.push(mainFilePath)
                if (getIsPathDirectory(mainFilePath)) {
                    name = "${mainFilePath.getFilenameFromPath()}/"
                    zout.putNextEntry(
                        ZipParameters().also {
                            it.fileNameInZip = name
                        }
                    )
                }

                while (!queue.isEmpty()) {
                    if (cancellations.contains(jobId)) {
                        cancellations.remove(jobId)
                        res.close()
                        return@forEach
                    }

                    mainFilePath = queue.pop()
                    if (getIsPathDirectory(mainFilePath)) {
                        if (isRestrictedSAFOnlyRoot(mainFilePath)) {
                            getAndroidSAFFileItems(mainFilePath, true) { files ->
                                for (file in files) {
                                    name = file.path.relativizeWith(base)
                                    if (getIsPathDirectory(file.path)) {
                                        queue.push(file.path)
                                        name = "${name.trimEnd('/')}/"
                                        zout.putNextEntry(zipEntry(name))
                                    } else {
                                        zout.putNextEntry(zipEntry(name))
                                        getFileInputStreamSync(file.path)!!.copyTo(zout)
                                        zout.closeEntry()
                                    }
                                }
                            }
                        } else {
                            val mainFile = File(mainFilePath)
                            for (file in mainFile.listFiles()) {
                                name = file.path.relativizeWith(base)
                                if (getIsPathDirectory(file.absolutePath)) {
                                    queue.push(file.absolutePath)
                                    name = "${name.trimEnd('/')}/"
                                    zout.putNextEntry(zipEntry(name))
                                } else {
                                    zout.putNextEntry(zipEntry(name))
                                    getFileInputStreamSync(file.path)!!.copyTo(zout)
                                    zout.closeEntry()
                                }
                            }
                        }

                    } else {
                        name = if (base == currentPath) currentPath.getFilenameFromPath() else mainFilePath.relativizeWith(base)
                        zout.putNextEntry(zipEntry(name))
                        getFileInputStreamSync(mainFilePath)!!.copyTo(zout)
                        zout.closeEntry()
                    }
                }
            }
        } catch (exception: Exception) {
            showErrorToast(exception)
            return false
        } finally {
            res.close()
        }
        return true
    }

    private fun showNotification(jobId: Int, fileName: String, title: String, progress: Int, progressMax: Int): Notification {
        val channelId = "simple_file_manager_compression"
        val label = getString(R.string.app_name)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (isOreoPlus()) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            NotificationChannel(channelId, label, importance).apply {
                setSound(null, null)
                notificationManager.createNotificationChannel(this)
            }
        }

        val priority = Notification.PRIORITY_DEFAULT
        val icon = R.drawable.ic_decompress_vector
        val visibility = NotificationCompat.VISIBILITY_PUBLIC

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(fileName)
            .setSmallIcon(icon)
            .setContentIntent(getOpenAppIntent())
            .setPriority(priority)
            .setVisibility(visibility)
            .addAction(com.simplemobiletools.commons.R.drawable.ic_cross_vector, getString(R.string.cancel), getCancelIntent(jobId))
            .setSound(null)
            .setOngoing(true)
            .setAutoCancel(true)

        if (progressMax > 0) {
            builder.setProgress(progressMax, progress, false)
        } else {
            builder.setProgress(progress, progress, true)
        }

        return builder.build()
    }

    private fun getOpenAppIntent(): PendingIntent {
        val intent = getLaunchIntent() ?: Intent(this, SplashActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getCancelIntent(jobId: Int): PendingIntent {
        val intent = Intent(this, CompressionService::class.java)
        intent.action = ACTION_CANCEL
        intent.putExtra(EXTRA_JOB_ID, jobId)
        return PendingIntent.getService(this, jobId, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun startServiceJob(jobId: Int, title: String, mainTitle: String) {
        running.add(jobId)
        startForeground(jobId, showNotification(jobId, title, mainTitle, 0, 0))
    }

    private fun finishServiceJob(jobId: Int) {
        ContextCompat.getMainExecutor(this).execute {
            notificationManager.cancel(jobId)
            running.remove(jobId)
            if (running.isEmpty()) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            }
        }
    }
}

