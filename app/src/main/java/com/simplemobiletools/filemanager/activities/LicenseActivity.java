package com.simplemobiletools.filemanager.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.simplemobiletools.filemanager.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class LicenseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.license_butterknife_title)
    public void butterKnifeClicked() {
        openUrl(R.string.butterknife_url);
    }

    private void openUrl(int id) {
        final String url = getResources().getString(id);
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}
