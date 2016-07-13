package com.simplemobiletools.filemanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.simplemobiletools.filemanager.R;
import com.simplemobiletools.filemanager.models.FileDirItem;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ItemsAdapter extends BaseAdapter {
    private final List<FileDirItem> mItems;
    private final LayoutInflater mInflater;

    public ItemsAdapter(Context context, List<FileDirItem> items) {
        mItems = items;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final FileDirItem item = mItems.get(position);
        viewHolder.name.setText(item.getName());

        return convertView;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void updateItems(List<FileDirItem> newItems) {
        mItems.clear();
        mItems.addAll(newItems);
        notifyDataSetChanged();
    }

    static class ViewHolder {
        @BindView(R.id.item_name) TextView name;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
