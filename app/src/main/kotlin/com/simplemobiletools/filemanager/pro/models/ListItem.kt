package com.simplemobiletools.filemanager.pro.models

import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.models.FileDirItem
import java.io.File

data class ListItem(val mPath: String, val mName: String = "", var mIsDirectory: Boolean = false, var mChildren: Int = 0, var mSize: Long = 0L, var mModified: Long = 0L,
                    var isSectionTitle: Boolean) : FileDirItem(mPath, mName, mIsDirectory, mChildren, mSize, mModified) {
    fun getSignature(): String {
        val lastModified = if (modified > 1) {
            modified
        } else {
            File(path).lastModified()
        }

        return "$path-$lastModified-$size"
    }

    fun getKey() = ObjectKey(getSignature())
}
