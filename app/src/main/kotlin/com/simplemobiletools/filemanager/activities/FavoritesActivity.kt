package com.simplemobiletools.filemanager.activities

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.activity_favorites.*
import kotlinx.android.synthetic.main.item_favorite.view.*

class FavoritesActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        updateFavorites()
    }

    private fun updateFavorites() {
        favorites_holder.removeAllViews()
        val favorites = config.favorites
        favorites_placeholder.beVisibleIf(favorites.isEmpty())
        favorites_placeholder.setTextColor(config.textColor)

        for (favorite in favorites) {
            layoutInflater.inflate(R.layout.item_favorite, null, false).apply {
                favorite_title.apply {
                    text = favorite
                    setTextColor(config.textColor)
                }
                favorite_icon.apply {
                    setColorFilter(config.textColor, PorterDuff.Mode.SRC_IN)
                    setOnClickListener {
                        config.removeFavorite(favorite)
                        updateFavorites()
                    }
                }
                favorites_holder.addView(this)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_favorites, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_favorite -> addFavorite()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun addFavorite() {
        FilePickerDialog(this, pickFile = false, showHidden = config.shouldShowHidden) {
            config.addFavorite(it)
            updateFavorites()
        }
    }
}
