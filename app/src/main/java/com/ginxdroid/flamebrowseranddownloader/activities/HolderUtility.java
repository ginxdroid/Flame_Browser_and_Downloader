package com.ginxdroid.flamebrowseranddownloader.activities;

import android.content.Context;
import android.view.View;
import android.webkit.WebSettings;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

public class HolderUtility {
    private final NormalTabsRVAdapter.ViewHolder viewHolder;
    private final Context context;
    private final MainActivity activity;
    private final CoordinatorLayout recyclerViewContainer;
    private final NormalTabsRVAdapter normalTabsRVAdapter;
    private final DatabaseHandler db;

    public HolderUtility(NormalTabsRVAdapter.ViewHolder viewHolder, Context context, MainActivity activity,
                         CoordinatorLayout recyclerViewContainer, NormalTabsRVAdapter normalTabsRVAdapter, DatabaseHandler db) {
        this.viewHolder = viewHolder;
        this.context = context;
        this.activity = activity;
        this.recyclerViewContainer = recyclerViewContainer;
        this.normalTabsRVAdapter = normalTabsRVAdapter;
        this.db = db;
    }

    void showTextScalingPopup(View popupView)
    {
        try {
            MaterialTextView sliderValueTV = popupView.findViewById(R.id.sliderValueTV);
            Slider scaleSlider = popupView.findViewById(R.id.scaleSlider);
            final WebSettings webSettings = viewHolder.webView.getSettings();
            int currentZoomLevel = webSettings.getTextZoom();
            sliderValueTV.setText(String.valueOf(currentZoomLevel));

            scaleSlider.addOnChangeListener((slider, value, fromUser) -> {
                int finalIntValue = (int) value;
                sliderValueTV.setText(String.valueOf(finalIntValue));
                webSettings.setTextZoom(finalIntValue);
            });
        } catch (Exception e)
        {
            normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
        }
    }
}
