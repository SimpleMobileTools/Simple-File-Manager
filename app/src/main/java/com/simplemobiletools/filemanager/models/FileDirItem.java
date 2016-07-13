package com.simplemobiletools.filemanager.models;

public class FileDirItem implements Comparable {
    private final String mPath;
    private final String mName;
    private final boolean mIsDirectory;

    public FileDirItem(String path, String name, boolean isDirectory) {
        mPath = path;
        mName = name;
        mIsDirectory = isDirectory;
    }

    public String getPath() {
        return mPath;
    }

    public String getName() {
        return mName;
    }

    public boolean getIsDirectory() {
        return mIsDirectory;
    }

    @Override
    public int compareTo(Object object) {
        final FileDirItem item = (FileDirItem) object;
        if (mIsDirectory && !item.getIsDirectory()) {
            return -1;
        } else if (!mIsDirectory && item.getIsDirectory()) {
            return 1;
        }

        return mName.compareToIgnoreCase(item.getName());
    }

    @Override
    public String toString() {
        return "FileDirItem{" +
                "name=" + getName() +
                ", isDirectory=" + getIsDirectory() +
                ", path=" + getPath() + "}";
    }
}
