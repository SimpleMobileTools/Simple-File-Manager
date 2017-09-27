package com.simplemobiletools.filemanager

import android.app.Application
import com.github.ajalt.reprint.core.Reprint

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Reprint.initialize(this)
    }
}
