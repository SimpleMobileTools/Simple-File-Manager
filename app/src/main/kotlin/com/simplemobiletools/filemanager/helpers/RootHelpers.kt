package com.simplemobiletools.filemanager.helpers

import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.activities.SimpleActivity
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

    fun getFiles(path: String, callback: (fileDirItems: ArrayList<FileDirItem>) -> Unit) {
        val command = object : Command(0, "ls -la $path | awk '{print \$1,\$NF}'") {
            override fun commandOutput(id: Int, line: String) {
                val parts = line.split(" ")

                val files = ArrayList<FileDirItem>()
                val filename = parts[1]
                val filePath = "${path.trimEnd('/')}/$filename"
                val isDirectory = parts[0].startsWith("d")
                val fileDirItem = FileDirItem(filePath, filename, isDirectory, 0, 0)
                files.add(fileDirItem)
                callback(files)
                super.commandOutput(id, line)
            }

            override fun commandTerminated(id: Int, reason: String?) {
                super.commandTerminated(id, reason)
            }

            override fun commandCompleted(id: Int, exitcode: Int) {
                super.commandCompleted(id, exitcode)
            }
        }
        RootTools.getShell(true).add(command)
    }
}
