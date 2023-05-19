package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.sheets.LicenseSheet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CreditsActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);

        final ImageButton backIB = findViewById(R.id.backIB);
        backIB.setOnClickListener(CreditsActivity.this);

        final TextView homePageMDI,viewLicenseMDI;
        homePageMDI = findViewById(R.id.homePageMDI);
        viewLicenseMDI = findViewById(R.id.viewLicenseMDI);
        homePageMDI.setOnClickListener(CreditsActivity.this);
        viewLicenseMDI.setOnClickListener(CreditsActivity.this);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.backIB)
        {
            finish();
        } else if(id == R.id.homePageMDI)
        {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/google/material-design-icons")));
            } catch (Exception e)
            {
                Toast.makeText(this, getString(R.string.app_not_found_for_opening_privacy_policy), Toast.LENGTH_SHORT).show();
            }
        } else if(id == R.id.viewLicenseMDI)
        {
            viewLicense();
        }
    }

    private void viewLicense()
    {
        new Thread(() -> {
            String licenseText = null;
            try (InputStream stream = CreditsActivity.this.getAssets().open("license.txt"); BufferedReader reader =
                    new BufferedReader(new InputStreamReader(stream)))
            {
                String line;
                final StringBuilder returnString = new StringBuilder();
                while((line = reader.readLine()) != null)
                {
                    returnString.append(line).append(System.lineSeparator());
                }

                licenseText = returnString.toString();
            } catch (Exception e)
            {
                Toast.makeText(CreditsActivity.this, R.string.oops_general_message, Toast.LENGTH_SHORT).show();
            } finally {
                try {
                    final String finalLicenseText = licenseText;
                    CreditsActivity.this.runOnUiThread(() -> {
                        try {
                            LicenseSheet licenseSheet = new LicenseSheet();
                            Bundle bundle = new Bundle();
                            bundle.putString("licenseText",finalLicenseText);
                            licenseSheet.setArguments(bundle);
                            licenseSheet.show(CreditsActivity.this.getSupportFragmentManager(),"licenseSheet");
                        } catch (Exception ignored) {}
                    });
                } catch (Exception e)
                {
                    Toast.makeText(CreditsActivity.this, R.string.oops_general_message, Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
    }
}