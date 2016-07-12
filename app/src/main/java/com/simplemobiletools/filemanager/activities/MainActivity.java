package com.simplemobiletools.filemanager.activities;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;

import com.simplemobiletools.filemanager.R;
import com.simplemobiletools.filemanager.Utils;
import com.simplemobiletools.filemanager.models.Directory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        List<Directory> dirs = getDirectories();
        Collections.sort(dirs);
    }

    private List<Directory> getDirectories() {
        final List<Directory> dirs = new ArrayList<>();
        final String path = Environment.getExternalStorageDirectory().toString();
        final File root = new File(path);
        File[] files = root.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                final String dirPath = file.getAbsolutePath();
                final String dirName = Utils.getFilename(dirPath);
                dirs.add(new Directory(dirPath, dirName));
            }
        }
        return dirs;
    }
}
