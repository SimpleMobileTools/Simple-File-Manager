package com.simplemobiletools.filemanager

import android.app.Application
import com.github.ajalt.reprint.core.Reprint
import com.simplemobiletools.filemanager.extensions.config
import java.util.*

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (config.useEnglish) {
            val conf = resources.configuration
            conf.locale = Locale.ENGLISH
            resources.updateConfiguration(conf, resources.displayMetrics)
        }
        Reprint.initialize(this)
    }
}
