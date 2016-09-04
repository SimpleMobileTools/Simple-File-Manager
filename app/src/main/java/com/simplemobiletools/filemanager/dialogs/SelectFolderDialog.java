package com.simplemobiletools.filemanager.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.simplemobiletools.filemanager.Breadcrumbs;
import com.simplemobiletools.filemanager.Config;
import com.simplemobiletools.filemanager.R;
import com.simplemobiletools.filemanager.Utils;
import com.simplemobiletools.filemanager.adapters.ItemsAdapter;
import com.simplemobiletools.filemanager.fragments.ItemsFragment;
import com.simplemobiletools.filemanager.models.FileDirItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectFolderDialog extends DialogFragment {
    private static String mPath;
    private static ListView mListView;
    private static Breadcrumbs mBreadcrumbs;

    public static SelectFolderDialog newInstance(String path) {
        mPath = path;
        return new SelectFolderDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.directory_picker, null);
        mListView = (ListView) view.findViewById(R.id.directory_picker_list);
        mBreadcrumbs = (Breadcrumbs) view.findViewById(R.id.directory_picker_breadcrumbs);

        updateItems();
        setupBreadcrumbs();

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.select_destination));
        builder.setView(view);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendResult();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        return builder.create();
    }

    private void updateItems() {
        final List<FileDirItem> items = getItems(mPath);
        if (!containsDirectory(items)) {
            sendResult();
            return;
        }

        Collections.sort(items);
        final ItemsAdapter adapter = new ItemsAdapter(getContext(), items);
        mListView.setAdapter(adapter);

        mBreadcrumbs.setInitialBreadcrumb(mPath);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final FileDirItem item = items.get(position);
                if (item.getIsDirectory()) {
                    mPath = item.getPath();
                    updateItems();
                }
            }
        });
    }

    private void sendResult() {
        Intent intent = new Intent();
        intent.putExtra(ItemsFragment.SELECT_FOLDER_PATH, mPath);
        getTargetFragment().onActivityResult(ItemsFragment.SELECT_FOLDER_REQUEST, Activity.RESULT_OK, intent);
        dismiss();
    }

    private void setupBreadcrumbs() {
        mBreadcrumbs.setListener(new Breadcrumbs.BreadcrumbsListener() {
            @Override
            public void breadcrumbClicked(int id) {
                final FileDirItem item = (FileDirItem) mBreadcrumbs.getChildAt(id).getTag();
                mPath = item.getPath();
                updateItems();
            }
        });
    }

    private List<FileDirItem> getItems(String path) {
        final boolean showHidden = Config.newInstance(getContext()).getShowHidden();
        final List<FileDirItem> items = new ArrayList<>();
        final File base = new File(path);
        File[] files = base.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory())
                    continue;

                if (!showHidden && file.isHidden())
                    continue;

                final String curPath = file.getAbsolutePath();
                final String curName = Utils.getFilename(curPath);
                int children = getChildren(file);
                long size = file.length();

                items.add(new FileDirItem(curPath, curName, file.isDirectory(), children, size));
            }
        }
        return items;
    }

    private int getChildren(File file) {
        if (file.listFiles() == null || !file.isDirectory())
            return 0;

        return file.listFiles().length;
    }

    private boolean containsDirectory(List<FileDirItem> items) {
        for (FileDirItem item : items) {
            if (item.getIsDirectory()) {
                return true;
            }
        }
        return false;
    }
}
