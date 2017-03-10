package com.simplemobiletools.filemanager.activities

import android.os.Bundle
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupShowHidden()
    }

    private fun setupShowHidden() {
        settings_show_hidden.isChecked = config.showHidden
        settings_show_hidden_holder.setOnClickListener {
            settings_show_hidden.toggle()
            config.showHidden = settings_show_hidden.isChecked
        }
    }
}
