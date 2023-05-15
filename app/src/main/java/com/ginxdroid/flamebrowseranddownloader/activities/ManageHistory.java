package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Canvas;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.CustomEditText;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.models.HistoryItem;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;

public class ManageHistory extends AppCompatActivity implements View.OnClickListener {

    private ConstraintLayout historyContainer;
    private ManageHistoryRVAdapter manageHistoryRVAdapter;
    private RelativeLayout searchHistoryLL;
    private DatabaseHandler db;
    private RelativeLayout historyLL;
    private MaterialButton clearBrowsingData;
    private CustomEditText searchHistoryEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_history);

        db = DatabaseHandler.getInstance(ManageHistory.this);
        historyContainer = findViewById(R.id.historyContainer);

        RecyclerView historyRV = historyContainer.findViewById(R.id.historyRV);
        historyRV.setItemViewCacheSize(0);
        manageHistoryRVAdapter = new ManageHistoryRVAdapter(ManageHistory.this);
        manageHistoryRVAdapter.setHasStableIds(false);

        final ImageButton backButtonSearchLL, searchHistoryIBRL, backIBRL;

        clearBrowsingData = historyContainer.findViewById(R.id.clearHistoryButton);
        historyLL = historyContainer.findViewById(R.id.historyLL);
        searchHistoryIBRL = historyLL.findViewById(R.id.searchHistoryIB);
        backIBRL = historyLL.findViewById(R.id.backIB);
        searchHistoryLL = historyContainer.findViewById(R.id.searchHistoryLL);
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
                    manageHistoryRVAdapter.setSearchedHistory(editable.toString());
                } else {
                    manageHistoryRVAdapter.setHistory();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        clearBrowsingData.setOnClickListener(ManageHistory.this);
        searchHistoryIBRL.setOnClickListener(ManageHistory.this);
        backButtonSearchLL.setOnClickListener(ManageHistory.this);
        backIBRL.setOnClickListener(ManageHistory.this);

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
                manageHistoryRVAdapter.removeItem(viewHolder.getBindingAdapterPosition());
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(historyRV);


        historyRV.setLayoutManager(new LinearLayoutManager(ManageHistory.this));
        historyRV.setAdapter(manageHistoryRVAdapter);
        manageHistoryRVAdapter.setHistory();

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.backIB)
        {
            finish();
        } else if(id == R.id.backButtonSearchLL)
        {
            clearBrowsingData.setVisibility(View.VISIBLE);
            searchHistoryLL.setVisibility(View.GONE);
            historyLL.setVisibility(View.VISIBLE);
            manageHistoryRVAdapter.setHistory();
            Editable editable = searchHistoryEditText.getText();
            if(editable != null)
            {
                editable.clear();
            }
        } else if(id == R.id.searchHistoryIB)
        {
            clearBrowsingData.setVisibility(View.GONE);
            searchHistoryLL.setVisibility(View.VISIBLE);
            historyLL.setVisibility(View.GONE);
        } else if(id == R.id.clearHistoryButton)
        {
            createDeletePopup();
        }
    }

    private void createDeletePopup()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(ManageHistory.this);

        View view = ManageHistory.this.getLayoutInflater().inflate(R.layout.popup_delete_all_history, historyContainer, false);

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

                int size = manageHistoryRVAdapter.clearAllHistory();
                ManageHistory.this.runOnUiThread(() -> manageHistoryRVAdapter.notifyItemRangeRemoved(0, size));


            } catch (Exception ignored) {}
            finally {
                ManageHistory.this.runOnUiThread(() -> {
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

    @Override
    public void onBackPressed() {
        if(searchHistoryLL.getVisibility() == View.VISIBLE)
        {
            searchHistoryLL.findViewById(R.id.backButtonSearchLL).callOnClick();
        } else {
            super.onBackPressed();
        }
    }
}