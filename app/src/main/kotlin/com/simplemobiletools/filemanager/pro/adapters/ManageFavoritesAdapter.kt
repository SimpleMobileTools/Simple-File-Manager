package com.simplemobiletools.filemanager.pro.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.extensions.config
import kotlinx.android.synthetic.main.item_manage_favorite.view.*
import java.util.*

class ManageFavoritesAdapter(activity: BaseSimpleActivity, var favorites: ArrayList<String>, val listener: RefreshRecyclerViewListener?,
                             recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    private val config = activity.config

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_remove_only

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_remove -> removeSelection()
        }
    }

    override fun getSelectableItemCount() = favorites.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = favorites.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = favorites.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun prepareActionMode(menu: Menu) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_manage_favorite, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favorite = favorites[position]
        holder.bindView(favorite, true, true) { itemView, layoutPosition ->
            setupView(itemView, favorite, selectedKeys.contains(favorite.hashCode()))
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = favorites.size

    private fun setupView(view: View, favorite: String, isSelected: Boolean) {
        view.apply {
            manage_favorite_title.apply {
                text = favorite
                setTextColor(config.textColor)
            }

            manage_favorite_holder?.isSelected = isSelected
        }
    }

    private fun removeSelection() {
        val removeFavorites = ArrayList<String>(selectedKeys.size)
        val positions = ArrayList<Int>()
        selectedKeys.forEach {
            val key = it
            val position = favorites.indexOfFirst { it.hashCode() == key }
            if (position != -1) {
                positions.add(position)

                val favorite = getItemWithKey(key)
                if (favorite != null) {
                    removeFavorites.add(favorite)
                    config.removeFavorite(favorite)
                }
            }
        }

        positions.sortDescending()
        removeSelectedItems(positions)

        favorites.removeAll(removeFavorites)
        if (favorites.isEmpty()) {
            listener?.refreshItems()
        }
    }

    private fun getItemWithKey(key: Int): String? = favorites.firstOrNull { it.hashCode() == key }
}
