package com.ginxdroid.flamebrowseranddownloader.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.BuildConfig;
import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.sheets.MainMenuSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.TextScalingSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.ThemesSheet;
import com.ginxdroid.flamebrowseranddownloader.sheets.UpgradeSheet;
import com.google.android.material.button.MaterialButton;

public class MenuPagerRVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final NormalTabsRVAdapter.ViewHolder viewHolder;
    private final DatabaseHandler db;
    private final MainActivity activity;
    private final CoordinatorLayout recyclerViewContainer;
    private final NormalTabsRVAdapter normalTabsRVAdapter;
    private final CustomHorizontalManager customHorizontalManager;
    private final MainMenuSheet mainMenuSheet;
    private final LayoutInflater inflater;

    public MenuPagerRVAdapter(Context context, NormalTabsRVAdapter.ViewHolder viewHolder, DatabaseHandler db, MainActivity activity,
                              CoordinatorLayout recyclerViewContainer, NormalTabsRVAdapter normalTabsRVAdapter,
                              CustomHorizontalManager customHorizontalManager, MainMenuSheet mainMenuSheet, LayoutInflater inflater) {
        this.context = context;
        this.viewHolder = viewHolder;
        this.db = db;
        this.activity = activity;
        this.recyclerViewContainer = recyclerViewContainer;
        this.normalTabsRVAdapter = normalTabsRVAdapter;
        this.customHorizontalManager = customHorizontalManager;
        this.mainMenuSheet = mainMenuSheet;
        this.inflater = inflater;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType)
        {
            case 0: {
                View screenViewOne = LayoutInflater.from(context).inflate(R.layout.menu_page_one, parent, false);
                return new MenuHolderOne(screenViewOne);
            }
            case 1: {
                View screenViewOne = LayoutInflater.from(context).inflate(R.layout.menu_page_two, parent, false);
                return new MenuHolderTwo(screenViewOne);
            }
            default: {
                View view = inflater.inflate(R.layout.empty_row,parent,false);
                return new ViewHolderEmpty(view);
            }

        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        if(viewHolder != null)
        {
            if(viewHolder.isHPCVisible)
            {
                return 1;
            } else {
                return 2;
            }
        } else {
            return 1;
        }
    }

    static class ViewHolderEmpty extends RecyclerView.ViewHolder{
        public ViewHolderEmpty(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class MenuHolderOne extends RecyclerView.ViewHolder{

        public MenuHolderOne(@NonNull View itemView) {
            super(itemView);

            final MaterialButton bookmarksBtn,downloadsBtn,historyBtn,webModeBtn,closeBtn,siteSettingsBtn,
                    upgradeBtn,themesBtn;

            bookmarksBtn = itemView.findViewById(R.id.bookmarksBtn);
            downloadsBtn = itemView.findViewById(R.id.downloadsBtn);
            historyBtn = itemView.findViewById(R.id.historyBtn);
            webModeBtn = itemView.findViewById(R.id.webModeBtn);
            closeBtn = itemView.findViewById(R.id.closeBtn);
            siteSettingsBtn = itemView.findViewById(R.id.siteSettingsBtn);
            upgradeBtn = itemView.findViewById(R.id.upgradeBtn);
            themesBtn = itemView.findViewById(R.id.themesBtn);

            if(viewHolder != null)
            {
                if(viewHolder.desktopMode)
                {
                    Drawable drawable = ContextCompat.getDrawable(context,R.drawable.round_desktop_windows_24);
                    webModeBtn.setText(R.string.desktop_mode);
                    webModeBtn.setIcon(drawable);
                } else {
                    Drawable drawable = ContextCompat.getDrawable(context,R.drawable.round_smartphone_24);
                    webModeBtn.setText(R.string.mobile_mode);
                    webModeBtn.setIcon(drawable);
                }

                Drawable drawable = ContextCompat.getDrawable(context,R.drawable.close_this_tab);
                closeBtn.setText(R.string.close_this_tab);
                closeBtn.setIcon(drawable);
            } else {
                Drawable drawable = ContextCompat.getDrawable(context,R.drawable.close_all_tabs);
                closeBtn.setText(R.string.close_all_tabs);
                closeBtn.setIcon(drawable);
            }

            View.OnClickListener onClickListener = view -> {
                int id = view.getId();

                if(id == R.id.bookmarksBtn)
                {

                } else if(id == R.id.downloadsBtn)
                {
                    mainMenuSheet.dismiss();
                    activity.startActivity(new Intent(context, DownloadsActivity.class));
                }else if(id == R.id.historyBtn)
                {

                }else if(id == R.id.webModeBtn)
                {
                    if(viewHolder != null)
                    {
                        if(!viewHolder.isHPCVisible)
                        {
                            try {
                                final WebSettings webSettings = viewHolder.webView.getSettings();
                                if(viewHolder.desktopMode)
                                {
                                    String newUserAgent = WebSettings.getDefaultUserAgent(context)+" Flame/" + BuildConfig.VERSION_NAME;
                                    newUserAgent = newUserAgent.replace("; wv","");
                                    webSettings.setUserAgentString(newUserAgent);

                                    if(!viewHolder.isProgressBarVisible) {
                                        viewHolder.makeProgressBarVisible();
                                    }

                                    if(HelperTextUtility.isNotEmpty(viewHolder.webViewURLString))
                                    {
                                        viewHolder.webView.loadUrl(viewHolder.webViewURLString);
                                    }

                                    viewHolder.desktopMode = false;

                                    Drawable desktopModeOff = ContextCompat.getDrawable(context,R.drawable.round_smartphone_24);
                                    webModeBtn.setText(R.string.mobile_mode);
                                    webModeBtn.setIcon(desktopModeOff);
                                } else {
                                    String defaultUA = webSettings.getUserAgentString();
                                    String androidOSString = defaultUA.substring(defaultUA.indexOf("("),defaultUA.indexOf(")")+1);
                                    String newUserAgentString = defaultUA.replace(androidOSString,"(X11; LinuxX86_64)")
                                            .replace("Mobile ","");

                                    webSettings.setUserAgentString(newUserAgentString);

                                    if(!viewHolder.isProgressBarVisible) {
                                        viewHolder.makeProgressBarVisible();
                                    }

                                    if(HelperTextUtility.isNotEmpty(viewHolder.webViewURLString))
                                    {
                                        viewHolder.webView.loadUrl(viewHolder.webViewURLString);
                                    }

                                    viewHolder.desktopMode = true;
                                    Drawable desktopModeOn = ContextCompat.getDrawable(context,R.drawable.round_desktop_windows_24);
                                    webModeBtn.setText(R.string.desktop_mode);
                                    webModeBtn.setIcon(desktopModeOn);
                                }
                            } catch (Exception e)
                            {
                                try {
                                    final WebSettings webSettings = viewHolder.webView.getSettings();

                                    String newUserAgent = WebSettings.getDefaultUserAgent(context)+" Flame/" + BuildConfig.VERSION_NAME;
                                    newUserAgent = newUserAgent.replace("; wv","");
                                    webSettings.setUserAgentString(newUserAgent);

                                    if(!viewHolder.isProgressBarVisible) {
                                        viewHolder.makeProgressBarVisible();
                                    }

                                    if(HelperTextUtility.isNotEmpty(viewHolder.webViewURLString))
                                    {
                                        viewHolder.webView.loadUrl(viewHolder.webViewURLString);
                                    }

                                    viewHolder.desktopMode = false;

                                    Drawable desktopModeOff = ContextCompat.getDrawable(context,R.drawable.round_smartphone_24);
                                    webModeBtn.setText(R.string.mobile_mode);
                                    webModeBtn.setIcon(desktopModeOff);

                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
                                } catch (Exception e1)
                                {
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
                                }
                            }
                        } else {
                            try {
                                final WebSettings webSettings = viewHolder.webView.getSettings();
                                if(viewHolder.desktopMode)
                                {
                                    String newUserAgent = WebSettings.getDefaultUserAgent(context)+" Flame/" + BuildConfig.VERSION_NAME;
                                    newUserAgent = newUserAgent.replace("; wv","");
                                    webSettings.setUserAgentString(newUserAgent);

                                    viewHolder.desktopMode = false;

                                    Drawable desktopModeOff = ContextCompat.getDrawable(context,R.drawable.round_smartphone_24);
                                    webModeBtn.setText(R.string.mobile_mode);
                                    webModeBtn.setIcon(desktopModeOff);
                                } else {
                                    String defaultUA = webSettings.getUserAgentString();
                                    String androidOSString = defaultUA.substring(defaultUA.indexOf("("),defaultUA.indexOf(")")+1);
                                    String newUserAgentString = defaultUA.replace(androidOSString,"(X11; LinuxX86_64)")
                                            .replace("Mobile ","");

                                    webSettings.setUserAgentString(newUserAgentString);


                                    viewHolder.desktopMode = true;
                                    Drawable desktopModeOn = ContextCompat.getDrawable(context,R.drawable.round_desktop_windows_24);
                                    webModeBtn.setText(R.string.desktop_mode);
                                    webModeBtn.setIcon(desktopModeOn);
                                }
                            } catch (Exception e)
                            {
                                try {
                                    final WebSettings webSettings = viewHolder.webView.getSettings();
                                    String newUserAgent = WebSettings.getDefaultUserAgent(context)+" Flame/" + BuildConfig.VERSION_NAME;
                                    newUserAgent = newUserAgent.replace("; wv","");
                                    webSettings.setUserAgentString(newUserAgent);

                                    viewHolder.desktopMode = false;

                                    Drawable desktopModeOff = ContextCompat.getDrawable(context,R.drawable.round_smartphone_24);
                                    webModeBtn.setText(R.string.mobile_mode);
                                    webModeBtn.setIcon(desktopModeOff);

                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);

                                }catch (Exception e1)
                                {
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
                                }

                            }
                        }
                    }


                }else if(id == R.id.closeBtn)
                {
                    if(customHorizontalManager.ifNotScrolling())
                    {
                        mainMenuSheet.dismiss();

                        if(viewHolder != null)
                        {
                            viewHolder.itemView.animate().translationY(-normalTabsRVAdapter.getRecyclerViewContainerHeight())
                                    .withEndAction(() -> activity.runOnUiThread(() -> {
                                        viewHolder.webViewContainer.removeView(viewHolder.webView);
                                        viewHolder.webView.removeAllViews();
                                        viewHolder.webView.destroy();

                                        normalTabsRVAdapter.removeTabAndSwitch(viewHolder.getBindingAdapterPosition(), viewHolder.itemView);
                                    })).setDuration(175).start();
                        } else {
                            normalTabsRVAdapter.removeAllTabs();
                        }
                    }

                }else if(id == R.id.siteSettingsBtn)
                {
                    mainMenuSheet.dismiss();
                    activity.startActivity(new Intent(context, SiteSettingsActivity.class));
                }else if(id == R.id.upgradeBtn)
                {
                    mainMenuSheet.dismiss();
                    try {
                        new UpgradeSheet().show(activity.getSupportFragmentManager(),"upgradeSheet");
                    }catch (Exception ignored)
                    {}
                }else if(id == R.id.themesBtn)
                {
                    mainMenuSheet.dismiss();
                    try {
                        new ThemesSheet().show(activity.getSupportFragmentManager(),"themesSheet");
                    }catch (Exception ignored)
                    {}
                }

            };

            bookmarksBtn.setOnClickListener(onClickListener);
            downloadsBtn.setOnClickListener(onClickListener);
            historyBtn.setOnClickListener(onClickListener);
            webModeBtn.setOnClickListener(onClickListener);
            closeBtn.setOnClickListener(onClickListener);
            siteSettingsBtn.setOnClickListener(onClickListener);
            upgradeBtn.setOnClickListener(onClickListener);
            themesBtn.setOnClickListener(onClickListener);
        }
    }

    public class MenuHolderTwo extends RecyclerView.ViewHolder{

        public MenuHolderTwo(@NonNull View itemView) {
            super(itemView);

            final MaterialButton findInPageBtn,shareBtn,copyLinkBtn,printBtn,addToQuickLinkBtn,bookmarkThisBtn,
                    setAsHPBtn,textScaleBtn;

            findInPageBtn = itemView.findViewById(R.id.findInPageBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            copyLinkBtn = itemView.findViewById(R.id.copyLinkBtn);
            printBtn = itemView.findViewById(R.id.printBtn);
            addToQuickLinkBtn = itemView.findViewById(R.id.addToQuickLinkBtn);
            bookmarkThisBtn = itemView.findViewById(R.id.bookmarkThisBtn);
            setAsHPBtn = itemView.findViewById(R.id.setAsHPBtn);
            textScaleBtn = itemView.findViewById(R.id.textScaleBtn);

            View.OnClickListener onClickListener = view -> {
                int id = view.getId();
                if(id == R.id.findInPageBtn)
                {
                    mainMenuSheet.dismiss();
                    viewHolder.showSearchInPageDialog();
                }else if(id == R.id.shareBtn)
                {
                    if(HelperTextUtility.isNotEmpty(viewHolder.webViewURLString))
                    {
                        mainMenuSheet.dismiss();
                        try {
                            Intent share = new Intent(Intent.ACTION_SEND);
                            share.setTypeAndNormalize("text/plain");
                            share.putExtra(Intent.EXTRA_TEXT, viewHolder.webViewURLString);
                            activity.startActivity(Intent.createChooser(share,"Share via"));
                        } catch (Exception e)
                        {
                            normalTabsRVAdapter.showToastFromMainActivity(R.string.app_not_found);
                        }
                    } else {
                        normalTabsRVAdapter.showToastFromMainActivity(R.string.unable_to_find_wp_url);
                    }
                } else if(id ==R.id.copyLinkBtn)
                {
                    if(HelperTextUtility.isNotEmpty(viewHolder.webViewURLString))
                    {
                        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText("URL",viewHolder.webViewURLString);
                        if(clipboardManager != null)
                        {
                            clipboardManager.setPrimaryClip(clipData);
                        }

                        normalTabsRVAdapter.showToastFromMainActivity(R.string.copied_to_clipboard);
                    } else {
                        normalTabsRVAdapter.showToastFromMainActivity(R.string.unable_to_find_wp_url);
                    }
                } else if(id == R.id.printBtn)
                {
                    mainMenuSheet.dismiss();
                    try {
                        //Get a PrintManager instance
                        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
                        String jobName = context.getString(R.string.app_name)+" Document";

                        //Get a print adapter instance
                        PrintDocumentAdapter printAdapter = viewHolder.webView.createPrintDocumentAdapter(jobName);

                        //Create a print job with name and adapter instance
                        if(printManager != null)
                        {
                            printManager.print(jobName,printAdapter,new PrintAttributes.Builder().build());
                        }else {
                            normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
                        }
                    } catch (Exception e)
                    {
                        normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
                    }
                } else if(id == R.id.addToQuickLinkBtn)
                {

                } else if(id == R.id.bookmarkThisBtn)
                {

                } else if(id == R.id.setAsHPBtn)
                {

                } else if(id == R.id.textScaleBtn)
                {
                    mainMenuSheet.dismiss();
                    try {
                        new TextScalingSheet().show(activity.getSupportFragmentManager(),"textScalingSheet");
                    } catch (Exception ignored) {}
                }

            };

            findInPageBtn.setOnClickListener(onClickListener);
            shareBtn.setOnClickListener(onClickListener);
            copyLinkBtn.setOnClickListener(onClickListener);
            printBtn.setOnClickListener(onClickListener);
            addToQuickLinkBtn.setOnClickListener(onClickListener);
            bookmarkThisBtn.setOnClickListener(onClickListener);
            setAsHPBtn.setOnClickListener(onClickListener);
            textScaleBtn.setOnClickListener(onClickListener);
        }
    }
}
