package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.ginxdroid.flamebrowseranddownloader.R;
import com.google.android.material.button.MaterialButton;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton backIB = findViewById(R.id.backIB);
        backIB.setOnClickListener(SettingsActivity.this);

        MaterialButton manageBTV,manageHPTV,manageHTV;

        manageHPTV = findViewById(R.id.manageHPTV);
        manageHPTV.setOnClickListener(SettingsActivity.this);

        manageBTV = findViewById(R.id.manageBTV);
        manageBTV.setOnClickListener(SettingsActivity.this);

        manageHTV = findViewById(R.id.manageHTV);
        manageHTV.setOnClickListener(SettingsActivity.this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if(id == R.id.manageHPTV)
        {
            SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, ManageHomePages.class));
        } else if(id == R.id.backIB)
        {
            SettingsActivity.this.finish();
        } else if(id == R.id.manageBTV)
        {
            SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, ManageBookmarks.class));
        } else if(id == R.id.manageHTV)
        {
            SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, ManageHistory.class));

        }
    }
}