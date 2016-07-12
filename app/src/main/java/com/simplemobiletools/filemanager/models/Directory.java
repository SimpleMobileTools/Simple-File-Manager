package com.simplemobiletools.filemanager.models;

public class Directory implements Comparable {
    private final String path;
    private final String name;

    public Directory(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(Object object) {
        final Directory directory = (Directory) object;
        return this.name.compareToIgnoreCase(directory.getName());
    }

    @Override
    public String toString() {
        return "Directory {" +
                "name=" + getName() +
                ", path=" + getPath() + "}";
    }
}
