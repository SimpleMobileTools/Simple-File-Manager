package com.simplemobiletools.filemanager.activities

import android.content.Intent
import android.os.Bundle
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.SecurityDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PROTECTION_FINGERPRINT
import com.simplemobiletools.commons.helpers.SHOW_ALL_TABS
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.config
import com.simplemobiletools.filemanager.helpers.RootHelpers
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*

class SettingsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onResume() {
        super.onResume()

        setupPurchaseThankYou()
        setupCustomizeColors()
        setupUseEnglish()
        setupAvoidWhatsNew()
        setupManageFavorites()
        setupShowHidden()
        setupPasswordProtection()
        setupKeepLastModified()
        setupShowInfoBubble()
        setupEnableRootAccess()
        updateTextColors(settings_holder)
        setupSectionColors()
    }

    private fun setupSectionColors() {
        val adjustedPrimaryColor = getAdjustedPrimaryColor()
        arrayListOf(visibility_label, file_operations_label, scrolling_label, security_label).forEach {
            it.setTextColor(adjustedPrimaryColor)
        }
    }

    private fun setupPurchaseThankYou() {
        settings_purchase_thank_you_holder.beVisibleIf(config.appRunCount > 10 && !isThankYouInstalled())
        settings_purchase_thank_you_holder.setOnClickListener {
            launchPurchaseThankYouIntent()
        }
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

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            System.exit(0)
        }
    }

    private fun setupAvoidWhatsNew() {
        settings_avoid_whats_new.isChecked = config.avoidWhatsNew
        settings_avoid_whats_new_holder.setOnClickListener {
            settings_avoid_whats_new.toggle()
            config.avoidWhatsNew = settings_avoid_whats_new.isChecked
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
            SecurityDialog(this, config.passwordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isPasswordProtectionOn
                    settings_password_protection.isChecked = !hasPasswordProtection
                    config.isPasswordProtectionOn = !hasPasswordProtection
                    config.passwordHash = if (hasPasswordProtection) "" else hash
                    config.protectionType = type

                    if (config.isPasswordProtectionOn) {
                        val confirmationTextId = if (config.protectionType == PROTECTION_FINGERPRINT)
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

    private fun setupShowInfoBubble() {
        settings_show_info_bubble.isChecked = config.showInfoBubble
        settings_show_info_bubble_holder.setOnClickListener {
            settings_show_info_bubble.toggle()
            config.showInfoBubble = settings_show_info_bubble.isChecked
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
