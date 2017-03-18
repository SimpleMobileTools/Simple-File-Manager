package com.simplemobiletools.filemanager.activities

import android.graphics.PorterDuff
import android.os.Bundle
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
        val folders = config.favorites

        for (folder in folders) {
            layoutInflater.inflate(R.layout.item_favorite, null, false).apply {
                favorite_title.apply {
                    text = folder
                    setTextColor(config.textColor)
                }
                favorite_icon.apply {
                    setColorFilter(config.textColor, PorterDuff.Mode.SRC_IN)
                    setOnClickListener {
                        config.removeFavorite(folder)
                        updateFavorites()
                    }
                }
                favorites_holder.addView(this)
            }
        }
    }
}
