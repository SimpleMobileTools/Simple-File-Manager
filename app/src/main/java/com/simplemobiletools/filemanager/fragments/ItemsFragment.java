package com.simplemobiletools.filemanager.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.util.Pair;
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
import android.widget.TextView;

import com.simplemobiletools.filemanager.Config;
import com.simplemobiletools.filemanager.Constants;
import com.simplemobiletools.filemanager.R;
import com.simplemobiletools.filemanager.Utils;
import com.simplemobiletools.filemanager.adapters.ItemsAdapter;
import com.simplemobiletools.filemanager.asynctasks.CopyTask;
import com.simplemobiletools.filemanager.dialogs.SelectFolderDialog;
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
        ListView.OnTouchListener, CopyTask.CopyListener {
    @BindView(R.id.items_list) ListView mListView;
    @BindView(R.id.items_swipe_refresh) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.items_holder) CoordinatorLayout mCoordinatorLayout;

    public static final int SELECT_FOLDER_REQUEST = 1;
    public static final String SELECT_FOLDER_PATH = "path";

    private List<FileDirItem> mItems;
    private ItemInteractionListener mListener;
    private List<String> mToBeDeleted;
    private String mPath;
    private String mCopyDestinationPath;
    private Snackbar mSnackbar;
    private AlertDialog mCopyDialog;
    private TextView mDestinationView;

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
        mCopyDestinationPath = mPath;
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
                final String curName = Utils.getFilename(curPath);
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
        if (item.getIsDirectory()) {
            if (mListener != null)
                mListener.itemClicked(item);
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
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

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
            case R.id.cab_share:
                shareFiles();
                mode.finish();
                return true;
            case R.id.cab_copy:
                displayCopyDialog();
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
            Utils.showToast(getContext(), R.string.no_files_selected);
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

    private void displayRenameDialog() {
        final List<Integer> itemIndexes = getSelectedItemIndexes();
        if (itemIndexes.isEmpty())
            return;

        final int itemIndex = itemIndexes.get(0);
        final FileDirItem item = mItems.get(itemIndex);
        final View renameView = getActivity().getLayoutInflater().inflate(R.layout.rename_item, null);
        final EditText itemName = (EditText) renameView.findViewById(R.id.item_name);
        itemName.setText(item.getName());

        final int renameString = (item.getIsDirectory()) ? R.string.rename_directory : R.string.rename_file;
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getResources().getString(renameString));
        builder.setView(renameView);
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

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

    private void displayCopyDialog() {
        final List<Integer> itemIndexes = getSelectedItemIndexes();
        if (itemIndexes.isEmpty())
            return;

        final View copyView = getActivity().getLayoutInflater().inflate(R.layout.copy_item, null);

        final TextView source = (TextView) copyView.findViewById(R.id.source);
        source.setText(mPath + "/");

        mDestinationView = (TextView) copyView.findViewById(R.id.destination);
        mDestinationView.setOnClickListener(destinationPicker);

        final int copyString = (itemIndexes.size() == 1) ? R.string.copy_item : R.string.copy_items;
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getResources().getString(copyString));
        builder.setView(copyView);
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

        mCopyDialog = builder.create();
        mCopyDialog.show();
        mCopyDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String destinationPath = mDestinationView.getText().toString().trim();
                if (destinationPath.equals(getResources().getString(R.string.select_destination))) {
                    Utils.showToast(getContext(), R.string.please_select_destination);
                    return;
                }

                final File destinationDir = new File(destinationPath);
                if (!destinationDir.exists()) {
                    Utils.showToast(getContext(), R.string.invalid_destination);
                    return;
                }

                final List<File> itemsToCopy = new ArrayList<>(itemIndexes.size());
                for (Integer i : itemIndexes) {
                    FileDirItem item = mItems.get(i);
                    itemsToCopy.add(new File(item.getPath()));
                }

                final RadioGroup radio = (RadioGroup) copyView.findViewById(R.id.dialog_radio_group);
                if (radio.getCheckedRadioButtonId() == R.id.dialog_radio_copy) {
                    Utils.showToast(getContext(), R.string.copying);
                    final Pair<List<File>, File> pair = new Pair<>(itemsToCopy, destinationDir);
                    new CopyTask(ItemsFragment.this).execute(pair);
                } else {
                    for (File f : itemsToCopy) {
                        f.renameTo(new File(destinationDir, f.getName()));
                    }

                    mCopyDialog.dismiss();
                    fillItems();
                }
            }
        });

        mCopyDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mCopyDestinationPath = mPath;
            }
        });
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

    private View.OnClickListener destinationPicker = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            SelectFolderDialog dialog = SelectFolderDialog.newInstance(mCopyDestinationPath);
            dialog.setTargetFragment(ItemsFragment.this, SELECT_FOLDER_REQUEST);
            dialog.show(getFragmentManager(), "selectFolder");
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_FOLDER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            mDestinationView.setText(data.getStringExtra(SELECT_FOLDER_PATH));
        }
        super.onActivityResult(requestCode, resultCode, data);
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

    @Override
    public void copySucceeded() {
        mCopyDialog.dismiss();
        fillItems();
    }

    @Override
    public void copyFailed() {
        Utils.showToast(getContext(), R.string.copy_failed);
    }

    public interface ItemInteractionListener {
        void itemClicked(FileDirItem item);
    }
}
