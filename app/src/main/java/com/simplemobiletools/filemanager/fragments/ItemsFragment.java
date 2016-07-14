package com.simplemobiletools.filemanager.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ItemsFragment extends android.support.v4.app.Fragment
        implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, ListView.MultiChoiceModeListener,
        ListView.OnTouchListener {
    @BindView(R.id.items_list) ListView mListView;
    @BindView(R.id.items_swipe_refresh) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.items_holder) CoordinatorLayout mCoordinatorLayout;

    private List<FileDirItem> mItems;
    private ItemInteractionListener mListener;
    private List<String> mToBeDeleted;
    private String mPath;
    private Snackbar mSnackbar;

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
        mToBeDeleted = new ArrayList<>();
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

    @Override
    public void onPause() {
        super.onPause();
        deleteItems();
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
        mListView.setOnTouchListener(this);
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

            if (mToBeDeleted.contains(curPath))
                continue;

            int children = getChildren(file);
            long size = file.length();

            items.add(new FileDirItem(curPath, curName, file.isDirectory(), children, size));
        }
        return items;
    }

    private int getChildren(File file) {
        if (file.isDirectory()) {
            if (mShowHidden) {
                return file.listFiles().length;
            } else {
                return file.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return !file.isHidden();
                    }
                }).length;
            }
        }
        return 0;
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
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(path));
            if (mimeType == null)
                mimeType = "text/plain";

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
                    final File file = new File(mPath, name);
                    if (file.exists()) {
                        Utils.showToast(getContext(), R.string.name_taken);
                        return;
                    }
                    final RadioGroup radio = (RadioGroup) newItemView.findViewById(R.id.dialog_radio_group);
                    if (radio.getCheckedRadioButtonId() == R.id.dialog_radio_directory) {
                        if (!createDirectory(file, alertDialog)) {
                            errorOccurred();
                        }
                    } else {
                        if (!createFile(file, alertDialog)) {
                            errorOccurred();
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

    private void errorOccurred() {
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
        final MenuItem menuItem = menu.findItem(R.id.cab_rename);
        menuItem.setVisible(mSelectedItemsCnt == 1);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cab_rename:
                displayRenameDialog();
                mode.finish();
                return true;
            case R.id.cab_delete:
                prepareForDeleting();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    private void displayRenameDialog() {
        final int itemIndex = getSelectedItemIndex();
        if (itemIndex == -1)
            return;

        final FileDirItem item = mItems.get(itemIndex);
        final View renameView = getActivity().getLayoutInflater().inflate(R.layout.rename_item, null);
        final EditText itemName = (EditText) renameView.findViewById(R.id.item_name);
        itemName.setText(item.getName());

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        int renameString = R.string.rename_file;
        if (item.getIsDirectory())
            renameString = R.string.rename_directory;

        builder.setTitle(getResources().getString(renameString));
        builder.setView(renameView);
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = itemName.getText().toString().trim();
                if (Utils.isNameValid(name)) {
                    final File currFile = new File(mPath, item.getName());
                    final File newFile = new File(mPath, name);

                    if (newFile.exists()) {
                        Utils.showToast(getContext(), R.string.name_taken);
                        return;
                    }

                    if (currFile.renameTo(newFile)) {
                        alertDialog.dismiss();
                        fillItems();
                    } else {
                        errorOccurred();
                    }
                } else {
                    Utils.showToast(getContext(), R.string.invalid_name);
                }
            }
        });
    }

    private int getSelectedItemIndex() {
        final SparseBooleanArray items = mListView.getCheckedItemPositions();
        int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                return items.keyAt(i);
            }
        }
        return -1;
    }

    private void prepareForDeleting() {
        mToBeDeleted.clear();
        final SparseBooleanArray items = mListView.getCheckedItemPositions();
        final int cnt = items.size();
        int deletedCnt = 0;
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                final int id = items.keyAt(i);
                final String path = mItems.get(id).getPath();
                mToBeDeleted.add(path);
                deletedCnt++;
            }
        }

        notifyDeletion(deletedCnt);
    }

    private void notifyDeletion(int cnt) {
        final Resources res = getResources();
        final String msg = res.getQuantityString(R.plurals.items_deleted, cnt, cnt);
        mSnackbar = Snackbar.make(mCoordinatorLayout, msg, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction(res.getString(R.string.undo), undoDeletion);
        mSnackbar.setActionTextColor(Color.WHITE);
        mSnackbar.show();
        fillItems();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mSnackbar != null && mSnackbar.isShown()) {
            deleteItems();
        }

        return false;
    }

    private void deleteItems() {
        if (mToBeDeleted == null || mToBeDeleted.isEmpty())
            return;

        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }

        final List<String> updatedFiles = new ArrayList<>();
        for (String delPath : mToBeDeleted) {
            final File file = new File(delPath);
            if (file.exists()) {
                deleteItem(file);
            }
        }

        mToBeDeleted.clear();
    }

    private void deleteItem(File item) {
        if (item.isDirectory()) {
            for (File child : item.listFiles()) {
                deleteItem(child);
            }
        }

        item.delete();
    }

    private View.OnClickListener undoDeletion = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mToBeDeleted.clear();
            mSnackbar.dismiss();
            fillItems();
        }
    };

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mSelectedItemsCnt = 0;
    }

    public interface ItemInteractionListener {
        void itemClicked(String path);
    }
}
