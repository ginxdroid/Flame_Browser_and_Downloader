package com.ginxdroid.flamebrowseranddownloader.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.CustomEditText;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.models.SearchItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;
import com.google.common.net.InternetDomainName;

import java.net.URL;

public class HolderUtility {
    final int VOICE_RECORD_REQUEST_PERMISSION_CODE = 2;
    private final NormalTabsRVAdapter.ViewHolder viewHolder;
    private final Context context;
    private final MainActivity activity;
    private final CoordinatorLayout recyclerViewContainer;
    private final NormalTabsRVAdapter normalTabsRVAdapter;
    private final DatabaseHandler db;
    private Runnable workRunnable = null;
    private Handler handler = null;

    public HolderUtility(NormalTabsRVAdapter.ViewHolder viewHolder, Context context, MainActivity activity,
                         CoordinatorLayout recyclerViewContainer, NormalTabsRVAdapter normalTabsRVAdapter, DatabaseHandler db) {
        this.viewHolder = viewHolder;
        this.context = context;
        this.activity = activity;
        this.recyclerViewContainer = recyclerViewContainer;
        this.normalTabsRVAdapter = normalTabsRVAdapter;
        this.db = db;
    }


    void showSearchEnginesPopup(View anchor,ImageView innerFaviconView)
    {
        try {
            View popupView = activity.getLayoutInflater().inflate(R.layout.show_more_se, recyclerViewContainer,false);
            popupView.setTranslationX(popupView.getTranslationX() + 10);
            popupView.setTranslationY(popupView.getTranslationY() + 10);

            int finalWidth = Math.min(normalTabsRVAdapter.getRecyclerViewContainerWidth(),
                    normalTabsRVAdapter.getRecyclerViewContainerHeight());

            int eight = context.getResources().getDimensionPixelSize(R.dimen.eight);

            final PopupWindow popupWindow = new PopupWindow(popupView,(int)(finalWidth * 0.6), ViewGroup.LayoutParams.WRAP_CONTENT,true);
            popupWindow.setElevation(eight);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setAnimationStyle(R.style.PopupWindowAnimationStyleSmallPopupWindow);

            final RecyclerView searchEnginesRV = popupView.findViewById(R.id.searchEnginesRV);
            final SearchEngineAdapter searchEngineAdapter = new SearchEngineAdapter(innerFaviconView,db,context,
                    normalTabsRVAdapter,viewHolder,activity.getLayoutInflater());
            searchEnginesRV.setLayoutManager(new LinearLayoutManager(context));
            searchEngineAdapter.setHasStableIds(false);
            searchEnginesRV.setAdapter(searchEngineAdapter);
            searchEngineAdapter.setDefaultSearchEngineItems();

            popupWindow.showAsDropDown(anchor,0,0, Gravity.START);

            try {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(anchor.getWindowToken(), 0);
                }
            } catch (Exception ignored) {
            }

        }catch (Exception ignored) {
        }
    }

    void openSearchPopup(String url)
    {
        final View layout = normalTabsRVAdapter.getLayoutInflater().inflate(R.layout.popup_search, recyclerViewContainer, false);

        normalTabsRVAdapter.searchDialog = new Dialog(context, R.style.full_screen_dialog);
        normalTabsRVAdapter.searchDialog.setContentView(layout);
        final Window window = normalTabsRVAdapter.searchDialog.getWindow();
        WindowManager.LayoutParams layoutParams;

        if(window != null)
        {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
            layoutParams = window.getAttributes();
            if(layoutParams != null)
            {
                layoutParams.windowAnimations = R.style.NetworkPopupWindowAnimationStyle;
                window.setAttributes(layoutParams);
            }
        }

        final ImageButton editCBIB,loadCBIB,voiceLauncherIB,closeSearchETHP;
        final RelativeLayout searchETRL;
        final ConstraintLayout cbMetaDataRL;
        final TextView clipBoardDataTV;
        final ImageView innerSearchEngineIV;
        final CustomEditText popupSearchETHP;

        cbMetaDataRL = layout.findViewById(R.id.cbMetaDataRL);
        editCBIB = cbMetaDataRL.findViewById(R.id.editCBIB);
        loadCBIB = cbMetaDataRL.findViewById(R.id.loadCBIB);
        clipBoardDataTV = cbMetaDataRL.findViewById(R.id.clipBoardDataTV);

        searchETRL = layout.findViewById(R.id.searchETRL);
        innerSearchEngineIV = searchETRL.findViewById(R.id.innerSearchEngineIV);
        voiceLauncherIB = searchETRL.findViewById(R.id.voiceLauncherIB);
        closeSearchETHP = searchETRL.findViewById(R.id.closeSearchETHP);
        popupSearchETHP = searchETRL.findViewById(R.id.popupSearchETHP);

        innerSearchEngineIV.setImageResource(0);
        innerSearchEngineIV.setImageResource(normalTabsRVAdapter.getSeFavResId());

        popupSearchETHP.requestFocus();

        //RecyclerView
        RecyclerView searchRVHomePage = layout.findViewById(R.id.searchRVHomePage);
        searchRVHomePage.setLayoutManager(new LinearLayoutManager(context));
        SearchRVHomePageAdapter searchRVHomePageAdapter = new SearchRVHomePageAdapter(popupSearchETHP,normalTabsRVAdapter.searchDialog,
                cbMetaDataRL,voiceLauncherIB,closeSearchETHP,db,viewHolder,normalTabsRVAdapter);
        searchRVHomePageAdapter.setHasStableIds(false);
        searchRVHomePage.setAdapter(searchRVHomePageAdapter);
        searchRVHomePageAdapter.setDefaultSearchItems();



        try {
            if(!viewHolder.isHPCVisible && HelperTextUtility.isNotEmpty(url))
            {
                Editable pasteHereET = popupSearchETHP.getText();
                if(pasteHereET != null)
                {
                    pasteHereET.clear();
                }

                popupSearchETHP.append(url);
                popupSearchETHP.selectAll();
                voiceLauncherIB.setVisibility(View.INVISIBLE);
                closeSearchETHP.setVisibility(View.VISIBLE);
            }
        } catch (Exception ignored) {}

        try {
            String clipValue = HelperTextUtility.getClipString(context);
            if(HelperTextUtility.isNotEmpty(clipValue))
            {
                cbMetaDataRL.setVisibility(View.VISIBLE);
                clipBoardDataTV.setText(clipValue.replaceAll(System.lineSeparator()," "));
            } else {
                cbMetaDataRL.setVisibility(View.GONE);
            }
        } catch (Exception e)
        {
            cbMetaDataRL.setVisibility(View.GONE);
        }

        handler = new Handler(Looper.getMainLooper());

        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    try {
                        if(workRunnable != null)
                        {
                            handler.removeCallbacks(workRunnable);
                            workRunnable = null;
                        }
                    } catch (Exception ignored){}

                    Editable editable = popupSearchETHP.getText();

                    if(HelperTextUtility.isNotEmpty(editable))
                    {
                        final String fieldVal = editable.toString();
                        voiceLauncherIB.setVisibility(View.INVISIBLE);
                        closeSearchETHP.setVisibility(View.VISIBLE);

                        workRunnable = () -> doSearch(fieldVal);
                        handler.postDelayed(workRunnable,175);

                        cbMetaDataRL.setVisibility(View.GONE);
                    } else {
                        closeSearchETHP.setVisibility(View.INVISIBLE);
                        voiceLauncherIB.setVisibility(View.VISIBLE);

                        searchRVHomePageAdapter.setDefaultSearchItems();

                        try {
                            String clipValue = HelperTextUtility.getClipString(context);
                            if(HelperTextUtility.isNotEmpty(clipValue))
                            {
                                cbMetaDataRL.setVisibility(View.VISIBLE);
                                clipBoardDataTV.setText(clipValue.replaceAll(System.lineSeparator()," "));
                            } else {
                                cbMetaDataRL.setVisibility(View.GONE);
                            }

                        } catch (Exception e)
                        {
                            cbMetaDataRL.setVisibility(View.GONE);
                        }
                    }
                } catch (Exception ignored)
                {}
            }

            private void doSearch(String fieldVal)
            {
                try {
                    if(cbMetaDataRL.getVisibility() != View.VISIBLE)
                    {
                        searchRVHomePageAdapter.setSearchItems(fieldVal);
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };

        popupSearchETHP.addTextChangedListener(textWatcher);


        View.OnClickListener onClickListener = view -> {
            int id = view.getId();
            if(id == R.id.innerSearchEngineIV)
            {
                showSearchEnginesPopup(view,(ImageView)view);
            } else if(id == R.id.voiceLauncherIB)
            {
                checkAndLaunchVoiceLauncher();
            } else if(id == R.id.closeSearchETHP)
            {
                Editable editable = popupSearchETHP.getText();
                if(editable!= null)
                {
                    editable.clear();
                }

                closeSearchETHP.setVisibility(View.INVISIBLE);
                voiceLauncherIB.setVisibility(View.VISIBLE);
            } else if(id == R.id.editCBIB)
            {
                try {
                    String clipValue = clipBoardDataTV.getText().toString();
                    if(HelperTextUtility.isNotEmpty(clipValue))
                    {
                        popupSearchETHP.setText(clipValue);
                        popupSearchETHP.setSelection(clipValue.length());
                        cbMetaDataRL.setVisibility(View.GONE);
                        voiceLauncherIB.setVisibility(View.INVISIBLE);
                        closeSearchETHP.setVisibility(View.VISIBLE);
                    } else {
                        normalTabsRVAdapter.showToastFromMainActivity(R.string.empty_clipboard);
                    }
                } catch (Exception e)
                {
                    normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
                }
            } else if(id == R.id.loadCBIB)
            {
                try {
                    String clipValue = clipBoardDataTV.getText().toString();
                    if(HelperTextUtility.isNotEmpty(clipValue))
                    {
                        checkAndLoad(clipValue,normalTabsRVAdapter.searchDialog);
                    } else {
                        normalTabsRVAdapter.showToastFromMainActivity(R.string.empty_clipboard);
                    }
                } catch (Exception e)
                {
                    normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
                }
            }
        };

        innerSearchEngineIV.setOnClickListener(onClickListener);
        voiceLauncherIB.setOnClickListener(onClickListener);
        closeSearchETHP.setOnClickListener(onClickListener);
        editCBIB.setOnClickListener(onClickListener);
        loadCBIB.setOnClickListener(onClickListener);

        popupSearchETHP.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            try {
                if(actionId == EditorInfo.IME_ACTION_GO)
                {
                    Editable editable = popupSearchETHP.getText();
                    if(HelperTextUtility.isNotEmpty(editable))
                    {
                        checkAndLoad(editable.toString(), normalTabsRVAdapter.searchDialog);
                    }

                    return true;
                }
            } catch (Exception e)
            {
                normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
            }
            return true;
        });

        normalTabsRVAdapter.searchDialog.setOnDismissListener(dialogInterface -> {
            popupSearchETHP.removeTextChangedListener(textWatcher);
            try {
                if(workRunnable != null)
                {
                    handler.removeCallbacks(workRunnable);
                    workRunnable = null;
                }
            } catch (Exception ignored){}

            normalTabsRVAdapter.searchDialog = null;
        });


        normalTabsRVAdapter.searchDialog.setCanceledOnTouchOutside(true);
        normalTabsRVAdapter.searchDialog.setCancelable(true);
        normalTabsRVAdapter.searchDialog.show();

    }

    void checkAndLaunchVoiceLauncher()
    {
        try {
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,"en-US");
                activity.voiceLauncher.launch(intent);
            } else {
                if(ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.RECORD_AUDIO))
                {
                    //Explain to the user why we needed this permission
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    View view = normalTabsRVAdapter.getLayoutInflater().inflate(R.layout.popup_permission_needed,
                            recyclerViewContainer,false);

                    TextView whyNeededTV = view.findViewById(R.id.whyNeededTV);
                    whyNeededTV.setText(R.string.why_need_microphone_permission);

                    builder.setView(view);
                    final AlertDialog dialog = builder.create();

                    MaterialButton grantPermissionDialogBtn,closePermissionDialogBtn;
                    grantPermissionDialogBtn = view.findViewById(R.id.grantPermissionDialogBtn);
                    closePermissionDialogBtn = view.findViewById(R.id.closePermissionDialogBtn);

                    grantPermissionDialogBtn.setOnClickListener(view1 -> {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.RECORD_AUDIO},VOICE_RECORD_REQUEST_PERMISSION_CODE);
                    });

                    closePermissionDialogBtn.setOnClickListener(view12 -> dialog.dismiss());

                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                } else {
                    ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.RECORD_AUDIO},VOICE_RECORD_REQUEST_PERMISSION_CODE);
                }
            }
        } catch (ActivityNotFoundException activityNotFoundException)
        {
            normalTabsRVAdapter.showToastFromMainActivity(R.string.activity_for_handling_voice_search_is_not_found);
        } catch (Exception e)
        {
            normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
        }
    }

    void checkAndLoad(String keyWord, Dialog dialog)
    {
        String url = checkAndGet(keyWord);
        dialog.dismiss();
        loadSearch(url);
    }

    private void loadSearch(String urlString)
    {
        if(viewHolder.isHPCVisible)
        {
            viewHolder.setClearHistory();
        }

        viewHolder.homePageCL.setVisibility(View.INVISIBLE);
        viewHolder.isHPCVisible = false;
        viewHolder.webViewContainer.setVisibility(View.VISIBLE);

        viewHolder.webView.evaluateJavascript("javascript:document.open();document.close();",null);
        viewHolder.webView.loadUrl(urlString);

        if(!viewHolder.isProgressBarVisible)
        {
            viewHolder.makeProgressBarVisible();
        }

        normalTabsRVAdapter.setDecorations(urlString,viewHolder);
    }

    String checkAndGet(String keyWord)
    {
        try {
            new URL(keyWord);

            if(normalTabsRVAdapter.isNetworkUrl(keyWord))
            {
                if(!normalTabsRVAdapter.incognitoMode && db.getIsSaveSearchHistory() == 1)
                {
                    if(db.checkNotContainsSearchItem(keyWord))
                    {
                        SearchItem searchItem = new SearchItem();
                        searchItem.setSeItemTitle(keyWord);
                        db.addSearchItem(searchItem);
                    }
                }

                return keyWord;
            }
            else {
                if(!normalTabsRVAdapter.incognitoMode && db.getIsSaveSearchHistory() == 1) {

                    if(db.checkNotContainsSearchItem(keyWord))
                    {
                        SearchItem searchItem = new SearchItem();
                        searchItem.setSeItemTitle(keyWord);
                        db.addSearchItem(searchItem);
                    }

                }

                return normalTabsRVAdapter.getSearchEngineURL() + keyWord;
            }
        } catch (Exception e)
        {
            try {

                @SuppressWarnings("UnstableApiUsage") InternetDomainName internetDomainName = InternetDomainName.from(keyWord);

                //noinspection UnstableApiUsage
                if(internetDomainName.hasPublicSuffix() && internetDomainName.hasParent())
                {
                    if(!normalTabsRVAdapter.incognitoMode && db.getIsSaveSearchHistory() == 1) {

                        if(db.checkNotContainsSearchItem(keyWord))
                        {
                            SearchItem searchItem = new SearchItem();
                            searchItem.setSeItemTitle(keyWord);
                            db.addSearchItem(searchItem);
                        }

                    }

                    return URLUtil.guessUrl(keyWord);
                } else //noinspection UnstableApiUsage
                    if (internetDomainName.isTopPrivateDomain()){
                    if(!normalTabsRVAdapter.incognitoMode && db.getIsSaveSearchHistory() == 1) {
                        if(db.checkNotContainsSearchItem(keyWord))
                        {
                            SearchItem searchItem = new SearchItem();
                            searchItem.setSeItemTitle(keyWord);
                            db.addSearchItem(searchItem);
                        }
                    }

                    return URLUtil.guessUrl(keyWord);
                } else {
                    if(!normalTabsRVAdapter.incognitoMode && db.getIsSaveSearchHistory() == 1) {
                        if(db.checkNotContainsSearchItem(keyWord))
                        {
                            SearchItem searchItem = new SearchItem();
                            searchItem.setSeItemTitle(keyWord);
                            db.addSearchItem(searchItem);
                        }
                    }

                    return normalTabsRVAdapter.getSearchEngineURL() + keyWord;
                }
            } catch (Exception f)
            {
                if(!normalTabsRVAdapter.incognitoMode && db.getIsSaveSearchHistory() == 1) {
                    if(db.checkNotContainsSearchItem(keyWord))
                    {
                        SearchItem searchItem = new SearchItem();
                        searchItem.setSeItemTitle(keyWord);
                        db.addSearchItem(searchItem);
                    }
                }

                return normalTabsRVAdapter.getSearchEngineURL() + keyWord;

            }
        }

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
