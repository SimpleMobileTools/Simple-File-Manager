package com.simplemobiletools.filemanager.asynctasks

import android.content.Context
import android.os.AsyncTask
import android.support.v4.util.Pair
import android.util.Log
import com.simplemobiletools.filemanager.Utils
import com.simplemobiletools.filemanager.extensions.rescanItem
import com.simplemobiletools.filemanager.fragments.ItemsFragment
import java.io.*
import java.lang.ref.WeakReference
import java.util.*

class CopyTask(listener: CopyTask.CopyListener, val context: Context, val deleteAfterCopy: Boolean) : AsyncTask<Pair<List<File>, File>, Void, Boolean>() {
    private val TAG = CopyTask::class.java.simpleName
    private var mListener: WeakReference<CopyListener>? = null
    private var mMovedFiles: ArrayList<File>

    init {
        mListener = WeakReference(listener)
        mMovedFiles = arrayListOf()
    }

    override fun doInBackground(vararg params: Pair<List<File>, File>): Boolean? {
        val pair = params[0]
        val files = pair.first
        for (file in files) {
            try {
                val curFile = File(pair.second, file.name)
                if (curFile.exists())
                    continue

                copy(file, curFile)
            } catch (e: Exception) {
                Log.e(TAG, "copy $e")
                return false
            }
        }

        if (deleteAfterCopy) {
            for (file in mMovedFiles) {
                if (Utils.needsStupidWritePermissions(context, file.absolutePath)) {
                    Utils.getFileDocument(context, file.absolutePath).delete()
                } else {
                    file.delete()
                }
            }
        }
        return true
    }

    @Throws(Exception::class)
    private fun copy(source: File, destination: File) {
        if (source.isDirectory) {
            copyDirectory(source, destination)
        } else {
            copyFile(source, destination)
        }
    }

    private fun copyDirectory(source: File, destination: File) {
        if (!destination.exists()) {
            if (Utils.needsStupidWritePermissions(context, destination.absolutePath)) {
                val document = Utils.getFileDocument(context, destination.absolutePath)
                document.createDirectory(destination.name)
            } else if (!destination.mkdirs()) {
                throw IOException("Could not create dir ${destination.absolutePath}")
            }
        }

        val children = source.list()
        for (child in children) {
            val newFile = File(source, child)
            if (Utils.needsStupidWritePermissions(context, destination.absolutePath)) {
                if (newFile.isDirectory) {
                    copyDirectory(newFile, File(destination, child))
                } else {
                    var document = Utils.getFileDocument(context, destination.absolutePath)
                    document = document.createFile("", child)

                    val inputStream = FileInputStream(newFile)
                    val out = context.contentResolver.openOutputStream(document.uri)
                    copyStream(inputStream, out)
                    context.rescanItem(destination)
                    mMovedFiles.add(source)
                }
            } else {
                copy(newFile, File(destination, child))
            }
        }
    }

    private fun copyFile(source: File, destination: File) {
        val directory = destination.parentFile
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not create dir ${directory.absolutePath}")
        }

        val inputStream = FileInputStream(source)
        val out: OutputStream?
        if (Utils.needsStupidWritePermissions(context, destination.absolutePath)) {
            var document = Utils.getFileDocument(context, destination.absolutePath)
            document = document.createFile("", destination.name)
            out = context.contentResolver.openOutputStream(document.uri)
        } else {
            out = FileOutputStream(destination)
        }

        copyStream(inputStream, out)
        context.rescanItem(destination)
        mMovedFiles.add(source)
    }

    private fun copyStream(inputStream: InputStream, out: OutputStream?) {
        val buf = ByteArray(1024)
        var len: Int
        while (true) {
            len = inputStream.read(buf)
            if (len <= 0)
                break
            out?.write(buf, 0, len)
        }
    }

    override fun onPostExecute(success: Boolean) {
        val listener = mListener?.get() ?: return

        if (success) {
            listener.copySucceeded(if (deleteAfterCopy) ItemsFragment.ACTION_MOVE else ItemsFragment.ACTION_COPY)
        } else {
            listener.copyFailed()
        }
    }

    interface CopyListener {
        fun copySucceeded(action: Int)

        fun copyFailed()
    }
}
