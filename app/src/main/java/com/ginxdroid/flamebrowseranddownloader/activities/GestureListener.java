package com.ginxdroid.flamebrowseranddownloader.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ginxdroid.flamebrowseranddownloader.BuildConfig;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GestureListener extends GestureDetector.SimpleOnGestureListener {
    private final NormalTabsRVAdapter.ViewHolder viewHolder;
    private final AppCompatActivity activity;
    private final NormalTabsRVAdapter normalTabsRVAdapter;
    private final Context context;

    public GestureListener(NormalTabsRVAdapter.ViewHolder viewHolder, AppCompatActivity activity, NormalTabsRVAdapter normalTabsRVAdapter, Context context) {
        this.viewHolder = viewHolder;
        this.activity = activity;
        this.normalTabsRVAdapter = normalTabsRVAdapter;
        this.context = context;
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        return true;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {
        try {
            WebView.HitTestResult result = viewHolder.webView.getHitTestResult();

            switch (result.getType())
            {
                case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                {
                    View popupView = activity.getLayoutInflater().inflate(R.layout.menu_layout,viewHolder.webView,false);
                    int finalWidth = Math.min(normalTabsRVAdapter.getRecyclerViewContainerWidth(), normalTabsRVAdapter.getRecyclerViewContainerHeight());
                    final PopupWindow popupWindow = new PopupWindow(popupView,(int)(finalWidth * 0.5), ViewGroup.LayoutParams.WRAP_CONTENT,true);
                    popupWindow.setOutsideTouchable(true);
                    popupWindow.setAnimationStyle(R.style.PopupWindowAnimationStyleSmallPopupWindow);
                    popupWindow.setElevation(normalTabsRVAdapter.getEight());

                    LinearLayout menuLayoutLL;
                    MaterialButton openInNewTab, openInNewTabAndSwitch, openImageInNewTab,
                            openImageInNewTabAndSwitch, saveImage, copyLinkText,
                            copyImageURL, copyLink, shareLink, shareImageURL;

                    menuLayoutLL = popupView.findViewById(R.id.menuLayoutLL);
                    openInNewTab = popupView.findViewById(R.id.openInNewTab);
                    openInNewTabAndSwitch = popupView.findViewById(R.id.openInNewTabAndSwitch);
                    openImageInNewTab = popupView.findViewById(R.id.openImageInNewTab);
                    openImageInNewTabAndSwitch = popupView.findViewById(R.id.openImageInNewTabAndSwitch);
                    saveImage = popupView.findViewById(R.id.saveImage);
                    copyLinkText = popupView.findViewById(R.id.copyLinkText);
                    copyImageURL = popupView.findViewById(R.id.copyImageURL);
                    copyLink = popupView.findViewById(R.id.copyLink);
                    shareLink = popupView.findViewById(R.id.shareLink);
                    shareImageURL = popupView.findViewById(R.id.shareImageURL);

                    menuLayoutLL.removeView(openImageInNewTab);
                    menuLayoutLL.removeView(openImageInNewTabAndSwitch);
                    menuLayoutLL.removeView(saveImage);
                    menuLayoutLL.removeView(copyImageURL);
                    menuLayoutLL.removeView(shareImageURL);

                    final IncomingHandler incomingHandler = new IncomingHandler(Looper.getMainLooper());
                    Message msg = incomingHandler.obtainMessage();
                    viewHolder.webView.requestFocusNodeHref(msg);

                    View.OnClickListener onClickListener = view -> {
                        int id = view.getId();
                        if(id == R.id.copyLinkText)
                        {
                            popupWindow.dismiss();
                            Bundle dataBundle = incomingHandler.getDataBundle();
                            if(dataBundle != null)
                            {
                                String linkText = dataBundle.getString("title");
                                if(linkText != null)
                                {
                                    ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clipData = ClipData.newPlainText("link text",linkText);
                                    if(clipboardManager != null)
                                    {
                                        clipboardManager.setPrimaryClip(clipData);
                                    }
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.link_copied_successfully);
                                } else {
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_copy_empty_link_text);
                                }
                            }
                        } else if(id == R.id.shareLink)
                        {
                            popupWindow.dismiss();
                            String linkURL = result.getExtra();
                            if(linkURL != null)
                            {
                                try {
                                    Intent share = new Intent(Intent.ACTION_SEND);
                                    share.setTypeAndNormalize("text/plain");
                                    share.putExtra(Intent.EXTRA_TEXT,linkURL);
                                    activity.startActivity(Intent.createChooser(share,activity.getString(R.string.share_via)));
                                } catch (Exception e12)
                                {
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.unable_to_share_url);
                                }
                            } else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_share_empty_link);
                            }
                        } else if(id == R.id.copyLink)
                        {
                            popupWindow.dismiss();
                            String linkURL = result.getExtra();
                            if(linkURL != null)
                            {
                                ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clipData = ClipData.newPlainText("link URL",linkURL);
                                if(clipboardManager != null)
                                {
                                    clipboardManager.setPrimaryClip(clipData);
                                }
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.link_copied);
                            } else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_copy_empty_link);
                            }
                        } else if (id == R.id.openInNewTab) {
                            popupWindow.dismiss();
                            String linkURL = result.getExtra();
                            if(linkURL != null)
                            {
                                normalTabsRVAdapter.set();
                                normalTabsRVAdapter.addNewTab(linkURL,1);
                            }
                            else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_open_empty_url);
                            }
                        } else if (id == R.id.openInNewTabAndSwitch) {
                            popupWindow.dismiss();
                            String linkURL = result.getExtra();
                            if(linkURL != null)
                            {
                                if(viewHolder.isInFullScreenMode)
                                {
                                    viewHolder.exitFullScreenMode();
                                }
                                normalTabsRVAdapter.addNewTab(linkURL,4);
                                viewHolder.veryCommonAddWork();
                            }else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_open_empty_url);
                            }
                        }
                    };

                    copyLinkText.setOnClickListener(onClickListener);
                    shareLink.setOnClickListener(onClickListener);
                    copyLink.setOnClickListener(onClickListener);
                    openInNewTab.setOnClickListener(onClickListener);
                    openInNewTabAndSwitch.setOnClickListener(onClickListener);

                    popupWindow.showAtLocation(viewHolder.webView, Gravity.NO_GRAVITY,(int)(e.getX()),(int)(e.getY()));

                    break;
                }
                case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                {
                    View popupView = activity.getLayoutInflater().inflate(R.layout.menu_layout,viewHolder.webView,false);
                    int finalWidth = Math.min(normalTabsRVAdapter.getRecyclerViewContainerWidth(), normalTabsRVAdapter.getRecyclerViewContainerHeight());
                    final PopupWindow popupWindow = new PopupWindow(popupView,(int)(finalWidth * 0.5), ViewGroup.LayoutParams.WRAP_CONTENT,true);
                    popupWindow.setOutsideTouchable(true);
                    popupWindow.setAnimationStyle(R.style.PopupWindowAnimationStyleSmallPopupWindow);
                    popupWindow.setElevation(normalTabsRVAdapter.getEight());

                    LinearLayout menuLayoutLL;
                    MaterialButton openInNewTab, openInNewTabAndSwitch, openImageInNewTab,
                            openImageInNewTabAndSwitch, saveImage, copyLinkText,
                            copyImageURL, copyLink, shareLink, shareImageURL;

                    menuLayoutLL = popupView.findViewById(R.id.menuLayoutLL);
                    openInNewTab = popupView.findViewById(R.id.openInNewTab);
                    openInNewTabAndSwitch = popupView.findViewById(R.id.openInNewTabAndSwitch);
                    openImageInNewTab = popupView.findViewById(R.id.openImageInNewTab);
                    openImageInNewTabAndSwitch = popupView.findViewById(R.id.openImageInNewTabAndSwitch);
                    saveImage = popupView.findViewById(R.id.saveImage);
                    copyLinkText = popupView.findViewById(R.id.copyLinkText);
                    copyImageURL = popupView.findViewById(R.id.copyImageURL);
                    copyLink = popupView.findViewById(R.id.copyLink);
                    shareLink = popupView.findViewById(R.id.shareLink);
                    shareImageURL = popupView.findViewById(R.id.shareImageURL);

                    menuLayoutLL.removeView(openImageInNewTab);
                    menuLayoutLL.removeView(openImageInNewTabAndSwitch);
                    menuLayoutLL.removeView(saveImage);
                    menuLayoutLL.removeView(copyImageURL);
                    menuLayoutLL.removeView(shareImageURL);

                    final IncomingHandler incomingHandler = new IncomingHandler(Looper.getMainLooper());
                    Message msg = incomingHandler.obtainMessage();
                    viewHolder.webView.requestFocusNodeHref(msg);

                    View.OnClickListener onClickListener = view -> {
                        int id = view.getId();
                        if(id == R.id.copyLinkText)
                        {
                            popupWindow.dismiss();
                            Bundle dataBundle = incomingHandler.getDataBundle();
                            if(dataBundle != null)
                            {
                                String linkText = dataBundle.getString("title");
                                if(linkText != null)
                                {
                                    ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clipData = ClipData.newPlainText("link text",linkText);
                                    if(clipboardManager != null)
                                    {
                                        clipboardManager.setPrimaryClip(clipData);
                                    }
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.link_copied_successfully);
                                } else {
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_copy_empty_link_text);
                                }
                            }
                        } else if(id == R.id.shareLink)
                        {
                            popupWindow.dismiss();
                            String linkURL = null;

                            Bundle dataBundle = incomingHandler.getDataBundle();
                            if(dataBundle != null)
                            {
                                linkURL = dataBundle.getString("url");
                            }

                            if(linkURL != null)
                            {
                                try {
                                    Intent share = new Intent(Intent.ACTION_SEND);
                                    share.setTypeAndNormalize("text/plain");
                                    share.putExtra(Intent.EXTRA_TEXT,linkURL);
                                    activity.startActivity(Intent.createChooser(share,activity.getString(R.string.share_via)));
                                } catch (Exception e13)
                                {
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.unable_to_share_url);
                                }
                            } else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_share_empty_link);
                            }
                        } else if(id == R.id.copyLink)
                        {
                            popupWindow.dismiss();
                            String linkURL = null;

                            Bundle dataBundle = incomingHandler.getDataBundle();
                            if(dataBundle != null)
                            {
                                linkURL = dataBundle.getString("url");
                            }

                            if(linkURL != null)
                            {
                                ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clipData = ClipData.newPlainText("link URL",linkURL);
                                if(clipboardManager != null)
                                {
                                    clipboardManager.setPrimaryClip(clipData);
                                }
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.link_copied);
                            } else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_copy_empty_link);
                            }
                        } else if (id == R.id.openInNewTab) {
                            popupWindow.dismiss();
                            String linkURL = null;

                            Bundle dataBundle = incomingHandler.getDataBundle();
                            if(dataBundle != null)
                            {
                                linkURL = dataBundle.getString("url");
                            }

                            if(linkURL != null)
                            {
                                normalTabsRVAdapter.set();
                                normalTabsRVAdapter.addNewTab(linkURL,1);
                            }
                            else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_open_empty_url);
                            }
                        } else if (id == R.id.openInNewTabAndSwitch) {
                            popupWindow.dismiss();
                            String linkURL = null;

                            Bundle dataBundle = incomingHandler.getDataBundle();
                            if(dataBundle != null)
                            {
                                linkURL = dataBundle.getString("url");
                            }

                            if(linkURL != null)
                            {
                                if(viewHolder.isInFullScreenMode)
                                {
                                    viewHolder.exitFullScreenMode();
                                }
                                normalTabsRVAdapter.addNewTab(linkURL,4);
                                viewHolder.veryCommonAddWork();
                            }else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_open_empty_url);
                            }
                        }
                    };

                    copyLinkText.setOnClickListener(onClickListener);
                    shareLink.setOnClickListener(onClickListener);
                    copyLink.setOnClickListener(onClickListener);
                    openInNewTab.setOnClickListener(onClickListener);
                    openInNewTabAndSwitch.setOnClickListener(onClickListener);

                    popupWindow.showAtLocation(viewHolder.webView, Gravity.NO_GRAVITY,(int)(e.getX()),(int)(e.getY()));

                    break;
                }
                case WebView.HitTestResult.IMAGE_TYPE:
                {
                    View popupView = activity.getLayoutInflater().inflate(R.layout.menu_layout,viewHolder.webView,false);
                    int finalWidth = Math.min(normalTabsRVAdapter.getRecyclerViewContainerWidth(), normalTabsRVAdapter.getRecyclerViewContainerHeight());
                    final PopupWindow popupWindow = new PopupWindow(popupView,(int)(finalWidth * 0.5), ViewGroup.LayoutParams.WRAP_CONTENT,true);
                    popupWindow.setOutsideTouchable(true);
                    popupWindow.setAnimationStyle(R.style.PopupWindowAnimationStyleSmallPopupWindow);
                    popupWindow.setElevation(normalTabsRVAdapter.getEight());

                    LinearLayout menuLayoutLL;
                    MaterialButton openInNewTab, openInNewTabAndSwitch, openImageInNewTab,
                            openImageInNewTabAndSwitch, saveImage, copyLinkText,
                            copyImageURL, copyLink, shareLink, shareImageURL;

                    menuLayoutLL = popupView.findViewById(R.id.menuLayoutLL);
                    openInNewTab = popupView.findViewById(R.id.openInNewTab);
                    openInNewTabAndSwitch = popupView.findViewById(R.id.openInNewTabAndSwitch);
                    openImageInNewTab = popupView.findViewById(R.id.openImageInNewTab);
                    openImageInNewTabAndSwitch = popupView.findViewById(R.id.openImageInNewTabAndSwitch);
                    saveImage = popupView.findViewById(R.id.saveImage);
                    copyLinkText = popupView.findViewById(R.id.copyLinkText);
                    copyImageURL = popupView.findViewById(R.id.copyImageURL);
                    copyLink = popupView.findViewById(R.id.copyLink);
                    shareLink = popupView.findViewById(R.id.shareLink);
                    shareImageURL = popupView.findViewById(R.id.shareImageURL);

                    menuLayoutLL.removeView(openInNewTab);
                    menuLayoutLL.removeView(openInNewTabAndSwitch);
                    menuLayoutLL.removeView(copyLinkText);
                    menuLayoutLL.removeView(copyLink);
                    menuLayoutLL.removeView(shareLink);

                    final IncomingHandler incomingHandler = new IncomingHandler(Looper.getMainLooper());
                    Message msg = incomingHandler.obtainMessage();
                    viewHolder.webView.requestFocusNodeHref(msg);

                    View.OnClickListener onClickListener = view -> {
                        int id = view.getId();
                        if(id == R.id.saveImage)
                        {
                            popupWindow.dismiss();
                            String downloadImageURL = null;

                            Bundle dataBundle = incomingHandler.getDataBundle();
                            if(dataBundle != null) {
                                downloadImageURL = dataBundle.getString("src");
                            }

                            if(downloadImageURL == null) {
                                downloadImageURL = result.getExtra();
                            }

                            if(downloadImageURL != null)
                            {
                                new AddImageDownloadTaskData(downloadImageURL, viewHolder.webView.getUrl()).start();
                            } else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_download_image_from_empty_url);
                            }

                        } else if(id == R.id.openImageInNewTabAndSwitch)
                        {

                            popupWindow.dismiss();
                            String downloadImageURL = null;

                            Bundle dataBundle = incomingHandler.getDataBundle();
                            if(dataBundle != null) {
                                downloadImageURL = dataBundle.getString("src");
                            }

                            if(downloadImageURL == null) {
                                downloadImageURL = result.getExtra();
                            }

                            if(downloadImageURL != null)
                            {
                                if(viewHolder.isInFullScreenMode)
                                {
                                    viewHolder.exitFullScreenMode();
                                }
                                normalTabsRVAdapter.addNewTab(downloadImageURL,4);
                                viewHolder.veryCommonAddWork();
                            }else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_open_empty_image_url);
                            }

                        } else if(id == R.id.openImageInNewTab)
                        {

                            popupWindow.dismiss();
                            String downloadImageURL = null;

                            Bundle dataBundle = incomingHandler.getDataBundle();
                            if(dataBundle != null) {
                                downloadImageURL = dataBundle.getString("src");
                            }

                            if(downloadImageURL == null) {
                                downloadImageURL = result.getExtra();
                            }

                            if(downloadImageURL != null)
                            {
                                normalTabsRVAdapter.set();
                                normalTabsRVAdapter.addNewTab(downloadImageURL,1);
                            }
                            else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_open_empty_url);
                            }
                        } else if (id == R.id.shareImageURL) {

                            popupWindow.dismiss();
                            String downloadImageURL = null;

                            Bundle dataBundle = incomingHandler.getDataBundle();
                            if(dataBundle != null) {
                                downloadImageURL = dataBundle.getString("src");
                            }

                            if(downloadImageURL == null) {
                                downloadImageURL = result.getExtra();
                            }

                            if(downloadImageURL != null)
                            {
                                try {
                                    Intent share = new Intent(Intent.ACTION_SEND);
                                    share.setTypeAndNormalize("text/plain");
                                    share.putExtra(Intent.EXTRA_TEXT,downloadImageURL);
                                    activity.startActivity(Intent.createChooser(share,activity.getString(R.string.share_via)));
                                } catch (Exception e1)
                                {
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.unable_to_share_url);
                                }
                            }
                            else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_share_empty_link);
                            }

                        } else if (id == R.id.copyImageURL) {
                            popupWindow.dismiss();
                            String downloadImageURL = null;

                            Bundle dataBundle = incomingHandler.getDataBundle();
                            if(dataBundle != null) {
                                downloadImageURL = dataBundle.getString("src");
                            }

                            if(downloadImageURL == null) {
                                downloadImageURL = result.getExtra();
                            }

                            if(downloadImageURL != null)
                            {
                                ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clipData = ClipData.newPlainText("image URL",downloadImageURL);
                                if(clipboardManager != null)
                                {
                                    clipboardManager.setPrimaryClip(clipData);
                                }
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.link_copied_successfully);
                            }
                            else {
                                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_share_empty_link);
                            }
                        }
                    };

                    saveImage.setOnClickListener(onClickListener);
                    openImageInNewTab.setOnClickListener(onClickListener);
                    openImageInNewTabAndSwitch.setOnClickListener(onClickListener);
                    shareImageURL.setOnClickListener(onClickListener);
                    copyImageURL.setOnClickListener(onClickListener);

                    popupWindow.showAtLocation(viewHolder.webView, Gravity.NO_GRAVITY,(int)(e.getX()),(int)(e.getY()));

                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private class AddImageDownloadTaskData extends Thread {
        private final String downloadImageURL;
        private final String pageURL;
        private String length;
        private String contentDisposition;
        private String contentType;
        private String newUserAgent;
        private AddImageDownloadTaskData(String downloadImageURL, String pageURL)
        {
            this.downloadImageURL = downloadImageURL;
            this.pageURL = pageURL;
        }

        @Override
        public void run() {
            super.run();
            try {
                int s = 0;
                if(!normalTabsRVAdapter.isNetworkUrl(downloadImageURL))
                {
                    try {
                        if(downloadImageURL.startsWith("data:"))
                        {
                            try {
                                newUserAgent = WebSettings.getDefaultUserAgent(context)+" Flame/"+ BuildConfig.VERSION_NAME;
                                newUserAgent = newUserAgent.replace("; wv","");

                                s = 1;
                            } catch (Exception ignored) {}
                            finally {
                                final int finalS = s;

                                activity.runOnUiThread(() -> {
                                    switch (finalS)
                                    {
                                        case 1:
                                            viewHolder.onListenDownload(downloadImageURL,newUserAgent,null,null,pageURL,
                                                    -1,null);
                                            break;
                                        case 0:
                                            normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
                                            break;
                                    }
                                });
                            }
                        } else {
                            normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_download_file_from_such_url);
                        }
                    } catch (Exception f)
                    {
                        normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_download_file_from_such_url);
                    }
                } else {
                    HttpURLConnection connection = null;

                    try {
                        URL url = new URL(downloadImageURL);
                        newUserAgent = WebSettings.getDefaultUserAgent(context)+" Flame/"+ BuildConfig.VERSION_NAME;
                        newUserAgent = newUserAgent.replace("; wv","");

                        connection = (HttpURLConnection) url.openConnection();
                        connection.setReadTimeout(45000);
                        connection.setConnectTimeout(45000);
                        connection.setRequestProperty("User-Agent",newUserAgent);

                        if((connection.getResponseCode() / 100) == 2)
                        {
                            length = connection.getHeaderField("Content-Length");
                            contentDisposition = connection.getHeaderField("Content-Disposition");
                            contentType = connection.getHeaderField("Content-Type");

                            s = 1;
                        } else {
                            s = 5;
                        }
                    } catch (MalformedURLException e1)
                    {
                        s = 2;
                    } catch (IOException e2)
                    {
                        s = 3;
                    } catch (NumberFormatException e3)
                    {
                        s = 4;
                    } catch (Exception ignored) {

                    }finally {
                        try {
                            if(connection != null)
                            {
                                connection.getInputStream().close();
                                connection.disconnect();
                            }
                        } catch (Exception ignored) {}

                        final int finalS = s;

                        activity.runOnUiThread(() -> {
                            switch (finalS)
                            {
                                case 1:
                                {
                                    try {
                                        viewHolder.onListenDownload(downloadImageURL,newUserAgent,contentDisposition,contentType,pageURL,
                                                Long.parseLong(length),null);
                                    } catch (NumberFormatException e)
                                    {
                                        viewHolder.onListenDownload(downloadImageURL,newUserAgent,contentDisposition,contentType,pageURL,
                                                -1,null);
                                    } catch (Exception e2)
                                    {
                                        normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
                                    }

                                    break;
                                }
                                case 0:
                                case 3: {
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
                                    break;
                                }
                                case 4:
                                {
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.image_size_not_found);
                                    break;
                                }
                                case 2:
                                {
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.malformed_url);
                                    break;
                                }
                                case 5:
                                {
                                    normalTabsRVAdapter.showToastFromMainActivity(R.string.file_not_found_on_url);
                                    break;
                                }
                            }
                        });
                    }
                }
            } catch (Exception e12)
            {
                normalTabsRVAdapter.showToastFromMainActivity(R.string.cannot_download_file_from_such_url);
            }
        }
    }

    private static class IncomingHandler extends Handler {
        Bundle dataBundle;

        public IncomingHandler(@NonNull Looper looper) {
            super(looper);
        }

        private Bundle getDataBundle()
        {
            return dataBundle;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            dataBundle = msg.getData();
        }
    }
}
