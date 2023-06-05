package com.ginxdroid.flamebrowseranddownloader.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ginxdroid.flamebrowseranddownloader.BuildConfig;
import com.ginxdroid.flamebrowseranddownloader.R;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        final TextView versionCodeTV = findViewById(R.id.versionCodeTV);
        String versionName = BuildConfig.VERSION_NAME;
        versionCodeTV.setText(versionName);

        final ImageButton backIB = findViewById(R.id.backIB);
        backIB.setOnClickListener(view -> finish());

        final LinearLayout legalInformationLL = findViewById(R.id.legalInformationLL);
        legalInformationLL.setOnClickListener(view -> startActivity(new Intent(AboutActivity.this,LegalInformation.class)));
    }
}