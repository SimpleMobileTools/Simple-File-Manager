package com.simplemobiletools.filemanager.models;

public class FileDirItem implements Comparable {
    private final String mPath;
    private final String mName;
    private final boolean mIsDirectory;
    private final int mChildren;
    private final long mSize;

    public FileDirItem(String path, String name, boolean isDirectory, int children, long size) {
        mPath = path;
        mName = name;
        mIsDirectory = isDirectory;
        mChildren = children;
        mSize = size;
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

    public int getChildren() {
        return mChildren;
    }

    public long getSize() {
        return mSize;
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
                ", path=" + getPath() +
                ", children=" + getChildren() +
                ", size=" + getSize() +"}";
    }
}
