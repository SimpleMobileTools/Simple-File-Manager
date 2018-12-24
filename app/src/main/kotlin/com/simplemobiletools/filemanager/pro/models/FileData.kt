package com.simplemobiletools.filemanager.pro.models

import com.amaze.filepreloaderlibrary.datastructures.DataContainer
import com.simplemobiletools.commons.extensions.getDirectChildrenCount
import com.simplemobiletools.commons.extensions.getProperSize
import com.simplemobiletools.commons.models.FileDirItem
import java.io.File

class FileData(path: String) : DataContainer(path) {
    companion object {
        var showHidden = false
        var isSortingBySize = false
    }

    var fileDirItem: FileDirItem? = null

    init {
        val file = File(path)
        val curPath = file.absolutePath
        val curName = file.name
        if (!showHidden && curName.startsWith(".")) else {
            val isDirectory = file.isDirectory
            val children = if (isDirectory) file.getDirectChildrenCount(showHidden) else 0
            val size = if (isDirectory) {
                if (isSortingBySize) {
                    file.getProperSize(showHidden)
                } else {
                    0L
                }
            } else {
                file.length()
            }

            fileDirItem = FileDirItem(curPath, curName, isDirectory, children, size)
        }
    }
}
