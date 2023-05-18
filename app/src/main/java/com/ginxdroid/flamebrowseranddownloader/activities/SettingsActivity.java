package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.appcompat.app.AppCompatActivity;
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

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.google.android.material.button.MaterialButton;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
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

        MaterialButton manageBTV,manageHPTV,manageHTV,manageSHTV;

        manageHPTV = findViewById(R.id.manageHPTV);
        manageHPTV.setOnClickListener(SettingsActivity.this);

        manageBTV = findViewById(R.id.manageBTV);
        manageBTV.setOnClickListener(SettingsActivity.this);

        manageHTV = findViewById(R.id.manageHTV);
        manageHTV.setOnClickListener(SettingsActivity.this);

        manageSHTV = findViewById(R.id.manageSHTV);
        manageSHTV.setOnClickListener(SettingsActivity.this);
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


}