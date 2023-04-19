package com.ginxdroid.flamebrowseranddownloader.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        final DatabaseHandler db = DatabaseHandler.getInstance(BaseActivity.this);
        switch (db.getCurrentThemeType())
        {
            case 0:
            case 1:
                //light or dark
                setCurrentTheme(db);
                break;
            case 2:
                //auto or battery saver

                //checking battery saver
                PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if(powerManager.isPowerSaveMode())
                {
                    //ON enable dark theme
                    db.updateIsDarkWebUI(1);
                    db.updateCurrentThemeID(2);
                    setTheme(R.style.DarkAppTheme);
                } else {
                    //OFF check for api level for using auto mode i.e. follow system mode
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    {
                        //check dark theme
                        switch (getApplicationContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        {
                            case Configuration.UI_MODE_NIGHT_NO:
                            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                                //set light theme
                                db.updateIsDarkWebUI(0);
                                db.updateCurrentThemeID(1);
                                setTheme(R.style.AppTheme);
                                break;
                            case Configuration.UI_MODE_NIGHT_YES:
                                //set dark theme
                                db.updateIsDarkWebUI(1);
                                db.updateCurrentThemeID(2);
                                setTheme(R.style.DarkAppTheme);
                                break;
                        }
                    } else {
                        //Lower version enable light theme
                        db.updateIsDarkWebUI(0);
                        db.updateCurrentThemeID(1);
                        setTheme(R.style.AppTheme);
                    }
                }
                break;
        }

        super.onCreate(savedInstanceState);
    }

    private void setCurrentTheme(DatabaseHandler db)
    {
        switch (db.getCurrentThemeId())
        {
            case 1:
                setTheme(R.style.AppTheme);
                break;
            case 2:
                setTheme(R.style.DarkAppTheme);
                break;
            case 3:
                setTheme(R.style.LightBlueTheme);
                break;
            case 4:
                setTheme(R.style.LightOrangeTheme);
                break;
            case 5:
                setTheme(R.style.DarkYellowTheme);
                break;
            case 6:
                setTheme(R.style.DarkLightBlueTheme);
                break;
            case 7:
                setTheme(R.style.DarkPinkTheme);
                break;
            case 8:
                setTheme(R.style.DarkOrangeTheme);
                break;
            case 9:
                setTheme(R.style.LightPinkTheme);
                break;
            case 10:
                setTheme(R.style.LightLightBlueTheme);
                break;
            case 11:
                setTheme(R.style.LightTropicalRainForestTheme);
                break;
            case 12:
                setTheme(R.style.LightPurpleTheme);
                break;
            case 13:
                setTheme(R.style.DarkPurpleTheme);
                break;
        }
    }
}
