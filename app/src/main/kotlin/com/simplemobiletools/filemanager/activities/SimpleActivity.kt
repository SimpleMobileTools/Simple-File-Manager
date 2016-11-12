package com.simplemobiletools.filemanager.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem

import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.Constants
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filepicker.extensions.isShowingWritePermissions
import java.io.File

open class SimpleActivity : AppCompatActivity() {
    lateinit var mConfig: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        mConfig = Config.newInstance(applicationContext)
        setTheme(if (mConfig.isDarkTheme) R.style.AppTheme_Dark else R.style.AppTheme)
        super.onCreate(savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == Constants.OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK && resultData != null) {
            saveTreeUri(resultData)
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun saveTreeUri(resultData: Intent) {
        val treeUri = resultData.data
        mConfig.treeUri = treeUri.toString()

        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(treeUri, takeFlags)
    }

    fun isShowingPermDialog(file: File) = isShowingWritePermissions(file, mConfig.treeUri, Constants.OPEN_DOCUMENT_TREE)
}
