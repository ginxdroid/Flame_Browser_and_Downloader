package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.models.SiteSettingsModel;
import com.ginxdroid.flamebrowseranddownloader.models.UserPreferences;
import com.ginxdroid.flamebrowseranddownloader.sheets.ClearRecordsSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.MessageSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.ResetSettingsSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.TipsSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.ump.UserMessagingPlatform;

public class SettingsActivity extends BaseActivity implements View.OnClickListener, ResetSettingsSheet.BottomSheetListener {
    private DatabaseHandler db;
    private LinearLayout container;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        container = findViewById(R.id.container);

        db = DatabaseHandler.getInstance(SettingsActivity.this);

        final MaterialButton searchEngineTV = findViewById(R.id.searchEngineTV);
        searchEngineTV.setOnClickListener(SettingsActivity.this);

        ImageButton backIB = findViewById(R.id.backIB);
        backIB.setOnClickListener(SettingsActivity.this);

        MaterialButton manageBTV,manageHPTV,manageHTV,manageSHTV,securityTV,tipsTV,clearRecordsTV,
                resetToDefaultTV,shareLinkBtn,aboutBtn,startupAndExitFeaturesTV,downloaderSettingsTV,resetConsentBtn;

        manageHPTV = findViewById(R.id.manageHPTV);
        manageHPTV.setOnClickListener(SettingsActivity.this);

        manageBTV = findViewById(R.id.manageBTV);
        manageBTV.setOnClickListener(SettingsActivity.this);

        manageHTV = findViewById(R.id.manageHTV);
        manageHTV.setOnClickListener(SettingsActivity.this);

        manageSHTV = findViewById(R.id.manageSHTV);
        manageSHTV.setOnClickListener(SettingsActivity.this);

        securityTV = findViewById(R.id.securityTV);
        securityTV.setOnClickListener(SettingsActivity.this);

        tipsTV = findViewById(R.id.tipsTV);
        tipsTV.setOnClickListener(SettingsActivity.this);

        clearRecordsTV = findViewById(R.id.clearRecordsTV);
        clearRecordsTV.setOnClickListener(SettingsActivity.this);

        resetToDefaultTV = findViewById(R.id.resetToDefaultTV);
        resetToDefaultTV.setOnClickListener(SettingsActivity.this);

        shareLinkBtn = findViewById(R.id.shareLinkBtn);
        shareLinkBtn.setOnClickListener(SettingsActivity.this);

        aboutBtn = findViewById(R.id.aboutBtn);
        aboutBtn.setOnClickListener(SettingsActivity.this);

        startupAndExitFeaturesTV = findViewById(R.id.startupAndExitFeaturesTV);
        startupAndExitFeaturesTV.setOnClickListener(SettingsActivity.this);

        downloaderSettingsTV = findViewById(R.id.downloaderSettingsTV);
        downloaderSettingsTV.setOnClickListener(SettingsActivity.this);

