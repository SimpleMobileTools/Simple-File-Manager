package com.simplemobiletools.filemanager.helpers

import android.text.TextUtils
import com.simplemobiletools.commons.extensions.areDigitsOnly
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

    fun getFiles(activity: SimpleActivity, path: String, callback: (fileDirItems: ArrayList<FileDirItem>) -> Unit) {
        val files = ArrayList<FileDirItem>()
        val showHidden = activity.config.shouldShowHidden

        val cmd = "ls -la $path | awk '{ system(\"echo \"\$1\" \"\$4\" `find $path/\"\$NF\" -mindepth 1 -maxdepth 1 | wc -l` \"\$NF\" \")}'"
        val command = object : Command(0, cmd) {
            override fun commandOutput(id: Int, line: String) {
                val parts = line.split(" ")
                if (parts.size >= 4) {
                    val permissions = parts[0].trim()
                    val isDirectory = permissions.startsWith("d")
                    val isFile = permissions.startsWith("-")
                    val size = if (isFile) parts[1].trim() else "0"
                    val childrenCnt = if (isFile) "0" else parts[2].trim()
                    val filename = TextUtils.join(" ", parts.subList(3, parts.size)).trimStart('/')

                    if ((!showHidden && filename.startsWith(".")) || (!isDirectory && !isFile) || !size.areDigitsOnly() || !childrenCnt.areDigitsOnly()) {
                        super.commandOutput(id, line)
                        return
                    }

                    val fileSize = size.toLong()
                    val filePath = "${path.trimEnd('/')}/$filename"
                    val fileDirItem = FileDirItem(filePath, filename, isDirectory, childrenCnt.toInt(), fileSize)
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

        try {
            RootTools.getShell(true).add(command)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }
}
