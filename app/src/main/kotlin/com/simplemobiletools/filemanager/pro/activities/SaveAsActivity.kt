package com.simplemobiletools.filemanager.pro.activities

import android.content.Intent
import android.os.Bundle
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.filemanager.pro.R

class SaveAsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_as)

        if (intent.action == Intent.ACTION_SEND && intent.extras?.containsKey(Intent.EXTRA_STREAM) == true) {

        } else {
            toast(R.string.unknown_error_occurred)
            finish()
        }
    }
}
