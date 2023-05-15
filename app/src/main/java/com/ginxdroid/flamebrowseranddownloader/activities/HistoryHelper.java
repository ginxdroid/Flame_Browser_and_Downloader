package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.CustomEditText;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.classes.TextDrawable;
import com.ginxdroid.flamebrowseranddownloader.models.HistoryItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HistoryHelper {
    private final DatabaseHandler db;
    private CustomEditText searchHistoryEditText;
    private final NormalTabsRVAdapter normalTabsRVAdapter;
    private final AppCompatActivity activity;
    private HistoryRVAdapter historyRVAdapter;

    public HistoryHelper(DatabaseHandler db, NormalTabsRVAdapter normalTabsRVAdapter, AppCompatActivity activity,
                         Context context, final ViewGroup container) {
        this.db = db;
        this.normalTabsRVAdapter = normalTabsRVAdapter;
        this.activity = activity;

        showHistoryDialog(context,container);
    }

    private class HistoryRVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {
        private final DatabaseHandler db;
        private final ArrayList<Integer> historyItems;
        private final Context context;
        private final LayoutInflater inflater;
        private final String dumpPath;
        private final ViewGroup container;

        public HistoryRVAdapter(Context context, ViewGroup container) {
            this.context = context;
            this.container = container;
            db = DatabaseHandler.getInstance(context);
            historyItems = new ArrayList<>();
            inflater = LayoutInflater.from(context);
            dumpPath = context.getFilesDir().getAbsolutePath()+ File.separator+ "favicon" + File.separator + "no_file_ABC_XYZ";
        }



        @SuppressLint("NotifyDataSetChanged")
        void setHistory()
        {
            if(historyItems.size() > 0)
            {
                historyItems.clear();
            }

            historyItems.addAll(db.getAllHistoryItemsIDs());
            notifyDataSetChanged();
        }

        @SuppressLint("NotifyDataSetChanged")
        void setSearchedHistory(String title)
        {
            if(historyItems.size() > 0)
            {
                historyItems.clear();
            }

            historyItems.addAll(db.getAllHistoryItemsIDsWithTitle(title));
            notifyDataSetChanged();
        }

        int clearAllHistory()
        {
            int size = historyItems.size();
            if(size > 0)
            {
                historyItems.clear();
            }

            return size;
        }

        void removeItem(int adapterPosition)
        {
            try {
                HistoryItem historyItem = null;
                try {
                    final Integer hId = historyItems.get(adapterPosition);
                    historyItem = db.getHistoryItem(hId);

                    final HistoryItem finalHistoryItem = historyItem;
                    new Thread(() -> {
                        final String faviconPath = finalHistoryItem.getHiFaviconPath();
                        if(db.checkNotContainsFaviconInBookmarks(faviconPath) && db.checkNotContainsFaviconInQuickLinks(faviconPath)&&
                                db.checkNotContainsFaviconInHomePages(faviconPath) && db.checkNotContainsFaviconInHistoryMore(faviconPath,hId))
                        {
                            File file = new File(faviconPath);
                            if(file.exists())
                            {
                                //noinspection ResultOfMethodCallIgnored
                                file.delete();
                            }
                        }
                    }).start();

                    db.deleteHistoryItem(hId);
                } finally {
                    historyItems.remove(adapterPosition);
                    notifyItemRemoved(adapterPosition);

                    if(historyItem != null)
                    {
                        if(db.getHistoryItemsByDateSize(historyItem.getHiDate()) == 0)
                        {
                            int id = db.getHistoryItemIdWithDate(historyItem.getHiDate());
                            db.deleteHistoryItem(id);
                            setHistory();
                        }
                    }
                }
            } catch (Exception e)
            {
                setHistory();
            }
        }

        @Override
        public int getItemViewType(int position) {
            try {
                return db.getHistoryItemType(historyItems.get(position));
            } catch (Exception e)
            {
                return -1;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType)
            {
                case 0:
                {
                    final View view = inflater.inflate(R.layout.history_date_row,parent,false);
                    return new HistoryRVAdapter.DateViewHolder(view);
                }
                case 1:
                {
                    final View view = inflater.inflate(R.layout.history_item_row,parent,false);
                    return new HistoryRVAdapter.HistoryViewHolder(view);
                }
                default:
                {
                    final View view = inflater.inflate(R.layout.empty_row,parent,false);
                    return new HistoryRVAdapter.ViewHolderEmpty(view);
                }

            }

        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int itemId = historyItems.get(position);
            HistoryItem historyItem = db.getHistoryItem(itemId);
            switch (holder.getItemViewType())
            {
                case 0:
                    if(new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(new Date(System.currentTimeMillis())).equals(historyItem.getHiDate()))
                    {
                        String finalString = "Today - "+historyItem.getHiDate();
                        ((HistoryRVAdapter.DateViewHolder)holder).hiDate.setText(finalString);
                    }else if(new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(new Date(System.currentTimeMillis() - (1000 * 60 * 60 *24)))
                            .equals(historyItem.getHiDate()))
                    {
                        String finalString = "Yesterday - "+historyItem.getHiDate();
                        ((HistoryRVAdapter.DateViewHolder)holder).hiDate.setText(finalString);
                    } else {
                        ((HistoryRVAdapter.DateViewHolder)holder).hiDate.setText(historyItem.getHiDate());
                    }
                    break;
                case 1:
                    HistoryRVAdapter.HistoryViewHolder viewHolder = (HistoryRVAdapter.HistoryViewHolder)holder;
                    viewHolder.hFaviconIV.setBackground(null);
                    viewHolder.hFaviconIV.setImageBitmap(null);
                    viewHolder.hFaviconIV.setBackgroundResource(0);

                    if(historyItem.getHiFaviconPath() != null && historyItem.getHiFaviconPath().equals("R.drawable"))
                    {
                        viewHolder.hFaviconIV.setBackgroundResource(R.drawable.splash_logo_light);
                    } else {
                        try {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inPreferredConfig = Bitmap.Config.RGB_565;
                            File file = new File(historyItem.getHiFaviconPath());
                            if(file.exists() && !historyItem.getHiFaviconPath().equals(dumpPath))
                            {
                                Bitmap bitmap = BitmapFactory.decodeFile(historyItem.getHiFaviconPath(), options);
                                viewHolder.hFaviconIV.setImageBitmap(bitmap);
                            } else
                            {
                                if(historyItem.getHiTitle().equals("No title"))
                                {
                                    viewHolder.hFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                                } else {
                                    String firstC = historyItem.getHiTitle().substring(0,1);
                                    viewHolder.hFaviconIV.setBackground(new TextDrawable(context,firstC));
                                }
                            }
                        } catch (Exception e)
                        {
                            if(historyItem.getHiTitle().equals("No title"))
                            {
                                viewHolder.hFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                            } else {
                                String firstC = historyItem.getHiTitle().substring(0,1);
                                viewHolder.hFaviconIV.setBackground(new TextDrawable(context,firstC));
                            }
                        }
                    }

                    viewHolder.hTitle.setText(historyItem.getHiTitle());
                    viewHolder.hUrl.setText(historyItem.getHiURL());
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return historyItems.size();
        }

        class ViewHolderEmpty extends RecyclerView.ViewHolder {
            public ViewHolderEmpty(@NonNull View itemView) {
                super(itemView);
            }
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {

            private final TextView hTitle,hUrl;
            private final ImageView hFaviconIV;

            public HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                hTitle = itemView.findViewById(R.id.hTitle);
                hUrl = itemView.findViewById(R.id.hUrl);
                final MaterialCardView historyRL = itemView.findViewById(R.id.historyRL);
                hFaviconIV = historyRL.findViewById(R.id.hFaviconIV);

                final ImageButton hDeleteIB = itemView.findViewById(R.id.hDeleteIB);

                View.OnClickListener onClickListener = view -> {
                    try {
                        int itemId = historyItems.get(getBindingAdapterPosition());
                        HistoryItem historyItem = db.getHistoryItem(itemId);

                        if(normalTabsRVAdapter.getViewHolder() != null)
                        {
                            normalTabsRVAdapter.set();
                            normalTabsRVAdapter.addNewTab(historyItem.getHiURL(), 6);
                        } else {
                            normalTabsRVAdapter.addNewTab(historyItem.getHiURL(),4);
                        }

                        createMessagePopup(container);
                    } catch (Exception e)
                    {
                        normalTabsRVAdapter.showToastFromMainActivity(R.string.oops_general_message);
                    }
                };

                hTitle.setOnClickListener(onClickListener);
                hUrl.setOnClickListener(onClickListener);
                historyRL.setOnClickListener(onClickListener);

                hDeleteIB.setOnClickListener(view -> removeItem(getBindingAdapterPosition()));
            }

            private void createMessagePopup(final ViewGroup container)
            {
                AlertDialog.Builder innerDialogBuilder = new AlertDialog.Builder(context);
                View view = activity.getLayoutInflater().inflate(R.layout.popup_action_message,container,false);

                TextView popupActionMessage = view.findViewById(R.id.popupActionMessage);
                popupActionMessage.setText(R.string.opened_in_background);

                innerDialogBuilder.setView(view);
                final AlertDialog innerDialog = innerDialogBuilder.create();
                Window window = innerDialog.getWindow();
                WindowManager.LayoutParams layoutParams;
                if(window != null)
                {
                    layoutParams = window.getAttributes();
                    if(layoutParams != null)
                    {
                        layoutParams.windowAnimations = R.style.DialogStyle2;
                        layoutParams.gravity = Gravity.BOTTOM;
                        window.setAttributes(layoutParams);
                    }
                }

                innerDialog.setCanceledOnTouchOutside(false);
                innerDialog.setCancelable(false);
                innerDialog.show();

                new Handler(Looper.getMainLooper()).postDelayed(innerDialog::dismiss,500);
            }

        }

        class DateViewHolder extends RecyclerView.ViewHolder {
            private final TextView hiDate;
            public DateViewHolder(@NonNull View itemView) {
                super(itemView);
                hiDate = itemView.findViewById(R.id.hiDate);
            }
        }
    }

    void showHistoryDialog(final Context context, final ViewGroup container)
    {
        try {
            final View layout = activity.getLayoutInflater().inflate(R.layout.popup_history,container,false);
            final Dialog dialog = new Dialog(context, R.style.full_screen_dialog);

            dialog.setContentView(layout);
            final Window window = dialog.getWindow();
            WindowManager.LayoutParams layoutParams;
            if(window != null)
            {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
                layoutParams = window.getAttributes();
                if(layoutParams != null)
                {
                    layoutParams.windowAnimations = R.style.ChooserStyle;
                    layoutParams.gravity = Gravity.BOTTOM;
                    window.setAttributes(layoutParams);
                }
            }

            ConstraintLayout historyContainer = layout.findViewById(R.id.historyContainer);
            RecyclerView historyRV = historyContainer.findViewById(R.id.historyRV);
            historyRV.setItemViewCacheSize(0);
            historyRVAdapter = new HistoryRVAdapter(context,container);
            historyRVAdapter.setHasStableIds(false);

            final ImageButton backButtonSearchLL;

            MaterialButton clearBrowsingData = historyContainer.findViewById(R.id.clearHistoryButton);
            final RelativeLayout searchHistoryLL = historyContainer.findViewById(R.id.searchHistoryLL);
            backButtonSearchLL = searchHistoryLL.findViewById(R.id.backButtonSearchLL);
            searchHistoryEditText = searchHistoryLL.findViewById(R.id.searchHistoryEditText);

            searchHistoryEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    Editable editable = searchHistoryEditText.getText();

                    if(HelperTextUtility.isNotEmpty(editable))
                    {
                        historyRVAdapter.setSearchedHistory(editable.toString());
                    } else {
                        historyRVAdapter.setHistory();
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });



            clearBrowsingData.setOnClickListener(view -> createDeletePopup(context, activity, container));
            backButtonSearchLL.setOnClickListener(view -> {
                historyRVAdapter.setHistory();
                Editable editable = searchHistoryEditText.getText();
                if(editable != null)
                {
                    editable.clear();
                }
            });

            ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
                private int viewHolderWidth;
                @Override
                public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    int dragFlags = 0;
                    int swipeFlags;

                    if(viewHolder.getItemViewType() == 0)
                    {
                        swipeFlags = 0;
                    } else {
                        swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                        viewHolderWidth = recyclerView.getWidth();
                        return makeMovementFlags(dragFlags,swipeFlags);
                    }

                    return makeMovementFlags(dragFlags,swipeFlags);
                }

                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                    View view = viewHolder.itemView;
                    view.setTranslationX(dX);
                    view.setAlpha(1f - (Math.abs(dX) / viewHolderWidth));
                }

                @Override
                public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    View view = viewHolder.itemView;
                    view.setTranslationX(0f);
                    view.setAlpha(1f);
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    historyRVAdapter.removeItem(viewHolder.getBindingAdapterPosition());
                }
            };

            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(historyRV);


            historyRV.setLayoutManager(new LinearLayoutManager(context));
            historyRV.setAdapter(historyRVAdapter);
            historyRVAdapter.setHistory();

            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        } catch (Exception ignored) {}
    }

    private void createDeletePopup(Context context, AppCompatActivity activity, ViewGroup container)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View view = activity.getLayoutInflater().inflate(R.layout.popup_delete_all_history, container, false);

        final MaterialButton yesButton, noButton;
        final TextView heyHTV,doYouReallyHTV,doneTVH;
        final ProgressBar deletingProgressBarH;
        final ImageView deletedIVH;
        final MaterialButton closeButtonH;
        final LinearLayout yesNoButtonsLLH;

        heyHTV = view.findViewById(R.id.heyHTV);
        doYouReallyHTV = view.findViewById(R.id.doYouReallyHTV);
        doneTVH = view.findViewById(R.id.doneTVH);
        deletingProgressBarH = view.findViewById(R.id.deletingProgressBarH);
        deletedIVH = view.findViewById(R.id.deletedIVH);
        closeButtonH = view.findViewById(R.id.closeButtonH);
        yesNoButtonsLLH = view.findViewById(R.id.yesNoButtonsLLH);
        yesButton = yesNoButtonsLLH.findViewById(R.id.yesButtonDeleteHistory);
        noButton = yesNoButtonsLLH.findViewById(R.id.noButtonDeleteHistory);

        builder.setView(view);
        final AlertDialog dialog = builder.create();

        yesButton.setOnClickListener(view1 -> new DeleteHistory(heyHTV, doYouReallyHTV, doneTVH, deletingProgressBarH, deletedIVH,
                closeButtonH, yesNoButtonsLLH, dialog).start());

        noButton.setOnClickListener(view12 -> dialog.dismiss());
        closeButtonH.setOnClickListener(view12 -> dialog.dismiss());

        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        dialog.show();
    }

    private class DeleteHistory extends Thread
    {
        private final TextView doneTVH;
        private final ProgressBar deletingProgressBarH;
        private final ImageView deletedIVH;
        private final MaterialButton closeButtonH;
        private final AlertDialog dialog;

        private DeleteHistory(TextView heyHTV,TextView doYouReallyHTV,TextView doneTVH,ProgressBar deletingProgressBarH,
                              ImageView deletedIVH, MaterialButton closeButtonH, LinearLayout yesNoButtonsLLH, AlertDialog dialog)
        {
            this.doneTVH = doneTVH;
            this.deletingProgressBarH = deletingProgressBarH;
            this.deletedIVH = deletedIVH;
            this.closeButtonH = closeButtonH;
            this.dialog = dialog;

            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            heyHTV.setVisibility(View.GONE);
            doYouReallyHTV.setVisibility(View.GONE);
            yesNoButtonsLLH.setVisibility(View.GONE);
            deletingProgressBarH.setVisibility(View.VISIBLE);
            deletedIVH.setScaleX(0.0f);
            deletedIVH.setScaleY(0.0f);
            deletedIVH.setAlpha(0.0f);
        }

        @Override
        public void run() {
            super.run();
            try {
                ArrayList<Integer> historyItemsAL = db.getAllHistoryItemsIDs();
                for(int i=0; i < historyItemsAL.size(); i++)
                {
                    HistoryItem historyItem = db.getHistoryItem(historyItemsAL.get(i));
                    final String faviconPath = historyItem.getHiFaviconPath();

                    if(db.checkNotContainsFaviconInBookmarks(faviconPath) && db.checkNotContainsFaviconInQuickLinks(faviconPath)&&
                            db.checkNotContainsFaviconInHomePages(faviconPath))
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

                int size = historyRVAdapter.clearAllHistory();
                activity.runOnUiThread(() -> historyRVAdapter.notifyItemRangeRemoved(0, size));


            } catch (Exception ignored) {}
            finally {
                activity.runOnUiThread(() -> {
                    deletingProgressBarH.setVisibility(View.GONE);
                    deletedIVH.setVisibility(View.VISIBLE);
                    doneTVH.setVisibility(View.VISIBLE);
                    closeButtonH.setVisibility(View.VISIBLE);
                    deletedIVH.animate().withLayer().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(175)
                            .withEndAction(() -> {
                                dialog.setCanceledOnTouchOutside(true);
                                dialog.setCancelable(true);
                            }).start();

                });
            }
        }
    }
}
