package com.simplemobiletools.filemanager.helpers

import android.content.Context
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.extensions.config
import com.stericson.RootShell.execution.Command
import com.stericson.RootTools.RootTools
import java.util.*

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
        val SEPARATOR = "|||"

        val command = object : Command(0, "ls -la $path | awk '{print \$1,\"$SEPARATOR\",$4,\"$SEPARATOR\",\$NF}'") {
            override fun commandOutput(id: Int, line: String) {
                val parts = line.split(SEPARATOR)

                val filename = parts[2].trim().trimStart('/')
                if (showHidden || !filename.startsWith(".")) {
                    val filePath = "${path.trimEnd('/')}/$filename"
                    val permissions = parts[0].trim()
                    val isDirectory = permissions.startsWith("d")
                    val fileSize = if (permissions.startsWith("-")) parts[1].trim().toLong() else 0L
                    val fileDirItem = FileDirItem(filePath, filename, isDirectory, 0, fileSize)
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
        val shell = RootTools.getShell(true)
        oldItems.forEach {
            val command = object : Command(0, "find ${it.path} -mindepth 1 -maxdepth 1 | wc -l") {
                override fun commandOutput(id: Int, line: String) {
                    val areDigitsOnly = line.matches(Regex("[0-9 ]+"))
                    if (areDigitsOnly) {
                        val children = line.trim().toInt()
                        val fileDirItem = FileDirItem(it.path, it.name, it.isDirectory, children, it.size)
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
            shell.add(command)
        }
    }
}
