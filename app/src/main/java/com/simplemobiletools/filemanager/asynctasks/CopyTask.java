package com.simplemobiletools.filemanager.asynctasks;

import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.List;

public class CopyTask extends AsyncTask<Pair<List<File>, File>, Void, Boolean> {
    private static final String TAG = CopyTask.class.getSimpleName();

    private static WeakReference<CopyListener> mListener;
    private static File destinationDir;

    public CopyTask(CopyListener listener) {
        mListener = new WeakReference<>(listener);
    }

    @Override
    protected Boolean doInBackground(Pair<List<File>, File>... params) {
        final Pair<List<File>, File> pair = params[0];
        final List<File> files = pair.first;
        for (File file : files) {
            try {
                destinationDir = new File(pair.second, file.getName());
                copy(file, destinationDir);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "copy " + e);
            }
        }
        return false;
    }

    private void copy(File source, File destination) throws Exception {
        if (source.isDirectory()) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw new IOException("Could not create dir " + destination.getAbsolutePath());
            }

            final String[] children = source.list();
            for (String child : children) {
                copy(new File(source, child), new File(destination, child));
            }
        } else {
            final File directory = destination.getParentFile();
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Could not create dir " + directory.getAbsolutePath());
            }

            final InputStream in = new FileInputStream(source);
            final OutputStream out = new FileOutputStream(destination);

            final byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        final CopyListener listener = mListener.get();
        if (listener == null)
            return;

        if (success) {
            listener.copySucceeded(destinationDir);
        } else {
            listener.copyFailed();
        }
    }

    public interface CopyListener {
        void copySucceeded(File destinationDir);

        void copyFailed();
    }
}
