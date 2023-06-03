package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.models.SiteSettingsModel;
import com.ginxdroid.flamebrowseranddownloader.sheets.CookiesSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.ResetSiteSettingsSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.SecurityMessageSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SiteSettings extends BaseActivity implements SecurityMessageSheet.BottomSheetListener,
        ResetSiteSettingsSheet.BottomSheetListener {

    private DatabaseHandler db;
    private Toast toast = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_settings);

        db = DatabaseHandler.getInstance(SiteSettings.this);
        final LinearLayout container = findViewById(R.id.container);

        final ImageButton backIB = container.findViewById(R.id.backIB);
        final MaterialButton resetSiteSettingsBtn = container.findViewById(R.id.resetSiteSettingsBtn);
        final ImageButton cookiesInfoIB = container.findViewById(R.id.cookiesInfoIB);
        final RadioButton allowAllCookiesRB = container.findViewById(R.id.allowAllCookiesRB);
        final RadioButton blockThirdPartyCookiesRB = container.findViewById(R.id.blockThirdPartyCookiesRB);
        final RadioButton blockAllCookiesRB = container.findViewById(R.id.blockAllCookiesRB);
        final RadioButton askBeforeLocationRB = container.findViewById(R.id.askBeforeLocationRB);
        final RadioButton neverAskBeforeLocationRB = container.findViewById(R.id.neverAskBeforeLocationRB);
        final SwitchMaterial javaScriptSwitch = container.findViewById(R.id.javaScriptSwitch);
        final SwitchMaterial saveSitesSwitch = container.findViewById(R.id.saveSitesSwitch);
        final SwitchMaterial saveSearchHistorySwitch = container.findViewById(R.id.saveSearchHistorySwitch);

        SiteSettingsModel siteSettingsModel = db.getSiteSettings();
        javaScriptSwitch.setChecked(siteSettingsModel.getSsJavaScript() == 1);
        saveSitesSwitch.setChecked(siteSettingsModel.getSsSaveSitesInHistory() == 1);
        saveSearchHistorySwitch.setChecked(siteSettingsModel.getSsSaveSearchHistory() == 1);

        saveSearchHistorySwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked){
                db.updateSaveSearchHistoryStatus(1);
            } else {
                db.updateSaveSearchHistoryStatus(0);
            }

            showToast(R.string.success);
        });

        saveSitesSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked){
                db.updateSaveSitesInHistoryStatus(1);
                showMessageDialog(R.string.from_now_on_will_save, false);
            } else {
                db.updateSaveSitesInHistoryStatus(0);
                showMessageDialog(R.string.from_now_on_will_not_save, false);
            }

        });

        javaScriptSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked){
                db.updateJavaScriptStatus(1);
            } else {
                db.updateJavaScriptStatus(0);
            }

            showMessageDialog(R.string.applied_javascript_settings, false);

        });

        switch (siteSettingsModel.getSsCookies())
        {
            case 0:
                allowAllCookiesRB.setChecked(true);
                break;
            case 1:
                blockThirdPartyCookiesRB.setChecked(true);
                break;
            case 2:
                blockAllCookiesRB.setChecked(true);
                break;
        }

        allowAllCookiesRB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked){
                db.updateCookieStatus(0);
                showMessageDialog(R.string.applied_cookie_settings, false);
            }
        });

        blockThirdPartyCookiesRB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked){
                db.updateCookieStatus(1);
                showMessageDialog(R.string.applied_cookie_settings, false);
            }
        });

        blockAllCookiesRB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked){
                db.updateCookieStatus(2);
                showMessageDialog(R.string.applied_cookie_settings, false);
            }
        });

        if(siteSettingsModel.getSsLocation() == 1)
        {
            askBeforeLocationRB.setChecked(true);
        } else {
            neverAskBeforeLocationRB.setChecked(true);
        }

        askBeforeLocationRB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked){
                db.updateLocationStatus(1);
                showMessageDialog(R.string.applied_location_settings, false);
            }
        });

        neverAskBeforeLocationRB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked){
                db.updateLocationStatus(0);
                showMessageDialog(R.string.applied_location_settings, false);
            }
        });

        View.OnClickListener onClickListener = view -> {
            int id = view.getId();
            if(id == R.id.resetSiteSettingsBtn)
            {
                try {
                    new ResetSiteSettingsSheet().show(SiteSettings.this.getSupportFragmentManager(),"resetSiteSettingsSheet");
                } catch (Exception ignored) {}
            } else if(id == R.id.backIB)
            {
                finish();
            } else if(id == R.id.cookiesInfoIB)
            {
                try {
                    new CookiesSheet().show(SiteSettings.this.getSupportFragmentManager(),"cookiesSheet");
                } catch (Exception ignored) {}
            }
        };
        resetSiteSettingsBtn.setOnClickListener(onClickListener);
        backIB.setOnClickListener(onClickListener);
        cookiesInfoIB.setOnClickListener(onClickListener);

    }

    private void showMessageDialog(int resID, final boolean isRestart)
    {
        db.updateSiteSettingsChanged(1);
        try {
            SecurityMessageSheet securityMessageSheet = new SecurityMessageSheet();
            Bundle bundle = new Bundle();
            bundle.putBoolean("isRestart",isRestart);
            bundle.putInt("resID",resID);
            securityMessageSheet.setArguments(bundle);
            securityMessageSheet.show(SiteSettings.this.getSupportFragmentManager(),"securityMessageSheet");
        } catch (Exception ignored) {}
    }

    private void showToast(int resId)
    {
        if(toast != null)
        {
            toast.cancel();
        }

        toast = Toast.makeText(SiteSettings.this,resId,Toast.LENGTH_SHORT);
        toast.show();

    }

    @Override
    public void messageDismissed(boolean isRestart) {
        try {
            if(isRestart)
            {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
                overridePendingTransition(R.anim.alpha_in,R.anim.alpha_out);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onShowResetSiteSettingsSheet(View popupView, ResetSiteSettingsSheet sheet) {
        try {
            final MaterialButton resetBtn = popupView.findViewById(R.id.resetBtn);
            resetBtn.setOnClickListener(view -> {
                SiteSettingsModel siteSettingsModel = new SiteSettingsModel();
                siteSettingsModel.setSsId(1);
                siteSettingsModel.setSsLocation(1);
                siteSettingsModel.setSsCookies(1);
                siteSettingsModel.setSsJavaScript(1);
                siteSettingsModel.setSsSaveSitesInHistory(1);
                siteSettingsModel.setSsSaveSearchHistory(1);
                db.addSiteSettings(siteSettingsModel);

                showMessageDialog(R.string.restart_flame_browser_only, true);
                sheet.dismiss();
            });

            final MaterialButton cancelBtn = popupView.findViewById(R.id.cancelBtn);
            cancelBtn.setOnClickListener(view -> sheet.dismiss());

        } catch (Exception ignored) {}
    }
}