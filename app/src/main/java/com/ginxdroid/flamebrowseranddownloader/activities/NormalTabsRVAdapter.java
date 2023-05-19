package com.ginxdroid.flamebrowseranddownloader.activities;

import static android.view.View.VISIBLE;

import static com.ginxdroid.flamebrowseranddownloader.classes.ResourceFinder.getResId;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.ArrayMap;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.ginxdroid.flamebrowseranddownloader.BuildConfig;
import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.CustomEditText;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.models.HistoryItem;
import com.ginxdroid.flamebrowseranddownloader.models.SearchEngineItem;
import com.ginxdroid.flamebrowseranddownloader.models.SiteSettingsModel;
import com.ginxdroid.flamebrowseranddownloader.sheets.MainMenuSheet;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NormalTabsRVAdapter extends RecyclerView.Adapter<NormalTabsRVAdapter.ViewHolder>{
    private final ArrayList<String> urlsAL;
    private final ArrayList<Message> webViews;
    private final ArrayList<ViewHolder> viewHolders;

    private final MainActivity mainActivity;
    private final Context context;
    private final DatabaseHandler db;
    private final CustomHorizontalManager customHorizontalManager;
    private final CoordinatorLayout recyclerViewContainer;
    private final LayoutInflater inflater;
    private final RecyclerView recyclerView;
    private boolean isSwiping = false;
    private int recyclerViewContainerHeight, recyclerViewContainerWidth,newWidth,newHeight,appBarHeight;
    private float scaleX,scaleY;
    private final BottomAppBar bottomAppBar;
    private boolean isScrolling = false;
    private final RecyclerView.RecycledViewPool viewPool;
    private final int eight,eighty;
    private final CookieManager cookieManager;
    private boolean enableThirdPartyCookies;
    private final int colorPrimary,colorSurface;
    private final boolean isDarkWebUI,javaScriptEnabled,askLocation;
    private final ForegroundColorSpan span;
    private int seFavResId;
    private final Paint paint;
    private NormalTabsRVAdapter.ViewHolder viewHolder;
    private final String fileDirPath,dumpPath;
    private boolean saveHistory;
    Dialog searchDialog = null;
    private String searchEngineURL;
    boolean incognitoMode;
    private final int WEB_REQUEST_RECORD_AUDIO = 11;

    private PermissionRequest mPermissionRequest = null;
    private final SiteSettingsModel siteSettingsModel;
    private String callbackOrigin = null;
    private GeolocationPermissions.Callback geoLocationCallback = null;

    public NormalTabsRVAdapter(Context context, MainActivity mainActivity,
                               CustomHorizontalManager customHorizontalManager, CoordinatorLayout recyclerViewContainer,
                               RecyclerView recyclerView, BottomAppBar bottomAppBar) {
        this.context = context;
        this.mainActivity = mainActivity;
        this.customHorizontalManager = customHorizontalManager;
        this.recyclerViewContainer = recyclerViewContainer;
        this.recyclerView = recyclerView;
        this.bottomAppBar = bottomAppBar;

        incognitoMode = false;

        fileDirPath = context.getFilesDir().getAbsolutePath()+ File.separator+ "favicon" + File.separator;
        dumpPath = fileDirPath + "no_file_ABC_XYZ";

        eighty=context.getResources().getDimensionPixelSize(R.dimen.eighty);
        eight=context.getResources().getDimensionPixelSize(R.dimen.eight);

        urlsAL = new ArrayList<>();
        webViews = new ArrayList<>();
        viewHolders = new ArrayList<>();

        paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        viewPool = new RecyclerView.RecycledViewPool();





        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                isScrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
            }
        });

        db = DatabaseHandler.getInstance(context);
        siteSettingsModel = db.getSiteSettings();
        SearchEngineItem searchEngineItem = db.getCurrentSearchEngineItem();
        this.searchEngineURL = searchEngineItem.getSEItemURL();
        try {
            if(searchEngineItem.getSEIsDefault() == 1)
            {
                seFavResId = getResId(context,searchEngineItem.getSEItemTitle().toLowerCase(),"drawable",context.getPackageName());
            }else {
                seFavResId = R.drawable.round_search_24;
            }
        } catch (Exception e)
        {
            seFavResId = R.drawable.round_search_24;
        }



        isDarkWebUI = db.getDarkWebUI() == 1;

        saveHistory = siteSettingsModel.getSsSaveSitesInHistory() == 1;
        javaScriptEnabled = siteSettingsModel.getSsJavaScript() == 1;

        cookieManager = CookieManager.getInstance();

        switch (siteSettingsModel.getSsCookies())
        {
            case 0:
                cookieManager.setAcceptCookie(true);
                enableThirdPartyCookies = true;
                break;
            case 1:
                cookieManager.setAcceptCookie(true);
                enableThirdPartyCookies = false;
                break;
            case 2:
                cookieManager.setAcceptCookie(false);
                enableThirdPartyCookies = false;
                break;
        }

        askLocation = siteSettingsModel.getSsLocation() == 1;

        inflater = LayoutInflater.from(context);
        customHorizontalManager.setNormalTabsRVAdapter(NormalTabsRVAdapter.this);

        Resources.Theme theme = context.getTheme();

        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(R.attr.primaryText, typedValue, true);
        int primaryText = typedValue.data;
        span = new ForegroundColorSpan(primaryText);

        TypedValue typedValue2 = new TypedValue();
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue2, true);
        colorPrimary = typedValue2.data;

        TypedValue typedValue1 = new TypedValue();
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface,typedValue1,true);
        colorSurface = typedValue1.data;

        bottomAppBar.post(() -> {
            int width = recyclerViewContainer.getWidth();
            int height = recyclerViewContainer.getHeight();

            setSpecs(width,height);
        });

    }


    void invokeGeolocationCallback(boolean invoke)
    {
        geoLocationCallback.invoke(callbackOrigin,invoke,false);
        geoLocationCallback = null;
        callbackOrigin = null;
    }

    void setIncognitoMode(boolean incognitoMode)
    {
        this.incognitoMode = incognitoMode;
        if(incognitoMode)
        {
            saveHistory = false;
        } else {
            saveHistory = siteSettingsModel.getSsSaveSitesInHistory() == 1;
        }
    }

    void loadVoiceSearchQuery(String keyWord)
    {

        if(searchDialog!=null)
        {
            searchDialog.dismiss();
        }

        if(viewHolder.homePageCL.getVisibility()== VISIBLE)
        {
            viewHolder.setClearHistory();
        }
        viewHolder.homePageCL.setVisibility(View.INVISIBLE);
        viewHolder.isHPCVisible = false;

        viewHolder.webViewContainer.setVisibility(VISIBLE);


        String url = viewHolder.holderUtility.checkAndGet(keyWord);

        viewHolder.webView.evaluateJavascript("javascript:document.open();document.close();",null);
        viewHolder.webView.loadUrl(url);

        if(!viewHolder.isProgressBarVisible)
        {
            viewHolder.makeProgressBarVisible();
        }

        setDecorations(url, viewHolder);
    }

    void setSearchFaviconResId(int seFavResId, String searchEngineURL)
    {
        this.seFavResId = seFavResId;
        this.searchEngineURL = searchEngineURL;
    }

    void setSearchFaviconOnResume()
    {
        SearchEngineItem searchEngineItem = db.getCurrentSearchEngineItem();
        this.searchEngineURL = searchEngineItem.getSEItemURL();
        try {
            if(searchEngineItem.getSEIsDefault() == 1)
            {
                seFavResId = getResId(context,searchEngineItem.getSEItemTitle().toLowerCase(),"drawable",context.getPackageName());
            }else {
                seFavResId = R.drawable.round_search_24;
            }
        } catch (Exception e)
        {
            seFavResId = R.drawable.round_search_24;
        }finally {
            if(viewHolder != null)
            {
                viewHolder.emptyCV.callMiniSetNow();
            }
        }
    }

    void setSearchEngineURL(String searchEngineURL)
    {
        this.searchEngineURL = searchEngineURL;
    }

    void setSeFavResId(int seFavResId)
    {
        this.seFavResId = seFavResId;
    }

    String getSearchEngineURL()
    {return searchEngineURL;}

    int getSeFavResId()
    {return seFavResId;}

    String getFileDirPath()
    {
        return fileDirPath;
    }

    String getDumpPath()
    {
        return dumpPath;
    }

    void setSpecs(int recyclerViewContainerWidth, int recyclerViewContainerHeight)
    {
        try {
            this.recyclerViewContainerWidth = recyclerViewContainerWidth;
            this.recyclerViewContainerHeight = recyclerViewContainerHeight;
            this.appBarHeight = bottomAppBar.getHeight();
            newWidth = (int) (recyclerViewContainerWidth * 0.55);
            newHeight = (int) (recyclerViewContainerHeight * 0.55);

            scaleX = (((float) newWidth) / ((float) recyclerViewContainerWidth));
            scaleY = (((float) newHeight) / ((float) recyclerViewContainerHeight));

        } catch (Exception ignored){}
    }

    void setIsSwiping(boolean isSwiping)
    {
        this.isSwiping = isSwiping;
    }

    synchronized void addNewTab(String urlString, int viewType)
    {
        urlsAL.add(urlString);
        customHorizontalManager.setLayDownType(viewType);
        int itemCount = getItemCount();
        recyclerView.setItemViewCacheSize(itemCount);
        notifyItemInserted(itemCount - 1);
    }

    synchronized void removeTab(int position, View itemView)
    {
        webViews.remove(position);
        viewHolders.remove(position);
        urlsAL.remove(position);

        customHorizontalManager.setDeletePos(position);
        customHorizontalManager.setLayDownType(0);
        recyclerView.removeView(itemView);
        notifyItemRemoved(position);
    }

    synchronized void removeTabRange()
    {
        int size = webViews.size();
        if(size > 0)
        {
            webViews.clear();
            viewHolders.clear();
            urlsAL.clear();
        }

        customHorizontalManager.setLayDownType(10);
        recyclerView.removeAllViews();
        notifyItemRangeRemoved(0,size);

        recyclerView.setItemViewCacheSize(0);
        recyclerView.setLayoutManager(null);
        recyclerView.setAdapter(null);
        recyclerView.setLayoutManager(customHorizontalManager);
        recyclerView.setAdapter(NormalTabsRVAdapter.this);
    }

    synchronized void removeTabAndSwitch(int position, View view)
    {
        try {
            webViews.remove(position);
            viewHolders.remove(position);
            urlsAL.remove(position);
        } finally {
            try {
                customHorizontalManager.setLayDownType(9);
                int itemCount = getItemCount();
                if(itemCount == 0)
                {
                    customHorizontalManager.setCurrentActivePos(-1);
                    viewHolder = null;
                } else {
                    int currentActivePosition = customHorizontalManager.getCurrentActivePos();
                    int nextLast = itemCount - 1;
                    if(currentActivePosition > nextLast)
                    {
                        customHorizontalManager.setCurrentActivePos(nextLast);
                    }
                }
            } finally {
                recyclerView.removeView(view);
                notifyItemRemoved(position);
            }
        }
    }

    synchronized void removeAllTabs()
    {
        int size = viewHolders.size();

        if(size > 0)
        {
            int last = size - 1;
            try{
                for(int i = 0;i < last;i++)
                {
                    viewHolders.get(i).removeThisTab();
                }
            }finally {
                viewHolders.get(last).removeLastTab();
            }
        }
    }

    @NonNull
    @Override
    public NormalTabsRVAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = inflater.inflate(R.layout.web_row, parent, false);
        return new NormalTabsRVAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NormalTabsRVAdapter.ViewHolder holder, int position) {
        holder.quickLinksRV.setRecycledViewPool(viewPool);

        String urlString = urlsAL.get(position);
        switch (urlString)
        {
            case "NewTab":
            {
                holder.homePageCL.setVisibility(VISIBLE);
                holder.isHPCVisible = true;
                holder.lastURL = null;

                holder.showHPControls();
                webViews.add(null);
                holder.changeImageTo(R.drawable.round_refresh_24);
                break;
            }
            default: {
                webViews.add(null);

                holder.webViewContainer.setVisibility(VISIBLE);

                holder.webView.loadUrl(urlString);

                if (!holder.isProgressBarVisible) {

                    holder.makeProgressBarVisible();
                }

                setDecorInner(urlString, holder);
                break;
            }
        }

        viewHolders.add(holder);
        setTabCountTVText(getItemCount(),holder.tabsCountChildTVHP);

        if(customHorizontalManager.getCurrentActivePos() == position)
        {
            holder.isLayoutInvisible = true;

            viewHolder = holder;

//        todo    if (currentActiveCallBack!=null)
//            {
//                currentActiveCallBack.remove();
//            }

//     todo       currentActiveCallBack = holder.onBackPressedCallback;
//            dispatcher.addCallback(activity,currentActiveCallBack);
//            holder.onBackPressedCallback.setEnabled(true);

        } else {
            holder.isLayoutInvisible = false;
       //todo     holder.webView.onPause();
            holder.webView.setVisibility(View.GONE);
            holder.isWebViewGone = true;
        }
    }

    private void initializeWebView(final WebView webView, final ViewHolder viewHolder)
    {
        //tweak settings for our webview

        webView.setBackgroundColor(colorSurface);
        webView.setLongClickable(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        final WebSettings webSettings = webView.getSettings();

        if(isDarkWebUI)
        {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
                {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webSettings, true);
                }
            }else {

                if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
                {
                    //noinspection deprecation
                    WebSettingsCompat.setForceDark(webSettings,WebSettingsCompat.FORCE_DARK_ON);

                    if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY))
                    {
                        //noinspection deprecation
                        WebSettingsCompat.setForceDarkStrategy
                                (webSettings,WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING);
                    }
                }
            }
        } else {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webSettings, false);
                }
            }else {

                if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
                {
                    //noinspection deprecation
                    WebSettingsCompat.setForceDark(webSettings,WebSettingsCompat.FORCE_DARK_OFF);
                }
            }
        }

        webSettings.setJavaScriptEnabled(javaScriptEnabled);
        webSettings.setGeolocationEnabled(askLocation);
        webView.setInitialScale(0);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);

        webSettings.setAllowFileAccess(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        String newUserAgent = WebSettings.getDefaultUserAgent(context)+"Flame/"+ BuildConfig.VERSION_NAME;
        newUserAgent = newUserAgent.replace("; wv","");
        webSettings.setUserAgentString(newUserAgent);

        if(Build.VERSION.SDK_INT <= 26)
        {
            webSettings.setSaveFormData(false);
        }

        cookieManager.setAcceptThirdPartyCookies(webView,enableThirdPartyCookies);

        webView.setWebChromeClient(viewHolder.webChromeClient);
        webView.setWebViewClient(viewHolder.webViewClient);

    }

    public boolean isNetworkUrl(String url) {
        int length = url.length();
        return (((length > 7) && url.substring(0, 7).equalsIgnoreCase("http://"))
                || ((length > 8) && url.substring(0, 8).equalsIgnoreCase("https://")));
    }

    void setDecorations(String url, NormalTabsRVAdapter.ViewHolder viewHolder)
    {
        try{
            if(viewHolder.isLayoutInvisible)
            {
                setDecorInner(url,viewHolder);
            }else{
                viewHolder.webViewURLString = url;
            }
        }catch (Exception ignored){}
    }

    private void setDecorInner(String url, NormalTabsRVAdapter.ViewHolder viewHolder)
    {
        try{
            viewHolder.webViewURLString = url;

            URL aURL = new URL(url);
            if(aURL.getProtocol().equals("https"))
            {
                url = url.replace("https://","");
                int adder = 0;
                if(url.contains("www.")){
                    url = url.replace("www.","");
                    adder = 4;
                }

                viewHolder.connectionInformationIB.setImageResource(0);
                viewHolder.connectionInformationIB.setImageResource(R.drawable.round_lock_24);

                final int hostLength = aURL.getHost().length() - adder;
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(url);
                spannableStringBuilder.setSpan(span,0,hostLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                viewHolder.searchEditText.setText(spannableStringBuilder);

            } else {
                url = url.replace("http://","");
                int adder = 0;
                if(url.contains("www.")){
                    url = url.replace("www.","");
                    adder = 4;
                }

                viewHolder.connectionInformationIB.setImageResource(0);
                viewHolder.connectionInformationIB.setImageResource(R.drawable.round_info_24);

                final int hostLength = aURL.getHost().length() - adder;
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(url);
                spannableStringBuilder.setSpan(span,0,hostLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                viewHolder.searchEditText.setText(spannableStringBuilder);
            }

        } catch (Exception e)
        {
            try {
                String otherHost = url.split(":")[0];

                viewHolder.connectionInformationIB.setImageResource(0);
                viewHolder.connectionInformationIB.setImageResource(R.drawable.round_info_24);
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(url);
                spannableStringBuilder.setSpan(span,0,otherHost.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                viewHolder.searchEditText.setText(spannableStringBuilder);
            } catch (Exception ignored){}
        }
    }

    LayoutInflater getLayoutInflater()
    {
        return inflater;
    }

    ViewHolder getViewHolder() {
        return viewHolder;
    }

    int getRecyclerViewContainerHeight() {
        return recyclerViewContainerHeight;
    }

    int getRecyclerViewContainerWidth() {
        return recyclerViewContainerWidth;
    }

    int getEight() {
        return eight;
    }

    void set()
    {
        setTabCountTVText(getItemCount() + 1, viewHolder.tabsCountChildTVHP);
    }

    private void setTabCountTVText(int count, TextView tabsCountChildTVHP)
    {
        if(count > 99)
        {
            tabsCountChildTVHP.setText(R.string.max_count);
        } else {
            tabsCountChildTVHP.setText(String.valueOf(count));
        }
    }

    void showToastFromMainActivity(int resId)
    {
        mainActivity.showToast(resId);
    }

    @Override
    public int getItemCount() {
        return urlsAL.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private boolean isWebViewGone = false;
        private final RelativeLayout emptyFrameRL;
        private final ImageButton tabPreviewIB;

        private final CustomMCV emptyCV;
        private boolean isEmptyFrameLRLVisible = true;

        private final RelativeLayout emptyFrameLRL;
        final ConstraintLayout bottomToolbarCL;
        private final ImageButton reloadIB,searchIB,showMoreIB,homePageIB;
        private final RelativeLayout tabsCountChildTVHPRL;
        private final TextView tabsCountChildTVHP;

        final CoordinatorLayout homePageCL;
        final RecyclerView quickLinksRV;
        final QuickLinksRVHomePageAdapter quickLinksRVHomePageAdapter;
        private final GridLayoutManager gridLayoutManager;

        final RelativeLayout webViewContainer;
        final WebView webView;

        private final WebChromeClient webChromeClient;
        private final WebViewClient webViewClient;

        boolean isHPCVisible = false;

        private boolean clearHistory = false;

        String webViewURLString = null;

        private boolean isLayoutInvisible;

        private final ConstraintLayout tabFSControlsInner;
        private final ImageView connectionInformationIB;
        final ImageView connectionInformationIBInner;

        private final ConstraintLayout outerL;
        private final CustomEditText searchEditText, searchEditTextInner;

        private final ProgressBar progressBar;

        boolean isProgressBarVisible = false;

        private ObjectAnimator globalAnimation = null;

        private String evaluatedStartedURL = " ";
        private String evaluatedFinishedURL = " ";
        private int historyProgress = 100;
        String lastURL;

        private final TextView cardTitle;
        private final ImageView faviconIV;

        private final ExecutorService service;
        private Future<?> future = null;

        boolean desktopMode = false;

        boolean isInFullScreenMode = false;

        final ConstraintLayout tabFSControlsRL;
        final View dividerView;
        final Guideline guideLine;

        private final ViewStub findBoxVS;
        private View inflatedView = null;

        RelativeLayout findBoxRL;

        final HolderUtility holderUtility;

        private boolean requireScroll = false;

        private final ArrayMap<String, Float> scrollURLAM = new ArrayMap<>();

        private AlertDialog confirmationDialog = null,innerConfirmationDialog = null, drmDialog = null,
                    locationDialog = null;

        private void putScrollPosition()
        {
            try {
                if(viewHolder.scrollURLAM.containsKey(viewHolder.webViewURLString))
                {
                    viewHolder.scrollURLAM.setValueAt(viewHolder.scrollURLAM.indexOfKey(viewHolder.webViewURLString),
                            viewHolder.calculateProgression());
                } else {
                    viewHolder.scrollURLAM.put(viewHolder.webViewURLString,viewHolder.calculateProgression());
                }
            } catch (Exception ignored) {}
        }

        private float calculateProgression() {
            float positionTopView = webView.getTop();
            float contentHeight = webView.getContentHeight();
            float currentScrollPosition = webView.getScrollY();
            return (currentScrollPosition - positionTopView) / contentHeight;
        }

        void veryCommonAddWork()
        {
            isLayoutInvisible = false;

            emptyFrameRL.setVisibility(View.VISIBLE);
            makeTitleInvisible();


            try {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(itemView.getWindowToken(), 0);
                }
            } catch (Exception ignored) {
            }

            //todo webView.onPause();

            if(isInFullScreenMode)
            {
                exitFullScreenMode();
            }

            if(future == null) {

                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (customHorizontalManager.ifNotScrolling()) {
                            try {
                                future = service.submit(new DrawBitmap());
                            } catch (Exception e) {
                                tabPreviewIB.setBackgroundColor(colorSurface);
                            }
                        } else {
                            handler.postDelayed(this, 275);
                        }
                    }
                }, 275);
            }
        }

        void showSearchInPageDialog()
        {
            try {
                try {
                    if (findBoxRL == null)
                    {
                        inflatedView = findBoxVS.inflate();

                        final TextView occurrenceCountTV;
                        final ImageButton closeIBFB,findAboveIBFB,findBelowIBFB;
                        final CustomEditText findEditTextFBLL;

                        findBoxRL = inflatedView.findViewById(R.id.findBoxRL);
                        occurrenceCountTV = findBoxRL.findViewById(R.id.occurrenceCountTV);
                        closeIBFB = findBoxRL.findViewById(R.id.closeIBFB);
                        findAboveIBFB = findBoxRL.findViewById(R.id.findAboveIBFB);
                        findBelowIBFB = findBoxRL.findViewById(R.id.findBelowIBFB);
                        findEditTextFBLL = findBoxRL.findViewById(R.id.findEditTextFBLL);

                        String value = (0) + "/" + 0;
                        occurrenceCountTV.setText(value);

                        findEditTextFBLL.setOnEditorActionListener((v, actionId, event) -> {

                                if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                                    //Do our action search work
                                    try {
                                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                                        if (imm != null) {
                                            imm.hideSoftInputFromWindow(findEditTextFBLL.getWindowToken(), 0);
                                        }
                                    } catch (Exception ignored) {
                                    }

                                    Editable editable = findEditTextFBLL.getText();

                                    if (HelperTextUtility.isNotEmpty(editable)) {
                                        webView.setFindListener((i, i1, b) -> {
                                            String value13;
                                            if (i1 != 0) {
                                                value13 = (i + 1) + "/" + i1;
                                            } else {
                                                value13 = i + "/" + i1;
                                            }

                                            occurrenceCountTV.setText(value13);

                                            if (i1 > 0) {
                                                findAboveIBFB.setOnClickListener(view -> webView.findNext(false));

                                                findBelowIBFB.setOnClickListener(view -> webView.findNext(true));

                                            }
                                        });
                                        webView.findAllAsync(editable.toString());

                                    } else {
                                        String value14 = (0) + "/" + 0;
                                        occurrenceCountTV.setText(value14);
                                    }
                                    return true;
                                }
                                return false;
                            });

                        findEditTextFBLL.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                webView.clearMatches();
                                Editable editable = findEditTextFBLL.getText();

                                if(HelperTextUtility.isNotEmpty(editable))
                                {
                                    webView.setFindListener((i, i1, b) -> {

                                        String value12;
                                        if(i1 != 0)
                                        {
                                            value12 = (i + 1) + "/"+i1;
                                        }else {
                                            value12 = i +"/" +i1;
                                        }

                                        occurrenceCountTV.setText(value12);

                                        if(i1 > 0)
                                        {
                                            findAboveIBFB.setOnClickListener(view -> webView.findNext(false));

                                            findBelowIBFB.setOnClickListener(view -> webView.findNext(true));
                                        }

                                    });

                                    webView.findAllAsync(editable.toString());
                                } else {
                                    Editable editableHere = findEditTextFBLL.getText();
                                    if(editableHere != null)
                                    {
                                        editableHere.clear();
                                    }

                                    String value = (0) +"/" +0;
                                    occurrenceCountTV.setText(value);

                                }
                            }

                            @Override
                            public void afterTextChanged(Editable editable) {

                            }
                        });

                        closeIBFB.setOnClickListener(view -> {
                            Editable editableHere = findEditTextFBLL.getText();
                            if(editableHere != null)
                            {
                                editableHere.clear();
                            }

                            findBoxRL.setVisibility(View.INVISIBLE);
                            webView.clearMatches();

                            try {
                                InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(findEditTextFBLL.getWindowToken(), 0);
                                }
                            } catch (Exception ignored) {
                            }
                        });
                    }
                } finally {
                    inflatedView.bringToFront();
                    findBoxRL.setVisibility(VISIBLE);
                }
            } catch (Exception ignored){}
        }


        private void removeThisTab()
        {
            isSwiping = true;
            itemView.animate()
                    .translationY(-recyclerViewContainerHeight)
                    .withEndAction(() -> mainActivity.runOnUiThread(() -> {
                        webViewContainer.removeView(webView);
                        webView.removeAllViews();
                        webView.destroy();

                        stopService();
                        isSwiping = false;

                    })).setDuration(175).start();

        }

        private void removeLastTab()
        {
            isSwiping = true;
            itemView.animate()
                    .translationY(-recyclerViewContainerHeight)
                    .withEndAction(() -> mainActivity.runOnUiThread(() -> {
                        webViewContainer.removeView(webView);
                        webView.removeAllViews();
                        webView.destroy();

                        stopService();
                        removeTabRange();

                        isSwiping = false;
                    })).setDuration(175).start();

        }


        void enterFullScreenMode()
        {
            isInFullScreenMode = true;
            bottomToolbarCL.setVisibility(View.GONE);
            tabFSControlsRL.setVisibility(View.GONE);
            dividerView.setVisibility(View.GONE);

            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
            params.guideBegin = 0;
            guideLine.setLayoutParams(params);
        }

        void exitFullScreenMode()
        {
            isInFullScreenMode = false;
            tabFSControlsRL.setVisibility(VISIBLE);
            dividerView.setVisibility(VISIBLE);
            bottomToolbarCL.setVisibility(VISIBLE);

            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
            params.guideBegin = appBarHeight;
            guideLine.setLayoutParams(params);

        }


        private void showHPControls()
        {
            connectionInformationIBInner.setImageResource(0);
            connectionInformationIBInner.setImageResource(seFavResId);

            Editable editable = searchEditText.getText();
            if(HelperTextUtility.isNotEmpty(editable))
            {
                editable.clear();
            }

            if(searchEditText.hasFocus())
            {
                searchEditText.clearFocus();
            }

            webViewURLString = null;

        }

        private void makeHPVisible()
        {
            isHPCVisible = true;
        }

        void setClearHistory()
        {
            clearHistory = true;
        }

        void makeProgressBarVisible()
        {
            progressBar.setProgress(0);
            progressBar.setVisibility(VISIBLE);
            isProgressBarVisible = true;
            changeImageTo(R.drawable.close_background);
        }

        private void makeProgressBarInvisible()
        {
            isProgressBarVisible = false;
            progressBar.setVisibility(View.INVISIBLE);
            changeImageTo(R.drawable.round_refresh_24);
        }

        private void changeImageTo(int resID)
        {
            try{
                reloadIB.setImageResource(0);
                reloadIB.setImageResource(resID);
            }catch (Exception ignored){}
        }

        private void setProgressBarProgressSpecial(int newProgress)
        {
            try{
                if(globalAnimation != null)
                {
                    globalAnimation.cancel();
                }

                if(newProgress == 100)
                {
                    makeProgressBarInvisible();
                    progressBar.setProgress(1000);
                }else{
                    progressBar.setProgress(newProgress * 10);
                }
            }catch (Exception ignored){}
        }

        void reloadWebPage()
        {
            makeProgressBarVisible();
            if(HelperTextUtility.isNotEmpty(webViewURLString))
            {
                webView.loadUrl(webViewURLString);
            }
        }

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            outerL = itemView.findViewById(R.id.outerL);
            guideLine = outerL.findViewById(R.id.guideLine);
            dividerView = outerL.findViewById(R.id.dividerView);
            tabFSControlsRL = outerL.findViewById(R.id.tabFSControlsRL);

            ImageButton voiceLauncherIBTab = tabFSControlsRL.findViewById(R.id.voiceLauncherIBTab);
            voiceLauncherIBTab.setOnClickListener(ViewHolder.this);
            connectionInformationIB = tabFSControlsRL.findViewById(R.id.connectionInformationIB);
            searchEditText = tabFSControlsRL.findViewById(R.id.searchEditText);
            searchEditText.setMovementMethod(null);

            emptyFrameRL = itemView.findViewById(R.id.emptyFrameRL);
            emptyFrameLRL = emptyFrameRL.findViewById(R.id.emptyFrameLRL);

            cardTitle = emptyFrameLRL.findViewById(R.id.cardTitle);
            faviconIV = emptyFrameLRL.findViewById(R.id.faviconIV);

            tabPreviewIB = itemView.findViewById(R.id.tabPreviewIB);
            emptyCV = itemView.findViewById(R.id.emptyCV);


            ImageButton closeTabIB = itemView.findViewById(R.id.closeTabIB);
            closeTabIB.setOnClickListener(ViewHolder.this);


            bottomToolbarCL = itemView.findViewById(R.id.bottomToolbarCL);
            findBoxVS = bottomToolbarCL.findViewById(R.id.findBoxVS);
            reloadIB = bottomToolbarCL.findViewById(R.id.reloadIB);
            searchIB = bottomToolbarCL.findViewById(R.id.searchIB);
            showMoreIB = bottomToolbarCL.findViewById(R.id.showMoreIB);
            homePageIB = bottomToolbarCL.findViewById(R.id.homePageIB);
            tabsCountChildTVHPRL = bottomToolbarCL.findViewById(R.id.tabsCountChildTVHPRL);
            tabsCountChildTVHP = tabsCountChildTVHPRL.findViewById(R.id.tabsCountChildTVHP);

            homePageCL = itemView.findViewById(R.id.homePageCL);
            tabFSControlsInner = homePageCL.findViewById(R.id.tabFSControlsInner);
            connectionInformationIBInner = tabFSControlsInner.findViewById(R.id.connectionInformationIBInner);
            ImageButton voiceLauncherIBTabInner = tabFSControlsInner.findViewById(R.id.voiceLauncherIBTabInner);
            voiceLauncherIBTabInner.setOnClickListener(ViewHolder.this);
            searchEditTextInner = tabFSControlsInner.findViewById(R.id.searchEditTextInner);
            searchEditTextInner.setMovementMethod(null);

            searchEditTextInner.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if(hasFocus)
                    {
                     searchEditTextInner.clearFocus();
                     try {
                         if(isHPCVisible)
                         {
                             holderUtility.openSearchPopup("");
                         }else {
                             if(!TextUtils.isEmpty(webViewURLString))
                             {
                                 holderUtility.openSearchPopup(webViewURLString);
                             } else {
                                 holderUtility.openSearchPopup("");
                             }
                         }
                     } catch (Exception e)
                     {
                         holderUtility.openSearchPopup("");
                     }
                    }
                }
            });

            searchEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if(hasFocus)
                    {
                        searchEditText.clearFocus();
                        try {
                            if(isHPCVisible)
                            {
                                holderUtility.openSearchPopup("");
                            }else {
                                if(!TextUtils.isEmpty(webViewURLString))
                                {
                                    holderUtility.openSearchPopup(webViewURLString);
                                } else {
                                    holderUtility.openSearchPopup("");
                                }
                            }
                        } catch (Exception e)
                        {
                            holderUtility.openSearchPopup("");
                        }
                    }
                }
            });


            quickLinksRV = homePageCL.findViewById(R.id.quickLinksRV);
            quickLinksRVHomePageAdapter = new QuickLinksRVHomePageAdapter(db,context,NormalTabsRVAdapter.this,
                    ViewHolder.this,mainActivity,inflater);
            gridLayoutManager = new GridLayoutManager(context, 4);

            quickLinksRV.setLayoutManager(gridLayoutManager);
            quickLinksRV.setAdapter(quickLinksRVHomePageAdapter);
            setQL();

            service = Executors.newSingleThreadExecutor();

            webViewContainer = itemView.findViewById(R.id.webViewContainer);
            webView = webViewContainer.findViewById(R.id.webView);
            progressBar = webViewContainer.findViewById(R.id.progressBar);


            holderUtility = new HolderUtility(ViewHolder.this,context,mainActivity,recyclerViewContainer,
                    NormalTabsRVAdapter.this,db);

            emptyCV.setListener(new CustomMCV.SetterListener() {
                @Override
                public void miniSetNow() {
                    connectionInformationIBInner.setImageResource(0);
                    connectionInformationIBInner.setImageResource(seFavResId);
                    setQL();
                }

                @Override
                public void selectNow() {
                    stopFetcher();
                    makeTitleInvisible();
                    selectThisTab(false);
                }

                @Override
                public void startSelect() {
                    emptyFrameLRL.setVisibility(View.INVISIBLE);
                }

                @Override
                public void makeTitleBarVisible() {
                    makeTitleVisible();
                }

                @Override
                public void makeTitleBarInvisible() {
                    makeTitleInvisible();
                }
            });


            reloadIB.setOnClickListener(ViewHolder.this);
            searchIB.setOnClickListener(ViewHolder.this);
            showMoreIB.setOnClickListener(ViewHolder.this);
            homePageIB.setOnClickListener(ViewHolder.this);
            tabsCountChildTVHPRL.setOnClickListener(ViewHolder.this);
            connectionInformationIBInner.setOnClickListener(ViewHolder.this);
            connectionInformationIB.setOnClickListener(ViewHolder.this);

            webChromeClient = new WebChromeClient(){

                @Override
                public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                    if(askLocation && !isHPCVisible)
                    {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                        View layout = inflater.inflate(R.layout.popup_location_allow_or_not,recyclerViewContainer,false);
                        TextView domainTV = layout.findViewById(R.id.domainTV);
                        domainTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.round_info_24,0,0,0);

                        try {
                            URL aURL = new URL(webViewURLString);
                            String finalString = aURL.getHost() + context.getString(R.string.wants_to_use_your_device_location);
                            domainTV.setText(finalString);
                        }catch (Exception e){
                            domainTV.setText(R.string.this_site_wants_to_use_device_location);
                        }

                        dialogBuilder.setView(layout);
                        locationDialog = dialogBuilder.create();

                        locationDialog.setCanceledOnTouchOutside(false);
                        locationDialog.setCancelable(false);

                        final MaterialButton blockBtn, allowBtn;
                        blockBtn = layout.findViewById(R.id.blockBtn);
                        allowBtn = layout.findViewById(R.id.allowBtn);

                        callbackOrigin = origin;
                        geoLocationCallback = callback;

                        locationDialog.setOnDismissListener(dialogInterface -> locationDialog = null);

                        blockBtn.setOnClickListener(view13 -> {
                            geoLocationCallback.invoke(callbackOrigin,false,false);
                            geoLocationCallback = null;
                            callbackOrigin = null;
                            locationDialog.dismiss();
                        });

                        allowBtn.setOnClickListener(view14 -> {
                            locationDialog.dismiss();
                            if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                            {
                                geoLocationCallback.invoke(callbackOrigin,true,false);
                                geoLocationCallback = null;
                                callbackOrigin = null;
                            } else {
                                if(ActivityCompat.shouldShowRequestPermissionRationale(mainActivity,Manifest.permission.ACCESS_FINE_LOCATION) ||
                                        ActivityCompat.shouldShowRequestPermissionRationale(mainActivity,Manifest.permission.ACCESS_COARSE_LOCATION))
                                {
                                    //Explain to the user why we needed this permission
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    View view = inflater.inflate(R.layout.popup_permission_needed,
                                            recyclerViewContainer,false);

                                    TextView whyNeededTV = view.findViewById(R.id.whyNeededTV);
                                    whyNeededTV.setText(R.string.why_need_location_permission);

                                    builder.setView(view);
                                    final AlertDialog dialog = builder.create();

                                    MaterialButton grantPermissionDialogBtn,closePermissionDialogBtn;
                                    grantPermissionDialogBtn = view.findViewById(R.id.grantPermissionDialogBtn);
                                    closePermissionDialogBtn = view.findViewById(R.id.closePermissionDialogBtn);

                                    grantPermissionDialogBtn.setOnClickListener(view11 -> {
                                        dialog.dismiss();
                                        mainActivity.locationPermissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION});

                                    });

                                    closePermissionDialogBtn.setOnClickListener(view12 -> dialog.dismiss());

                                    dialog.setCancelable(true);
                                    dialog.setCanceledOnTouchOutside(true);
                                    dialog.show();
                                }
                                else {
                                    mainActivity.locationPermissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION});
                                }

                            }
                        });


                        if(isLayoutInvisible)
                        {
                            locationDialog.show();
                        }

                    } else {
                        callback.invoke(callbackOrigin,false,false);
                    }
                    super.onGeolocationPermissionsShowPrompt(origin, callback);
                }

                @Override
                public void onPermissionRequest(PermissionRequest request) {
                    try {
                        String[] resources = request.getResources();
                        for(String r : resources)
                        {
                            if(r.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                            {
                                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                                View layout = inflater.inflate(R.layout.popup_web_microphone_allow_or_not, recyclerViewContainer, false);
                                TextView domainTV = layout.findViewById(R.id.domainTV);
                                try {
                                    URL aURL = new URL(webViewURLString);
                                    String finalString = aURL.getHost() + context.getString(R.string.want_to_use_your_microphone);
                                    domainTV.setText(finalString);
                                }catch (Exception ignored){}

                                dialogBuilder.setView(layout);
                                confirmationDialog = dialogBuilder.create();
                                confirmationDialog.setCanceledOnTouchOutside(false);
                                confirmationDialog.setCancelable(false);

                                final MaterialButton blockBtn, allowBtn;
                                blockBtn = layout.findViewById(R.id.blockBtn);
                                allowBtn = layout.findViewById(R.id.allowBtn);

                                mPermissionRequest = request;
                                confirmationDialog.setOnDismissListener(dialogInterface -> confirmationDialog = null);

                                blockBtn.setOnClickListener(view -> {
                                    denyWebVoicePermissionRequest();
                                    confirmationDialog.dismiss();
                                });

                                allowBtn.setOnClickListener(view1 -> {
                                    confirmationDialog.dismiss();

                                    if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                                    {
                                        grantWebVoicePermissionRequest();
                                    } else {
                                        if(ActivityCompat.shouldShowRequestPermissionRationale(mainActivity,Manifest.permission.RECORD_AUDIO))
                                        {
                                            //Explain to the user why we needed this permission
                                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                            View view = inflater.inflate(R.layout.popup_permission_needed,
                                                    recyclerViewContainer,false);

                                            TextView whyNeededTV = view.findViewById(R.id.whyNeededTV);
                                            whyNeededTV.setText(R.string.why_need_microphone_permission);

                                            builder.setView(view);
                                            innerConfirmationDialog = builder.create();

                                            MaterialButton grantPermissionDialogBtn,closePermissionDialogBtn;
                                            grantPermissionDialogBtn = view.findViewById(R.id.grantPermissionDialogBtn);
                                            closePermissionDialogBtn = view.findViewById(R.id.closePermissionDialogBtn);

                                            grantPermissionDialogBtn.setOnClickListener(view11 -> {
                                                innerConfirmationDialog.dismiss();
                                                ActivityCompat.requestPermissions(mainActivity,
                                                        new String[]{Manifest.permission.RECORD_AUDIO},WEB_REQUEST_RECORD_AUDIO);
                                            });

                                            closePermissionDialogBtn.setOnClickListener(view12 -> innerConfirmationDialog.dismiss());

                                            innerConfirmationDialog.setOnDismissListener(dialogInterface -> innerConfirmationDialog = null);

                                            innerConfirmationDialog.setCancelable(true);
                                            innerConfirmationDialog.setCanceledOnTouchOutside(true);
                                            innerConfirmationDialog.show();
                                        } else {
                                            ActivityCompat.requestPermissions(mainActivity,new String[]{Manifest.permission.RECORD_AUDIO},
                                                    WEB_REQUEST_RECORD_AUDIO);
                                        }
                                    }
                                });

                                if(isLayoutInvisible)
                                {
                                    confirmationDialog.show();
                                }

                            } else if(r.equals(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID))
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                View layout = inflater.inflate(R.layout.popup_location_allow_or_not, recyclerViewContainer, false);
                                TextView domainTV = layout.findViewById(R.id.domainTV);

                                domainTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.round_info_24,0,0,0);

                                try {
                                    URL aURL = new URL(webViewURLString);
                                    String finalString = aURL.getHost() + context.getString(R.string.wants_to_play_drm_protected_content);
                                    domainTV.setText(finalString);
                                }catch (Exception ignored){}

                                builder.setView(layout);
                                drmDialog = builder.create();
                                drmDialog.setCanceledOnTouchOutside(false);
                                drmDialog.setCancelable(false);

                                final MaterialButton blockBtn, allowBtn;
                                blockBtn = layout.findViewById(R.id.blockBtn);
                                allowBtn = layout.findViewById(R.id.allowBtn);

                                mPermissionRequest = request;
                                drmDialog.setOnDismissListener(dialogInterface -> drmDialog = null);

                                blockBtn.setOnClickListener(view -> {
                                    request.deny();
                                    drmDialog.dismiss();
                                });

                                allowBtn.setOnClickListener(view -> {
                                    request.grant(resources);
                                    drmDialog.dismiss();

                                });

                                if(isLayoutInvisible)
                                {
                                    drmDialog.show();
                                }

                            } else {
                                showToastFromMainActivity(R.string.unable_to_grant_permission);
                            }
                        }
                    } catch (ActivityNotFoundException e)
                    {
                        showToastFromMainActivity(R.string.activity_for_handling_voice_search_is_not_found);
                    } catch (Exception e)
                    {
                        showToastFromMainActivity(R.string.oops_general_message);
                    }


                }

                @Override
                public void onPermissionRequestCanceled(PermissionRequest request) {

                    //We dismiss the prompt UI here as the request is no longer valid
                    try {
                        mPermissionRequest = null;
                        if(confirmationDialog != null)
                        {
                            confirmationDialog.dismiss();
                        }

                        if(innerConfirmationDialog != null)
                        {
                            innerConfirmationDialog.dismiss();
                        }

                        if(drmDialog != null)
                        {
                            drmDialog.dismiss();
                        }
                    } catch (Exception ignored) {}
                }

                private String titleURL = " ";
                @Override
                public void onReceivedTitle(WebView view, String title) {
                    super.onReceivedTitle(view, title);
                    try{
                        String currentURL = view.getUrl();
                        if(!TextUtils.isEmpty(currentURL) && !currentURL.equals(titleURL))
                        {
                            titleURL = currentURL;
                            if(!isHPCVisible)
                            {
                                cardTitle.setText(title);
                            }else{
                                titleURL = " ";
                                cardTitle.setText(R.string.new_tab);
                            }
                        }
                    } catch (Exception e)
                    {
                        try{
                            titleURL = " ";
                            cardTitle.setText(R.string.new_tab);
                        }catch (Exception ignored){}
                    }
                }

                @Override
                public void onReceivedIcon(WebView view, Bitmap icon) {
                    super.onReceivedIcon(view, icon);
                    try{
                        if(!isHPCVisible)
                        {
                            try{
                                faviconIV.setBackground(null);
                                faviconIV.setImageBitmap(null);
                                faviconIV.setImageBitmap(icon);
                            }catch (Exception ignored){}

                            try {
                                final String fileName = view.getTitle();
                                if(!TextUtils.isEmpty(fileName)){
                                    new Thread(() -> {
                                        try{
                                            File root = new File(context.getFilesDir(), "favicon");
                                            if(!root.exists())
                                            {
                                                //noinspection ResultOfMethodCallIgnored
                                                root.mkdir();
                                            }

                                            File imageFile = new File(root,fileName);
                                            try{
                                                if(!imageFile.exists())
                                                {
                                                    //noinspection ResultOfMethodCallIgnored
                                                    imageFile.createNewFile();
                                                }

                                                FileOutputStream FileStream = new FileOutputStream(imageFile,false);
                                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                                icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                                byte[] byteArray = stream.toByteArray();

                                                FileStream.write(byteArray);
                                                FileStream.close();
                                            }catch (IOException ignored){}
                                        } catch (Exception ignored) {}
                                    }).start();
                                }
                            } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
                }

                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    super.onProgressChanged(view, newProgress);
                    try{
                        if(isProgressBarVisible)
                        {
                            historyProgress = newProgress;

                            if(isLayoutInvisible)
                            {
                                try{
                                    int currentPBProgress = progressBar.getProgress();

                                    if(globalAnimation != null)
                                    {
                                        globalAnimation.cancel();
                                    }

                                    if(newProgress == 100)
                                    {
                                        ObjectAnimator animation = ObjectAnimator.ofInt
                                                (progressBar,"progress",currentPBProgress,1000);
                                        animation.setDuration(1000);
                                        animation.addListener(new Animator.AnimatorListener() {
                                            @Override
                                            public void onAnimationStart(@NonNull Animator animator) {

                                            }

                                            @Override
                                            public void onAnimationEnd(@NonNull Animator animator) {
                                                makeProgressBarInvisible();
                                            }

                                            @Override
                                            public void onAnimationCancel(@NonNull Animator animator) {

                                            }

                                            @Override
                                            public void onAnimationRepeat(@NonNull Animator animator) {

                                            }
                                        });

                                        globalAnimation = animation;
                                        animation.start();
                                    } else {
                                        final int tempProgress = newProgress * 10;
                                        if(tempProgress < currentPBProgress)
                                        {
                                            progressBar.setProgress(tempProgress);
                                        } else {
                                            ObjectAnimator animation = ObjectAnimator.ofInt(progressBar,"progress",currentPBProgress,tempProgress);
                                            animation.setDuration(1000);
                                            globalAnimation = animation;
                                            animation.start();
                                        }
                                    }
                                } catch (Exception ignored){}
                            }
                        }
                    }catch (Exception ignored){}
                }



            };

            webViewClient = new WebViewClient(){

                @Override
                public void onPageFinished(WebView view, String url) {
                    try {
                        if(!evaluatedFinishedURL.equals(url))
                        {
                            evaluatedFinishedURL = url;

                            try {
                                final String fileNameTemp = view.getTitle();

                                new Thread(() -> {
                                    try {
                                        if(saveHistory && !isHPCVisible)
                                        {
                                            final String currentDate = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).
                                                    format(new Date(System.currentTimeMillis()));

                                            if(db.checkNotContainsHistoryItemDate(currentDate))
                                            {
                                                HistoryItem dateHistoryItem = new HistoryItem();
                                                dateHistoryItem.setHiFaviconPath("no");
                                                dateHistoryItem.setHiTitle("no");
                                                dateHistoryItem.setHiURL("no");
                                                dateHistoryItem.setHiDate(currentDate);
                                                dateHistoryItem.setHiType(0);
                                                db.addHistoryItem(dateHistoryItem);
                                            }

                                            if(db.checkNotContainsHistoryItem(url, currentDate))
                                            {
                                                HistoryItem historyItem = new HistoryItem();
                                                if(!TextUtils.isEmpty(fileNameTemp))
                                                {
                                                    String path = fileDirPath + fileNameTemp;
                                                    historyItem.setHiFaviconPath(path);
                                                    historyItem.setHiTitle(fileNameTemp);
                                                } else {
                                                    historyItem.setHiFaviconPath(dumpPath);
                                                    try {
                                                        URL aURL = new URL(url);
                                                        String host = aURL.getHost();

                                                        if(!TextUtils.isEmpty(host))
                                                        {
                                                            historyItem.setHiTitle(host);
                                                        } else {
                                                            historyItem.setHiTitle("No title");
                                                        }
                                                    } catch (Exception e)
                                                    {
                                                        historyItem.setHiTitle("No title");
                                                    }
                                                }

                                                historyItem.setHiURL(url);
                                                historyItem.setHiDate(currentDate);
                                                historyItem.setHiType(1);
                                                db.addHistoryItem(historyItem);
                                            }
                                        }
                                    } catch (Exception ignored) {}
                                }).start();
                            } catch (Exception ignored) {}

                            try {

                                if(requireScroll)
                                {
                                    requireScroll = false;

                                    if(HelperTextUtility.isNotEmpty(webViewURLString))
                                    {
                                        final Float mProgressToRestore = scrollURLAM.get(webViewURLString);
                                        if(mProgressToRestore != null)
                                        {
                                            webView.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    int contentHeight = webView.getContentHeight();

                                                    if(contentHeight > 0)
                                                    {
                                                        int top = webView.getTop();

                                                        float webViewSize = contentHeight - top;
                                                        float positionInWV = webViewSize * mProgressToRestore;
                                                        int scrollToSet = Math.round(top + positionInWV);
                                                        if(webView.getScrollY() < scrollToSet)
                                                        {
                                                            webView.scrollTo(0,scrollToSet);
                                                        }
                                                        webView.removeCallbacks(this);
                                                    } else {
                                                        webView.postDelayed(this,10);
                                                    }
                                                }
                                            },10);
                                        }
                                    }
                                }

                            } catch (Exception ignored) {}

                        }
                    } catch (Exception ignored) {}

                    super.onPageFinished(view, url);
                }

                @Override
                public void onLoadResource(WebView view, String url) {
                    super.onLoadResource(view, url);

                    try {
                        if(desktopMode)
                        {
                            view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]')"+
                            ".setAttribute('content','width=1024px, initial-scale='+ (window.screen.width / 1024));", null);
                        }
                    } catch (Exception ignored){}
                }

                @Override
                public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                    if(!isHPCVisible && !TextUtils.isEmpty(url) && !url.equals("about:blank"))
                    {
                        setDecorations(url,ViewHolder.this);
                    }
                    super.doUpdateVisitedHistory(view, url, isReload);
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    try{
                        if(!evaluatedStartedURL.equals(url))
                        {
                            evaluatedStartedURL = url;

                            if (!isHPCVisible) {
                                if (!isProgressBarVisible) {
                                    makeProgressBarVisible();
                                }

                                setDecorations(url, ViewHolder.this);
                            }

                            if (clearHistory) {
                                clearHistory = false;
                                view.clearHistory();
                                view.clearCache(true);
                            }

                        }
                    }catch (Exception ignored){}
                    super.onPageStarted(view, url, favicon);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return checkReturn(request.getUrl().toString(),view);
                }

                private boolean checkReturn(String url, WebView view)
                {
                    if(isNetworkUrl(url))
                    {
                        try {
                            putScrollPosition();
                        }catch (Exception ignored) {}

                        if(!isHPCVisible)
                        {
                            setDecorations(url, ViewHolder.this);
                        }

                        if(url.endsWith(".pdf"))
                        {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse(url),"application/pdf");
                            try{
                                mainActivity.startActivity(intent);
                            }catch (Exception e)
                            {
                                mainActivity.showToast(R.string.pdf_viewer_is_not_available);
                            }

                            return true;
                        }

                        return false;
                    }else {
                        try{
                            if(url.startsWith("data:"))
                            {

                                try {
                                    putScrollPosition();
                                }catch (Exception ignored) {}

                                setDecorations(url,ViewHolder.this);


                                if(url.endsWith(".pdf"))
                                {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.parse(url),"application/pdf");
                                    try{
                                        mainActivity.startActivity(intent);
                                    }catch (Exception e)
                                    {
                                        mainActivity.showToast(R.string.pdf_viewer_is_not_available);
                                    }

                                    return true;
                                }

                                return false;

                            } else if(url.startsWith("intent://"))
                            {
                                Intent intent = Intent.parseUri(url,Intent.URI_INTENT_SCHEME);
                                PackageManager packageManager = context.getPackageManager();
                                ResolveInfo info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                                if(info != null)
                                {
                                    context.startActivity(intent);
                                }
                            } else if(url.startsWith("mailto:"))
                            {
                                mainActivity.startActivity(new Intent(Intent.ACTION_SENDTO,Uri.parse(url)));
                            } else if(url.startsWith("tel:"))
                            {
                                mainActivity.startActivity(new Intent(Intent.ACTION_DIAL,Uri.parse(url)));
                            }else {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(url));
                                mainActivity.startActivity(intent);
                            }

                            if(view.canGoBack())
                            {
                                view.goBack();
                            }

                        } catch (Exception ignored){}

                        return true;
                    }
                }

            };


            initializeWebView(webView,ViewHolder.this);

            itemView.post(this::doGestureWork);

        }

        void denyWebVoicePermissionRequest()
        {
            mPermissionRequest.deny();
            mPermissionRequest = null;
        }

        void grantWebVoicePermissionRequest()
        {
            mPermissionRequest.grant(mPermissionRequest.getResources());
            mPermissionRequest = null;
        }


        private class DrawBitmap extends Thread{
            public DrawBitmap()
            {}

            @Override
            public void run() {
                super.run();

                try {
                    Bitmap rawBitmap = Bitmap.createBitmap(recyclerViewContainerWidth, recyclerViewContainerHeight, Bitmap.Config.RGB_565);
                    Canvas c = new Canvas(rawBitmap);
                    outerL.draw(c);

                    Bitmap resultBitmap = Bitmap.createBitmap(newWidth,newHeight, Bitmap.Config.RGB_565);

                    Matrix scaleMatrix = new Matrix();
                    scaleMatrix.setScale(scaleX, scaleY,0f,0f);

                    Canvas canvas = new Canvas(resultBitmap);
                    canvas.setMatrix(scaleMatrix);
                    canvas.drawBitmap(rawBitmap,0,0,paint);

                    rawBitmap.recycle();

                    mainActivity.runOnUiThread(() -> {
                        try {
                            tabPreviewIB.setImageBitmap(resultBitmap);
                            future = null;
                        } catch (Exception e)
                        {
                            try {
                                tabPreviewIB.setBackgroundColor(colorSurface);
                                future = null;
                            } catch (Exception ignored){}
                        }
                    });


                } catch (Exception e) {
                    mainActivity.runOnUiThread(() -> {
                        try {
                            tabPreviewIB.setBackgroundColor(colorSurface);
                            future = null;
                        } catch (Exception ignored){}
                    });
                }
            }
        }

        private void setQL()
        {
            try{
                gridLayoutManager.setSpanCount(Math.min(recyclerViewContainerWidth,recyclerViewContainerHeight) / eighty);
            }catch (Exception ignored)
            {}

            quickLinksRVHomePageAdapter.setQuickLinks();
        }

        private void makeTitleInvisible()
        {
            emptyFrameLRL.setVisibility(View.INVISIBLE);
            isEmptyFrameLRLVisible = false;
        }

        private void makeTitleVisible()
        {
            emptyFrameLRL.setVisibility(VISIBLE);
            isEmptyFrameLRLVisible = true;
        }

        @SuppressLint("ClickableViewAccessibility")
        private void doGestureWork()
        {
            tabPreviewIB.setOnTouchListener(new SwipeDismissTouchListener(context, recyclerView, NormalTabsRVAdapter.this,
                    itemView, new SwipeDismissTouchListener.DismissCallbacks() {
                @Override
                public void onDismiss() {
                    mainActivity.runOnUiThread(() -> removeTab(getBindingAdapterPosition(), itemView));
                }


                @Override
                public void onClick() {
                    if(!isSwiping)
                    {
                        stopFetcher();
                        makeTitleInvisible();

                        customHorizontalManager.minimalSelect(getBindingAdapterPosition(), itemView, emptyCV, () -> selectThisTab(true));
                    }
                }

                @Override
                public boolean canSwipe() {
                    return !isScrolling && customHorizontalManager.ifNotScrolling() && isEmptyFrameLRLVisible;
                }
            }));
        }

        private void selectThisTab(boolean isRelayout)
        {
            if (isWebViewGone) {
                isWebViewGone = false;
                webView.setVisibility(VISIBLE);

           //todo     customHorizontalManager.remeasureCurrentView();
            }


            emptyFrameRL.setVisibility(View.INVISIBLE);
            isLayoutInvisible = true;
            viewHolder = ViewHolder.this;

            tabPreviewIB.setImageBitmap(null);
            tabPreviewIB.setBackgroundColor(Color.TRANSPARENT);

            setQL();
            setTabCountTVText(getItemCount(), tabsCountChildTVHP);

            if(confirmationDialog != null)
            {
                confirmationDialog.show();
            }

            if(drmDialog != null)
            {
                drmDialog.show();
            }

            if(locationDialog != null)
            {
                locationDialog.show();
            }
        }

        private void stopService()
        {
            try {
                if (!service.isShutdown()){
                    service.shutdownNow();
                }
            }catch (Exception ignored){}
        }

        private void stopFetcher()
        {
            try {
                if(future != null)
                {
                    try {
                        if(!future.isCancelled()) {
                            future.cancel(true);
                        }
                    }finally {
                        future = null;
                    }
                }
            }catch (Exception ignored){}
        }

        @Override
        public void onClick(View view) {
            int id = view.getId();
            if(id == R.id.tabsCountChildTVHPRL)
            {

                isLayoutInvisible = false;
                viewHolder = null;

                try {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);

                    if(imm != null)
                    {
                        imm.hideSoftInputFromWindow(itemView.getWindowToken(),0);
                    }
                }catch (Exception ignored){}

                makeTitleVisible();

                customHorizontalManager.minimalDeselectItem(() ->{
                    try {
                        if(future == null)
                        {
                            future = service.submit(new DrawBitmap());
                        }
                    } catch (Exception e)
                    {
                        tabPreviewIB.setBackgroundColor(colorSurface);
                    }
                });
                emptyFrameRL.setVisibility(VISIBLE);

            }else if(id == R.id.closeTabIB)
            {
                if(customHorizontalManager.ifNotScrolling())
                {
                    isSwiping = true;
                    itemView.animate().translationY(-recyclerViewContainerHeight)
                            .withEndAction(() -> mainActivity.runOnUiThread(() -> {
                                stopService();
                                removeTab(getBindingAdapterPosition(), itemView);
                                isSwiping = false;
                            })).setDuration(175).start();
                }
            }else if(id == R.id.reloadIB)
            {
                try{
                    if(!isHPCVisible)
                    {
                        evaluatedStartedURL = " ";
                        evaluatedFinishedURL = " ";

                        if(isProgressBarVisible)
                        {
                            if(globalAnimation != null)
                            {globalAnimation.cancel();}

                            webView.stopLoading();
                            makeProgressBarInvisible();
                        } else {
                            reloadWebPage();
                        }
                    }
                }catch (Exception ignored){}
            }else if(id == R.id.searchIB)
            {
                try {
                    if(isHPCVisible)
                    {
                        holderUtility.openSearchPopup("");
                    }else {
                        if(!TextUtils.isEmpty(webViewURLString))
                        {
                            holderUtility.openSearchPopup(webViewURLString);
                        } else {
                            holderUtility.openSearchPopup("");
                        }
                    }
                } catch (Exception e)
                {
                    holderUtility.openSearchPopup("");
                }
            }else if(id == R.id.showMoreIB)
            {
                try{
                    new MainMenuSheet().show(mainActivity.getSupportFragmentManager(), "mainMenuBottomSheet");
                }catch (Exception ignored){}
            }else if(id == R.id.homePageIB)
            {
                String homePageURL = db.getHomePageURL();

                if(homePageURL.equals("NewTab"))
                {
                    faviconIV.setBackground(null);
                    faviconIV.setImageBitmap(null);
                    faviconIV.setBackgroundResource(R.drawable.public_earth_bg);

                    webView.stopLoading();

                    setClearHistory();

                    webViewContainer.setVisibility(View.INVISIBLE);
                    homePageCL.setVisibility(VISIBLE);
                    makeHPVisible();
                    lastURL = webView.getUrl();
                    webView.loadUrl("about:blank");

                    showHPControls();

                    if(globalAnimation != null)
                    {
                        globalAnimation.cancel();
                    }

                    makeProgressBarInvisible();

                    cardTitle.setText(R.string.new_tab);

                } else {
                    setDecorations(homePageURL, ViewHolder.this);

                    homePageCL.setVisibility(View.INVISIBLE);
                    isHPCVisible = false;
                    webViewContainer.setVisibility(VISIBLE);

                    setClearHistory();

                    webView.loadUrl(homePageURL);

                    if(!isProgressBarVisible)
                    {
                        makeProgressBarVisible();
                    }
                }

            }else if(id == R.id.connectionInformationIB)
            {

            }else if(id == R.id.connectionInformationIBInner)
            {
                holderUtility.showSearchEnginesPopup(connectionInformationIBInner,null);
            } else if(id == R.id.voiceLauncherIBTab || id == R.id.voiceLauncherIBTabInner)
            {
                holderUtility.checkAndLaunchVoiceLauncher();
            }
        }

        void showMoreWP(View popupView, MainMenuSheet mainMenuSheet)
        {
            try{
                MenuHelper.showMenu(popupView,mainMenuSheet,context,
                        ViewHolder.this,db,mainActivity,
                        recyclerViewContainer, NormalTabsRVAdapter.this,
                        customHorizontalManager,false,inflater);
            }catch (Exception ignored) {}
        }
    }
}
