package com.ginxdroid.flamebrowseranddownloader.activities;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
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
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.ginxdroid.flamebrowseranddownloader.BuildConfig;
import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.CustomEditText;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.google.android.material.bottomappbar.BottomAppBar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NormalTabsRVAdapter extends RecyclerView.Adapter<NormalTabsRVAdapter.ViewHolder>{
    private final ArrayList<String> urlsAL;
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

    private final int eighty;

    private final CookieManager cookieManager;
    private boolean enableThirdPartyCookies;
    private final int colorPrimary,colorSurface;

    private final boolean isDarkWebUI,javaScriptEnabled,askLocation;
    private final ForegroundColorSpan span;
    private int seFavResId;

    private final Paint paint;

    private NormalTabsRVAdapter.ViewHolder viewHolder;

    public NormalTabsRVAdapter(Context context, MainActivity mainActivity,
                               CustomHorizontalManager customHorizontalManager, CoordinatorLayout recyclerViewContainer,
                               RecyclerView recyclerView, BottomAppBar bottomAppBar) {
        this.context = context;
        this.mainActivity = mainActivity;
        this.customHorizontalManager = customHorizontalManager;
        this.recyclerViewContainer = recyclerViewContainer;
        this.recyclerView = recyclerView;
        this.bottomAppBar = bottomAppBar;

        eighty=context.getResources().getDimensionPixelSize(R.dimen.eighty);

        urlsAL = new ArrayList<>();

        paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        viewPool = new RecyclerView.RecycledViewPool();

        cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        enableThirdPartyCookies = true;


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                isScrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
            }
        });

        db = DatabaseHandler.getInstance(context);

        seFavResId = R.drawable.round_search_24;

        isDarkWebUI = db.getDarkWebUI() == 1;
        javaScriptEnabled = true;
        askLocation = true;

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
        urlsAL.remove(position);

        customHorizontalManager.setDeletePos(position);
        customHorizontalManager.setLayDownType(0);
        recyclerView.removeView(itemView);
        notifyItemRemoved(position);
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
                holder.homePageCL.setVisibility(View.VISIBLE);
                holder.isHPCVisible = true;
                holder.lastURL = null;

                holder.showHPControls();
                //todo: webViews.add(null);
                holder.changeImageTo(R.drawable.round_refresh_24);
                break;
            }
        }

        if(urlString.equals("NewTab"))
        {
            //this means this is our default home page layout
            holder.homePageCL.setVisibility(View.VISIBLE);
            holder.webViewContainer.setVisibility(View.INVISIBLE);

        }else {
            //this means that we need to hide our default home page and load webVIewContainer frame
            holder.homePageCL.setVisibility(View.INVISIBLE);
            holder.webViewContainer.setVisibility(View.VISIBLE);
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

    ViewHolder getViewHolder() {
        return viewHolder;
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

    @Override
    public int getItemCount() {
        return urlsAL.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final RelativeLayout emptyFrameRL;
        private final ImageButton tabPreviewIB;

        private final CustomMCV emptyCV;
        private boolean isEmptyFrameLRLVisible = true;

        private final RelativeLayout emptyFrameLRL;
        private final ConstraintLayout bottomToolbarCL;
        private final ImageButton reloadIB,searchIB,showMoreIB,homePageIB;
        private final RelativeLayout tabsCountChildTVHPRL;
        private final TextView tabsCountChildTVHP;

        final CoordinatorLayout homePageCL;
        private final RecyclerView quickLinksRV;
        private final QuickLinksRVHomePageAdapter quickLinksRVHomePageAdapter;
        private final GridLayoutManager gridLayoutManager;

        final RelativeLayout webViewContainer;
        final WebView webView;

        private final WebChromeClient webChromeClient;
        private final WebViewClient webViewClient;

        boolean isHPCVisible = false;

        private boolean clearHistory = false;

        private String webViewURLString = null;

        private boolean isLayoutInvisible;

        private final ImageView connectionInformationIB, connectionInformationIBInner;

        private final ConstraintLayout outerL;
        private final CustomEditText searchEditText;

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

        void setClearHistory()
        {
            clearHistory = true;
        }

        void makeProgressBarVisible()
        {
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
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
            connectionInformationIB = outerL.findViewById(R.id.connectionInformationIB);
            searchEditText = outerL.findViewById(R.id.searchEditText);

            emptyFrameRL = itemView.findViewById(R.id.emptyFrameRL);
            emptyFrameLRL = emptyFrameRL.findViewById(R.id.emptyFrameLRL);

            cardTitle = emptyFrameLRL.findViewById(R.id.cardTitle);
            faviconIV = emptyFrameLRL.findViewById(R.id.faviconIV);

            tabPreviewIB = itemView.findViewById(R.id.tabPreviewIB);
            emptyCV = itemView.findViewById(R.id.emptyCV);


            ImageButton closeTabIB = itemView.findViewById(R.id.closeTabIB);
            closeTabIB.setOnClickListener(ViewHolder.this);

            emptyFrameRL.setVisibility(View.INVISIBLE);
            isLayoutInvisible = true;

            bottomToolbarCL = itemView.findViewById(R.id.bottomToolbarCL);
            reloadIB = bottomToolbarCL.findViewById(R.id.reloadIB);
            searchIB = bottomToolbarCL.findViewById(R.id.searchIB);
            showMoreIB = bottomToolbarCL.findViewById(R.id.showMoreIB);
            homePageIB = bottomToolbarCL.findViewById(R.id.homePageIB);
            tabsCountChildTVHPRL = bottomToolbarCL.findViewById(R.id.tabsCountChildTVHPRL);
            tabsCountChildTVHP = tabsCountChildTVHPRL.findViewById(R.id.tabsCountChildTVHP);

            homePageCL = itemView.findViewById(R.id.homePageCL);
            connectionInformationIBInner = homePageCL.findViewById(R.id.connectionInformationIBInner);
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


            reloadIB.setOnClickListener(ViewHolder.this);
            searchIB.setOnClickListener(ViewHolder.this);
            showMoreIB.setOnClickListener(ViewHolder.this);
            homePageIB.setOnClickListener(ViewHolder.this);
            tabsCountChildTVHPRL.setOnClickListener(ViewHolder.this);
            connectionInformationIBInner.setOnClickListener(ViewHolder.this);
            connectionInformationIB.setOnClickListener(ViewHolder.this);

            webChromeClient = new WebChromeClient(){

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
            emptyFrameLRL.setVisibility(View.VISIBLE);
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
            emptyFrameRL.setVisibility(View.INVISIBLE);
            isLayoutInvisible = true;
            viewHolder = ViewHolder.this;

            tabPreviewIB.setImageBitmap(null);
            tabPreviewIB.setBackgroundColor(Color.TRANSPARENT);

            setQL();
            setTabCountTVText(getItemCount(), tabsCountChildTVHP);
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
                emptyFrameRL.setVisibility(View.VISIBLE);

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

            }else if(id == R.id.showMoreIB)
            {

            }else if(id == R.id.homePageIB)
            {

            }else if(id == R.id.connectionInformationIB)
            {

            }else if(id == R.id.connectionInformationIBInner)
            {

            }
        }
    }
}
