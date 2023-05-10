package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.sheets.MainMenuSheet;
import com.google.android.material.button.MaterialButton;

class MenuHelper {

    @SuppressLint("NotifyDataSetChanged")
    static void showMenu(View popupView, MainMenuSheet mainMenuSheet, Context context,
                         @Nullable NormalTabsRVAdapter.ViewHolder viewHolder, DatabaseHandler db,
                         MainActivity mainActivity, CoordinatorLayout recyclerViewContainer, NormalTabsRVAdapter normalTabsRVAdapter,
                         CustomHorizontalManager customHorizontalManager, boolean isMinimized, LayoutInflater inflater)
    {
        final MaterialButton settingsBtn, fullScreenBtn, forwardBtn, exitBtn;
        settingsBtn = popupView.findViewById(R.id.settingsBtn);
        fullScreenBtn = popupView.findViewById(R.id.fullScreenBtn);
        forwardBtn = popupView.findViewById(R.id.forwardBtn);
        exitBtn = popupView.findViewById(R.id.exitBtn);

        View.OnClickListener onClickListener = view -> {
            int id = view.getId();
            if(id == R.id.forwardBtn)
            {
                try {

                    //noinspection ConstantConditions
                    if(viewHolder.isHPCVisible)
                    {
                        if(HelperTextUtility.isNotEmpty(viewHolder.lastURL) && !viewHolder.lastURL.equals("about:blank"))
                        {
                            try {
                                viewHolder.webView.evaluateJavascript("javascript:document.open();document.close();",null);
                                viewHolder.homePageCL.setVisibility(View.INVISIBLE);
                                viewHolder.isHPCVisible = false;
                                viewHolder.webViewContainer.setVisibility(View.VISIBLE);
                                viewHolder.setClearHistory();
                                viewHolder.webView.loadUrl(viewHolder.lastURL);

                                if(!viewHolder.isProgressBarVisible)
                                {
                                    viewHolder.makeProgressBarVisible();
                                }

                                normalTabsRVAdapter.setDecorations(viewHolder.lastURL, viewHolder);
                                viewHolder.lastURL = null;
                                mainMenuSheet.dismiss();
                            } catch (Exception ignored) {}
                        } else {
                            normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_go_forward);
                        }
                    } else {
                        if(viewHolder.webView.canGoForward())
                        {
                            viewHolder.webView.goForward();
                        } else {
                            normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_go_forward);
                        }
                    }
                } catch (Exception ignored) {}
            }else if(id == R.id.settingsBtn)
            {
                mainMenuSheet.dismiss();
                mainActivity.startActivity(new Intent(context, SettingsActivity.class));
            }else if(id == R.id.fullScreenBtn)
            {
                //noinspection ConstantConditions
                if(viewHolder.isHPCVisible)
                {
                    normalTabsRVAdapter.showToastFromMainActivity(R.string.fullscreen_mode_is_only_for_web_pages);
                } else {
                    mainMenuSheet.dismiss();

                    viewHolder.enterFullScreenMode();
                    normalTabsRVAdapter.showToastFromMainActivity(R.string.exit_fullscreen_mode_message);
                }
            } else if(id == R.id.exitBtn)
            {
                mainMenuSheet.dismiss();
                mainActivity.finish();
            }
        };

        settingsBtn.setOnClickListener(onClickListener);
        fullScreenBtn.setOnClickListener(onClickListener);
        forwardBtn.setOnClickListener(onClickListener);
        exitBtn.setOnClickListener(onClickListener);

        if(isMinimized)
        {
            fullScreenBtn.setVisibility(View.INVISIBLE);
            forwardBtn.setVisibility(View.INVISIBLE);
        }

        final RecyclerView menuPagerRV = popupView.findViewById(R.id.menuPagerRV);
        menuPagerRV.setItemViewCacheSize(0);
        MenuPagerRVAdapter adapter = new MenuPagerRVAdapter(context,viewHolder,db,mainActivity,recyclerViewContainer,
                normalTabsRVAdapter,customHorizontalManager,mainMenuSheet,inflater);
        adapter.setHasStableIds(false);

        menuPagerRV.setLayoutManager(new LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false));
        menuPagerRV.setAdapter(adapter);

        RVIndicator indicator = popupView.findViewById(R.id.indicator);
        indicator.attachToRecyclerView(menuPagerRV);

        new PagerSnapHelper().attachToRecyclerView(menuPagerRV);
        adapter.notifyDataSetChanged();

        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);

            if (imm != null)
            {
                imm.hideSoftInputFromWindow(recyclerViewContainer.getWindowToken(),0);
            }
        } catch (Exception ignored) {}

    }

}
