package com.simplemobiletools.filemanager.activities

import android.os.Bundle
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.activity_read_text.*
import java.io.File

class ReadTextActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_text)

        read_text_view.setTextColor(config.textColor)
        val uri = intent.data
        if (uri.scheme == "file") {
            read_text_view.text = File(uri.path).readText()
        } else {
            read_text_view.text = contentResolver.openInputStream(uri).bufferedReader().use { it.readText() }
        }
    }
}
