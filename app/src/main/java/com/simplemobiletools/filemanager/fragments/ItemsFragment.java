package com.simplemobiletools.filemanager.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;

import com.simplemobiletools.filemanager.Config;
import com.simplemobiletools.filemanager.Constants;
import com.simplemobiletools.filemanager.R;
import com.simplemobiletools.filemanager.Utils;
import com.simplemobiletools.filemanager.adapters.ItemsAdapter;
import com.simplemobiletools.filemanager.models.FileDirItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ItemsFragment extends android.support.v4.app.Fragment
        implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, ListView.MultiChoiceModeListener {
    @BindView(R.id.items_list) ListView mListView;
    @BindView(R.id.items_swipe_refresh) SwipeRefreshLayout mSwipeRefreshLayout;

    private List<FileDirItem> mItems;
    private ItemInteractionListener mListener;
    private String mPath;

    private boolean mShowHidden;
    private int mSelectedItemsCnt;

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
        mItems = new ArrayList<>();
        fillItems();
        mSwipeRefreshLayout.setOnRefreshListener(this);
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
        mPath = getArguments().getString(Constants.PATH);
        final List<FileDirItem> newItems = getItems();
        Collections.sort(newItems);
        if (mItems != null && newItems.toString().equals(mItems.toString())) {
            return;
        }

        mItems = newItems;

        final ItemsAdapter adapter = new ItemsAdapter(getContext(), mItems);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(this);
        mListView.setMultiChoiceModeListener(this);
    }

    public void setListener(ItemInteractionListener listener) {
        mListener = listener;
    }

    private List<FileDirItem> getItems() {
        final List<FileDirItem> items = new ArrayList<>();
        final File base = new File(mPath);
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
                if (!tryGenericMimeType(intent, mimeType, file)) {
                    Utils.showToast(getContext(), R.string.no_app_found);
                }
            }
        }
    }

    private boolean tryGenericMimeType(Intent intent, String mimeType, File file) {
        final String genericMimeType = getGenericMimeType(mimeType);
        intent.setDataAndType(Uri.fromFile(file), genericMimeType);
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    @OnClick(R.id.items_fab)
    public void fabClicked(View view) {
        final View newItemView = getActivity().getLayoutInflater().inflate(R.layout.create_new, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getResources().getString(R.string.create_new));
        builder.setView(newItemView);
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText itemName = (EditText) newItemView.findViewById(R.id.item_name);
                final String name = itemName.getText().toString().trim();
                if (Utils.isNameValid(name)) {
                    File file = new File(mPath, name);
                    final RadioGroup radio = (RadioGroup) newItemView.findViewById(R.id.dialog_radio_group);
                    if (radio.getCheckedRadioButtonId() == R.id.dialog_radio_directory) {
                        if (!createDirectory(file, alertDialog)) {
                            errorCreatingItem();
                        }
                    } else {
                        if (!createFile(file, alertDialog)) {
                            errorCreatingItem();
                        }
                    }
                } else {
                    Utils.showToast(getContext(), R.string.invalid_name);
                }
            }
        });
    }

    private boolean createDirectory(File file, AlertDialog alertDialog) {
        if (file.mkdirs()) {
            alertDialog.dismiss();
            fillItems();
            return true;
        }
        return false;
    }

    private void errorCreatingItem() {
        Utils.showToast(getContext(), R.string.error_occurred);
    }

    private boolean createFile(File file, AlertDialog alertDialog) {
        try {
            if (file.createNewFile()) {
                alertDialog.dismiss();
                fillItems();
                return true;
            }
        } catch (IOException ignored) {

        }
        return false;
    }

    @Override
    public void onRefresh() {
        fillItems();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private String getGenericMimeType(String mimeType) {
        if (!mimeType.contains("/"))
            return mimeType;

        final String type = mimeType.substring(0, mimeType.indexOf("/"));
        return type + "/*";
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked) {
            mSelectedItemsCnt++;
        } else {
            mSelectedItemsCnt--;
        }

        if (mSelectedItemsCnt > 0) {
            mode.setTitle(String.valueOf(mSelectedItemsCnt));
        }

        mode.invalidate();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.cab, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cab_delete:
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mSelectedItemsCnt = 0;
    }

    public interface ItemInteractionListener {
        void itemClicked(String path);
    }
}
