package com.simplemobiletools.filemanager.activities

import android.content.Intent
import android.os.Bundle
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onResume() {
        super.onResume()

        setupCustomizeColors()
        setupManageFavorites()
        setupShowHidden()
        updateTextColors(settings_holder)
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupManageFavorites() {
        settings_manage_favorites_holder.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    private fun setupShowHidden() {
        settings_show_hidden.isChecked = config.showHidden
        settings_show_hidden_holder.setOnClickListener {
            settings_show_hidden.toggle()
            config.showHidden = settings_show_hidden.isChecked
        }
    }
}
