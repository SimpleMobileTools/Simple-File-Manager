package com.simplemobiletools.filemanager.activities;

import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.widget.SwitchCompat;

import com.simplemobiletools.filemanager.Config;
import com.simplemobiletools.filemanager.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends SimpleActivity {
    @BindView(R.id.settings_dark_theme) SwitchCompat mDarkThemeSwitch;
    @BindView(R.id.settings_show_hidden) SwitchCompat mShowHiddenSwitch;
    @BindView(R.id.settings_show_full_path) SwitchCompat mShowFullPathSwitch;

    private static Config mConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mConfig = Config.newInstance(getApplicationContext());
        ButterKnife.bind(this);

        setupDarkTheme();
        setupShowHidden();
        setupShowFullPath();
    }

    private void setupDarkTheme() {
        mDarkThemeSwitch.setChecked(mConfig.getIsDarkTheme());
    }

    private void setupShowHidden() {
        mShowHiddenSwitch.setChecked(mConfig.getShowHidden());
    }

    private void setupShowFullPath() {
        mShowFullPathSwitch.setChecked(mConfig.getShowFullPath());
    }

    @OnClick(R.id.settings_dark_theme_holder)
    public void handleDarkTheme() {
        mDarkThemeSwitch.setChecked(!mDarkThemeSwitch.isChecked());
        mConfig.setIsDarkTheme(mDarkThemeSwitch.isChecked());
        restartActivity();
    }

    @OnClick(R.id.settings_show_hidden_holder)
    public void handleShowHidden() {
        mShowHiddenSwitch.setChecked(!mShowHiddenSwitch.isChecked());
        mConfig.setShowHidden(mShowHiddenSwitch.isChecked());
    }

    @OnClick(R.id.settings_show_full_path_holder)
    public void handleShowFullPath() {
        mShowFullPathSwitch.setChecked(!mShowFullPathSwitch.isChecked());
        mConfig.setShowFullPath(mShowFullPathSwitch.isChecked());
    }

    private void restartActivity() {
        TaskStackBuilder.create(getApplicationContext()).addNextIntentWithParentStack(getIntent()).startActivities();
    }
}
