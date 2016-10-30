package com.simplemobiletools.filemanager.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.simplemobiletools.filemanager.Config;
import com.simplemobiletools.filemanager.Constants;
import com.simplemobiletools.filemanager.R;
import com.simplemobiletools.filemanager.Utils;
import com.simplemobiletools.filemanager.fragments.ItemsFragment;
import com.simplemobiletools.filepicker.models.FileDirItem;
import com.simplemobiletools.filepicker.views.Breadcrumbs;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends SimpleActivity implements ItemsFragment.ItemInteractionListener, Breadcrumbs.BreadcrumbsListener {
    @BindView(R.id.breadcrumbs) Breadcrumbs mBreadcrumbs;

    private static final int STORAGE_PERMISSION = 1;
    private static final int BACK_PRESS_TIMEOUT = 5000;

    private static boolean mShowFullPath;
    private static Config mConfig;
    private static boolean mWasBackJustPressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mBreadcrumbs.setListener(this);
        mConfig = Config.newInstance(getApplicationContext());
        tryInitFileManager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Utils.hasStoragePermission(getApplicationContext())) {
            final boolean showFullPath = mConfig.getShowFullPath();
            if (showFullPath != mShowFullPath)
                initRootFileManager();

            mShowFullPath = showFullPath;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mShowFullPath = mConfig.getShowFullPath();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Config.newInstance(getApplicationContext()).setIsFirstRun(false);
    }

    private void tryInitFileManager() {
        if (Utils.hasStoragePermission(getApplicationContext())) {
            initRootFileManager();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION);
        }
    }

    private void initRootFileManager() {
        openPath(Environment.getExternalStorageDirectory().toString());
    }

    private void openPath(String path) {
        mBreadcrumbs.setBreadcrumb(path, Environment.getExternalStorageDirectory().toString());
        final Bundle bundle = new Bundle();
        bundle.putString(Constants.PATH, path);

        final ItemsFragment fragment = new ItemsFragment();
        fragment.setArguments(bundle);
        fragment.setListener(this);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_holder, fragment).addToBackStack(path)
                .commitAllowingStateLoss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            case R.id.about:
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mBreadcrumbs.getChildCount() <= 1) {
            if (!mWasBackJustPressed) {
                mWasBackJustPressed = true;
                Utils.showToast(getApplicationContext(), R.string.press_back_again);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mWasBackJustPressed = false;
                    }
                }, BACK_PRESS_TIMEOUT);
            } else {
                finish();
            }
        } else {
            mBreadcrumbs.removeBreadcrumb();
            final FileDirItem item = mBreadcrumbs.getLastItem();
            openPath(item.getPath());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initRootFileManager();
            } else {
                Utils.showToast(getApplicationContext(), R.string.no_permissions);
                finish();
            }
        }
    }

    @Override
    public void itemClicked(FileDirItem item) {
        openPath(item.getPath());
    }

    @Override
    public void breadcrumbClicked(int id) {
        final FileDirItem item = (FileDirItem) mBreadcrumbs.getChildAt(id).getTag();
        openPath(item.getPath());
    }
}
