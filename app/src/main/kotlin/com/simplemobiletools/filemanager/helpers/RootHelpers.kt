package com.simplemobiletools.filemanager.helpers

import android.content.Context
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.extensions.config
import com.stericson.RootShell.execution.Command
import com.stericson.RootTools.RootTools

class RootHelpers {
    fun askRootIFNeeded(activity: SimpleActivity, callback: (success: Boolean) -> Unit) {
        val SIMPLE_MOBILE_TOOLS = "simple mobile tools"
        val command = object : Command(0, "echo $SIMPLE_MOBILE_TOOLS") {
            override fun commandOutput(id: Int, line: String) {
                if (line == SIMPLE_MOBILE_TOOLS)
                    callback(true)
                super.commandOutput(id, line)
            }

            override fun commandTerminated(id: Int, reason: String?) {
                super.commandTerminated(id, reason)
            }

            override fun commandCompleted(id: Int, exitcode: Int) {
                super.commandCompleted(id, exitcode)
            }
        }

        try {
            RootTools.getShell(true).add(command)
        } catch (exception: Exception) {
            activity.showErrorToast(exception)
            callback(false)
        }
    }

    fun getFiles(context: Context, path: String, callback: (fileDirItems: ArrayList<FileDirItem>) -> Unit) {
        val files = ArrayList<FileDirItem>()
        val showHidden = context.config.shouldShowHidden

        val command = object : Command(0, "ls -la $path | awk '{print \$1,\$NF}'") {
            override fun commandOutput(id: Int, line: String) {
                val parts = line.split(" ")

                val filename = parts[1].trimStart('/')
                if (showHidden || !filename.startsWith(".")) {
                    val filePath = "${path.trimEnd('/')}/$filename"
                    val isDirectory = parts[0].startsWith("d")
                    val fileDirItem = FileDirItem(filePath, filename, isDirectory, 0, 0)
                    files.add(fileDirItem)
                }

                super.commandOutput(id, line)
            }

            override fun commandTerminated(id: Int, reason: String?) {
                super.commandTerminated(id, reason)
            }

            override fun commandCompleted(id: Int, exitcode: Int) {
                super.commandCompleted(id, exitcode)
                getFileDirParameters(files, callback)
            }
        }
        RootTools.getShell(true).add(command)
    }

    fun getFileDirParameters(oldItems: ArrayList<FileDirItem>, callback: (fileDirItems: ArrayList<FileDirItem>) -> Unit) {
        val files = ArrayList<FileDirItem>()
        oldItems.forEach {
            val childrenCount = "find ${it.path} -mindepth 1 -maxdepth 1 | wc -l"
            val fileSize = if (it.isDirectory) "" else "wc -c < ${it.path}"
            val command = object : Command(0, "echo $($childrenCount) $($fileSize)") {
                override fun commandOutput(id: Int, line: String) {
                    val areDigitsOnly = line.matches(Regex("[0-9 ]+"))
                    if (areDigitsOnly) {
                        val parts = line.split(' ')
                        val children = parts[0].toInt()
                        val bytes = if (parts.size > 1) parts[1].toLong() else 0L
                        val fileDirItem = FileDirItem(it.path, it.name, it.isDirectory, children, bytes)
                        files.add(fileDirItem)
                    }
                    super.commandOutput(id, line)
                }

                override fun commandTerminated(id: Int, reason: String?) {
                    super.commandTerminated(id, reason)
                }

                override fun commandCompleted(id: Int, exitcode: Int) {
                    callback(files)
                    super.commandCompleted(id, exitcode)
                }
            }
            RootTools.getShell(true).add(command)
        }
    }
}
