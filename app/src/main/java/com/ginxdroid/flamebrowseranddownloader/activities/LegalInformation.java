package com.ginxdroid.flamebrowseranddownloader.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ginxdroid.flamebrowseranddownloader.R;
import com.google.android.material.button.MaterialButton;

public class LegalInformation extends BaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal_information);

        final MaterialButton privacyPolicyBtn,termsAndConditionsBtn,openSourceLicensesBtn;
        final ImageButton backIB;

        backIB = findViewById(R.id.backIB);
        backIB.setOnClickListener(LegalInformation.this);

        privacyPolicyBtn = findViewById(R.id.privacyPolicyBtn);
        privacyPolicyBtn.setOnClickListener(LegalInformation.this);

        termsAndConditionsBtn = findViewById(R.id.termsAndConditionsBtn);
        termsAndConditionsBtn.setOnClickListener(LegalInformation.this);

        openSourceLicensesBtn = findViewById(R.id.openSourceLicensesBtn);
        openSourceLicensesBtn.setOnClickListener(LegalInformation.this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.backIB)
        {
            finish();
        } else if(id == R.id.privacyPolicyBtn)
        {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://sites.google.com/view/ginxdroidbrowserwithdownloader/privacy-policy")));
            } catch (Exception e)
            {
                Toast.makeText(this, getString(R.string.app_not_found_for_opening_privacy_policy), Toast.LENGTH_SHORT).show();
            }
        } else if(id == R.id.termsAndConditionsBtn)
        {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://sites.google.com/view/ginxdroidbrowserwithdownloader/terms-and-conditions")));
            } catch (Exception e)
            {
                Toast.makeText(this, getString(R.string.app_not_found_for_opening_privacy_policy), Toast.LENGTH_SHORT).show();
            }
        } else if(id == R.id.openSourceLicensesBtn)
        {
            startActivity(new Intent(LegalInformation.this,CreditsActivity.class));
        }

    }
}