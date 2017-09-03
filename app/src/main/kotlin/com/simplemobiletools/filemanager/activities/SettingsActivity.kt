package com.simplemobiletools.filemanager.activities

import android.content.Intent
import android.os.Bundle
import com.simplemobiletools.commons.dialogs.SecurityDialog
import com.simplemobiletools.commons.extensions.handleHiddenFolderPasswordProtection
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.SHOW_ALL_TABS
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.config
import com.simplemobiletools.filemanager.helpers.RootHelpers
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
        setupPasswordProtection()
        setupEnableRootAccess()
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
            if (config.showHidden) {
                toggleShowHidden()
            } else {
                handleHiddenFolderPasswordProtection {
                    toggleShowHidden()
                }
            }
        }
    }

    private fun toggleShowHidden() {
        settings_show_hidden.toggle()
        config.showHidden = settings_show_hidden.isChecked
    }

    private fun setupPasswordProtection() {
        settings_password_protection.isChecked = config.isPasswordProtectionOn
        settings_password_protection_holder.setOnClickListener {
            val tabToShow = if (config.isPasswordProtectionOn) config.protectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.passwordHash, tabToShow) { hash, type ->
                val hasPasswordProtection = config.isPasswordProtectionOn
                settings_password_protection.isChecked = !hasPasswordProtection
                config.isPasswordProtectionOn = !hasPasswordProtection
                config.passwordHash = if (hasPasswordProtection) "" else hash
                config.protectionType = type
            }
        }
    }

    private fun setupEnableRootAccess() {
        settings_enable_root_access.isChecked = config.enableRootAccess
        settings_enable_root_access_holder.setOnClickListener {
            if (!config.enableRootAccess) {
                RootHelpers().askRootIFNeeded(this) {
                    toggleRootAccess(it)
                }
            } else {
                toggleRootAccess(false)
            }
        }
    }

    private fun toggleRootAccess(enable: Boolean) {
        settings_enable_root_access.isChecked = enable
        config.enableRootAccess = enable
    }
}
