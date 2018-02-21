package com.simplemobiletools.filemanager.helpers

import com.simplemobiletools.commons.extensions.areDigitsOnly
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.extensions.config
import com.stericson.RootShell.execution.Command
import com.stericson.RootTools.RootTools
import java.io.File
import java.util.*

class RootHelpers {
    fun askRootIfNeeded(activity: SimpleActivity, callback: (success: Boolean) -> Unit) {
        val command = object : Command(0, "ls -lA") {
            override fun commandOutput(id: Int, line: String) {
                callback(true)
                super.commandOutput(id, line)
            }
        }

        try {
            RootTools.getShell(true).add(command)
        } catch (exception: Exception) {
            activity.showErrorToast(exception)
            callback(false)
        }
    }

    fun getFiles(activity: SimpleActivity, path: String, callback: (originalPath: String, fileDirItems: ArrayList<FileDirItem>) -> Unit) {
        val files = ArrayList<FileDirItem>()
        val hiddenArgument = if (activity.config.shouldShowHidden) "-A " else ""
        val cmd = "ls $hiddenArgument$path"

        val command = object : Command(0, cmd) {
            override fun commandOutput(id: Int, line: String) {
                val file = File(path, line)
                val isDirectory = file.isDirectory
                val fileDirItem = FileDirItem(file.absolutePath, line, isDirectory, 0, 0)
                files.add(fileDirItem)
                super.commandOutput(id, line)
            }

            override fun commandCompleted(id: Int, exitcode: Int) {
                getChildrenCount(activity, files, path, callback)
                super.commandCompleted(id, exitcode)
            }
        }

        runCommand(activity, command)
    }

    private fun getChildrenCount(activity: SimpleActivity, files: ArrayList<FileDirItem>, path: String, callback: (originalPath: String, fileDirItems: ArrayList<FileDirItem>) -> Unit) {
        val hiddenArgument = if (activity.config.shouldShowHidden) "-A " else ""
        var cmd = ""
        files.forEach {
            cmd += if (it.isDirectory) {
                "ls $hiddenArgument${it.path} |wc -l;"
            } else {
                "echo 0;"
            }
        }
        cmd = cmd.trimEnd(';') + " | cat"

        val lines = ArrayList<String>()
        val command = object : Command(0, cmd) {
            override fun commandOutput(id: Int, line: String) {
                lines.add(line)
                super.commandOutput(id, line)
            }

            override fun commandCompleted(id: Int, exitcode: Int) {
                files.forEachIndexed { index, fileDirItem ->
                    val childrenCount = lines[index]
                    if (childrenCount.areDigitsOnly()) {
                        fileDirItem.children = childrenCount.toInt()
                    }
                }
                getFileSizes(activity, files, path, callback)
                super.commandCompleted(id, exitcode)
            }
        }

        runCommand(activity, command)
    }

    private fun getFileSizes(activity: SimpleActivity, files: ArrayList<FileDirItem>, path: String, callback: (originalPath: String, fileDirItems: ArrayList<FileDirItem>) -> Unit) {
        var cmd = ""
        files.forEach {
            cmd += if (it.isDirectory) {
                "echo 0;"
            } else {
                "stat -t ${it.path};"
            }
        }

        val lines = ArrayList<String>()
        val command = object : Command(0, cmd) {
            override fun commandOutput(id: Int, line: String) {
                lines.add(line)
                super.commandOutput(id, line)
            }

            override fun commandCompleted(id: Int, exitcode: Int) {
                files.forEachIndexed { index, fileDirItem ->
                    var line = lines[index]
                    if (line.isNotEmpty() && line != "0") {
                        if (line.length >= fileDirItem.path.length) {
                            line = line.substring(fileDirItem.path.length).trim()
                            val size = line.split(" ")[0]
                            if (size.areDigitsOnly()) {
                                fileDirItem.size = size.toLong()
                            }
                        }
                    }
                }
                callback(path, files)
                super.commandCompleted(id, exitcode)
            }
        }

        runCommand(activity, command)
    }

    private fun runCommand(activity: SimpleActivity, command: Command) {
        try {
            RootTools.getShell(true).add(command)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }
}
