package com.simplemobiletools.filemanager.asynctasks

import android.os.AsyncTask
import android.support.v4.util.Pair
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference

class CopyTask(listener: CopyTask.CopyListener) : AsyncTask<Pair<List<File>, File>, Void, Boolean>() {
    private val TAG = CopyTask::class.java.simpleName
    private var mListener: WeakReference<CopyListener>? = null
    private var destinationDir: File? = null

    init {
        mListener = WeakReference(listener)
    }

    override fun doInBackground(vararg params: Pair<List<File>, File>): Boolean? {
        val pair = params[0]
        val files = pair.first
        for (file in files) {
            try {
                destinationDir = File(pair.second, file.name)
                copy(file, destinationDir!!)
                return true
            } catch (e: Exception) {
                Log.e(TAG, "copy " + e)
            }

        }
        return false
    }

    @Throws(Exception::class)
    private fun copy(source: File, destination: File) {
        if (source.isDirectory) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw IOException("Could not create dir " + destination.absolutePath)
            }

            val children = source.list()
            for (child in children) {
                copy(File(source, child), File(destination, child))
            }
        } else {
            val directory = destination.parentFile
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Could not create dir " + directory.absolutePath)
            }

            val inputStream = FileInputStream(source)
            val out = FileOutputStream(destination)

            val buf = ByteArray(1024)
            var len: Int
            while (true) {
                len = inputStream.read(buf)
                if (len <= 0)
                    break
                out.write(buf, 0, len)
            }
        }
    }

    override fun onPostExecute(success: Boolean) {
        val listener = mListener?.get() ?: return

        if (success) {
            listener.copySucceeded(destinationDir!!)
        } else {
            listener.copyFailed()
        }
    }

    interface CopyListener {
        fun copySucceeded(destinationDir: File)

        fun copyFailed()
    }
}
