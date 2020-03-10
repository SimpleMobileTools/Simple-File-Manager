package com.simplemobiletools.filemanager.pro.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import com.simplemobiletools.commons.dialogs.ChangeDateTimeFormatDialog
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.dialogs.SecurityDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.helpers.RootHelpers
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*

class SettingsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onResume() {
        super.onResume()

        setupCustomizeColors()
        setupUseEnglish()
        setupManageFavorites()
        setupChangeDateTimeFormat()
        setupFontSize()
        setupShowHidden()
        setupHiddenItemPasswordProtection()
        setupAppPasswordProtection()
        setupFileDeletionPasswordProtection()
        setupKeepLastModified()
        setupEnableRootAccess()
        updateTextColors(settings_holder)
        setupSectionColors()
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setupSectionColors() {
        val adjustedPrimaryColor = getAdjustedPrimaryColor()
        arrayListOf(visibility_label, file_operations_label, security_label).forEach {
            it.setTextColor(adjustedPrimaryColor)
        }
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            System.exit(0)
        }
    }

    private fun setupManageFavorites() {
        settings_manage_favorites_holder.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    private fun setupChangeDateTimeFormat() {
        settings_change_date_time_format_holder.setOnClickListener {
            ChangeDateTimeFormatDialog(this) {}
        }
    }

    private fun setupFontSize() {
        settings_font_size.text = getFontSizeText()
        settings_font_size_holder.setOnClickListener {
            val items = arrayListOf(
                    RadioItem(FONT_SIZE_SMALL, getString(R.string.small)),
                    RadioItem(FONT_SIZE_MEDIUM, getString(R.string.medium)),
                    RadioItem(FONT_SIZE_LARGE, getString(R.string.large)),
                    RadioItem(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large)))

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                settings_font_size.text = getFontSizeText()
            }
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

    private fun setupHiddenItemPasswordProtection() {
        settings_password_protection.isChecked = config.isHiddenPasswordProtectionOn
        settings_password_protection_holder.setOnClickListener {
            val tabToShow = if (config.isHiddenPasswordProtectionOn) config.hiddenProtectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.hiddenPasswordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isHiddenPasswordProtectionOn
                    settings_password_protection.isChecked = !hasPasswordProtection
                    config.isHiddenPasswordProtectionOn = !hasPasswordProtection
                    config.hiddenPasswordHash = if (hasPasswordProtection) "" else hash
                    config.hiddenProtectionType = type

                    if (config.isHiddenPasswordProtectionOn) {
                        val confirmationTextId = if (config.hiddenProtectionType == PROTECTION_FINGERPRINT)
                            R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(this, "", confirmationTextId, R.string.ok, 0) { }
                    }
                }
            }
        }
    }

    private fun setupAppPasswordProtection() {
        settings_app_password_protection.isChecked = config.isAppPasswordProtectionOn
        settings_app_password_protection_holder.setOnClickListener {
            val tabToShow = if (config.isAppPasswordProtectionOn) config.appProtectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.appPasswordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isAppPasswordProtectionOn
                    settings_app_password_protection.isChecked = !hasPasswordProtection
                    config.isAppPasswordProtectionOn = !hasPasswordProtection
                    config.appPasswordHash = if (hasPasswordProtection) "" else hash
                    config.appProtectionType = type

                    if (config.isAppPasswordProtectionOn) {
                        val confirmationTextId = if (config.appProtectionType == PROTECTION_FINGERPRINT)
                            R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(this, "", confirmationTextId, R.string.ok, 0) { }
                    }
                }
            }
        }
    }

    private fun setupFileDeletionPasswordProtection() {
        settings_file_deletion_password_protection.isChecked = config.isDeletePasswordProtectionOn
        settings_file_deletion_password_protection_holder.setOnClickListener {
            val tabToShow = if (config.isDeletePasswordProtectionOn) config.deleteProtectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.deletePasswordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isDeletePasswordProtectionOn
                    settings_file_deletion_password_protection.isChecked = !hasPasswordProtection
                    config.isDeletePasswordProtectionOn = !hasPasswordProtection
                    config.deletePasswordHash = if (hasPasswordProtection) "" else hash
                    config.deleteProtectionType = type

                    if (config.isDeletePasswordProtectionOn) {
                        val confirmationTextId = if (config.deleteProtectionType == PROTECTION_FINGERPRINT)
                            R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(this, "", confirmationTextId, R.string.ok, 0) { }
                    }
                }
            }
        }
    }

    private fun setupKeepLastModified() {
        settings_keep_last_modified.isChecked = config.keepLastModified
        settings_keep_last_modified_holder.setOnClickListener {
            settings_keep_last_modified.toggle()
            config.keepLastModified = settings_keep_last_modified.isChecked
        }
    }

    private fun setupEnableRootAccess() {
        settings_enable_root_access_holder.beVisibleIf(config.isRootAvailable)
        settings_enable_root_access.isChecked = config.enableRootAccess
        settings_enable_root_access_holder.setOnClickListener {
            if (!config.enableRootAccess) {
                RootHelpers(this).askRootIfNeeded {
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
