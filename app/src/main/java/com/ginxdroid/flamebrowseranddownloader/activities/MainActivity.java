package com.ginxdroid.flamebrowseranddownloader.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.models.QuickLinkModel;
import com.ginxdroid.flamebrowseranddownloader.models.UserPreferences;
import com.ginxdroid.flamebrowseranddownloader.sheets.MainMenuSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.TextScalingSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.ThemesSheet;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends BaseActivity implements ThemesSheet.BottomSheetListener, View.OnClickListener,
    MainMenuSheet.BottomSheetListener, TextScalingSheet.BottomSheetListener {

    private DatabaseHandler db;
    private Toast toast = null;

    private CoordinatorLayout recyclerViewContainer;
    private RecyclerView normalTabsRV;
    private NormalTabsRVAdapter normalTabsRVAdapter;
    private CustomHorizontalManager customHorizontalManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerViewContainer = findViewById(R.id.recyclerViewContainer);

        try {
            db = DatabaseHandler.getInstance(MainActivity.this);
        }finally {
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
            initCommon();
        }
    }

    private void firstInitialization()
    {
        try{
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
            db.addUserPreferences(userPreferences);

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
            initCommon();
        }
    }

    private void initCommon()
    {


        final BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);

        final FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(MainActivity.this);

        ImageButton themesIB = bottomAppBar.findViewById(R.id.themesIB);
        themesIB.setOnClickListener(MainActivity.this);
        final ImageButton mainMenuIB = bottomAppBar.findViewById(R.id.mainMenuIB);
        mainMenuIB.setOnClickListener(MainActivity.this);

        normalTabsRV = findViewById(R.id.normalTabsRV);
        customHorizontalManager = new CustomHorizontalManager(recyclerViewContainer,MainActivity.this,bottomAppBar,fabAdd);
        customHorizontalManager.setItemPrefetchEnabled(false);

        normalTabsRV.setLayoutManager(customHorizontalManager);

        normalTabsRVAdapter = new NormalTabsRVAdapter(MainActivity.this,MainActivity.this,customHorizontalManager,
                recyclerViewContainer,normalTabsRV, bottomAppBar);
        normalTabsRVAdapter.setHasStableIds(false);

        normalTabsRV.setAdapter(normalTabsRVAdapter);

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
                    db.updateIsDarkWebUI(1);
                    db.changeTheme(1);
                    db.updateCurrentThemeID(2);
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    MainActivity.this.recreate();
                } else if (id == R.id.mcvDay)
                {
                    sheet.dismiss();
                    db.updateIsDarkWebUI(0);
                    db.changeTheme(0);
                    db.updateCurrentThemeID(1);
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    MainActivity.this.recreate();
                } else if(id == R.id.mcvFollowSystem)
                {
                    sheet.dismiss();
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
                db.updateIsDarkWebUI(0);
                MainActivity.this.recreate();
            });

            yesBtn.setOnClickListener(view12 -> {
                dialog.dismiss();
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
                db.updateIsDarkWebUI(1);
                MainActivity.this.recreate();
            });

            yesBtn.setOnClickListener(view12 -> {
                dialog.dismiss();
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
}