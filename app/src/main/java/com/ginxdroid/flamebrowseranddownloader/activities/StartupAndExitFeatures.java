package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Toast;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;

public class StartupAndExitFeatures extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup_and_exit_features);

        final DatabaseHandler db = DatabaseHandler.getInstance(StartupAndExitFeatures.this);

        final ImageButton backIB = findViewById(R.id.backIB);
        backIB.setOnClickListener(view -> finish());

        final RadioButton doNotOpenRB, directlyOpenRB;
        doNotOpenRB = findViewById(R.id.doNotOpenRB);
        directlyOpenRB = findViewById(R.id.directlyOpenRB);

        switch (db.isSaveRecentTabs())
        {
            case 0:
                doNotOpenRB.setChecked(true);
                break;
            case 1:
                directlyOpenRB.setChecked(true);
                break;
        }

        doNotOpenRB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked)
            {
                db.updateIsSaveRecentTabs(0);
                try {
                    db.truncateRecentSitesTable();
                }catch (Exception ignored) {}
                Toast.makeText(StartupAndExitFeatures.this,
                        StartupAndExitFeatures.this.getString(R.string.settings_saved_will_take_effect_when_you_revisit_tab_manager), Toast.LENGTH_SHORT).show();

            }
        });

        directlyOpenRB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked)
            {
                db.updateIsSaveRecentTabs(1);
                Toast.makeText(StartupAndExitFeatures.this,
                        StartupAndExitFeatures.this.getString(R.string.settings_saved_will_take_effect_when_you_revisit_tab_manager), Toast.LENGTH_SHORT).show();
            }
        });


    }
}