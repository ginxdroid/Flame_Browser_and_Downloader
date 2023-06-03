package com.ginxdroid.flamebrowseranddownloader.activities;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebStorage;
import android.webkit.WebViewDatabase;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.models.HomePageItem;
import com.ginxdroid.flamebrowseranddownloader.models.QuickLinkModel;
import com.ginxdroid.flamebrowseranddownloader.models.SiteSettingsModel;
import com.ginxdroid.flamebrowseranddownloader.models.UserPreferences;
import com.ginxdroid.flamebrowseranddownloader.sheets.AddNewDTaskSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.ClearRecordsSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.ConnectionInformationSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.EditQLNameSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.FileChooserSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.IncognitoInformationSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.MainMenuSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.PopupBlockedSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.QRContentShowSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.RelaunchSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.TextScalingSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.ThemesSheet;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.net.InternetDomainName;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class MainActivity extends BaseActivity implements ThemesSheet.BottomSheetListener, View.OnClickListener,
    MainMenuSheet.BottomSheetListener, TextScalingSheet.BottomSheetListener, EditQLNameSheet.BottomSheetListener,
        RelaunchSheet.BottomSheetListener, FileChooserSheet.BottomSheetListener, PopupBlockedSheet.BottomSheetListener,
    QRContentShowSheet.BottomSheetListener, ConnectionInformationSheet.BottomSheetListener, AddNewDTaskSheet.BottomSheetListener {

    private DatabaseHandler db;
    private Toast toast = null;

    private CoordinatorLayout recyclerViewContainer;
    private RecyclerView normalTabsRV;
    private NormalTabsRVAdapter normalTabsRVAdapter;
    private CustomHorizontalManager customHorizontalManager;
    private boolean recreating = false;
    private boolean incognitoMode = false;

    private RelativeLayout customViewContainer;


    final ActivityResultLauncher<Intent> voiceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    try {
                        if(result.getResultCode() == Activity.RESULT_OK)
                        {
                            Intent data = result.getData();
                            if(data != null)
                            {
                                ArrayList<String> strings = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                                if(strings != null)
                                {
                                    String keyword = strings.get(0);
                                    normalTabsRVAdapter.loadVoiceSearchQuery(keyword);
                                }
                            }
                        }
                    } catch (Exception e)
                    {
                        showToast(R.string.oops_general_message);
                    }
                }
            });

    final ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Boolean fineLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION,false);

                    if (fineLocationGranted != null && fineLocationGranted) {
                        // Precise location access granted.
                        showToast(R.string.permission_granted);
                        normalTabsRVAdapter.invokeGeolocationCallback(true);
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        // Only approximate location access granted.
                        showToast(R.string.permission_granted);

                        normalTabsRVAdapter.invokeGeolocationCallback(true);
                    } else {
                        // No location access granted.
                        if(!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) ||
                                !ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION))
                        {
                            // User selected the Never Ask Again Option
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                            View view = MainActivity.this.getLayoutInflater().inflate(R.layout.popup_now_goto_settings,
                                    recyclerViewContainer,false);

                            TextView goWhereAndWhatTV = view.findViewById(R.id.goWhereAndWhatTV);
                            goWhereAndWhatTV.setText(R.string.goto_settings_location);

                            builder.setView(view);
                            final android.app.AlertDialog dialog = builder.create();

                            MaterialButton nowGotoSettingsBtn,closeGotoSettingsDialogBtn;
                            nowGotoSettingsBtn = view.findViewById(R.id.nowGotoSettingsBtn);
                            closeGotoSettingsDialogBtn = view.findViewById(R.id.closeGotoSettingsDialogBtn);

                            nowGotoSettingsBtn.setOnClickListener(view1 -> {
                                dialog.dismiss();
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.setData(Uri.parse("package:"+MainActivity.this.getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                startActivity(intent);
                            });

                            closeGotoSettingsDialogBtn.setOnClickListener(view12 -> dialog.dismiss());

                            dialog.setCancelable(true);
                            dialog.setCanceledOnTouchOutside(true);
                            dialog.show();
                        } else {
                            showToast(R.string.permission_denied);
                        }

                        normalTabsRVAdapter.invokeGeolocationCallback(false);
                    }
                }
            });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            final int VOICE_RECORD_REQUEST_PERMISSION_CODE = 2;
            final int WEB_REQUEST_RECORD_AUDIO = 11;
            switch (requestCode)
            {
                case VOICE_RECORD_REQUEST_PERMISSION_CODE:
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED)
                    {
                        try {
                            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,"en-US");
                            voiceLauncher.launch(intent);
                        } catch (ActivityNotFoundException e)
                        {
                            showToast(R.string.activity_for_handling_voice_search_is_not_found);
                        } catch (Exception e1)
                        {
                            showToast(R.string.oops_general_message);
                        }
                    } else if(!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.RECORD_AUDIO))
                    {
                        // User selects NEVER ASK AGAIN option

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        View view = normalTabsRVAdapter.getLayoutInflater().inflate(R.layout.popup_now_goto_settings,
                                recyclerViewContainer,false);

                        TextView goWhereAndWhatTV = view.findViewById(R.id.goWhereAndWhatTV);
                        goWhereAndWhatTV.setText(R.string.goto_settings_microphone);

                        builder.setView(view);
                        final AlertDialog dialog = builder.create();

                        MaterialButton nowGotoSettingsBtn,closeGotoSettingsDialogBtn;
                        nowGotoSettingsBtn = view.findViewById(R.id.nowGotoSettingsBtn);
                        closeGotoSettingsDialogBtn = view.findViewById(R.id.closeGotoSettingsDialogBtn);

                        nowGotoSettingsBtn.setOnClickListener(view1 -> {
                            dialog.dismiss();
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.setData(Uri.parse("package:"+MainActivity.this.getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivity(intent);
                        });

                        closeGotoSettingsDialogBtn.setOnClickListener(view12 -> dialog.dismiss());

                        dialog.setCancelable(true);
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.show();

                    } else {
                        showToast(R.string.permission_denied);
                    }
                    break;
                case WEB_REQUEST_RECORD_AUDIO:
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED)
                    {
                        try {
                            normalTabsRVAdapter.getViewHolder().grantWebVoicePermissionRequest();
                        } catch (ActivityNotFoundException e)
                        {
                            showToast(R.string.activity_for_handling_voice_search_is_not_found);
                        } catch (Exception e1)
                        {
                            showToast(R.string.oops_general_message);
                        }
                    } else if(!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.RECORD_AUDIO))
                    {
                        // User selects NEVER ASK AGAIN option

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        View view = normalTabsRVAdapter.getLayoutInflater().inflate(R.layout.popup_now_goto_settings,
                                recyclerViewContainer,false);

                        TextView goWhereAndWhatTV = view.findViewById(R.id.goWhereAndWhatTV);
                        goWhereAndWhatTV.setText(R.string.goto_settings_microphone);

                        builder.setView(view);
                        final AlertDialog dialog = builder.create();

                        MaterialButton nowGotoSettingsBtn,closeGotoSettingsDialogBtn;
                        nowGotoSettingsBtn = view.findViewById(R.id.nowGotoSettingsBtn);
                        closeGotoSettingsDialogBtn = view.findViewById(R.id.closeGotoSettingsDialogBtn);

                        nowGotoSettingsBtn.setOnClickListener(view1 -> {
                            dialog.dismiss();
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.setData(Uri.parse("package:"+MainActivity.this.getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivity(intent);
                        });

                        closeGotoSettingsDialogBtn.setOnClickListener(view12 -> dialog.dismiss());

                        dialog.setCancelable(true);
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.show();

                        normalTabsRVAdapter.getViewHolder().denyWebVoicePermissionRequest();


                    } else {
                        showToast(R.string.permission_denied);

                        normalTabsRVAdapter.getViewHolder().denyWebVoicePermissionRequest();
                    }

                    break;

                }

            } catch (Exception e)
            {
                showToast(R.string.oops_general_message);
            }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try {
            if(customHorizontalManager != null && normalTabsRVAdapter != null)
            {
                try {
                    setIntent(intent);
                } catch (Exception e)
                {
                    showToast(R.string.oops_general_message);
                } finally {
                    try {
                        Bundle bundle = intent.getExtras();
                        if(bundle != null)
                        {
                            final String urlString = getTabOpenerLink();
                            if(urlString.equals("0"))
                            {
                                showToast(R.string.malformed_url);
                            } else if(!urlString.equals("1"))
                            {
                                NormalTabsRVAdapter.ViewHolder viewHolder = normalTabsRVAdapter.getViewHolder();

                                if(viewHolder == null)
                                {
                                    normalTabsRVAdapter.addNewTab(checkAndGet(urlString),4);
                                } else {
                                    if(customViewContainer.getVisibility() == View.VISIBLE)
                                    {
                                        normalTabsRVAdapter.set();
                                        normalTabsRVAdapter.addNewTab(checkAndGet(urlString),8);
                                        showToast(R.string.opened_in_background);
                                    } else {
                                        normalTabsRVAdapter.addNewTab(checkAndGet(urlString),4);
                                        viewHolder.veryCommonAddWork();
                                    }
                                }
                            }
                        }
                    } catch (Exception e)
                    {
                        showToast(R.string.oops_general_message);
                    }
                }
            }
        } catch (Exception f)
        {
            showToast(R.string.oops_general_message);
        }
    }

    private String getTabOpenerLink()
    {
        try {
            Intent intent = getIntent();
            String receivedAction = intent.getAction();

            if(!TextUtils.isEmpty(receivedAction))
            {
                switch (receivedAction)
                {
                    case Intent.ACTION_SEND:
                    {
                        String type = intent.getType();
                        if(type != null)
                        {
                            if("text/plain".equals(type))
                            {
                                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                                if(!TextUtils.isEmpty(sharedText))
                                {
                                    return sharedText;
                                } else {
                                    return "0";
                                }
                            } else {
                                return "0";
                            }
                        } else {
                            return "0";
                        }
                    }
                    case Intent.ACTION_WEB_SEARCH:
                    {
                        String sharedText = intent.getStringExtra(SearchManager.QUERY);
                        if(!TextUtils.isEmpty(sharedText))
                        {
                            return sharedText;
                        } else {
                            return "0";
                        }
                    }
                    case Intent.ACTION_VIEW:
                    {
                        Uri uri = intent.getData();
                        if(uri != null)
                        {
                            return uri.toString();
                        } else {
                            return "0";
                        }
                    }
                    default:
                    {
                        return "1";
                    }
                }
            } else {
                return "1";
            }
        } catch (Exception e)
        {
            return "1";
        }
    }

    private String checkAndGet(String value)
    {
        try {
            new URL(value);

            if(normalTabsRVAdapter.isNetworkUrl(value))
            {
                return value;
            }
            else {
                return normalTabsRVAdapter.getSearchEngineURL() + value;
            }
        } catch (Exception e)
        {
            try {
                @SuppressWarnings("UnstableApiUsage") InternetDomainName internetDomainName = InternetDomainName.from(value);
                //noinspection UnstableApiUsage
                if(internetDomainName.hasPublicSuffix() && internetDomainName.hasParent())
                {
                    return URLUtil.guessUrl(value);
                } else //noinspection UnstableApiUsage
                    if(internetDomainName.isTopPrivateDomain())
                {
                    return URLUtil.guessUrl(value);
                } else {
                    return normalTabsRVAdapter.getSearchEngineURL() + value;
                }
            } catch (Exception f)
            {
                return normalTabsRVAdapter.getSearchEngineURL() + value;
            }
        }
    }

    void addNow()
    {
        final String urlString = getTabOpenerLink();
        switch (urlString)
        {
            case "0":
            {
                showToast(R.string.malformed_url);
                normalCheckAndProceed();
                break;
            }
            case "1":
            {
                normalCheckAndProceed();
                break;
            }
            default:
            {
                try {
                    normalTabsRVAdapter.addNewTab(checkAndGet(urlString),4);
                    try {
                        db.truncateRecentSitesTable();
                    } catch (Exception ignored) {}
                } catch (Exception e)
                {
                     try {
                        db.truncateRecentSitesTable();
                    } catch (Exception ignored) {}
                }
                break;
            }
        }
    }

    private void normalCheckAndProceed()
    {
        try {
            if(db.isSaveRecentTabs() == 1)
            {
                ArrayList<String> recentURLs = db.getAllRecentSitesURLs();
                int size = recentURLs.size();

                if(size > 0)
                {
                    normalTabsRVAdapter.setURLS(recentURLs);
                    try {
                        db.truncateRecentSitesTable();
                    } catch (Exception ignored) {}
                }
                } else
                {
                    normalTabsRVAdapter.addNewTab(db.getHomePageURL(),4);
                }
        } catch (Exception e)
        {
            normalTabsRVAdapter.addNewTab(db.getHomePageURL(),4);

            try{
                db.truncateRecentSitesTable();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        try {
            outState.putString("incognito_state",String.valueOf(incognitoMode));
            outState.putStringArrayList("list_state",normalTabsRVAdapter.getCurrentURLs());
        }catch (Exception ignored){}
        finally {
            super.onSaveInstanceState(outState);
        }
    }

    public void setRecreating()
    {
        this.recreating = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Handle the splash screen transition
        SplashScreen.installSplashScreen(this);

        setContentView(R.layout.activity_main);
        db = DatabaseHandler.getInstance(MainActivity.this);

        recyclerViewContainer = findViewById(R.id.recyclerViewContainer);

        try {
            if(savedInstanceState != null)
            {
                initCommon(true,savedInstanceState);
                recreating = false;
            } else {
                set();
            }

        }catch (Exception e){
            set();
        }
    }

    private void set()
    {
        int count = db.getUserPreferencesCount();

        if(count == 0)
        {
            firstInitialization();
        }else {
            initCommon(false,null);
        }
    }

    final ActivityResultLauncher<Intent> selectDownloadPathLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    try {
                        if(result.getResultCode() == Activity.RESULT_OK)
                        {
                            Intent data = result.getData();
                            if(data != null)
                            {
                                Uri treeUri = data.getData();
                                if(treeUri != null)
                                {
                                    db.updateDownloadAddress(treeUri.toString());
                                    getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    normalTabsRVAdapter.dismissChooseDownloadTaskADAndProceed();
                                }
                            }
                        }
                    } catch (Exception e)
                    {
                        showToast(R.string.please_set_download_directory_again);
                    }
                }
            });

    private void firstInitialization()
    {
        try{
            SiteSettingsModel siteSettingsModel = new SiteSettingsModel();
            siteSettingsModel.setSsId(1);
            siteSettingsModel.setSsLocation(1);
            siteSettingsModel.setSsCookies(1);
            siteSettingsModel.setSsJavaScript(1);
            siteSettingsModel.setSsSaveSitesInHistory(1);
            siteSettingsModel.setSsSaveSearchHistory(1);
            siteSettingsModel.setSsIsChanged(0);
            db.addSiteSettings(siteSettingsModel);

            UserPreferences userPreferences = new UserPreferences();
            userPreferences.setUpKeyId(1);
            userPreferences.setDarkTheme(2);

            PowerManager powerManager = (PowerManager) MainActivity.this.getSystemService(Context.POWER_SERVICE);
            if(powerManager.isPowerSaveMode())
            {
                //ON enable dark theme
                userPreferences.setCurrentThemeID(2);
                userPreferences.setIsDarkWebUI(1);
            }else {
                //OFF check for dark theme system settings
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    //check for dark theme
                    switch (getApplicationContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    {
                        case Configuration.UI_MODE_NIGHT_NO:
                        case Configuration.UI_MODE_NIGHT_UNDEFINED:
                            // Set light theme for our app
                            userPreferences.setCurrentThemeID(1);
                            userPreferences.setIsDarkWebUI(0);
                            break;
                        case Configuration.UI_MODE_NIGHT_YES:
                            // Set dark theme for our app
                            userPreferences.setCurrentThemeID(2);
                            userPreferences.setIsDarkWebUI(1);
                            break;
                    }
                } else {
                    //Lower version enable light theme for our app
                    userPreferences.setCurrentThemeID(1);
                    userPreferences.setIsDarkWebUI(0);
                }
            }


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

            HomePageItem homePageItem = new HomePageItem();
            homePageItem.setHpURL("NewTab");
            homePageItem.setHpTitle("Flame Browser Default");
            homePageItem.setHpFaviconPath("R.drawable");
            db.addHomePageItem(homePageItem);

            //add default quick link items after table is created.
            {
                QuickLinkModel quickLinkModel = new QuickLinkModel();
                quickLinkModel.setQlURL("https://www.google.com/");
                quickLinkModel.setQlTitle("Google");
                quickLinkModel.setQlFaviconPath("R.drawable");
                quickLinkModel.setQlVisiblePosition(1);

                db.addQuickLinkItem(quickLinkModel);

            }

            {
                QuickLinkModel quickLinkModel = new QuickLinkModel();
                quickLinkModel.setQlURL("https://twitter.com/login?lang=en");
                quickLinkModel.setQlTitle("Twitter");
                quickLinkModel.setQlFaviconPath("R.drawable");
                quickLinkModel.setQlVisiblePosition(2);

                db.addQuickLinkItem(quickLinkModel);

            }

            {
                QuickLinkModel quickLinkModel = new QuickLinkModel();
                quickLinkModel.setQlURL("https://www.instagram.com/accounts/login/?hl=en");
                quickLinkModel.setQlTitle("Instagram");
                quickLinkModel.setQlFaviconPath("R.drawable");
                quickLinkModel.setQlVisiblePosition(3);

                db.addQuickLinkItem(quickLinkModel);

            }

            {
                QuickLinkModel quickLinkModel = new QuickLinkModel();
                quickLinkModel.setQlURL("https://www.facebook.com/");
                quickLinkModel.setQlTitle("Facebook");
                quickLinkModel.setQlFaviconPath("R.drawable");
                quickLinkModel.setQlVisiblePosition(4);

                db.addQuickLinkItem(quickLinkModel);
            }

            {
                QuickLinkModel quickLinkModel = new QuickLinkModel();
                quickLinkModel.setQlURL("https://www.wikipedia.org/");
                quickLinkModel.setQlTitle("Wikipedia");
                quickLinkModel.setQlFaviconPath("R.drawable");
                quickLinkModel.setQlVisiblePosition(5);

                db.addQuickLinkItem(quickLinkModel);
            }

        }finally {
            initCommon(false,null);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        NormalTabsRVAdapter.ViewHolder viewHolder;
        try {
            viewHolder = normalTabsRVAdapter.getViewHolder();
        } catch (Exception e)
        {viewHolder = null;}

        try {
            if(viewHolder != null)
            {
                viewHolder.webView.onResume();
            }
        } catch (Exception ignored) {}

        try {
            normalTabsRVAdapter.setSearchFaviconOnResume();
        } catch (Exception ignored) {}

        showRestartPopup();
    }

    void showRestartPopup()
    {
        try {
            if(db.getIsSiteSettingsChanged() == 1)
            {
                db.updateSiteSettingsChanged(0);
                new RelaunchSheet().show(MainActivity.this.getSupportFragmentManager(),"relaunchSheet");
            }
        }catch (Exception ignored) {}
    }

    private void initCommon(boolean isNightModeChanged, Bundle savedInstanceState)
    {
        customViewContainer = recyclerViewContainer.findViewById(R.id.customViewContainer);

        final BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);

        final FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(MainActivity.this);


        ImageButton clearRecordsIB = bottomAppBar.findViewById(R.id.clearRecordsIB);
        clearRecordsIB.setOnClickListener(MainActivity.this);
        ImageButton themesIB = bottomAppBar.findViewById(R.id.themesIB);
        themesIB.setOnClickListener(MainActivity.this);
        final ImageButton mainMenuIB = bottomAppBar.findViewById(R.id.mainMenuIB);
        mainMenuIB.setOnClickListener(MainActivity.this);

        final ImageButton incognitoIB = bottomAppBar.findViewById(R.id.incognitoIB);
        incognitoIB.setOnClickListener(view -> {
            if(incognitoMode)
            {
                //switch back to normal mode
                incognitoMode = false;
                normalTabsRVAdapter.setIncognitoMode(false);
                incognitoIB.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,R.drawable.incognito_connectivity));

                //delete metadata
                new IncognitoClearRecords().start();
                showToast(R.string.incognito_mode_off_logged_out_from_all_sites_cleared_cookies);
            } else {
                //switch to incognito mode
                incognitoMode = true;
                normalTabsRVAdapter.setIncognitoMode(true);
                incognitoIB.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,R.drawable.incognito_connectivity_selected));

                showIncognitoInfo();
            }
        });


        normalTabsRV = findViewById(R.id.normalTabsRV);
        customHorizontalManager = new CustomHorizontalManager(recyclerViewContainer,MainActivity.this,bottomAppBar,fabAdd);
        customHorizontalManager.setItemPrefetchEnabled(false);

        normalTabsRV.setLayoutManager(customHorizontalManager);

        normalTabsRVAdapter = new NormalTabsRVAdapter(MainActivity.this,MainActivity.this,customHorizontalManager,
                recyclerViewContainer,normalTabsRV, bottomAppBar,isNightModeChanged,savedInstanceState,customViewContainer);
        normalTabsRVAdapter.setHasStableIds(false);

        normalTabsRV.setAdapter(normalTabsRVAdapter);

        try {
            if(isNightModeChanged)
            {
                if(savedInstanceState != null)
                {
                    final String incognitoString = savedInstanceState.getString("incognito_state");
                    if(HelperTextUtility.isNotEmpty(incognitoString))
                    {
                        if(incognitoString.equals("true"))
                        {
                            //switch to incognito mode
                            incognitoMode = true;
                            normalTabsRVAdapter.setIncognitoMode(true);
                            incognitoIB.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,R.drawable.incognito_connectivity_selected));
                            showIncognitoInfo();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

    }

    private void showIncognitoInfo()
    {
        try {
            new IncognitoInformationSheet().show(MainActivity.this.getSupportFragmentManager(),"incognitoInformationSheet");
        } catch (Exception ignored) {}
    }

    @Override
    public void showFileChooser(View popupView, FileChooserSheet fileChooserSheet) {
        try {
            final MaterialButton chooseFileButton = popupView.findViewById(R.id.chooseFileButton);
            chooseFileButton.setOnClickListener(view -> {
                normalTabsRVAdapter.fileChooserOpened = true;
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setTypeAndNormalize("*/*");
                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
                selectFileLauncher.launch(contentSelectionIntent);
                fileChooserSheet.dismiss();
            });
        } catch (Exception ignored) {}
    }


    final ActivityResultLauncher<ScanOptions> qrLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    showToast(R.string.unable_to_find_contents);
                } else {
                    //We will show QRContentShowSheet
                    try {
                        QRContentShowSheet sheet = new QRContentShowSheet();
                        Bundle bundle = new Bundle();
                        bundle.putString("content",result.getContents());
                        sheet.setArguments(bundle);
                        sheet.show(MainActivity.this.getSupportFragmentManager(),"qrContentShowSheet");
                    } catch (Exception e) {
                        showToast(R.string.oops_general_message);
                    }
                }
            });

    @Override
    public void onDismissed() {
        try {
            if(!normalTabsRVAdapter.fileChooserOpened)
            {
                normalTabsRVAdapter.uploadMessage.onReceiveValue(null);
                normalTabsRVAdapter.uploadMessage = null;
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onShowPopupBlocked(View popupView, PopupBlockedSheet popupBlockedSheet) {
        try {
            normalTabsRVAdapter.getViewHolder().showBlockedDialog(popupView, popupBlockedSheet);
        }catch (Exception ignored) {}
    }

    @Override
    public void onPopupBlockedDismissed() {
        try {
            normalTabsRVAdapter.getViewHolder().popupBlockedDismissed();
        }catch (Exception ignored) {}
    }

    @Override
    public void loadQRData(String content) {
        try {
            normalTabsRVAdapter.loadQRSearchQuery(content);
        }catch (Exception e) {showToast(R.string.oops_general_message);}
    }

    @Override
    public void showConnectionInformationDialog(View popupView) {
        try {
            normalTabsRVAdapter.getViewHolder().showConnectionInformationDialog(popupView);
        } catch (Exception ignored) {}
    }

    @Override
    public void addTaskSheetDismissed() {
        //todo  showNotificationPermissionPrompt();
    }


    private class IncognitoClearRecords extends Thread
    {
        IncognitoClearRecords()
        {}

        private void clearApplicationData()
        {
            File cache = MainActivity.this.getCacheDir();

            String parentFile = cache.getParent();
            if(parentFile != null)
            {
                File appDir = new File(parentFile);
                if(appDir.exists())
                {
                    String[] children = appDir.list();

                    String databasePath = MainActivity.this.getDatabasePath("flameDatabase").getAbsolutePath();

                    if(children != null)
                    {
                        for(String s : children)
                        {
                            if(!s.equals("favicon") && !s.equals("databases"))
                            {
                                File file = new File(appDir,s);
                                if(!file.getAbsolutePath().equals(databasePath))
                                {
                                    deleteDir(file);
                                }
                            }
                        }
                    }
                }
            }

        }

        private void deleteDir(File dir)
        {
            if(dir != null && dir.isDirectory())
            {
                String[] children = dir.list();
                if(children != null)
                {
                    for(String child : children)
                    {
                        if(!child.equals("favicon") && !child.equals("databases"))
                        {
                            deleteDir(new File(dir,child));
                        }
                    }
                }
            }

            if(dir != null)
            {
                //noinspection ResultOfMethodCallIgnored
                dir.delete();
            }
        }

        @Override
        public void run() {
            super.run();

            try {
                try {
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.removeAllCookies(null);
                } catch (Exception ignored) {}

                try {
                    try {
                        clearApplicationData();
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}

                try {

                    File root = new File(MainActivity.this.getFilesDir(),"favicon");
                    if(root.exists())
                    {
                        String[] children = root.list();
                        if(children != null)
                        {
                            for(String s : children)
                            {
                                File file = new File(root,s);
                                if(file.exists())
                                {
                                    final String faviconPath = file.getPath();

                                    if(db.checkNotContainsFaviconInBookmarks(faviconPath) &&
                                            db.checkNotContainsFaviconInQuickLinks(faviconPath) && db.checkNotContainsFaviconInHomePages(faviconPath)
                                            && db.checkNotContainsFaviconInHistory(faviconPath))
                                    {
                                        File faviconFile = new File(faviconPath);
                                        if(faviconFile.exists())
                                        {
                                            //noinspection ResultOfMethodCallIgnored
                                            faviconFile.delete();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                try {

                    WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(MainActivity.this);
                    webViewDatabase.clearHttpAuthUsernamePassword();

                    if(Build.VERSION.SDK_INT < 26)
                    {
                        webViewDatabase.clearFormData();
                    }
                } catch (Exception ignored) {}

                try {
                    WebStorage webStorage = WebStorage.getInstance();
                    webStorage.deleteAllData();
                } catch (Exception ignored) {}

            } catch (Exception ignored){}
        }
    }


    @Override
    public void showThemesSheet(View popupView, ThemesSheet sheet) {
        try {

            final int currentThemeId = db.getCurrentThemeId();

            RecyclerView lightThemesRV = popupView.findViewById(R.id.lightThemesRV);
            final ThemesRVAdapter lightThemesRVAdapter = new ThemesRVAdapter(MainActivity.this,0,
                    MainActivity.this,sheet,recyclerViewContainer,currentThemeId);
            lightThemesRVAdapter.setHasStableIds(false);

            lightThemesRV.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
            lightThemesRV.setAdapter(lightThemesRVAdapter);
            lightThemesRVAdapter.setThemes(new ArrayList<>(Arrays.asList(1, 3, 4, 9, 10, 11, 12)));


            RecyclerView darkThemesRV = popupView.findViewById(R.id.darkThemesRV);
            final ThemesRVAdapter darkThemesRVAdapter = new ThemesRVAdapter(MainActivity.this,1,
                    MainActivity.this,sheet,recyclerViewContainer,currentThemeId);
            darkThemesRVAdapter.setHasStableIds(false);

            darkThemesRV.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
            darkThemesRV.setAdapter(darkThemesRVAdapter);
            darkThemesRVAdapter.setThemes(new ArrayList<>(Arrays.asList(2, 5, 6, 7, 8, 13)));

            final RadioButton yesRB = popupView.findViewById(R.id.yesRB);
            final RadioButton noRB = popupView.findViewById(R.id.noRB);

            if(db.getDarkWebUI() == 1)
            {
                yesRB.setChecked(true);
            }else {
                noRB.setChecked(true);
            }

            yesRB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if(isChecked)
                {
                    sheet.dismiss();
                    if(db.isDarkTheme())
                    {
                        setRecreating();
                        db.updateIsDarkWebUI(1);
                        MainActivity.this.recreate();
                    }else {
                        showDarkThemeChooserPrompt();
                    }
                }
            });

            noRB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if(isChecked)
                {
                    sheet.dismiss();
                    if(db.isDarkTheme())
                    {
                        showLightThemeChooserPrompt();
                    }else {
                        setRecreating();
                        db.updateIsDarkWebUI(0);
                        MainActivity.this.recreate();
                    }
                }
            });

            final MaterialCardView mcvDay, mcvNight, mcvFollowSystem;
            mcvDay = popupView.findViewById(R.id.mcvDay);
            mcvNight = popupView.findViewById(R.id.mcvNight);
            mcvFollowSystem = popupView.findViewById(R.id.mcvFollowSystem);

            final TextView followSystemTV = mcvFollowSystem.findViewById(R.id.followSystemTV);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                followSystemTV.setText(R.string.follow_system_theme_settings);
            } else {
                followSystemTV.setText(R.string.follow_battery_saver_state);
            }

            final int currentThemeType = db.getCurrentThemeType();
            if(currentThemeType == 2){
                mcvFollowSystem.setChecked(true);
            } else {
                if(db.getDarkWebUI() == 1 && currentThemeType == 1)
                {
                    //it is entirely a night mode enabled
                    mcvNight.setChecked(true);
                }else{
                    //day mode
                    mcvDay.setChecked(true);
                }
            }

            View.OnClickListener onClickListener = view -> {
                final int id = view.getId();

                if(id == R.id.mcvNight)
                {
                    sheet.dismiss();
                    setRecreating();
                    db.updateIsDarkWebUI(1);
                    db.changeTheme(1);
                    db.updateCurrentThemeID(2);
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    MainActivity.this.recreate();
                } else if (id == R.id.mcvDay)
                {
                    sheet.dismiss();
                    setRecreating();
                    db.updateIsDarkWebUI(0);
                    db.changeTheme(0);
                    db.updateCurrentThemeID(1);
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    MainActivity.this.recreate();
                } else if(id == R.id.mcvFollowSystem)
                {
                    sheet.dismiss();
                    setRecreating();
                    db.changeTheme(2);
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                    }

                    MainActivity.this.recreate();
                }

            };

            mcvDay.setOnClickListener(onClickListener);
            mcvNight.setOnClickListener(onClickListener);
            mcvFollowSystem.setOnClickListener(onClickListener);




        }catch (Exception e)
        {
            showToast(R.string.oops_general_message);
        }
    }

    private void showLightThemeChooserPrompt()
    {
        try {
            //Show alert dialog
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);

            final View view = MainActivity.this.getLayoutInflater().inflate(R.layout.popup_light_theme_chooser,recyclerViewContainer,false);

            dialogBuilder.setView(view);
            AlertDialog dialog = dialogBuilder.create();

            //Getting views
            MaterialButton noBtn, yesBtn;
            noBtn = view.findViewById(R.id.noBtn);
            yesBtn = view.findViewById(R.id.yesBtn);

            noBtn.setOnClickListener(view1 -> {
                dialog.dismiss();
                setRecreating();
                db.updateIsDarkWebUI(0);
                MainActivity.this.recreate();
            });

            yesBtn.setOnClickListener(view12 -> {
                dialog.dismiss();
                setRecreating();
                db.updateIsDarkWebUI(0);
                db.changeTheme(0);
                db.updateCurrentThemeID(1);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                MainActivity.this.recreate();
            });

            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
        }catch (Exception ignored)
        {}
    }


    private void showDarkThemeChooserPrompt()
    {
        try {
            //Show alert dialog
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);

            final View view = MainActivity.this.getLayoutInflater().inflate(R.layout.popup_dark_theme_chooser,recyclerViewContainer,false);

            dialogBuilder.setView(view);
            AlertDialog dialog = dialogBuilder.create();

            //Getting views
            MaterialButton noBtn, yesBtn;
            noBtn = view.findViewById(R.id.noBtn);
            yesBtn = view.findViewById(R.id.yesBtn);

            noBtn.setOnClickListener(view1 -> {
                dialog.dismiss();
                setRecreating();
                db.updateIsDarkWebUI(1);
                MainActivity.this.recreate();
            });

            yesBtn.setOnClickListener(view12 -> {
                dialog.dismiss();
                setRecreating();
                db.updateIsDarkWebUI(1);
                db.changeTheme(1);
                db.updateCurrentThemeID(2);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                MainActivity.this.recreate();
            });

            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
        }catch (Exception ignored)
        {}
    }

    void showToast(int resID)
    {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(MainActivity.this, resID, Toast.LENGTH_SHORT);
        toast.show();
    }


    @Override
    public void onClick(View view) {
        try {
            int id = view.getId();
            if(id == R.id.themesIB)
            {
                try {
                    new ThemesSheet().show(MainActivity.this.getSupportFragmentManager(),"themesSheet");
                }catch (Exception ignored)
                {}
            } else if(id == R.id.fabAdd)
            {
                normalTabsRVAdapter.addNewTab(db.getHomePageURL(), 4);
            } else if(id == R.id.mainMenuIB)
            {
                //open our menu
                try{
                    new MainMenuSheet().show(MainActivity.this.getSupportFragmentManager(), "mainMenuBottomSheet");
                }catch (Exception ignored){}
            } else if(id == R.id.clearRecordsIB)
            {
                try {
                    new ClearRecordsSheet().show(MainActivity.this.getSupportFragmentManager(),"clearRecordsSheet");
                } catch (Exception ignored) {}
            }

        }catch (Exception ignored){}
    }


    @Override
    public void onShowMenu(View popupView, MainMenuSheet sheet) {
        try{
            NormalTabsRVAdapter.ViewHolder viewHolder = normalTabsRVAdapter.getViewHolder();
            if(viewHolder != null)
            {
                viewHolder.showMoreWP(popupView, sheet);
            } else {
                showMore(popupView, sheet);
            }

        }catch (Exception ignored){}
    }

    private void showMore(View popupView, MainMenuSheet mainMenuSheet)
    {
        try {
            MenuHelper.showMenu(popupView, mainMenuSheet, MainActivity.this, null, db,
                    MainActivity.this,recyclerViewContainer, normalTabsRVAdapter, customHorizontalManager, true,
                    normalTabsRVAdapter.getLayoutInflater());
        } catch (Exception ignored){}
    }

    @Override
    public void showTextScalingPopup(View view) {
        try {
            normalTabsRVAdapter.getViewHolder().holderUtility.showTextScalingPopup(view);
        } catch (Exception ignored) {}
    }

    @Override
    public void changeQLNameSetQLNow(int bindingAdapterPosition) {
        try {
            normalTabsRVAdapter.getViewHolder().quickLinksRVHomePageAdapter.onItemTitleChanged(bindingAdapterPosition);
        } catch (Exception ignored) {}
    }

    @Override
    public void relaunchNow() {
        try {
            recreating = true;
            MainActivity.this.recreate();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onStop() {

        try {
            db.truncateRecentSitesTable();
            if(!incognitoMode && db.isSaveRecentTabs() != 0)
            {
               ArrayList<String> currentURLs = normalTabsRVAdapter.getCurrentURLs();

               if(currentURLs != null && currentURLs.size() > 0)
               {
                   for(String url: currentURLs)
                   {
                       db.addRecentSiteURL(url);
                   }
               }
            }
        } catch (Exception ignored){}

        NormalTabsRVAdapter.ViewHolder viewHolder;
        try {
            viewHolder = normalTabsRVAdapter.getViewHolder();
        } catch (Exception e)
        {
            viewHolder = null;
        }

        try {
            //Pause webview
            if(viewHolder != null)
            {
                viewHolder.webView.onPause();
            }
        } catch (Exception ignored) {}


        super.onStop();

    }

    @Override
    protected void onDestroy() {
        try {

            try {
                NormalTabsRVAdapter.ViewHolder viewHolder = normalTabsRVAdapter.getViewHolder();
                if(viewHolder != null)
                {
                    MainActivity.this.runOnUiThread(() -> {
                        viewHolder.webView.onPause();
                        viewHolder.webView.stopLoading();

                        viewHolder.webViewContainer.removeView(viewHolder.webView);
                        viewHolder.webView.removeAllViews();
                        viewHolder.webView.destroy();
                    });
                }
            } catch (Exception ignored) {}

        }finally {
            try {
                if(!recreating)
                {
                    if(incognitoMode)
                    {
                        new IncognitoClearRecords().start();
                    }
                }
            } catch (Exception ignored){}
            finally {
                super.onDestroy();
            }
        }
    }

    final ActivityResultLauncher<Intent> selectFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    try {
                        if(result.getResultCode() == Activity.RESULT_OK)
                        {
                            Intent data = result.getData();
                            Uri[] results = null;

                            if(data != null)
                            {
                                String dataString = data.getDataString();
                                ClipData clipData = data.getClipData();
                                if(clipData != null)
                                {
                                    results = new Uri[clipData.getItemCount()];
                                    for(int i = 0;i<clipData.getItemCount();i++)
                                    {
                                        ClipData.Item item = clipData.getItemAt(i);
                                        results[i] = item.getUri();
                                    }

                                }

                                if(dataString != null)
                                {
                                    results = new Uri[]{Uri.parse(dataString)};
                                }

                                normalTabsRVAdapter.setUploadMessage(results);

                            } else {
                                normalTabsRVAdapter.nullifyFileChooserUpload();
                            }
                        }
                    } catch (Exception e)
                    {
                        showToast(R.string.oops_general_message);
                    }
                }
            });

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        reEnter();
        super.onConfigurationChanged(newConfig);
    }

    private ViewTreeObserver.OnGlobalLayoutListener listener = null;
    private void reEnter()
    {
        try {
            if(customHorizontalManager != null && normalTabsRVAdapter != null && customViewContainer != null)
            {
                final int oldWidth = recyclerViewContainer.getWidth();
                final int oldHeight = recyclerViewContainer.getHeight();
                final int customViewContainerVisibility = customViewContainer.getVisibility();

                if(listener != null)
                {
                    recyclerViewContainer.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
                    listener = null;
                }

                listener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final int recyclerViewContainerWidth = recyclerViewContainer.getWidth();
                        final int recyclerViewContainerHeight = recyclerViewContainer.getHeight();

                        if(recyclerViewContainerWidth != oldWidth || recyclerViewContainerHeight != oldHeight)
                        {
                            if(customViewContainerVisibility != View.VISIBLE)
                            {
                                customHorizontalManager.setRecyclerViewContainerHeight();
                                normalTabsRVAdapter.setSpecs(recyclerViewContainerWidth,recyclerViewContainerHeight);
                                NormalTabsRVAdapter.ViewHolder viewHolder = normalTabsRVAdapter.getViewHolder();
                                if(viewHolder != null)
                                {
                                    viewHolder.setQL();
                                    customHorizontalManager.setLayDownType(5);
                                } else {
                                    customHorizontalManager.setLayDownType(3);
                                }
                                normalTabsRV.requestLayout();
                            } else {
                                try {
                                    if(normalTabsRVAdapter.mCustomView != null)
                                    {
                                        final Window window = MainActivity.this.getWindow();
                                        WindowCompat.setDecorFitsSystemWindows(window,false);
                                        WindowInsetsControllerCompat windowInsetsControllerCompat = new WindowInsetsControllerCompat(window,recyclerViewContainer);
                                        windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars());
                                        windowInsetsControllerCompat.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                                        customViewContainer.setKeepScreenOn(true);
                                    }
                                } catch (Exception ignored) {}
                            }

                            recyclerViewContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            listener = null;
                        }
                    }
                };

                recyclerViewContainer.getViewTreeObserver().addOnGlobalLayoutListener(listener);
            }
        } catch (Exception ignored) {}
    }
}