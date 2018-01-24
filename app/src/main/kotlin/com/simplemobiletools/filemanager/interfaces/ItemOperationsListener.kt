package com.simplemobiletools.filemanager.interfaces

import java.io.File
import java.util.*

interface ItemOperationsListener {
    fun refreshItems()

    fun deleteFiles(files: ArrayList<File>)

    fun selectedPaths(paths: ArrayList<String>)
}
