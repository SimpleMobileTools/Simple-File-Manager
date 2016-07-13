package com.simplemobiletools.filemanager.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;

import com.simplemobiletools.filemanager.Config;
import com.simplemobiletools.filemanager.Constants;
import com.simplemobiletools.filemanager.R;
import com.simplemobiletools.filemanager.Utils;
import com.simplemobiletools.filemanager.adapters.ItemsAdapter;
import com.simplemobiletools.filemanager.models.FileDirItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ItemsFragment extends android.support.v4.app.Fragment implements AdapterView.OnItemClickListener {
    @BindView(R.id.items_list) ListView mListView;

    private List<FileDirItem> mItems;
    private ItemInteractionListener mListener;
    private boolean mShowHidden;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.items_fragment, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mShowHidden = Config.newInstance(getContext()).getShowHidden();
        fillItems();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mShowHidden != Config.newInstance(getContext()).getShowHidden()) {
            mShowHidden = !mShowHidden;
            fillItems();
        }
    }

    private void fillItems() {
        final String path = getArguments().getString(Constants.PATH);
        mItems = getItems(path);
        Collections.sort(mItems);

        final ItemsAdapter adapter = new ItemsAdapter(getContext(), mItems);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(this);
    }

    public void setListener(ItemInteractionListener listener) {
        mListener = listener;
    }

    private List<FileDirItem> getItems(String path) {
        final List<FileDirItem> items = new ArrayList<>();
        final File base = new File(path);
        File[] files = base.listFiles();
        for (File file : files) {
            final String curPath = file.getAbsolutePath();
            final String curName = Utils.getFilename(curPath);
            if (!mShowHidden && curName.startsWith("."))
                continue;

            items.add(new FileDirItem(curPath, curName, file.isDirectory()));
        }
        return items;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final FileDirItem item = mItems.get(position);
        if (item.getIsDirectory()) {
            if (mListener != null)
                mListener.itemClicked(item.getPath());
        } else {
            final String path = item.getPath();
            final File file = new File(path);
            final String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(path));
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), mimeType);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Utils.showToast(getContext(), R.string.no_app_found);
            }
        }
    }

    public interface ItemInteractionListener {
        void itemClicked(String path);
    }
}
