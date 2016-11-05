package com.simplemobiletools.filemanager;

import android.content.Context;
import android.content.SharedPreferences;

public class Config {
    private SharedPreferences mPrefs;

    public static Config newInstance(Context context) {
        return new Config(context);
    }

    public Config(Context context) {
        mPrefs = context.getSharedPreferences(Constants.PREFS_KEY, Context.MODE_PRIVATE);
    }

    public boolean getIsFirstRun() {
        return mPrefs.getBoolean(Constants.IS_FIRST_RUN, true);
    }

    public void setIsFirstRun(boolean firstRun) {
        mPrefs.edit().putBoolean(Constants.IS_FIRST_RUN, firstRun).apply();
    }

    public boolean getIsDarkTheme() {
        return mPrefs.getBoolean(Constants.IS_DARK_THEME, false);
    }

    public void setIsDarkTheme(boolean isDarkTheme) {
        mPrefs.edit().putBoolean(Constants.IS_DARK_THEME, isDarkTheme).apply();
    }

    public boolean getShowHidden() {
        return mPrefs.getBoolean(Constants.SHOW_HIDDEN, false);
    }

    public void setShowHidden(boolean show) {
        mPrefs.edit().putBoolean(Constants.SHOW_HIDDEN, show).apply();
    }

    public String getTreeUri() {
        return mPrefs.getString(Constants.TREE_URI, "");
    }

    public void setTreeUri(String uri) {
        mPrefs.edit().putString(Constants.TREE_URI, uri).apply();
    }
}
