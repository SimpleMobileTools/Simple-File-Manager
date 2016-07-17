package com.simplemobiletools.filemanager.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity implements ItemsFragment.ItemInteractionListener, Breadcrumbs.BreadcrumbsListener {
    @BindView(R.id.breadcrumbs) Breadcrumbs mBreadcrumbs;

    private static final int STORAGE_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mBreadcrumbs.setListener(this);
        tryInitFileManager();
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
    }

    private void openPath(String path) {
        final Bundle bundle = new Bundle();
        bundle.putString(Constants.PATH, path);

        final ItemsFragment fragment = new ItemsFragment();
        fragment.setArguments(bundle);
        fragment.setListener(this);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_holder, fragment).addToBackStack(path).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                final Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        final int cnt = getSupportFragmentManager().getBackStackEntryCount();
        if (cnt == 1)
            finish();
        else {
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
        final int children = mBreadcrumbs.getChildCount() - 1;
        final int removeCnt = children - id;
        for (int i = 0; i < removeCnt; i++) {
            getSupportFragmentManager().popBackStack();
            mBreadcrumbs.removeBreadcrumb();
        }
    }
}
