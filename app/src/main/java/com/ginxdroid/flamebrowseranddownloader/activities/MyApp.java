package com.ginxdroid.flamebrowseranddownloader.activities;

import android.app.Application;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        int currentThemeType = DatabaseHandler.getInstance(this).getCurrentThemeType();

        switch (currentThemeType)
        {
            case 0:
                //day ui
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                //night ui
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 2:
                //follow system
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                }
                break;
        }
    }
}
