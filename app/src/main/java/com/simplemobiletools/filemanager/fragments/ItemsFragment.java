package com.simplemobiletools.filemanager.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.provider.DocumentFile;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import com.simplemobiletools.filemanager.asynctasks.CopyTask;
import com.simplemobiletools.filemanager.dialogs.CopyDialog;
import com.simplemobiletools.filemanager.dialogs.CreateNewItemDialog;
import com.simplemobiletools.filemanager.dialogs.RenameItemDialog;
import com.simplemobiletools.filepicker.models.FileDirItem;
import com.simplemobiletools.fileproperties.dialogs.PropertiesDialog;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ItemsFragment extends android.support.v4.app.Fragment
        implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, ListView.MultiChoiceModeListener,
        ListView.OnTouchListener, CopyTask.CopyListener {
    @BindView(R.id.items_list) ListView mListView;
    @BindView(R.id.items_swipe_refresh) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.items_holder) CoordinatorLayout mCoordinatorLayout;

    private static Map<String, Parcelable> mStates;

    private List<FileDirItem> mItems;
    private ItemInteractionListener mListener;
    private List<String> mToBeDeleted;
    private String mPath;
    private Snackbar mSnackbar;
    private Config mConfig;

    private boolean mShowHidden;
    private int mSelectedItemsCnt;

    public static int ACTION_COPY = 1;
    public static int ACTION_MOVE = 2;

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
        if (mStates == null)
            mStates = new HashMap<>();
        mConfig = Config.newInstance(getContext());
        mShowHidden = mConfig.getShowHidden();
        mItems = new ArrayList<>();
        mToBeDeleted = new ArrayList<>();
        fillItems();
        mSwipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mShowHidden != mConfig.getShowHidden()) {
            mShowHidden = !mShowHidden;
            mStates.remove(mPath);
            fillItems();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        deleteItems();
        mStates.put(mPath, mListView.onSaveInstanceState());
    }

    private void fillItems() {
        mPath = getArguments().getString(Constants.PATH);
        final List<FileDirItem> newItems = getItems(mPath);
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

        if (mStates != null && mStates.get(mPath) != null) {
            mListView.onRestoreInstanceState(mStates.get(mPath));
        }
    }

    public void setListener(ItemInteractionListener listener) {
        mListener = listener;
    }

    private List<FileDirItem> getItems(String path) {
        final List<FileDirItem> items = new ArrayList<>();
        final File base = new File(path);
        File[] files = base.listFiles();
        if (files != null) {
            for (File file : files) {
                final String curPath = file.getAbsolutePath();
                final String curName = Utils.Companion.getFilename(curPath);
                if (!mShowHidden && curName.startsWith("."))
                    continue;

                if (mToBeDeleted.contains(curPath))
                    continue;

                int children = getChildren(file);
                long size = file.length();

                items.add(new FileDirItem(curPath, curName, file.isDirectory(), children, size));
            }
        }
        return items;
    }

    private int getChildren(File file) {
        if (file.listFiles() == null)
            return 0;

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
        if (item.isDirectory()) {
            if (mListener != null)
                mListener.itemClicked(item);
        } else {
            final String path = item.getPath();
            final File file = new File(path);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.Companion.getFileExtension(path));
            if (mimeType == null)
                mimeType = "text/plain";

            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), mimeType);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                if (!tryGenericMimeType(intent, mimeType, file)) {
                    Utils.Companion.showToast(getContext(), R.string.no_app_found);
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
        new CreateNewItemDialog(getContext(), mPath, new CreateNewItemDialog.OnCreateNewItemListener() {
            @Override
            public void onSuccess() {
                fillItems();
            }
        });
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
        MenuItem menuItem = menu.findItem(R.id.cab_rename);
        menuItem.setVisible(mSelectedItemsCnt == 1);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cab_rename:
                displayRenameDialog();
                mode.finish();
                break;
            case R.id.cab_properties:
                displayPropertiesDialog();
                break;
            case R.id.cab_share:
                shareFiles();
                break;
            case R.id.cab_copy:
                displayCopyDialog();
                mode.finish();
                break;
            case R.id.cab_delete:
                prepareForDeleting();
                mode.finish();
                break;
            default:
                return false;
        }

        return true;
    }

    private void shareFiles() {
        final List<Integer> itemIndexes = getSelectedItemIndexes();
        if (itemIndexes.isEmpty())
            return;

        final ArrayList<Uri> uris = new ArrayList<>(itemIndexes.size());
        for (int i : itemIndexes) {
            final File file = new File(mItems.get(i).getPath());
            if (!file.isDirectory())
                uris.add(Uri.fromFile(file));
        }

        if (uris.isEmpty()) {
            Utils.Companion.showToast(getContext(), R.string.no_files_selected);
            return;
        }

        final String shareTitle = getResources().getString(R.string.share_via);
        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.shared_files));
        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        sendIntent.setType("*/*");
        startActivity(Intent.createChooser(sendIntent, shareTitle));
    }

    private void displayPropertiesDialog() {
        final List<Integer> itemIndexes = getSelectedItemIndexes();
        if (itemIndexes.isEmpty())
            return;

        if (itemIndexes.size() == 1) {
            showOneItemProperties();
        } else {
            showMultipleItemProperties(itemIndexes);
        }
    }

    private void showOneItemProperties() {
        final FileDirItem item = getSelectedItem();
        if (item == null)
            return;

        new PropertiesDialog(getActivity(), item.getPath(), mConfig.getShowHidden());
    }

    private void showMultipleItemProperties(List<Integer> itemIndexes) {
        final List<String> paths = new ArrayList<>(itemIndexes.size());
        for (int i : itemIndexes) {
            paths.add(mItems.get(i).getPath());
        }
        new PropertiesDialog(getActivity(), paths, mConfig.getShowHidden());
    }

    private void displayRenameDialog() {
        final FileDirItem item = getSelectedItem();
        if (item == null)
            return;

        new RenameItemDialog(getContext(), mPath, item, new RenameItemDialog.OnRenameItemListener() {
            @Override
            public void onSuccess() {
                fillItems();
            }
        });
    }

    private void displayCopyDialog() {
        final List<Integer> itemIndexes = getSelectedItemIndexes();
        if (itemIndexes.isEmpty())
            return;

        final ArrayList<File> itemsToCopy = new ArrayList<>(itemIndexes.size());
        for (Integer i : itemIndexes) {
            FileDirItem item = mItems.get(i);
            itemsToCopy.add(new File(item.getPath()));
        }

        new CopyDialog(getActivity(), itemsToCopy, this, new CopyDialog.OnCopyListener() {
            @Override
            public void onSuccess() {
                fillItems();
            }
        });
    }

    private FileDirItem getSelectedItem() {
        final List<Integer> itemIndexes = getSelectedItemIndexes();
        if (itemIndexes.isEmpty())
            return null;

        final int itemIndex = itemIndexes.get(0);
        return mItems.get(itemIndex);
    }

    private List<Integer> getSelectedItemIndexes() {
        final List<Integer> selectedItems = new ArrayList<>();
        final SparseBooleanArray items = mListView.getCheckedItemPositions();
        int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                selectedItems.add(items.keyAt(i));
            }
        }
        return selectedItems;
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

        if (Utils.Companion.needsStupidWritePermissions(getContext(), item.getAbsolutePath())) {
            final DocumentFile document = Utils.Companion.getFileDocument(getContext(), item.getAbsolutePath(), mConfig.getTreeUri());
            document.delete();
        } else {
            item.delete();
        }
        MediaScannerConnection.scanFile(getContext(), new String[]{item.getAbsolutePath()}, null, null);
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

    @Override
    public void copySucceeded(int action) {
        fillItems();
        Utils.Companion.showToast(getContext(), action == ACTION_COPY ? R.string.copying_success : R.string.moving_success);
    }

    @Override
    public void copyFailed() {
        Utils.Companion.showToast(getContext(), R.string.copying_failed);
    }

    public interface ItemInteractionListener {
        void itemClicked(FileDirItem item);
    }
}
