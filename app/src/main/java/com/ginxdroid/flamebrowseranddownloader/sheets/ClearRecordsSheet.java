package com.ginxdroid.flamebrowseranddownloader.sheets;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebViewDatabase;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.models.HistoryItem;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClearRecordsSheet extends BottomSheetDialogFragment {
    private ExecutorService service = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_clear_records, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;

            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if(bottomSheet != null)
            {
                BottomSheetBehavior.from(bottomSheet).setPeekHeight(bottomSheet.getHeight());
            }
        });

        return dialog;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final LinearLayout checkTVLL = view.findViewById(R.id.checkTVLL);
        final CheckedTextView clearHistoryCB,clearSearchHistoryCB,clearCookiesCB,clearCacheCB,clearWebViewDatabaseCB,clearUnusedFaviconsCB,clearWebStorageCB;
        final MaterialButton clearButton;

        clearHistoryCB = checkTVLL.findViewById(R.id.clearHistoryCB);
        clearSearchHistoryCB = checkTVLL.findViewById(R.id.clearSearchHistoryCB);
        clearCookiesCB = checkTVLL.findViewById(R.id.clearCookiesCB);
        clearCacheCB = checkTVLL.findViewById(R.id.clearCacheCB);
        clearWebViewDatabaseCB = checkTVLL.findViewById(R.id.clearWebViewDatabaseCB);
        clearUnusedFaviconsCB = checkTVLL.findViewById(R.id.clearUnusedFaviconsCB);
        clearWebStorageCB = checkTVLL.findViewById(R.id.clearWebStorageCB);
        clearButton = checkTVLL.findViewById(R.id.clearButton);

        final LinearLayout clearingLL = view.findViewById(R.id.clearingLL);
        final ProgressBar deletingProgressBarH = clearingLL.findViewById(R.id.deletingProgressBarH);
        final ImageView deletedIVH = clearingLL.findViewById(R.id.deletedIVH);
        final TextView doneTVH = clearingLL.findViewById(R.id.doneTVH);
        final MaterialButton closeButtonH = clearingLL.findViewById(R.id.closeButtonH);

        clearHistoryCB.setChecked(true);
        clearSearchHistoryCB.setChecked(true);
        clearCookiesCB.setChecked(true);
        clearCacheCB.setChecked(true);
        clearWebViewDatabaseCB.setChecked(true);
        clearUnusedFaviconsCB.setChecked(true);
        clearWebStorageCB.setChecked(true);

        final Context context = getContext();

        assert context != null;
        final Drawable checkedBackground = ContextCompat.getDrawable(context,R.drawable.round_check_box_24);
        final Drawable uncheckedBackground = ContextCompat.getDrawable(context,R.drawable.unchecked_background);

        clearHistoryCB.setCheckMarkDrawable(checkedBackground);
        clearSearchHistoryCB.setCheckMarkDrawable(checkedBackground);
        clearCookiesCB.setCheckMarkDrawable(checkedBackground);
        clearCacheCB.setCheckMarkDrawable(checkedBackground);
        clearWebViewDatabaseCB.setCheckMarkDrawable(checkedBackground);
        clearUnusedFaviconsCB.setCheckMarkDrawable(checkedBackground);
        clearWebStorageCB.setCheckMarkDrawable(checkedBackground);

        View.OnClickListener onClickListener = view1 -> {
            int id = view1.getId();
            if(id == R.id.clearUnusedFaviconsCB)
            {
                if(clearUnusedFaviconsCB.isChecked())
                {
                    clearUnusedFaviconsCB.setChecked(false);
                    clearUnusedFaviconsCB.setCheckMarkDrawable(uncheckedBackground);
                } else {
                    clearUnusedFaviconsCB.setChecked(true);
                    clearUnusedFaviconsCB.setCheckMarkDrawable(checkedBackground);
                }
            } else if(id == R.id.clearHistoryCB)
            {
                if(clearHistoryCB.isChecked())
                {
                    clearHistoryCB.setChecked(false);
                    clearHistoryCB.setCheckMarkDrawable(uncheckedBackground);
                } else {
                    clearHistoryCB.setChecked(true);
                    clearHistoryCB.setCheckMarkDrawable(checkedBackground);
                }
            } else if(id == R.id.clearSearchHistoryCB)
            {
                if(clearSearchHistoryCB.isChecked())
                {
                    clearSearchHistoryCB.setChecked(false);
                    clearSearchHistoryCB.setCheckMarkDrawable(uncheckedBackground);
                } else {
                    clearSearchHistoryCB.setChecked(true);
                    clearSearchHistoryCB.setCheckMarkDrawable(checkedBackground);
                }
            } else if(id == R.id.clearCookiesCB)
            {
                if(clearCookiesCB.isChecked())
                {
                    clearCookiesCB.setChecked(false);
                    clearCookiesCB.setCheckMarkDrawable(uncheckedBackground);
                } else {
                    clearCookiesCB.setChecked(true);
                    clearCookiesCB.setCheckMarkDrawable(checkedBackground);
                }
            } else if(id == R.id.clearCacheCB)
            {
                if(clearCacheCB.isChecked())
                {
                    clearCacheCB.setChecked(false);
                    clearCacheCB.setCheckMarkDrawable(uncheckedBackground);
                } else {
                    clearCacheCB.setChecked(true);
                    clearCacheCB.setCheckMarkDrawable(checkedBackground);
                }
            } else if(id == R.id.clearWebViewDatabaseCB)
            {
                if(clearWebViewDatabaseCB.isChecked())
                {
                    clearWebViewDatabaseCB.setChecked(false);
                    clearWebViewDatabaseCB.setCheckMarkDrawable(uncheckedBackground);
                } else {
                    clearWebViewDatabaseCB.setChecked(true);
                    clearWebViewDatabaseCB.setCheckMarkDrawable(checkedBackground);
                }
            }else if(id == R.id.clearWebStorageCB)
            {
                if(clearWebStorageCB.isChecked())
                {
                    clearWebStorageCB.setChecked(false);
                    clearWebStorageCB.setCheckMarkDrawable(uncheckedBackground);
                } else {
                    clearWebStorageCB.setChecked(true);
                    clearWebStorageCB.setCheckMarkDrawable(checkedBackground);
                }
            } else if(id == R.id.clearButton)
            {
                service = Executors.newSingleThreadExecutor();
                service.submit(new ClearRecords(clearHistoryCB.isChecked(),clearSearchHistoryCB.isChecked(),clearCookiesCB.isChecked(),
                        clearCacheCB.isChecked(),clearWebViewDatabaseCB.isChecked(),clearWebStorageCB.isChecked(),clearUnusedFaviconsCB.isChecked(),clearingLL,
                        checkTVLL,doneTVH,deletingProgressBarH,deletedIVH,closeButtonH,context));
            } else if(id == R.id.closeButtonH)
            {
                ClearRecordsSheet.this.dismiss();
            }
        };

        clearHistoryCB.setOnClickListener(onClickListener);
        clearUnusedFaviconsCB.setOnClickListener(onClickListener);
        clearSearchHistoryCB.setOnClickListener(onClickListener);
        clearCookiesCB.setOnClickListener(onClickListener);
        clearCacheCB.setOnClickListener(onClickListener);
        clearWebViewDatabaseCB.setOnClickListener(onClickListener);
        clearButton.setOnClickListener(onClickListener);
        closeButtonH.setOnClickListener(onClickListener);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if(service != null)
        {
            if(!service.isShutdown())
            {
                service.shutdown();
            }

            service = null;
        }
        super.onDismiss(dialog);
    }


    private class ClearRecords extends Thread
    {

        private final boolean clearHistoryCB;
        private final boolean clearSearchHistoryCB;
        private final boolean clearCookiesCB;
        private final boolean clearCacheCB;
        private final boolean clearWebViewDatabaseCB;
        private final boolean clearWebStorageCB;
        private final boolean clearUnusedFaviconsCB;
        private final TextView doneTVH;
        private final ProgressBar deletingProgressBarH;
        private final ImageView deletedIVH;
        private final MaterialButton closeButtonH;
        private final Context context;

        ClearRecords(boolean clearHistoryCB,boolean clearSearchHistoryCB, boolean clearCookiesCB, boolean clearCacheCB, boolean clearWebViewDatabaseCB,
                     boolean clearWebStorageCB, boolean clearUnusedFaviconsCB, LinearLayout clearingLL, LinearLayout checkTVLL,
                     TextView doneTVH, ProgressBar deletingProgressBarH, ImageView deletedIVH, MaterialButton closeButtonH, Context context)
        {
            this.clearHistoryCB = clearHistoryCB;
            this.clearSearchHistoryCB = clearSearchHistoryCB;
            this.clearCookiesCB = clearCookiesCB;
            this.clearCacheCB = clearCacheCB;
            this.clearWebViewDatabaseCB = clearWebViewDatabaseCB;
            this.clearWebStorageCB = clearWebStorageCB;
            this.clearUnusedFaviconsCB = clearUnusedFaviconsCB;
            this.doneTVH = doneTVH;
            this.deletingProgressBarH = deletingProgressBarH;
            this.deletedIVH = deletedIVH;
            this.closeButtonH = closeButtonH;
            this.context = context;

            ClearRecordsSheet.this.setCancelable(false);
            checkTVLL.setVisibility(View.GONE);
            clearingLL.setVisibility(View.VISIBLE);
            deletingProgressBarH.setVisibility(View.VISIBLE);
            deletedIVH.setScaleX(0.0f);
            deletedIVH.setScaleY(0.0f);
            deletedIVH.setAlpha(0.0f);
        }

        private void clearApplicationData()
        {
            File cache = context.getCacheDir();

            String parentFile = cache.getParent();
            if(parentFile != null)
            {
                File appDir = new File(parentFile);
                if(appDir.exists())
                {
                    String[] children = appDir.list();

                    String databasePath = context.getDatabasePath("flameDatabase").getAbsolutePath();

                    if(children != null)
                    {
                        for(String s : children)
                        {
                            if(!s.equals("favicon") && !s.equals("databases"))
                            {
                                File file = new File(appDir,s);
                                if(!file.getAbsolutePath().equals(databasePath))
                                {
                                    deleteDir(file);
                                }
                            }
                        }
                    }
                }
            }

        }

        private void deleteDir(File dir)
        {
            if(dir != null && dir.isDirectory())
            {
                String[] children = dir.list();
                if(children != null)
                {
                    for(String child : children)
                    {
                        if(!child.equals("favicon") && !child.equals("databases"))
                        {
                            deleteDir(new File(dir,child));
                        }
                    }
                }
            }

            if(dir != null)
            {
                //noinspection ResultOfMethodCallIgnored
                dir.delete();
            }
        }

        @Override
        public void run() {
            super.run();

            try {
                final DatabaseHandler db = DatabaseHandler.getInstance(context);
                try {
                    if(clearHistoryCB)
                    {
                        ArrayList<Integer> historyItemsAL = db.getAllHistoryItemsIDs();
                        for(int i = 0; i < historyItemsAL.size(); i++)
                        {
                            HistoryItem historyItem = db.getHistoryItem(historyItemsAL.get(i));
                            final String faviconPath = historyItem.getHiFaviconPath();

                            if(db.checkNotContainsFaviconInBookmarks(faviconPath) &&
                            db.checkNotContainsFaviconInQuickLinks(faviconPath) && db.checkNotContainsFaviconInHomePages(faviconPath))
                            {
                                File file = new File(faviconPath);
                                if(file.exists())
                                {
                                    //noinspection ResultOfMethodCallIgnored
                                    file.delete();
                                }
                            }

                            db.deleteHistoryItem(historyItem.getHiKeyId());

                        }
                    }
                } catch (Exception ignored) {}

                try {
                    if(clearSearchHistoryCB)
                    {
                        db.deleteSearchHistory();
                    }
                } catch (Exception ignored) {}

                try {
                    if(clearCookiesCB)
                    {
                        CookieManager cookieManager = CookieManager.getInstance();
                        cookieManager.removeAllCookies(null);
                    }
                } catch (Exception ignored) {}

                try {
                    if(clearCacheCB)
                    {
                        try {
                            clearApplicationData();
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}

                try {
                    if(clearWebViewDatabaseCB)
                    {
                        WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(context);
                        webViewDatabase.clearHttpAuthUsernamePassword();

                        if(Build.VERSION.SDK_INT < 26)
                        {
                            webViewDatabase.clearFormData();
                        }
                    }
                } catch (Exception ignored) {}

                try {
                    if(clearWebStorageCB)
                    {
                        WebStorage webStorage = WebStorage.getInstance();
                        webStorage.deleteAllData();
                    }
                } catch (Exception ignored) {}

                try {
                    if(clearUnusedFaviconsCB)
                    {
                        File root = new File(context.getFilesDir(),"favicon");
                        if(root.exists())
                        {
                            String[] children = root.list();
                            if(children != null)
                            {
                                for(String s : children)
                                {
                                    File file = new File(root,s);
                                    if(file.exists())
                                    {
                                        final String faviconPath = file.getPath();

                                        if(db.checkNotContainsFaviconInBookmarks(faviconPath) &&
                                                db.checkNotContainsFaviconInQuickLinks(faviconPath) && db.checkNotContainsFaviconInHomePages(faviconPath)
                                        && db.checkNotContainsFaviconInHistory(faviconPath))
                                        {
                                            File faviconFile = new File(faviconPath);
                                            if(faviconFile.exists())
                                            {
                                                //noinspection ResultOfMethodCallIgnored
                                                faviconFile.delete();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
            finally {

                //noinspection ConstantConditions
                getActivity().runOnUiThread(() -> {
                    deletingProgressBarH.setVisibility(View.GONE);
                    deletedIVH.setVisibility(View.VISIBLE);
                    doneTVH.setVisibility(View.VISIBLE);
                    closeButtonH.setVisibility(View.VISIBLE);
                    deletedIVH.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(175)
                            .withEndAction(() -> ClearRecordsSheet.this.setCancelable(true)).start();
                });
            }
        }
    }
}
