package com.simplemobiletools.filemanager.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.simplemobiletools.filemanager.Breadcrumbs;
import com.simplemobiletools.filemanager.Config;
import com.simplemobiletools.filemanager.Constants;
import com.simplemobiletools.filemanager.R;
import com.simplemobiletools.filemanager.Utils;
import com.simplemobiletools.filemanager.fragments.ItemsFragment;
import com.simplemobiletools.filemanager.models.FileDirItem;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends SimpleActivity implements ItemsFragment.ItemInteractionListener, Breadcrumbs.BreadcrumbsListener {
    @BindView(R.id.breadcrumbs) Breadcrumbs mBreadcrumbs;

    private static final int STORAGE_PERMISSION = 1;
    private static int mRootFoldersCnt;
    private static boolean mShowFullPath;
    private static Config mConfig;

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
        final String path = Environment.getExternalStorageDirectory().toString();
        openPath(path);
        mBreadcrumbs.setInitialBreadcrumb(path);
        mRootFoldersCnt = mBreadcrumbs.getChildCount();
    }

    private void openPath(String path) {
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
        final int cnt = mBreadcrumbs.getChildCount() - mRootFoldersCnt;
        if (cnt <= 0) {
            finish();
        } else {
            mBreadcrumbs.removeBreadcrumb();
            super.onBackPressed();
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
        mBreadcrumbs.addBreadcrumb(item, true);
    }

    @Override
    public void breadcrumbClicked(int id) {
        final FileDirItem item = (FileDirItem) mBreadcrumbs.getChildAt(id).getTag();
        final String path = item.getPath();
        mBreadcrumbs.setInitialBreadcrumb(path);
        openPath(path);
    }
}