        resetConsentBtn = findViewById(R.id.resetConsentBtn);
        resetConsentBtn.setOnClickListener(SettingsActivity.this);
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
        } else if(id == R.id.searchEngineTV)
        {
            showSearchEnginesPopup(view);
        }else if(id == R.id.manageSHTV)
        {
            SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, ManageSearchHistory.class));
        } else if(id == R.id.securityTV)
        {
            SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, SiteSettings.class));
        } else if(id == R.id.tipsTV)
        {
            new TipsSheet().show(SettingsActivity.this.getSupportFragmentManager(),"tipsSheet");
        } else if(id == R.id.clearRecordsTV)
        {
            try {
                new ClearRecordsSheet().show(SettingsActivity.this.getSupportFragmentManager(),"clearRecordsSheet");
            } catch (Exception ignored) {}
        }else if(id == R.id.resetToDefaultTV)
        {
            try {
                new ResetSettingsSheet().show(SettingsActivity.this.getSupportFragmentManager(),"resetSettingsSheet");
            } catch (Exception ignored) {}
        } else if(id == R.id.shareLinkBtn)
        {
            try {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setTypeAndNormalize("text/plain");
                share.putExtra(Intent.EXTRA_TEXT,"https://play.google.com/store/apps/details?id=com.ginxdroid.flamebrowseranddownloader");
                startActivity(Intent.createChooser(share,"Share via"));
            } catch (Exception e){
                Toast.makeText(this, SettingsActivity.this.getString(R.string.no_app_found), Toast.LENGTH_SHORT).show();
            }
        } else if(id == R.id.aboutBtn)
        {
            startActivity(new Intent(SettingsActivity.this,AboutActivity.class));
        } else if(id == R.id.startupAndExitFeaturesTV)
        {
            startActivity(new Intent(SettingsActivity.this, StartupAndExitFeatures.class));
        } else if (id == R.id.downloaderSettingsTV) {
            startActivity(new Intent(SettingsActivity.this, DownloaderSettings.class));
        } else if(id == R.id.resetConsentBtn)
        {
            try {
                UserMessagingPlatform.getConsentInformation(SettingsActivity.this).reset();
                Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
            } catch (Exception e)
            {
                Toast.makeText(this, R.string.oops_something_went_wrong_please_try_again_later, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showSearchEnginesPopup(View anchor)
    {
        try {
            View popupView = SettingsActivity.this.getLayoutInflater().inflate(R.layout.show_more_se, container,false);
            popupView.setTranslationX(popupView.getTranslationX() + 10);
            popupView.setTranslationY(popupView.getTranslationY() + 10);

            int finalWidth = Math.min(container.getWidth(),
                    container.getHeight());

            int eight = SettingsActivity.this.getResources().getDimensionPixelSize(R.dimen.eight);

            final PopupWindow popupWindow = new PopupWindow(popupView,(int)(finalWidth * 0.6), ViewGroup.LayoutParams.WRAP_CONTENT,true);
            popupWindow.setElevation(eight);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setAnimationStyle(R.style.PopupWindowAnimationStyleSmallPopupWindow);

            final RecyclerView searchEnginesRV = popupView.findViewById(R.id.searchEnginesRV);
            final SearchEngineAdapter searchEngineAdapter = new SearchEngineAdapter(null,db,SettingsActivity.this,
                    null,null,SettingsActivity.this.getLayoutInflater());
            searchEnginesRV.setLayoutManager(new LinearLayoutManager(SettingsActivity.this));
            searchEngineAdapter.setHasStableIds(false);
            searchEnginesRV.setAdapter(searchEngineAdapter);
            searchEngineAdapter.setDefaultSearchEngineItems();

            popupWindow.showAsDropDown(anchor,0,0, Gravity.START);

            try {
                InputMethodManager imm = (InputMethodManager) SettingsActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(anchor.getWindowToken(), 0);
                }
            } catch (Exception ignored) {
            }

        }catch (Exception ignored) {
        }
    }


    @Override
    public void onShowResetToDefaultsSheet(View popupView, ResetSettingsSheet sheet) {
        try {
            MaterialButton cancelBtn,resetBtn;
            cancelBtn = popupView.findViewById(R.id.cancelBtn);
            resetBtn = popupView.findViewById(R.id.resetBtn);

            cancelBtn.setOnClickListener(view -> sheet.dismiss());

            resetBtn.setOnClickListener(view -> {
                UserPreferences userPreferences = new UserPreferences();
                userPreferences.setUpKeyId(1);
                userPreferences.setDarkTheme(db.getCurrentThemeType());
                userPreferences.setCurrentThemeID(db.getCurrentThemeId());
                userPreferences.setIsDarkWebUI(db.getDarkWebUI());
                userPreferences.setHomePageURL("NewTab");
                userPreferences.setSearchEngineURL("https://www.google.com/search?q=");
                userPreferences.setIsSaveRecentTabs(1);
                userPreferences.setBrowserTutorialInfo(1);
                userPreferences.setDownloadPath("Not found");
                userPreferences.setAutoResumeStatus(1);
                userPreferences.setSimultaneousTasks(1);
                userPreferences.setDefaultSegments(6);
                userPreferences.setDirectDownload(0);
                userPreferences.setShowOptimization(1);
                db.addUserPreferences(userPreferences);

                SiteSettingsModel siteSettingsModel = new SiteSettingsModel();
                siteSettingsModel.setSsId(1);
                siteSettingsModel.setSsLocation(1);
                siteSettingsModel.setSsCookies(1);
                siteSettingsModel.setSsJavaScript(1);
                siteSettingsModel.setSsSaveSitesInHistory(1);
                siteSettingsModel.setSsSaveSearchHistory(1);
                db.addSiteSettings(siteSettingsModel);

                sheet.dismiss();
                showMessageDialog();


            });

        } catch (Exception ignored){}
    }

    private void showMessageDialog()
    {
        db.updateSiteSettingsChanged(1);
        try{
            MessageSheet messageSheet = new MessageSheet();
            Bundle bundle = new Bundle();
            bundle.putInt("message",R.string.restart_flame_browser_only);
            messageSheet.setArguments(bundle);
            messageSheet.show(SettingsActivity.this
                    .getSupportFragmentManager(), "messageSheet");
        }catch(Exception ignored)
        {}
    }

}