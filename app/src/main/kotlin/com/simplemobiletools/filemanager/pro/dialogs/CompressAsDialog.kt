package com.simplemobiletools.filemanager.pro.dialogs

import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.databinding.DialogCompressAsBinding
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.helpers.CompressionFormat

class CompressAsDialog(
    val activity: BaseSimpleActivity,
    val path: String,
    val callback: (destination: String, compressionFormat: CompressionFormat, password: String?) -> Unit
) {
    private val binding = DialogCompressAsBinding.inflate(activity.layoutInflater)

    init {
        val filename = path.getFilenameFromPath()
        val indexOfDot = if (filename.contains('.') && !activity.getIsPathDirectory(path)) filename.lastIndexOf(".") else filename.length
        val baseFilename = filename.substring(0, indexOfDot)
        var realPath = path.getParentPath()

        binding.apply {
            filenameValue.setText(baseFilename)

            folder.setText(activity.humanizePath(realPath))
            folder.setOnClickListener {
                FilePickerDialog(activity, realPath, false, activity.config.shouldShowHidden(), true, true, showFavoritesButton = true) {
                    folder.setText(activity.humanizePath(it))
                    realPath = it
                }
            }

            compressionFormatValue.apply {
                setOnClickListener {
                    activity.hideKeyboard(filenameValue)
                }
                val adapter = ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, CompressionFormat.values().map { it.extension })
                setAdapter(adapter)
                setText(adapter.getItem(0), false)

                setOnItemClickListener { _, _, i, _ ->
                    val compressionFormat = CompressionFormat.entries[i]
                    filenameHint.hint = String.format(activity.getString(R.string.filename_without_extension), compressionFormat.extension)
                    passwordProtect.beVisibleIf(compressionFormat.canCreateEncryptedArchive)
                    enterPasswordHint.beVisibleIf(compressionFormat.canCreateEncryptedArchive && passwordProtect.isChecked)
                }
            }
            passwordProtect.setOnCheckedChangeListener { _, _ ->
                enterPasswordHint.beVisibleIf(passwordProtect.isChecked)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.compress_as) { alertDialog ->
                    alertDialog.showKeyboard(binding.filenameValue)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                        val name = binding.filenameValue.value
                        var password: String? = null
                        if (binding.passwordProtect.isChecked) {
                            password = binding.password.value
                            if (password.isEmpty()) {
                                activity.toast(R.string.empty_password_new)
                                return@OnClickListener
                            }
                        }
                        when {
                            name.isEmpty() -> activity.toast(R.string.empty_name)
                            name.isAValidFilename() -> {
                                val newPath = "$realPath/$name${getSelectedCompressionFormat().extension}"
                                if (activity.getDoesFilePathExist(newPath)) {
                                    activity.toast(R.string.name_taken)
                                    return@OnClickListener
                                }

                                alertDialog.dismiss()
                                callback(newPath, getSelectedCompressionFormat(), password)
                            }

                            else -> activity.toast(R.string.invalid_name)
                        }
                    })
                }
            }
    }

    private fun getSelectedCompressionFormat() = CompressionFormat.fromExtension(binding.compressionFormatValue.text.toString())
}
