package com.simplemobiletools.filemanager.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.item_manage_favorite.view.*

class ManageFavoritesAdapter(activity: BaseSimpleActivity, var favorites: ArrayList<String>, val listener: RefreshRecyclerViewListener?,
                             recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    private val config = activity.config

    override fun getActionMenuId() = R.menu.cab_delete_only

    override fun prepareActionMode(menu: Menu) {}

    override fun prepareItemSelection(view: View) {}

    override fun markItemSelection(select: Boolean, view: View?) {
        view?.manage_favorite_holder?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = favorites.size

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) = createViewHolder(R.layout.item_manage_favorite, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favorite = favorites[position]
        val view = holder.bindView(favorite) { itemView, layoutPosition ->
            setupView(itemView, favorite)
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = favorites.size

    private fun setupView(view: View, favorite: String) {
        view.apply {
            manage_favorite_title.apply {
                text = favorite
                setTextColor(config.textColor)
            }
        }
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            deleteSelection()
        }
    }

    private fun deleteSelection() {
        val removeFavorites = ArrayList<String>(selectedPositions.size)

        selectedPositions.sortedDescending().forEach {
            val favorite = favorites[it]
            removeFavorites.add(favorite)
            config.removeFavorite(favorite)
        }

        favorites.removeAll(removeFavorites)
        removeSelectedItems()
        if (favorites.isEmpty()) {
            listener?.refreshItems()
        }
    }
}
