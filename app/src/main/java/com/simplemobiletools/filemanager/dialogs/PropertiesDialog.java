package com.simplemobiletools.filemanager.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.simplemobiletools.filemanager.R;
import com.simplemobiletools.filemanager.models.FileDirItem;

public class PropertiesDialog extends DialogFragment {
    private static FileDirItem mItem;

    public static PropertiesDialog newInstance(FileDirItem item) {
        mItem = item;
        return new PropertiesDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int title = (mItem.getIsDirectory()) ? R.string.directory_properties : R.string.file_properties;

        final View infoView = getActivity().getLayoutInflater().inflate(R.layout.item_properties, null);
        if (mItem.getIsDirectory()) {
            infoView.findViewById(R.id.properties_files_count_label).setVisibility(View.VISIBLE);
            infoView.findViewById(R.id.properties_files_count).setVisibility(View.VISIBLE);
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getResources().getString(title));
        builder.setView(infoView);
        builder.setPositiveButton(R.string.ok, null);

        return builder.create();
    }
}
