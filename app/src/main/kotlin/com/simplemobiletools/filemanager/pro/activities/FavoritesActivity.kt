package com.simplemobiletools.filemanager.pro.activities

import android.graphics.Paint
import android.os.Bundle
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.helpers.NavigationIcon
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.adapters.ManageFavoritesAdapter
import com.simplemobiletools.filemanager.pro.extensions.config
import kotlinx.android.synthetic.main.activity_favorites.*

class FavoritesActivity : SimpleActivity(), RefreshRecyclerViewListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        setupOptionsMenu()
        updateFavorites()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(manage_favorites_toolbar, NavigationIcon.Arrow)
    }

    private fun setupOptionsMenu() {
        manage_favorites_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_favorite -> addFavorite()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun updateFavorites() {
        val favorites = ArrayList<String>()
        config.favorites.mapTo(favorites) { it }
        manage_favorites_placeholder.beVisibleIf(favorites.isEmpty())
        manage_favorites_placeholder.setTextColor(getProperTextColor())

        manage_favorites_placeholder_2.apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            beVisibleIf(favorites.isEmpty())
            setTextColor(getProperPrimaryColor())
            setOnClickListener {
                addFavorite()
            }
        }

        ManageFavoritesAdapter(this, favorites, this, manage_favorites_list) { }.apply {
            manage_favorites_list.adapter = this
        }
    }

    override fun refreshItems() {
        updateFavorites()
    }

    private fun addFavorite() {
        FilePickerDialog(this, pickFile = false, showHidden = config.shouldShowHidden, canAddShowHiddenButton = true) {
            config.addFavorite(it)
            updateFavorites()
        }
    }
}
