package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.classes.URLEditText;
import com.ginxdroid.flamebrowseranddownloader.models.UserPreferences;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.DownloadTask;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialBindDownloadTask;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialEight;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialFour;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialHalfTask;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialOne;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialSix;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialSixteen;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialThirtyTwo;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialTwo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    private final ConstraintLayout mainContainer;
    private final FirstActivity firstActivity;
    private final RelativeLayout tabsRL;
    private final LinearLayout editLL;
    private final MaterialButton propertiesIB;
    private final MaterialButton refreshIB;
    private final DatabaseHandler db;

    private final ArrayList<Integer> downloadTaskIDs;
    private final LayoutInflater inflater;
    private RecyclerView recyclerView;
    private TextView popupActionMessageTV;

    private AlertDialog dialog = null;

    private int singleTaskPos = -1,currentSelectedTaskID = -1;
    private boolean showCheckBoxes = false;
    private final ArrayList<Integer> downloadingOrStartedSelectedTasksArrayList;
    private final ArrayList<Integer> pausedOrErrorSelectedTasksArrayList;

    private DeleteTasks deletionWork = null;

    public RecyclerViewAdapter(Context context, FirstActivity firstActivity, ConstraintLayout mainContainer,
                               RelativeLayout tabsRL, LinearLayout editLL, MaterialButton propertiesIB, MaterialButton refreshIB) {
        this.context = context;
        this.db = DatabaseHandler.getInstance(context);
        this.mainContainer = mainContainer;
        this.firstActivity = firstActivity;
        this.tabsRL = tabsRL;
        this.editLL = editLL;
        this.propertiesIB = propertiesIB;
        this.refreshIB = refreshIB;

        downloadTaskIDs = new ArrayList<>();
        downloadingOrStartedSelectedTasksArrayList = new ArrayList<>();
        pausedOrErrorSelectedTasksArrayList = new ArrayList<>();

        inflater = LayoutInflater.from(context);
    }

    void analyzeCurrentQueuedTasks()
    {
        final ArrayList<Integer> tempTasks = new ArrayList<>(downloadTaskIDs);
        for(Integer dTID : tempTasks)
        {
            if(db.isTaskCompleted(dTID))
            {
                int pos = downloadTaskIDs.indexOf(dTID);
                downloadTaskIDs.remove(dTID);
                notifyItemRemoved(pos);
                firstActivity.notifyCompletedTask(dTID);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    void refreshTasks()
    {
        try {
            if(dialog != null)
            {
                dialog.dismiss();
                dialog = null;
            }

            showCheckBoxes = false;

            recyclerView.removeAllViews();
            notifyDataSetChanged();
        }finally {
            if(downloadingOrStartedSelectedTasksArrayList.size() > 0)
            {
                downloadingOrStartedSelectedTasksArrayList.clear();
            }

            if(pausedOrErrorSelectedTasksArrayList.size() > 0)
            {
                pausedOrErrorSelectedTasksArrayList.clear();
            }

            firstActivity.setCountTVText(0);

            try {
                analyzeCurrentQueuedTasks();
            } finally {
                firstActivity.setQueuedCount(downloadTaskIDs.size());
                firstActivity.deselectSelectedCompleteTasks();
            }

        }
    }

    void setTasksCompletion()
    {
        try {
            if(dialog != null)
            {
                dialog.dismiss();
                dialog = null;
            }

            showCheckBoxes = false;

            for(Integer dTID : downloadingOrStartedSelectedTasksArrayList)
            {
                notifyItemChanged(downloadTaskIDs.indexOf(dTID));
            }

            for(Integer dTID : pausedOrErrorSelectedTasksArrayList)
            {
                notifyItemChanged(downloadTaskIDs.indexOf(dTID));
            }
        } finally {
            if(downloadingOrStartedSelectedTasksArrayList.size() > 0)
            {
                downloadingOrStartedSelectedTasksArrayList.clear();
            }

            if(pausedOrErrorSelectedTasksArrayList.size() > 0)
            {
                pausedOrErrorSelectedTasksArrayList.clear();
            }

            firstActivity.setCountTVText(0);

            try {
                analyzeCurrentQueuedTasks();
            } finally {
                firstActivity.setQueuedCount(downloadTaskIDs.size());
                firstActivity.deselectSelectedCompleteTasks();
            }
        }
    }

    void showProperties()
    {
       try {
            Intent intent = new Intent(context,FileDetailsActivity.class);
            intent.putExtra("id",String.valueOf(currentSelectedTaskID));
            context.startActivity(intent);
        } catch (Exception e)
        {
            Toast.makeText(context, R.string.oops_general_message, Toast.LENGTH_SHORT).show();
        }
    }

    void showRefreshAddress()
    {
        try {
            createRefreshDownloadAddressPopupDialog(currentSelectedTaskID,singleTaskPos);
        } catch (Exception e)
        {
            Toast.makeText(context, R.string.oops_general_message, Toast.LENGTH_SHORT).show();
        }finally {
            hideSelectCheckbox();
            firstActivity.setEditInvisible();
        }
    }

    void doDeletionWork()
    {
        if(deletionWork != null)
        {
            deletionWork.start();
            deletionWork = null;
        }
    }



    @SuppressLint("NotifyDataSetChanged")
    void setTasks()
    {
        if(downloadTaskIDs.size() > 0)
        {
            downloadTaskIDs.clear();
        }
        downloadTaskIDs.addAll(db.getAllDownloadTaskIDs());
        recyclerView.removeAllViews();
        notifyDataSetChanged();
        firstActivity.setQueuedCount(downloadTaskIDs.size());
    }

    void reflectChange(int dId)
    {
        notifyItemChanged(downloadTaskIDs.indexOf(dId));
    }

    void setRecyclerView(RecyclerView recyclerView)
    {
        this.recyclerView = recyclerView;
    }

    @Override
    public int getItemViewType(int position) {
        return db.getSegmentsForDownloadTask(downloadTaskIDs.get(position));
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType)
        {
            case 1: {
                final View view = inflater.inflate(R.layout.list_row_one,parent,false);
                return new RecyclerViewAdapter.ViewHolderOne(view);
            }
            case 2: {
                final View view = inflater.inflate(R.layout.list_row_two,parent,false);
                return new RecyclerViewAdapter.ViewHolderTwo(view);
            }
            case 4: {
                final View view = inflater.inflate(R.layout.list_row_four,parent,false);
                return new RecyclerViewAdapter.ViewHolderFour(view);
            }
            case 6: {
                final View view = inflater.inflate(R.layout.list_row_six,parent,false);
                return new RecyclerViewAdapter.ViewHolderSix(view);
            }
            case 8: {
                final View view = inflater.inflate(R.layout.list_row_eight,parent,false);
                return new RecyclerViewAdapter.ViewHolderEight(view);
            }
            case 16: {
                final View view = inflater.inflate(R.layout.list_row_sixteen,parent,false);
                return new RecyclerViewAdapter.ViewHolderSixteen(view);
            }
            case 32: {
                final View view = inflater.inflate(R.layout.list_row_thirty_two,parent,false);
                return new RecyclerViewAdapter.ViewHolderThirtyTwo(view);
            }
            default: {
                final View view = inflater.inflate(R.layout.empty_row,parent,false);
                return new RecyclerViewAdapter.ViewHolderEmpty(view);
            }
        }
    }

    static class ViewHolderEmpty extends RecyclerView.ViewHolder {
        public ViewHolderEmpty(@NonNull View itemView) {
            super(itemView);
        }
    }

    class BaseHolder extends RecyclerView.ViewHolder {
        final TextView fileNameTV, timeLeftTV, downloadSpeedTV,resumableOrNotTV;
        final ImageButton startPauseIB;
        final MaterialCardView mcv;


        public BaseHolder(@NonNull View itemView) {
            super(itemView);

            mcv = itemView.findViewById(R.id.mcv);
            fileNameTV = mcv.findViewById(R.id.fileName);
            timeLeftTV = mcv.findViewById(R.id.timeLeft);
            downloadSpeedTV = mcv.findViewById(R.id.downloadSpeed);
            resumableOrNotTV = mcv.findViewById(R.id.resumableOrNotTV);
            startPauseIB = mcv.findViewById(R.id.startPauseIB);

            final View.OnClickListener onClickListener = view -> {
                final int pos = getBindingAdapterPosition();
                final int taskID = downloadTaskIDs.get(pos);

                if(showCheckBoxes)
                {

                    if(!mcv.isChecked())
                    {
                        mcv.setChecked(true);
                        int currentStatus = db.getCurrentStatusOfDT(downloadTaskIDs.get(pos));
                        if(currentStatus == 2 || currentStatus == 1 || currentStatus == 6)
                        {
                            downloadingOrStartedSelectedTasksArrayList.add(taskID);
                        } else {
                            pausedOrErrorSelectedTasksArrayList.add(taskID);

                            try {
                                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                if(notificationManager != null)
                                {
                                    notificationManager.cancel(taskID);
                                }
                            } catch (Exception ignored) {}
                        }
                    } else {
                        mcv.setChecked(false);

                        if(downloadingOrStartedSelectedTasksArrayList.contains(taskID))
                        {
                            downloadingOrStartedSelectedTasksArrayList.remove((Integer)taskID);
                        } else if(pausedOrErrorSelectedTasksArrayList.contains(taskID)) {
                            pausedOrErrorSelectedTasksArrayList.remove((Integer)taskID);
                        }
                    }

                    final int downloadingOrStartedSize = downloadingOrStartedSelectedTasksArrayList.size();
                    final int size = downloadingOrStartedSize + pausedOrErrorSelectedTasksArrayList.size();

                    firstActivity.setCountTVText(size);

                    if(size > 1)
                    {
                        propertiesIB.setVisibility(View.INVISIBLE);
                        refreshIB.setVisibility(View.INVISIBLE);
                    } else if(size < 1) {
                        showCheckBoxes = false;
                        firstActivity.setEditInvisible();
                    } else {
                        if(downloadingOrStartedSize > 0)
                        {
                            currentSelectedTaskID = downloadingOrStartedSelectedTasksArrayList.get(0);
                        } else {
                            currentSelectedTaskID = pausedOrErrorSelectedTasksArrayList.get(0);
                        }

                        singleTaskPos = downloadTaskIDs.indexOf(currentSelectedTaskID);
                        propertiesIB.setVisibility(View.VISIBLE);
                        if(db.getPauseResumeSupported(currentSelectedTaskID) == 0)
                        {
                            refreshIB.setVisibility(View.INVISIBLE);
                        } else {
                            refreshIB.setVisibility(View.VISIBLE);
                        }
                    }


                }
                else {
                    pauseOrResumeDownloadOrError(db.getDownloadTask(taskID),startPauseIB,pos);
                }
            };
            startPauseIB.setOnClickListener(onClickListener);
            mcv.setOnClickListener(onClickListener);

            mcv.setOnLongClickListener(view -> {

                if(!mcv.isChecked())
                {
                    showCheckBoxes = true;
                    editLL.setVisibility(View.VISIBLE);
                    tabsRL.setVisibility(View.GONE);
                    propertiesIB.setVisibility(View.VISIBLE);

                    final int pos = getBindingAdapterPosition();
                    currentSelectedTaskID = downloadTaskIDs.get(pos);
                    singleTaskPos = pos;

                    if(db.getPauseResumeSupported(currentSelectedTaskID) == 0)
                    {
                        refreshIB.setVisibility(View.INVISIBLE);
                    } else {
                        refreshIB.setVisibility(View.VISIBLE);
                    }

                    mcv.setChecked(true);
                    int currentStatus = db.getCurrentStatusOfDT(downloadTaskIDs.get(pos));
                    if(currentStatus == 2 || currentStatus == 1 || currentStatus == 6)
                    {
                        downloadingOrStartedSelectedTasksArrayList.add(downloadTaskIDs.get(pos));
                    } else {
                        int dTID = downloadTaskIDs.get(pos);
                        pausedOrErrorSelectedTasksArrayList.add(dTID);

                        try {
                            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            if(notificationManager != null)
                            {
                                notificationManager.cancel(dTID);
                            }
                        } catch (Exception ignored) {}
                    }

                    firstActivity.setCountTVText(downloadingOrStartedSelectedTasksArrayList.size() +
                            pausedOrErrorSelectedTasksArrayList.size());

                }

                return true;
            });
        }

        private void pauseOrResumeDownloadOrError(DownloadTask downloadTask, ImageButton startStopPauseIB, int position)
        {
            switch (downloadTask.getCurrentStatus())
            {
                case 1:
                case 3:
                case 6:
                case 2:
                {
                    //Pause download
                    Intent intent = new Intent(context, DownloadingService.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("dStatus","Pause");
                    bundle.putInt("dId",downloadTask.getKeyId());
                    intent.putExtras(bundle);
                    context.startService(intent);
                    startStopPauseIB.setBackgroundResource(R.drawable.start_background);
                    break;
                }
                case 4:
                {
                    //Resume download
                    startStopPauseIB.setBackgroundResource(R.drawable.round_pause_24);
                    db.updateDownloadTaskNA(downloadTask.getKeyId(),1,"Queued","-");

                    Intent intent = new Intent(context, DownloadingService.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("dStatus","ResumeFromRv");
                    bundle.putInt("dId",downloadTask.getKeyId());
                    intent.putExtras(bundle);

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    {
                        context.startForegroundService(intent);
                    } else {
                        context.startService(intent);
                    }
                    notifyItemChanged(position);
                    break;
                }
                case 5:
                {
                    startStopPauseIB.setBackgroundResource(R.drawable.error_background);
                    createErrorDetailsPopupDialog(downloadTask.getKeyId(),position,startStopPauseIB);
                }
            }
        }

        private void createErrorDetailsPopupDialog(final int dTID, final int position,ImageButton startPauseIB)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = inflater.inflate(R.layout.popup_error_details,mainContainer,false);
            builder.setView(view);
            final AlertDialog dialog = builder.create();
            
            final TextView fileNameErrorDetailsPopupTV,errorErrorDetailsPopupTV,youMayErrorDetailsPopupTV;
            fileNameErrorDetailsPopupTV = view.findViewById(R.id.fileNameErrorDetailsPopupTV);
            errorErrorDetailsPopupTV = view.findViewById(R.id.errorErrorDetailsPopupTV);
            youMayErrorDetailsPopupTV = view.findViewById(R.id.youMayErrorDetailsPopupTV);
            
            final MaterialButton retryButtonErrorDetailsPopup,startFromBeginningErrorDetailsPopup,refreshDownloadAddressErrorDetailsPopup,
                    closeButtonErrorDetailsPopup;
            retryButtonErrorDetailsPopup = view.findViewById(R.id.retryButtonErrorDetailsPopup);
            startFromBeginningErrorDetailsPopup = view.findViewById(R.id.startFromBeginningErrorDetailsPopup);
            refreshDownloadAddressErrorDetailsPopup = view.findViewById(R.id.refreshDownloadAddressErrorDetailsPopup);
            closeButtonErrorDetailsPopup = view.findViewById(R.id.closeButtonErrorDetailsPopup);
            
            final DownloadTask downloadTask = db.getDownloadTask(dTID);
            fileNameErrorDetailsPopupTV.setText(downloadTask.getFileName());
            
            switch (downloadTask.getWhichError())
            {
                case "MalformedURLException":
                {
                    errorErrorDetailsPopupTV.setText(R.string.file_not_found_on_url);
                    youMayErrorDetailsPopupTV.setText(R.string.you_may);
                    refreshDownloadAddressErrorDetailsPopup.setVisibility(View.VISIBLE);
                    startFromBeginningErrorDetailsPopup.setVisibility(View.GONE);
                    retryButtonErrorDetailsPopup.setVisibility(View.VISIBLE);
                    break;
                }
                case "NetworkInterruptedAndPRNOException":
                case "PauseResumeNotSupportedException":
                {
                    errorErrorDetailsPopupTV.setText(R.string.pause_resume_not_supported);
                    youMayErrorDetailsPopupTV.setText(R.string.you_may);
                    refreshDownloadAddressErrorDetailsPopup.setVisibility(View.GONE);
                    startFromBeginningErrorDetailsPopup.setVisibility(View.VISIBLE);
                    retryButtonErrorDetailsPopup.setVisibility(View.GONE);
                    break;
                }
                case "OutOfSpaceException":
                {
                    errorErrorDetailsPopupTV.setText(R.string.insufficient_device_storage);
                    youMayErrorDetailsPopupTV.setText(R.string.you_may_delete_some_files_from_storage_and_try_again);
                    refreshDownloadAddressErrorDetailsPopup.setVisibility(View.GONE);
                    startFromBeginningErrorDetailsPopup.setVisibility(View.GONE);
                    retryButtonErrorDetailsPopup.setVisibility(View.VISIBLE);
                    break;
                }
                case "ConnectionTimedOutException":
                {
                    errorErrorDetailsPopupTV.setText(R.string.connection_time_out);
                    youMayErrorDetailsPopupTV.setText(R.string.you_may);
                    refreshDownloadAddressErrorDetailsPopup.setVisibility(View.VISIBLE);
                    startFromBeginningErrorDetailsPopup.setVisibility(View.GONE);
                    retryButtonErrorDetailsPopup.setVisibility(View.VISIBLE);
                    break;
                }
                case "ServerTemporarilyUnavailable":
                {
                    errorErrorDetailsPopupTV.setText(R.string.server_temporarily_unavailable);
                    youMayErrorDetailsPopupTV.setText(R.string.you_may);
                    refreshDownloadAddressErrorDetailsPopup.setVisibility(View.VISIBLE);
                    startFromBeginningErrorDetailsPopup.setVisibility(View.GONE);
                    retryButtonErrorDetailsPopup.setVisibility(View.VISIBLE);
                    break;
                }
                case "DirectoryNotFoundException":
                {
                    errorErrorDetailsPopupTV.setText(R.string.download_directory_not_found);
                    youMayErrorDetailsPopupTV.setVisibility(View.GONE);
                    refreshDownloadAddressErrorDetailsPopup.setVisibility(View.GONE);
                    startFromBeginningErrorDetailsPopup.setVisibility(View.GONE);
                    retryButtonErrorDetailsPopup.setVisibility(View.VISIBLE);
                    break;
                }
                case "SeveralRetriesException":
                {
                    errorErrorDetailsPopupTV.setText(R.string.failed_after_several_retries);
                    youMayErrorDetailsPopupTV.setText(R.string.you_may);
                    refreshDownloadAddressErrorDetailsPopup.setVisibility(View.VISIBLE);
                    startFromBeginningErrorDetailsPopup.setVisibility(View.GONE);
                    retryButtonErrorDetailsPopup.setVisibility(View.VISIBLE);
                    break;
                }
                case "UnknownError":
                {
                    errorErrorDetailsPopupTV.setText(R.string.unknown_error);
                    youMayErrorDetailsPopupTV.setText(R.string.you_may);
                    refreshDownloadAddressErrorDetailsPopup.setVisibility(View.VISIBLE);
                    startFromBeginningErrorDetailsPopup.setVisibility(View.GONE);
                    retryButtonErrorDetailsPopup.setVisibility(View.VISIBLE);
                    break;
                }
            }

            View.OnClickListener onClickListener = view1 -> {
                int id = view1.getId();
                if(id == R.id.retryButtonErrorDetailsPopup)
                {
                    //Resume download
                    db.updateDownloadTaskNA(downloadTask.getKeyId(),3,"Starting","-");

                    //Resume download
                    startPauseIB.setBackgroundResource(R.drawable.round_pause_24);

                    Intent intent = new Intent(context, DownloadingService.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("dStatus","ResumeFromRv");
                    bundle.putInt("dId",downloadTask.getKeyId());
                    intent.putExtras(bundle);

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    {
                        context.startForegroundService(intent);
                    } else {
                        context.startService(intent);
                    }
                    notifyItemChanged(position);
                    dialog.dismiss();

                    final androidx.appcompat.app.AlertDialog innerDialog = createNormalMessagePopup(R.string.retrying);
                    new Handler(Looper.getMainLooper()).postDelayed(innerDialog::dismiss,500);
                } else if (id == R.id.startFromBeginningErrorDetailsPopup) {
                    startAgainFromBeginning(downloadTask,position,dialog);
                } else if (id == R.id.refreshDownloadAddressErrorDetailsPopup) {
                    dialog.dismiss();
                    createRefreshDownloadAddressPopupDialog(dTID, position);
                } else if (id == R.id.closeButtonErrorDetailsPopup) {
                    dialog.dismiss();
                }
            };

            retryButtonErrorDetailsPopup.setOnClickListener(onClickListener);
            startFromBeginningErrorDetailsPopup.setOnClickListener(onClickListener);
            refreshDownloadAddressErrorDetailsPopup.setOnClickListener(onClickListener);
            closeButtonErrorDetailsPopup.setOnClickListener(onClickListener);

            dialog.show();

        }

        private void startAgainFromBeginning(DownloadTask downloadTask, int position, Dialog dialog)
        {
            UserPreferences userPreferences = db.getHalfUserPreferences();

            DownloadTask freshDownloadTask = db.getDownloadTask(downloadTask.getKeyId());

            freshDownloadTask.setFileName(downloadTask.getFileName());
            freshDownloadTask.setUrl(downloadTask.getUrl());
            long contentLength = downloadTask.getTotalBytes();
            freshDownloadTask.setTotalBytes(contentLength);

            if(contentLength == -1 || contentLength == 0)
            {
                freshDownloadTask.setChunkMode(1);
            } else {
                freshDownloadTask.setChunkMode(0);
            }

            freshDownloadTask.setDirPath(userPreferences.getDownloadPath());
            freshDownloadTask.setDownloadedBytes(0L);
            freshDownloadTask.setCurrentStatus(2);
            freshDownloadTask.setCurrentProgress(0);
            freshDownloadTask.setDownloadSpeed("Starting");
            freshDownloadTask.setTimeLeft("-");
            String pauseRSStatus = downloadTask.getPauseResumeSupported();
            freshDownloadTask.setPauseResumeSupported(pauseRSStatus);
            freshDownloadTask.setWhichError("NotAny");
            freshDownloadTask.setIsPauseResumeSupported(downloadTask.getIsPauseResumeSupported());

            freshDownloadTask.setTPB1(0);
            freshDownloadTask.setTPB2(0);
            freshDownloadTask.setTPB3(0);
            freshDownloadTask.setTPB4(0);
            freshDownloadTask.setTPB5(0);
            freshDownloadTask.setTPB6(0);
            freshDownloadTask.setTPB7(0);
            freshDownloadTask.setTPB8(0);
            freshDownloadTask.setTPB9(0);
            freshDownloadTask.setTPB10(0);
            freshDownloadTask.setTPB11(0);
            freshDownloadTask.setTPB12(0);
            freshDownloadTask.setTPB13(0);
            freshDownloadTask.setTPB14(0);
            freshDownloadTask.setTPB15(0);
            freshDownloadTask.setTPB16(0);
            freshDownloadTask.setTPB17(0);
            freshDownloadTask.setTPB18(0);
            freshDownloadTask.setTPB19(0);
            freshDownloadTask.setTPB20(0);
            freshDownloadTask.setTPB21(0);
            freshDownloadTask.setTPB22(0);
            freshDownloadTask.setTPB23(0);
            freshDownloadTask.setTPB24(0);
            freshDownloadTask.setTPB25(0);
            freshDownloadTask.setTPB26(0);
            freshDownloadTask.setTPB27(0);
            freshDownloadTask.setTPB28(0);
            freshDownloadTask.setTPB29(0);
            freshDownloadTask.setTPB30(0);
            freshDownloadTask.setTPB31(0);
            freshDownloadTask.setTPB32(0);

            freshDownloadTask.setTSS1(0L);
            freshDownloadTask.setTSS2(0L);
            freshDownloadTask.setTSS3(0L);
            freshDownloadTask.setTSS4(0L);
            freshDownloadTask.setTSS5(0L);
            freshDownloadTask.setTSS6(0L);
            freshDownloadTask.setTSS7(0L);
            freshDownloadTask.setTSS8(0L);
            freshDownloadTask.setTSS9(0L);
            freshDownloadTask.setTSS10(0L);
            freshDownloadTask.setTSS11(0L);
            freshDownloadTask.setTSS12(0L);
            freshDownloadTask.setTSS13(0L);
            freshDownloadTask.setTSS14(0L);
            freshDownloadTask.setTSS15(0L);
            freshDownloadTask.setTSS16(0L);
            freshDownloadTask.setTSS17(0L);
            freshDownloadTask.setTSS18(0L);
            freshDownloadTask.setTSS19(0L);
            freshDownloadTask.setTSS20(0L);
            freshDownloadTask.setTSS21(0L);
            freshDownloadTask.setTSS22(0L);
            freshDownloadTask.setTSS23(0L);
            freshDownloadTask.setTSS24(0L);
            freshDownloadTask.setTSS25(0L);
            freshDownloadTask.setTSS26(0L);
            freshDownloadTask.setTSS27(0L);
            freshDownloadTask.setTSS28(0L);
            freshDownloadTask.setTSS29(0L);
            freshDownloadTask.setTSS30(0L);
            freshDownloadTask.setTSS31(0L);
            freshDownloadTask.setTSS32(0L);

            db.updateDownloadTask(freshDownloadTask);

            dialog.dismiss();

            Intent intent = new Intent(context, DownloadingService.class);
            Bundle bundle1 = new Bundle();
            bundle1.putInt("dId",downloadTask.getKeyId());
            bundle1.putString("dStatus","downloadNow");
            intent.putExtras(bundle1);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                firstActivity.startForegroundService(intent);
            } else {
                firstActivity.startService(intent);
            }

            notifyItemChanged(position);

            final androidx.appcompat.app.AlertDialog innerDialog = createNormalMessagePopup(R.string.retrying);
            new Handler(Looper.getMainLooper()).postDelayed(innerDialog::dismiss,500);
        }
    }

    class ViewHolderOne extends BaseHolder {
        private final ProgressBar tProgressBar1;

        public ViewHolderOne(@NonNull View itemView) {
            super(itemView);
            tProgressBar1 = itemView.findViewById(R.id.tProgressBar1);
        }
    }

    class ViewHolderTwo extends BaseHolder {
        private final ProgressBar tProgressBar1,tProgressBar2;

        public ViewHolderTwo(@NonNull View itemView) {
            super(itemView);
            final LinearLayout pbContainer = itemView.findViewById(R.id.pbContainer);
            tProgressBar1 = pbContainer.findViewById(R.id.tProgressBar1);
            tProgressBar2 = pbContainer.findViewById(R.id.tProgressBar2);
        }
    }

    class ViewHolderFour extends BaseHolder {
        private final ProgressBar tProgressBar1,tProgressBar2,tProgressBar3,tProgressBar4;

        public ViewHolderFour(@NonNull View itemView) {
            super(itemView);
            final LinearLayout pbContainer = itemView.findViewById(R.id.pbContainer);
            tProgressBar1 = pbContainer.findViewById(R.id.tProgressBar1);
            tProgressBar2 = pbContainer.findViewById(R.id.tProgressBar2);
            tProgressBar3 = pbContainer.findViewById(R.id.tProgressBar3);
            tProgressBar4 = pbContainer.findViewById(R.id.tProgressBar4);
        }
    }

    class ViewHolderSix extends BaseHolder {
        private final ProgressBar tProgressBar1,tProgressBar2,tProgressBar3,tProgressBar4,tProgressBar5,tProgressBar6;

        public ViewHolderSix(@NonNull View itemView) {
            super(itemView);
            final LinearLayout pbContainer = itemView.findViewById(R.id.pbContainer);

            tProgressBar1 = pbContainer.findViewById(R.id.tProgressBar1);
            tProgressBar2 = pbContainer.findViewById(R.id.tProgressBar2);
            tProgressBar3 = pbContainer.findViewById(R.id.tProgressBar3);
            tProgressBar4 = pbContainer.findViewById(R.id.tProgressBar4);
            tProgressBar5 = pbContainer.findViewById(R.id.tProgressBar5);
            tProgressBar6 = pbContainer.findViewById(R.id.tProgressBar6);
        }
    }

    class ViewHolderEight extends BaseHolder {
        private final ProgressBar tProgressBar1,tProgressBar2,tProgressBar3,tProgressBar4,tProgressBar5,tProgressBar6,tProgressBar7,tProgressBar8;

        public ViewHolderEight(@NonNull View itemView) {
            super(itemView);
            final LinearLayout pbContainer = itemView.findViewById(R.id.pbContainer);

            tProgressBar1 = pbContainer.findViewById(R.id.tProgressBar1);
            tProgressBar2 = pbContainer.findViewById(R.id.tProgressBar2);
            tProgressBar3 = pbContainer.findViewById(R.id.tProgressBar3);
            tProgressBar4 = pbContainer.findViewById(R.id.tProgressBar4);
            tProgressBar5 = pbContainer.findViewById(R.id.tProgressBar5);
            tProgressBar6 = pbContainer.findViewById(R.id.tProgressBar6);
            tProgressBar7 = pbContainer.findViewById(R.id.tProgressBar7);
            tProgressBar8 = pbContainer.findViewById(R.id.tProgressBar8);
        }
    }

    class ViewHolderSixteen extends BaseHolder {
        private final ProgressBar tProgressBar1,tProgressBar2,tProgressBar3,tProgressBar4,tProgressBar5,tProgressBar6,tProgressBar7,tProgressBar8,
                tProgressBar9,tProgressBar10,tProgressBar11,tProgressBar12,tProgressBar13,tProgressBar14,tProgressBar15,tProgressBar16;

        public ViewHolderSixteen(@NonNull View itemView) {
            super(itemView);
            final LinearLayout pbContainer = itemView.findViewById(R.id.pbContainer);

            tProgressBar1 = pbContainer.findViewById(R.id.tProgressBar1);
            tProgressBar2 = pbContainer.findViewById(R.id.tProgressBar2);
            tProgressBar3 = pbContainer.findViewById(R.id.tProgressBar3);
            tProgressBar4 = pbContainer.findViewById(R.id.tProgressBar4);
            tProgressBar5 = pbContainer.findViewById(R.id.tProgressBar5);
            tProgressBar6 = pbContainer.findViewById(R.id.tProgressBar6);
            tProgressBar7 = pbContainer.findViewById(R.id.tProgressBar7);
            tProgressBar8 = pbContainer.findViewById(R.id.tProgressBar8);
            tProgressBar9 = pbContainer.findViewById(R.id.tProgressBar9);
            tProgressBar10 = pbContainer.findViewById(R.id.tProgressBar10);
            tProgressBar11 = pbContainer.findViewById(R.id.tProgressBar11);
            tProgressBar12 = pbContainer.findViewById(R.id.tProgressBar12);
            tProgressBar13 = pbContainer.findViewById(R.id.tProgressBar13);
            tProgressBar14 = pbContainer.findViewById(R.id.tProgressBar14);
            tProgressBar15 = pbContainer.findViewById(R.id.tProgressBar15);
            tProgressBar16 = pbContainer.findViewById(R.id.tProgressBar16);
        }
    }

    class ViewHolderThirtyTwo extends BaseHolder {
        private final ProgressBar tProgressBar1,tProgressBar2,tProgressBar3,tProgressBar4,tProgressBar5,tProgressBar6,tProgressBar7,tProgressBar8,
                tProgressBar9,tProgressBar10,tProgressBar11,tProgressBar12,tProgressBar13,tProgressBar14,tProgressBar15,tProgressBar16,
                tProgressBar17,tProgressBar18,tProgressBar19,tProgressBar20,tProgressBar21,tProgressBar22,tProgressBar23,tProgressBar24,
                tProgressBar25,tProgressBar26,tProgressBar27,tProgressBar28,tProgressBar29,tProgressBar30,tProgressBar31,tProgressBar32;

        public ViewHolderThirtyTwo(@NonNull View itemView) {
            super(itemView);
            final LinearLayout pbContainer = itemView.findViewById(R.id.pbContainer);

            tProgressBar1 = pbContainer.findViewById(R.id.tProgressBar1);
            tProgressBar2 = pbContainer.findViewById(R.id.tProgressBar2);
            tProgressBar3 = pbContainer.findViewById(R.id.tProgressBar3);
            tProgressBar4 = pbContainer.findViewById(R.id.tProgressBar4);
            tProgressBar5 = pbContainer.findViewById(R.id.tProgressBar5);
            tProgressBar6 = pbContainer.findViewById(R.id.tProgressBar6);
            tProgressBar7 = pbContainer.findViewById(R.id.tProgressBar7);
            tProgressBar8 = pbContainer.findViewById(R.id.tProgressBar8);
            tProgressBar9 = pbContainer.findViewById(R.id.tProgressBar9);
            tProgressBar10 = pbContainer.findViewById(R.id.tProgressBar10);
            tProgressBar11 = pbContainer.findViewById(R.id.tProgressBar11);
            tProgressBar12 = pbContainer.findViewById(R.id.tProgressBar12);
            tProgressBar13 = pbContainer.findViewById(R.id.tProgressBar13);
            tProgressBar14 = pbContainer.findViewById(R.id.tProgressBar14);
            tProgressBar15 = pbContainer.findViewById(R.id.tProgressBar15);
            tProgressBar16 = pbContainer.findViewById(R.id.tProgressBar16);
            tProgressBar17 = pbContainer.findViewById(R.id.tProgressBar17);
            tProgressBar18 = pbContainer.findViewById(R.id.tProgressBar18);
            tProgressBar19 = pbContainer.findViewById(R.id.tProgressBar19);
            tProgressBar20 = pbContainer.findViewById(R.id.tProgressBar20);
            tProgressBar21 = pbContainer.findViewById(R.id.tProgressBar21);
            tProgressBar22 = pbContainer.findViewById(R.id.tProgressBar22);
            tProgressBar23 = pbContainer.findViewById(R.id.tProgressBar23);
            tProgressBar24 = pbContainer.findViewById(R.id.tProgressBar24);
            tProgressBar25 = pbContainer.findViewById(R.id.tProgressBar25);
            tProgressBar26 = pbContainer.findViewById(R.id.tProgressBar26);
            tProgressBar27 = pbContainer.findViewById(R.id.tProgressBar27);
            tProgressBar28 = pbContainer.findViewById(R.id.tProgressBar28);
            tProgressBar29 = pbContainer.findViewById(R.id.tProgressBar29);
            tProgressBar30 = pbContainer.findViewById(R.id.tProgressBar30);
            tProgressBar31 = pbContainer.findViewById(R.id.tProgressBar31);
            tProgressBar32 = pbContainer.findViewById(R.id.tProgressBar32);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = holder.getItemViewType();
        if(type == 1)
        {
            doPostWorkOne(db.getBindDownloadTask(downloadTaskIDs.get(position)),(ViewHolderOne)holder);
        } else if (type == 2) {
            doPostWorkTwo(db.getBindDownloadTask(downloadTaskIDs.get(position)),(ViewHolderTwo)holder);
        } else if (type == 4) {
            doPostWorkFour(db.getBindDownloadTask(downloadTaskIDs.get(position)),(ViewHolderFour)holder);
        } else if (type == 6) {
            doPostWorkSix(db.getBindDownloadTask(downloadTaskIDs.get(position)),(ViewHolderSix)holder);
        } else if (type == 8) {
            doPostWorkEight(db.getBindDownloadTask(downloadTaskIDs.get(position)),(ViewHolderEight)holder);
        } else if (type == 16) {
            doPostWorkSixteen(db.getBindDownloadTask(downloadTaskIDs.get(position)),(ViewHolderSixteen)holder);
        } else if (type == 32) {
            doPostWorkThirtyTwo(db.getBindDownloadTask(downloadTaskIDs.get(position)),(ViewHolderThirtyTwo)holder);
        }

    }

    private void doPostWorkOne(PartialBindDownloadTask downloadTask, RecyclerViewAdapter.ViewHolderOne viewHolder)
    {
        if(showCheckBoxes)
        {
            viewHolder.mcv.setChecked(downloadingOrStartedSelectedTasksArrayList.contains(downloadTask.getKeyId()) ||
                    pausedOrErrorSelectedTasksArrayList.contains(downloadTask.getKeyId()));
        } else {
            viewHolder.mcv.setChecked(false);
        }

        viewHolder.fileNameTV.setText(downloadTask.getFileName());
        viewHolder.timeLeftTV.setText(downloadTask.getTimeLeft());
        viewHolder.downloadSpeedTV.setText(downloadTask.getDownloadSpeed());
        viewHolder.resumableOrNotTV.setText(downloadTask.getPauseResumeSupported());

        switch (downloadTask.getCurrentStatus())
        {
            case 0:
            case 1:
            case 2:
            case 3:
            case 6:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.round_pause_24);
                break;
            case 4:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.start_background);
                break;
            case 5:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.error_background);
                break;
        }

        singleSegment(viewHolder,downloadTask.getKeyId());
    }


    private void doPostWorkTwo(PartialBindDownloadTask downloadTask, RecyclerViewAdapter.ViewHolderTwo viewHolder)
    {
        if(showCheckBoxes)
        {
            viewHolder.mcv.setChecked(downloadingOrStartedSelectedTasksArrayList.contains(downloadTask.getKeyId()) ||
                    pausedOrErrorSelectedTasksArrayList.contains(downloadTask.getKeyId()));
        } else {
            viewHolder.mcv.setChecked(false);
        }

        viewHolder.fileNameTV.setText(downloadTask.getFileName());
        viewHolder.timeLeftTV.setText(downloadTask.getTimeLeft());
        viewHolder.downloadSpeedTV.setText(downloadTask.getDownloadSpeed());
        viewHolder.resumableOrNotTV.setText(downloadTask.getPauseResumeSupported());

        switch (downloadTask.getCurrentStatus())
        {
            case 0:
            case 1:
            case 2:
            case 3:
            case 6:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.round_pause_24);
                break;
            case 4:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.start_background);
                break;
            case 5:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.error_background);
                break;
        }

        twoSegments(viewHolder,downloadTask.getKeyId());
    }

    private void doPostWorkFour(PartialBindDownloadTask downloadTask, RecyclerViewAdapter.ViewHolderFour viewHolder)
    {

        if(showCheckBoxes)
        {
            viewHolder.mcv.setChecked(downloadingOrStartedSelectedTasksArrayList.contains(downloadTask.getKeyId()) ||
                    pausedOrErrorSelectedTasksArrayList.contains(downloadTask.getKeyId()));
        } else {
            viewHolder.mcv.setChecked(false);
        }

        viewHolder.fileNameTV.setText(downloadTask.getFileName());
        viewHolder.timeLeftTV.setText(downloadTask.getTimeLeft());
        viewHolder.downloadSpeedTV.setText(downloadTask.getDownloadSpeed());
        viewHolder.resumableOrNotTV.setText(downloadTask.getPauseResumeSupported());

        switch (downloadTask.getCurrentStatus())
        {
            case 0:
            case 1:
            case 2:
            case 3:
            case 6:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.round_pause_24);
                break;
            case 4:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.start_background);
                break;
            case 5:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.error_background);
                break;
        }

        fourSegments(viewHolder,downloadTask.getKeyId());
    }

    private void doPostWorkSix(PartialBindDownloadTask downloadTask, RecyclerViewAdapter.ViewHolderSix viewHolder)
    {

        if(showCheckBoxes)
        {
            viewHolder.mcv.setChecked(downloadingOrStartedSelectedTasksArrayList.contains(downloadTask.getKeyId()) ||
                    pausedOrErrorSelectedTasksArrayList.contains(downloadTask.getKeyId()));
        } else {
            viewHolder.mcv.setChecked(false);
        }

        viewHolder.fileNameTV.setText(downloadTask.getFileName());
        viewHolder.timeLeftTV.setText(downloadTask.getTimeLeft());
        viewHolder.downloadSpeedTV.setText(downloadTask.getDownloadSpeed());
        viewHolder.resumableOrNotTV.setText(downloadTask.getPauseResumeSupported());

        switch (downloadTask.getCurrentStatus())
        {
            case 0:
            case 1:
            case 2:
            case 3:
            case 6:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.round_pause_24);
                break;
            case 4:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.start_background);
                break;
            case 5:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.error_background);
                break;
        }

        sixSegments(viewHolder,downloadTask.getKeyId());
    }

    private void doPostWorkEight(PartialBindDownloadTask downloadTask, RecyclerViewAdapter.ViewHolderEight viewHolder)
    {

        if(showCheckBoxes)
        {
            viewHolder.mcv.setChecked(downloadingOrStartedSelectedTasksArrayList.contains(downloadTask.getKeyId()) ||
                    pausedOrErrorSelectedTasksArrayList.contains(downloadTask.getKeyId()));
        } else {
            viewHolder.mcv.setChecked(false);
        }

        viewHolder.fileNameTV.setText(downloadTask.getFileName());
        viewHolder.timeLeftTV.setText(downloadTask.getTimeLeft());
        viewHolder.downloadSpeedTV.setText(downloadTask.getDownloadSpeed());
        viewHolder.resumableOrNotTV.setText(downloadTask.getPauseResumeSupported());

        switch (downloadTask.getCurrentStatus())
        {
            case 0:
            case 1:
            case 2:
            case 3:
            case 6:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.round_pause_24);
                break;
            case 4:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.start_background);
                break;
            case 5:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.error_background);
                break;
        }

        eightSegments(viewHolder,downloadTask.getKeyId());
    }

    private void doPostWorkSixteen(PartialBindDownloadTask downloadTask, RecyclerViewAdapter.ViewHolderSixteen viewHolder)
    {

        if(showCheckBoxes)
        {
            viewHolder.mcv.setChecked(downloadingOrStartedSelectedTasksArrayList.contains(downloadTask.getKeyId()) ||
                    pausedOrErrorSelectedTasksArrayList.contains(downloadTask.getKeyId()));
        } else {
            viewHolder.mcv.setChecked(false);
        }

        viewHolder.fileNameTV.setText(downloadTask.getFileName());
        viewHolder.timeLeftTV.setText(downloadTask.getTimeLeft());
        viewHolder.downloadSpeedTV.setText(downloadTask.getDownloadSpeed());
        viewHolder.resumableOrNotTV.setText(downloadTask.getPauseResumeSupported());

        switch (downloadTask.getCurrentStatus())
        {
            case 0:
            case 1:
            case 2:
            case 3:
            case 6:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.round_pause_24);
                break;
            case 4:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.start_background);
                break;
            case 5:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.error_background);
                break;
        }

        sixteenSegments(viewHolder,downloadTask.getKeyId());
    }

    private void doPostWorkThirtyTwo(PartialBindDownloadTask downloadTask, RecyclerViewAdapter.ViewHolderThirtyTwo viewHolder)
    {

        if(showCheckBoxes)
        {
            viewHolder.mcv.setChecked(downloadingOrStartedSelectedTasksArrayList.contains(downloadTask.getKeyId()) ||
                    pausedOrErrorSelectedTasksArrayList.contains(downloadTask.getKeyId()));
        } else {
            viewHolder.mcv.setChecked(false);
        }

        viewHolder.fileNameTV.setText(downloadTask.getFileName());
        viewHolder.timeLeftTV.setText(downloadTask.getTimeLeft());
        viewHolder.downloadSpeedTV.setText(downloadTask.getDownloadSpeed());
        viewHolder.resumableOrNotTV.setText(downloadTask.getPauseResumeSupported());

        switch (downloadTask.getCurrentStatus())
        {
            case 0:
            case 1:
            case 2:
            case 3:
            case 6:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.round_pause_24);
                break;
            case 4:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.start_background);
                break;
            case 5:
                viewHolder.startPauseIB.setBackgroundResource(R.drawable.error_background);
                break;
        }

        thirtyTwoSegments(viewHolder,downloadTask.getKeyId());
    }

    private void setProgressDirectly(ProgressBar progressBar, int newProgress)
    {
        progressBar.setProgress(newProgress,false);
    }

    private void setAnimation(ProgressBar progressBar,int newProgress)
    {
        progressBar.setProgress(newProgress,true);
    }

    private void singleSegment(ViewHolderOne viewHolder, int id)
    {
        PartialOne downloadTask = db.getSingleThreadedDT(id);
        switch (downloadTask.getChunkMode())
        {
            case 1:
                if(downloadTask.getCurrentStatus() == 2)
                {
                    viewHolder.tProgressBar1.setIndeterminate(true);
                } else {
                    viewHolder.tProgressBar1.setIndeterminate(false);
                    viewHolder.tProgressBar1.setProgress(0);
                }

                break;
            case 0:
                viewHolder.tProgressBar1.setIndeterminate(false);
                if(downloadTask.getCurrentStatus() == 2)
                {
                    setAnimation(viewHolder.tProgressBar1,downloadTask.getCurrentProgress());
                } else {
                    setProgressDirectly(viewHolder.tProgressBar1,downloadTask.getCurrentProgress());
                }

                break;
        }
    }

    private void twoSegments(ViewHolderTwo viewHolder, int id)
    {
        PartialTwo downloadTask = db.getTwoThreadedDT(id);
        if(downloadTask.getCurrentStatus() == 2)
        {
            setAnimation(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setAnimation(viewHolder.tProgressBar2,downloadTask.getTPB2());
        } else {
            setProgressDirectly(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setProgressDirectly(viewHolder.tProgressBar2,downloadTask.getTPB2());
        }
    }

    private void fourSegments(ViewHolderFour viewHolder, int id)
    {
        PartialFour downloadTask = db.getFourThreadedDT(id);
        if(downloadTask.getCurrentStatus() == 2)
        {
            setAnimation(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setAnimation(viewHolder.tProgressBar2,downloadTask.getTPB2());
            setAnimation(viewHolder.tProgressBar3,downloadTask.getTPB3());
            setAnimation(viewHolder.tProgressBar4,downloadTask.getTPB4());
        } else {
            setProgressDirectly(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setProgressDirectly(viewHolder.tProgressBar2,downloadTask.getTPB2());
            setProgressDirectly(viewHolder.tProgressBar3,downloadTask.getTPB3());
            setProgressDirectly(viewHolder.tProgressBar4,downloadTask.getTPB4());
        }
    }

    private void sixSegments(ViewHolderSix viewHolder, int id)
    {
        PartialSix downloadTask = db.getSixThreadedDT(id);
        if(downloadTask.getCurrentStatus() == 2)
        {
            setAnimation(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setAnimation(viewHolder.tProgressBar2,downloadTask.getTPB2());
            setAnimation(viewHolder.tProgressBar3,downloadTask.getTPB3());
            setAnimation(viewHolder.tProgressBar4,downloadTask.getTPB4());
            setAnimation(viewHolder.tProgressBar5,downloadTask.getTPB5());
            setAnimation(viewHolder.tProgressBar6,downloadTask.getTPB6());
        } else {
            setProgressDirectly(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setProgressDirectly(viewHolder.tProgressBar2,downloadTask.getTPB2());
            setProgressDirectly(viewHolder.tProgressBar3,downloadTask.getTPB3());
            setProgressDirectly(viewHolder.tProgressBar4,downloadTask.getTPB4());
            setProgressDirectly(viewHolder.tProgressBar5,downloadTask.getTPB5());
            setProgressDirectly(viewHolder.tProgressBar6,downloadTask.getTPB6());
        }
    }

    private void eightSegments(ViewHolderEight viewHolder, int id)
    {
        PartialEight downloadTask = db.getEightThreadedDT(id);
        if(downloadTask.getCurrentStatus() == 2)
        {
            setAnimation(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setAnimation(viewHolder.tProgressBar2,downloadTask.getTPB2());
            setAnimation(viewHolder.tProgressBar3,downloadTask.getTPB3());
            setAnimation(viewHolder.tProgressBar4,downloadTask.getTPB4());
            setAnimation(viewHolder.tProgressBar5,downloadTask.getTPB5());
            setAnimation(viewHolder.tProgressBar6,downloadTask.getTPB6());
            setAnimation(viewHolder.tProgressBar7,downloadTask.getTPB7());
            setAnimation(viewHolder.tProgressBar8,downloadTask.getTPB8());
        } else {
            setProgressDirectly(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setProgressDirectly(viewHolder.tProgressBar2,downloadTask.getTPB2());
            setProgressDirectly(viewHolder.tProgressBar3,downloadTask.getTPB3());
            setProgressDirectly(viewHolder.tProgressBar4,downloadTask.getTPB4());
            setProgressDirectly(viewHolder.tProgressBar5,downloadTask.getTPB5());
            setProgressDirectly(viewHolder.tProgressBar6,downloadTask.getTPB6());
            setProgressDirectly(viewHolder.tProgressBar7,downloadTask.getTPB7());
            setProgressDirectly(viewHolder.tProgressBar8,downloadTask.getTPB8());
        }
    }

    private void sixteenSegments(ViewHolderSixteen viewHolder, int id)
    {
        PartialSixteen downloadTask = db.getSixteenThreadedDT(id);
        if(downloadTask.getCurrentStatus() == 2)
        {
            setAnimation(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setAnimation(viewHolder.tProgressBar2,downloadTask.getTPB2());
            setAnimation(viewHolder.tProgressBar3,downloadTask.getTPB3());
            setAnimation(viewHolder.tProgressBar4,downloadTask.getTPB4());
            setAnimation(viewHolder.tProgressBar5,downloadTask.getTPB5());
            setAnimation(viewHolder.tProgressBar6,downloadTask.getTPB6());
            setAnimation(viewHolder.tProgressBar7,downloadTask.getTPB7());
            setAnimation(viewHolder.tProgressBar8,downloadTask.getTPB8());
            setAnimation(viewHolder.tProgressBar9,downloadTask.getTPB9());
            setAnimation(viewHolder.tProgressBar10,downloadTask.getTPB10());
            setAnimation(viewHolder.tProgressBar11,downloadTask.getTPB11());
            setAnimation(viewHolder.tProgressBar12,downloadTask.getTPB12());
            setAnimation(viewHolder.tProgressBar13,downloadTask.getTPB13());
            setAnimation(viewHolder.tProgressBar14,downloadTask.getTPB14());
            setAnimation(viewHolder.tProgressBar15,downloadTask.getTPB15());
            setAnimation(viewHolder.tProgressBar16,downloadTask.getTPB16());
        } else {

            setProgressDirectly(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setProgressDirectly(viewHolder.tProgressBar2,downloadTask.getTPB2());
            setProgressDirectly(viewHolder.tProgressBar3,downloadTask.getTPB3());
            setProgressDirectly(viewHolder.tProgressBar4,downloadTask.getTPB4());
            setProgressDirectly(viewHolder.tProgressBar5,downloadTask.getTPB5());
            setProgressDirectly(viewHolder.tProgressBar6,downloadTask.getTPB6());
            setProgressDirectly(viewHolder.tProgressBar7,downloadTask.getTPB7());
            setProgressDirectly(viewHolder.tProgressBar8,downloadTask.getTPB8());
            setProgressDirectly(viewHolder.tProgressBar9,downloadTask.getTPB9());
            setProgressDirectly(viewHolder.tProgressBar10,downloadTask.getTPB10());
            setProgressDirectly(viewHolder.tProgressBar11,downloadTask.getTPB11());
            setProgressDirectly(viewHolder.tProgressBar12,downloadTask.getTPB12());
            setProgressDirectly(viewHolder.tProgressBar13,downloadTask.getTPB13());
            setProgressDirectly(viewHolder.tProgressBar14,downloadTask.getTPB14());
            setProgressDirectly(viewHolder.tProgressBar15,downloadTask.getTPB15());
            setProgressDirectly(viewHolder.tProgressBar16,downloadTask.getTPB16());
        }
    }

    private void thirtyTwoSegments(ViewHolderThirtyTwo viewHolder, int id)
    {
        PartialThirtyTwo downloadTask = db.getThirtyTwoThreadedDT(id);
        if(downloadTask.getCurrentStatus() == 2)
        {
            setAnimation(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setAnimation(viewHolder.tProgressBar2,downloadTask.getTPB2());
            setAnimation(viewHolder.tProgressBar3,downloadTask.getTPB3());
            setAnimation(viewHolder.tProgressBar4,downloadTask.getTPB4());
            setAnimation(viewHolder.tProgressBar5,downloadTask.getTPB5());
            setAnimation(viewHolder.tProgressBar6,downloadTask.getTPB6());
            setAnimation(viewHolder.tProgressBar7,downloadTask.getTPB7());
            setAnimation(viewHolder.tProgressBar8,downloadTask.getTPB8());
            setAnimation(viewHolder.tProgressBar9,downloadTask.getTPB9());
            setAnimation(viewHolder.tProgressBar10,downloadTask.getTPB10());
            setAnimation(viewHolder.tProgressBar11,downloadTask.getTPB11());
            setAnimation(viewHolder.tProgressBar12,downloadTask.getTPB12());
            setAnimation(viewHolder.tProgressBar13,downloadTask.getTPB13());
            setAnimation(viewHolder.tProgressBar14,downloadTask.getTPB14());
            setAnimation(viewHolder.tProgressBar15,downloadTask.getTPB15());
            setAnimation(viewHolder.tProgressBar16,downloadTask.getTPB16());
            setAnimation(viewHolder.tProgressBar17,downloadTask.getTPB17());
            setAnimation(viewHolder.tProgressBar18,downloadTask.getTPB18());
            setAnimation(viewHolder.tProgressBar19,downloadTask.getTPB19());
            setAnimation(viewHolder.tProgressBar20,downloadTask.getTPB20());
            setAnimation(viewHolder.tProgressBar21,downloadTask.getTPB21());
            setAnimation(viewHolder.tProgressBar22,downloadTask.getTPB22());
            setAnimation(viewHolder.tProgressBar23,downloadTask.getTPB23());
            setAnimation(viewHolder.tProgressBar24,downloadTask.getTPB24());
            setAnimation(viewHolder.tProgressBar25,downloadTask.getTPB25());
            setAnimation(viewHolder.tProgressBar26,downloadTask.getTPB26());
            setAnimation(viewHolder.tProgressBar27,downloadTask.getTPB27());
            setAnimation(viewHolder.tProgressBar28,downloadTask.getTPB28());
            setAnimation(viewHolder.tProgressBar29,downloadTask.getTPB29());
            setAnimation(viewHolder.tProgressBar30,downloadTask.getTPB30());
            setAnimation(viewHolder.tProgressBar31,downloadTask.getTPB31());
            setAnimation(viewHolder.tProgressBar32,downloadTask.getTPB32());
        } else {

            setProgressDirectly(viewHolder.tProgressBar1,downloadTask.getTPB1());
            setProgressDirectly(viewHolder.tProgressBar2,downloadTask.getTPB2());
            setProgressDirectly(viewHolder.tProgressBar3,downloadTask.getTPB3());
            setProgressDirectly(viewHolder.tProgressBar4,downloadTask.getTPB4());
            setProgressDirectly(viewHolder.tProgressBar5,downloadTask.getTPB5());
            setProgressDirectly(viewHolder.tProgressBar6,downloadTask.getTPB6());
            setProgressDirectly(viewHolder.tProgressBar7,downloadTask.getTPB7());
            setProgressDirectly(viewHolder.tProgressBar8,downloadTask.getTPB8());
            setProgressDirectly(viewHolder.tProgressBar9,downloadTask.getTPB9());
            setProgressDirectly(viewHolder.tProgressBar10,downloadTask.getTPB10());
            setProgressDirectly(viewHolder.tProgressBar11,downloadTask.getTPB11());
            setProgressDirectly(viewHolder.tProgressBar12,downloadTask.getTPB12());
            setProgressDirectly(viewHolder.tProgressBar13,downloadTask.getTPB13());
            setProgressDirectly(viewHolder.tProgressBar14,downloadTask.getTPB14());
            setProgressDirectly(viewHolder.tProgressBar15,downloadTask.getTPB15());
            setProgressDirectly(viewHolder.tProgressBar16,downloadTask.getTPB16());
            setProgressDirectly(viewHolder.tProgressBar17,downloadTask.getTPB17());
            setProgressDirectly(viewHolder.tProgressBar18,downloadTask.getTPB18());
            setProgressDirectly(viewHolder.tProgressBar19,downloadTask.getTPB19());
            setProgressDirectly(viewHolder.tProgressBar20,downloadTask.getTPB20());
            setProgressDirectly(viewHolder.tProgressBar21,downloadTask.getTPB21());
            setProgressDirectly(viewHolder.tProgressBar22,downloadTask.getTPB22());
            setProgressDirectly(viewHolder.tProgressBar23,downloadTask.getTPB23());
            setProgressDirectly(viewHolder.tProgressBar24,downloadTask.getTPB24());
            setProgressDirectly(viewHolder.tProgressBar25,downloadTask.getTPB25());
            setProgressDirectly(viewHolder.tProgressBar26,downloadTask.getTPB26());
            setProgressDirectly(viewHolder.tProgressBar27,downloadTask.getTPB27());
            setProgressDirectly(viewHolder.tProgressBar28,downloadTask.getTPB28());
            setProgressDirectly(viewHolder.tProgressBar29,downloadTask.getTPB29());
            setProgressDirectly(viewHolder.tProgressBar30,downloadTask.getTPB30());
            setProgressDirectly(viewHolder.tProgressBar31,downloadTask.getTPB31());
            setProgressDirectly(viewHolder.tProgressBar32,downloadTask.getTPB32());
        }
    }

    @Override
    public int getItemCount() {
        return downloadTaskIDs.size();
    }



    private void createRefreshDownloadAddressPopupDialog(final int dTID,final int position)
    {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final View view = inflater.inflate(R.layout.popup_refresh_download_address,mainContainer,false);
            builder.setView(view);
            dialog = builder.create();

            final TextView fileNameTVRefreshPopup,pauseResumeSupportedTVRefreshPopup,errorTVRefreshPopup,
                    statusTVRefreshPopup;
            final MaterialButton refreshAndResumeButtonRefreshPopup,startFromBeginningRefreshPopup,
                    okButtonRefreshPopup,cancelButtonRefreshPopup;
            final URLEditText urlEditTextRefreshPopup;
            final ProgressBar progressBar;
            final ImageButton pasteRLIBRefreshPopup;

            pasteRLIBRefreshPopup = view.findViewById(R.id.pasteRLIBRefreshPopup);
            progressBar = view.findViewById(R.id.myProgressbarRefreshPopup);
            fileNameTVRefreshPopup = view.findViewById(R.id.fileNameTVRefreshPopup);
            pauseResumeSupportedTVRefreshPopup = view.findViewById(R.id.pauseResumeSupportedTVRefreshPopup);
            errorTVRefreshPopup = view.findViewById(R.id.errorTVRefreshPopup);
            statusTVRefreshPopup = view.findViewById(R.id.statusTVRefreshPopup);
            refreshAndResumeButtonRefreshPopup = view.findViewById(R.id.refreshAndResumeButtonRefreshPopup);
            startFromBeginningRefreshPopup = view.findViewById(R.id.startFromBeginningRefreshPopup);
            okButtonRefreshPopup = view.findViewById(R.id.okButtonRefreshPopup);
            cancelButtonRefreshPopup = view.findViewById(R.id.cancelButtonRefreshPopup);
            urlEditTextRefreshPopup = view.findViewById(R.id.urlEditTextRefreshPopup);

            final DownloadTask downloadTask = db.getDownloadTask(dTID);
            fileNameTVRefreshPopup.setText(downloadTask.getFileName());

            final ExecutorService executorService = Executors.newSingleThreadExecutor();

            urlEditTextRefreshPopup.addListener(() -> {
                Editable editable = urlEditTextRefreshPopup.getText();

                if(HelperTextUtility.isNotEmpty(editable))
                {
                    String fileUrl = editable.toString();

                    urlEditTextRefreshPopup.setEnabled(false);
                    executorService.submit(new RefreshAddress(downloadTask,fileUrl,pasteRLIBRefreshPopup,
                            progressBar,refreshAndResumeButtonRefreshPopup,startFromBeginningRefreshPopup,okButtonRefreshPopup,
                            cancelButtonRefreshPopup,errorTVRefreshPopup,pauseResumeSupportedTVRefreshPopup,statusTVRefreshPopup,
                            dialog,position));
                } else {
                    errorTVRefreshPopup.setText(R.string.connection_timed_out);
                    errorTVRefreshPopup.setVisibility(View.VISIBLE);
                }
            });

            pasteRLIBRefreshPopup.setOnClickListener(view1 -> {
                try {
                    String clipValue = HelperTextUtility.getClipString(context);

                    if(HelperTextUtility.isNotEmpty(clipValue))
                    {
                        Editable pasteHereET = urlEditTextRefreshPopup.getText();
                        if(pasteHereET != null)
                        {
                            pasteHereET.clear();
                        }

                        urlEditTextRefreshPopup.append(clipValue);
                        urlEditTextRefreshPopup.setEnabled(false);

                        executorService.submit(new RefreshAddress(downloadTask,clipValue,pasteRLIBRefreshPopup,
                                progressBar,refreshAndResumeButtonRefreshPopup,startFromBeginningRefreshPopup,okButtonRefreshPopup,
                                cancelButtonRefreshPopup,errorTVRefreshPopup,pauseResumeSupportedTVRefreshPopup,statusTVRefreshPopup,
                                dialog,position));
                    } else {
                        errorTVRefreshPopup.setText(R.string.check_url);
                        errorTVRefreshPopup.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e)
                {
                    errorTVRefreshPopup.setText(R.string.check_url);
                    errorTVRefreshPopup.setVisibility(View.VISIBLE);
                }
            });

            dialog.setOnDismissListener(dialogInterface -> {
                if(!executorService.isShutdown())
                {
                    executorService.shutdown();
                }
                firstActivity.resumeAd();
                singleTaskPos = -1;
            });

            firstActivity.removeAndPauseAd();

            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(true);
            dialog.show();

        } catch (Exception ignored) {}
    }



    private class RefreshAddress extends Thread
    {
        private final DownloadTask downloadTask;
        private final String fileUrl;
        private final ProgressBar progressBar;
        private final MaterialButton refreshAndResumeButtonRefreshPopup,startFromBeginningRefreshPopup,
                okButtonRefreshPopup,cancelButtonRefreshPopup;
        private final TextView errorTVRefreshPopup,pauseResumeSupportedTVRefreshPopup,statusTVRefreshPopup;

        private String fileExistsAtUrl = null, length = null, pauseResumeSupported = null;
        private int isPauseResumeSupported;
        private final Dialog dialog;
        private final int singleTaskPos;


        private RefreshAddress(DownloadTask downloadTask,String fileUrl,ImageButton pasteRLIBRefreshPopup,ProgressBar progressBar,
                               MaterialButton refreshAndResumeButtonRefreshPopup,MaterialButton startFromBeginningRefreshPopup,
                               MaterialButton okButtonRefreshPopup, MaterialButton cancelButtonRefreshPopup,
                               TextView errorTVRefreshPopup,TextView pauseResumeSupportedTVRefreshPopup, TextView statusTVRefreshPopup,
                               Dialog dialog, int singleTaskPos
                               )

        {
            this.downloadTask = downloadTask;
            this.fileUrl = fileUrl;
            this.progressBar = progressBar;
            this.refreshAndResumeButtonRefreshPopup = refreshAndResumeButtonRefreshPopup;
            this.startFromBeginningRefreshPopup = startFromBeginningRefreshPopup;
            this.okButtonRefreshPopup = okButtonRefreshPopup;
            this.cancelButtonRefreshPopup = cancelButtonRefreshPopup;
            this.errorTVRefreshPopup = errorTVRefreshPopup;
            this.pauseResumeSupportedTVRefreshPopup = pauseResumeSupportedTVRefreshPopup;
            this.statusTVRefreshPopup = statusTVRefreshPopup;
            this.dialog = dialog;
            this.singleTaskPos = singleTaskPos;

            progressBar.setVisibility(View.VISIBLE);
            pasteRLIBRefreshPopup.setVisibility(View.GONE);
        }

        @Override
        public void run() {
            super.run();

            HttpURLConnection connectionRange = null;
            HttpURLConnection connection = null;
            int s = 0;
            try {
                URL url = new URL(fileUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(45000);
                connection.setConnectTimeout(45000);
                connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                if((connection.getResponseCode() / 100) == 2)
                {
                    fileExistsAtUrl = "Yes";
                    length = connection.getHeaderField("Content-Length");
                    //now we will check if it supports pause-resume
                    String bytesRange = 0+"-";
                    connectionRange = (HttpURLConnection) url.openConnection();
                    connectionRange.setRequestProperty("Range","bytes="+bytesRange);
                    connectionRange.setReadTimeout(45000);
                    connectionRange.setConnectTimeout(45000);
                    connectionRange.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    if(connectionRange.getResponseCode() == HttpURLConnection.HTTP_PARTIAL)
                    {
                        pauseResumeSupported = "Resumable";
                        isPauseResumeSupported = 1;
                    } else {
                        pauseResumeSupported = "Unresumable";
                        isPauseResumeSupported = 0;
                    }

                    s = 1;
                } else {
                    s = 5;
                }
            } catch (SocketTimeoutException e)
            {
                s = 4;
            } catch (MalformedURLException e)
            {
                s = 2;
            } catch (IOException e)
            {
                s = 3;
            } catch (Exception ignored) {}
            finally {
                try {
                    if(connection != null)
                    {
                        connection.getInputStream().close();
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connectionRange != null)
                    {
                        connectionRange.getInputStream().close();
                        connectionRange.disconnect();
                    }
                } catch (Exception ignored) {}

                final int finalS = s;

                firstActivity.runOnUiThread(() -> {
                    switch (finalS)
                    {
                        case 1:
                        {
                            //we are successful
                            try {
                                if(downloadTask.getTotalBytes() == Long.parseLong(length))
                                {
                                    if(fileExistsAtUrl.equals("Yes"))
                                    {
                                        //file exists at server
                                        if(isPauseResumeSupported == 1)
                                        {
                                            progressBar.setVisibility(View.GONE);
                                            refreshAndResumeButtonRefreshPopup.setVisibility(View.VISIBLE);
                                            refreshAndResumeButtonRefreshPopup.setEnabled(true);
                                            refreshAndResumeButtonRefreshPopup.setText(R.string.refresh_download_address_resume);

                                            cancelButtonRefreshPopup.setVisibility(View.VISIBLE);
                                            startFromBeginningRefreshPopup.setVisibility(View.GONE);
                                            pauseResumeSupportedTVRefreshPopup.setText(pauseResumeSupported);
                                            pauseResumeSupportedTVRefreshPopup.setVisibility(View.VISIBLE);
                                            okButtonRefreshPopup.setVisibility(View.GONE);
                                        } else {
                                            progressBar.setVisibility(View.GONE);
                                            statusTVRefreshPopup.setText(R.string.file_does_not_support_resuming);
                                            statusTVRefreshPopup.setVisibility(View.VISIBLE);
                                            refreshAndResumeButtonRefreshPopup.setVisibility(View.GONE);
                                            startFromBeginningRefreshPopup.setVisibility(View.VISIBLE);
                                            pauseResumeSupportedTVRefreshPopup.setText(pauseResumeSupported);
                                            pauseResumeSupportedTVRefreshPopup.setVisibility(View.VISIBLE);
                                            okButtonRefreshPopup.setVisibility(View.GONE);
                                            cancelButtonRefreshPopup.setVisibility(View.VISIBLE);

                                        }
                                    } else {
                                        progressBar.setVisibility(View.GONE);
                                        statusTVRefreshPopup.setText(R.string.file_not_found_on_url);
                                        statusTVRefreshPopup.setVisibility(View.VISIBLE);
                                        refreshAndResumeButtonRefreshPopup.setVisibility(View.GONE);
                                        okButtonRefreshPopup.setVisibility(View.VISIBLE);
                                        startFromBeginningRefreshPopup.setVisibility(View.GONE);
                                        cancelButtonRefreshPopup.setVisibility(View.GONE);

                                    }
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    statusTVRefreshPopup.setText(R.string.file_size_mismatched);
                                    statusTVRefreshPopup.setVisibility(View.VISIBLE);
                                    refreshAndResumeButtonRefreshPopup.setVisibility(View.GONE);
                                    okButtonRefreshPopup.setVisibility(View.VISIBLE);
                                    startFromBeginningRefreshPopup.setVisibility(View.GONE);
                                    cancelButtonRefreshPopup.setVisibility(View.GONE);
                                }
                            } catch (NumberFormatException | NullPointerException e)
                            {
                                progressBar.setVisibility(View.GONE);
                                refreshAndResumeButtonRefreshPopup.setVisibility(View.GONE);
                                cancelButtonRefreshPopup.setVisibility(View.GONE);
                                okButtonRefreshPopup.setVisibility(View.VISIBLE);
                                startFromBeginningRefreshPopup.setVisibility(View.GONE);

                                errorTVRefreshPopup.setText(R.string.unable_to_find_file_size);
                                errorTVRefreshPopup.setVisibility(View.VISIBLE);
                            }


                            break;
                        }
                        case 0:
                        {
                            progressBar.setVisibility(View.GONE);
                            refreshAndResumeButtonRefreshPopup.setVisibility(View.GONE);
                            cancelButtonRefreshPopup.setVisibility(View.GONE);
                            okButtonRefreshPopup.setVisibility(View.VISIBLE);
                            startFromBeginningRefreshPopup.setVisibility(View.GONE);

                            statusTVRefreshPopup.setText(R.string.oops_general_message);
                            statusTVRefreshPopup.setVisibility(View.VISIBLE);

                            break;
                        }
                        case 2:
                        {
                            progressBar.setVisibility(View.GONE);
                            refreshAndResumeButtonRefreshPopup.setVisibility(View.GONE);
                            cancelButtonRefreshPopup.setVisibility(View.GONE);
                            okButtonRefreshPopup.setVisibility(View.VISIBLE);
                            startFromBeginningRefreshPopup.setVisibility(View.GONE);

                            errorTVRefreshPopup.setText(R.string.check_url);
                            errorTVRefreshPopup.setVisibility(View.VISIBLE);

                            break;
                        }
                        case 3:
                        {
                            progressBar.setVisibility(View.GONE);
                            refreshAndResumeButtonRefreshPopup.setVisibility(View.GONE);
                            cancelButtonRefreshPopup.setVisibility(View.GONE);
                            okButtonRefreshPopup.setVisibility(View.VISIBLE);
                            startFromBeginningRefreshPopup.setVisibility(View.GONE);

                            errorTVRefreshPopup.setText(R.string.network_not_found);
                            errorTVRefreshPopup.setVisibility(View.VISIBLE);
                            break;
                        }
                        case 5:
                        {
                            progressBar.setVisibility(View.GONE);
                            refreshAndResumeButtonRefreshPopup.setVisibility(View.GONE);
                            cancelButtonRefreshPopup.setVisibility(View.GONE);
                            okButtonRefreshPopup.setVisibility(View.VISIBLE);
                            startFromBeginningRefreshPopup.setVisibility(View.GONE);

                            statusTVRefreshPopup.setText(R.string.file_not_found_on_url);
                            statusTVRefreshPopup.setVisibility(View.VISIBLE);
                            break;
                        }
                        case 4:
                        {
                            progressBar.setVisibility(View.GONE);
                            refreshAndResumeButtonRefreshPopup.setVisibility(View.GONE);
                            cancelButtonRefreshPopup.setVisibility(View.GONE);
                            okButtonRefreshPopup.setVisibility(View.VISIBLE);
                            startFromBeginningRefreshPopup.setVisibility(View.GONE);

                            errorTVRefreshPopup.setText(R.string.connection_timed_out);
                            errorTVRefreshPopup.setVisibility(View.VISIBLE);
                            break;
                        }
                    }

                    refreshAndResumeButtonRefreshPopup.setOnClickListener(view -> {
                        int status = downloadTask.getCurrentStatus();
                        if(status == 1 || status == 2 || status == 3 || status == 6)
                        {
                            //to pause download first of all
                            downloadTask.setCurrentStatus(4);
                            db.updateDownloadTaskStatus(downloadTask.getKeyId(),4);

                            //Pause download
                            Intent intent = new Intent(context, DownloadingService.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("dStatus","Pause");
                            bundle.putInt("dId",downloadTask.getKeyId());
                            intent.putExtras(bundle);
                            context.startService(intent);
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            //update downloadTask and Resume
                            db.updateDownloadTaskUSSP(downloadTask.getKeyId(),isPauseResumeSupported,fileUrl,pauseResumeSupported);
                            dialog.dismiss();
                            Toast.makeText(context, R.string.resumed, Toast.LENGTH_SHORT).show();
                            notifyItemChanged(singleTaskPos);

                            Intent intent = new Intent(context, DownloadingService.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("dStatus","ResumeFromRv");
                            bundle.putInt("dId",downloadTask.getKeyId());
                            intent.putExtras(bundle);

                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            {
                                context.startForegroundService(intent);
                            } else {
                                context.startService(intent);
                            }

                        }, 500);
                    });

                    startFromBeginningRefreshPopup.setOnClickListener(view -> {
                        UserPreferences userPreferences = db.getHalfUserPreferences();

                        try
                        {
                            //delete the original file if it exists

                            DocumentFile pickedDir = DocumentFile.fromTreeUri(context, Uri.parse(downloadTask.getDirPath()));

                            if(pickedDir != null)
                            {
                                DocumentFile[] files = pickedDir.listFiles();

                                ArrayList<String> fileNames = new ArrayList<>();

                                for(DocumentFile file : files)
                                {
                                    fileNames.add(file.getName());
                                }

                                if(fileNames.contains(downloadTask.getFileName()))
                                {
                                    DocumentFile fileToBeDeleted = files[fileNames.indexOf(downloadTask.getFileName())];
                                    if(fileToBeDeleted != null)
                                    {
                                        fileToBeDeleted.delete();
                                    }
                                }
                            }

                            //start download by just updating metadata as new
                            DownloadTask freshDownloadTask = db.getDownloadTask(downloadTask.getKeyId());

                            freshDownloadTask.setFileName(downloadTask.getFileName());
                            freshDownloadTask.setUrl(fileUrl);
                            long contentLength = Long.parseLong(length);
                            freshDownloadTask.setTotalBytes(contentLength);

                            if(contentLength == -1 || contentLength == 0)
                            {
                                freshDownloadTask.setChunkMode(1);
                            } else {
                                freshDownloadTask.setChunkMode(0);
                            }

                            freshDownloadTask.setDirPath(userPreferences.getDownloadPath());
                            freshDownloadTask.setDownloadedBytes(0L);
                            freshDownloadTask.setCurrentStatus(2);
                            freshDownloadTask.setCurrentProgress(0);
                            freshDownloadTask.setDownloadSpeed("Queued");
                            freshDownloadTask.setTimeLeft("-");
                            freshDownloadTask.setPauseResumeSupported(pauseResumeSupported);
                            freshDownloadTask.setWhichError("NotAny");
                            freshDownloadTask.setIsPauseResumeSupported(isPauseResumeSupported);

                            freshDownloadTask.setTPB1(0);
                            freshDownloadTask.setTPB2(0);
                            freshDownloadTask.setTPB3(0);
                            freshDownloadTask.setTPB4(0);
                            freshDownloadTask.setTPB5(0);
                            freshDownloadTask.setTPB6(0);
                            freshDownloadTask.setTPB7(0);
                            freshDownloadTask.setTPB8(0);
                            freshDownloadTask.setTPB9(0);
                            freshDownloadTask.setTPB10(0);
                            freshDownloadTask.setTPB11(0);
                            freshDownloadTask.setTPB12(0);
                            freshDownloadTask.setTPB13(0);
                            freshDownloadTask.setTPB14(0);
                            freshDownloadTask.setTPB15(0);
                            freshDownloadTask.setTPB16(0);
                            freshDownloadTask.setTPB17(0);
                            freshDownloadTask.setTPB18(0);
                            freshDownloadTask.setTPB19(0);
                            freshDownloadTask.setTPB20(0);
                            freshDownloadTask.setTPB21(0);
                            freshDownloadTask.setTPB22(0);
                            freshDownloadTask.setTPB23(0);
                            freshDownloadTask.setTPB24(0);
                            freshDownloadTask.setTPB25(0);
                            freshDownloadTask.setTPB26(0);
                            freshDownloadTask.setTPB27(0);
                            freshDownloadTask.setTPB28(0);
                            freshDownloadTask.setTPB29(0);
                            freshDownloadTask.setTPB30(0);
                            freshDownloadTask.setTPB31(0);
                            freshDownloadTask.setTPB32(0);

                            freshDownloadTask.setTSS1(0L);
                            freshDownloadTask.setTSS2(0L);
                            freshDownloadTask.setTSS3(0L);
                            freshDownloadTask.setTSS4(0L);
                            freshDownloadTask.setTSS5(0L);
                            freshDownloadTask.setTSS6(0L);
                            freshDownloadTask.setTSS7(0L);
                            freshDownloadTask.setTSS8(0L);
                            freshDownloadTask.setTSS9(0L);
                            freshDownloadTask.setTSS10(0L);
                            freshDownloadTask.setTSS11(0L);
                            freshDownloadTask.setTSS12(0L);
                            freshDownloadTask.setTSS13(0L);
                            freshDownloadTask.setTSS14(0L);
                            freshDownloadTask.setTSS15(0L);
                            freshDownloadTask.setTSS16(0L);
                            freshDownloadTask.setTSS17(0L);
                            freshDownloadTask.setTSS18(0L);
                            freshDownloadTask.setTSS19(0L);
                            freshDownloadTask.setTSS20(0L);
                            freshDownloadTask.setTSS21(0L);
                            freshDownloadTask.setTSS22(0L);
                            freshDownloadTask.setTSS23(0L);
                            freshDownloadTask.setTSS24(0L);
                            freshDownloadTask.setTSS25(0L);
                            freshDownloadTask.setTSS26(0L);
                            freshDownloadTask.setTSS27(0L);
                            freshDownloadTask.setTSS28(0L);
                            freshDownloadTask.setTSS29(0L);
                            freshDownloadTask.setTSS30(0L);
                            freshDownloadTask.setTSS31(0L);
                            freshDownloadTask.setTSS32(0L);

                            db.updateDownloadTask(freshDownloadTask);

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                dialog.dismiss();
                                Toast.makeText(context, R.string.downloading_again_from_beginning, Toast.LENGTH_SHORT).show();
                                notifyItemChanged(singleTaskPos);

                                Intent intent = new Intent(context, DownloadingService.class);
                                Bundle bundle1 = new Bundle();
                                bundle1.putInt("dId",downloadTask.getKeyId());
                                bundle1.putString("dStatus","downloadNow");
                                intent.putExtras(bundle1);
                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                {
                                    context.startForegroundService(intent);
                                } else {
                                    context.startService(intent);
                                }
                            }, 500);

                        } catch (Exception ignored) {}
                    });

                    okButtonRefreshPopup.setOnClickListener(view -> dialog.dismiss());
                    cancelButtonRefreshPopup.setOnClickListener(view -> dialog.dismiss());
                });

            }
        }
    }


    void hideSelectCheckbox()
    {
        showCheckBoxes = false;
        try {
            try {
                for(Integer dTID : downloadingOrStartedSelectedTasksArrayList)
                {
                    notifyItemChanged(downloadTaskIDs.indexOf(dTID));
                }
            } finally {
                for(Integer dTID : pausedOrErrorSelectedTasksArrayList)
                {
                    notifyItemChanged(downloadTaskIDs.indexOf(dTID));
                }
            }
        } finally {
            if (downloadingOrStartedSelectedTasksArrayList.size() > 0)
            {
                downloadingOrStartedSelectedTasksArrayList.clear();
            }

            if (pausedOrErrorSelectedTasksArrayList.size() > 0)
            {
                pausedOrErrorSelectedTasksArrayList.clear();
            }

            firstActivity.setCountTVText(0);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    void selectAllTasks()
    {
        int size = downloadTaskIDs.size();
        if(size > 1)
        {
            for(int i = 0; i < size; i++)
            {
                int dTID = downloadTaskIDs.get(i);
                int currentStatus = db.getCurrentStatusOfDT(dTID);
                if(currentStatus == 2 || currentStatus == 1 || currentStatus == 6)
                {
                    if(!downloadingOrStartedSelectedTasksArrayList.contains(dTID))
                    {
                        downloadingOrStartedSelectedTasksArrayList.add(dTID);
                    }
                } else {
                    if(!pausedOrErrorSelectedTasksArrayList.contains(dTID))
                    {
                        pausedOrErrorSelectedTasksArrayList.add(dTID);
                        try {
                            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            if(notificationManager != null)
                            {
                                notificationManager.cancel(dTID);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            propertiesIB.setVisibility(View.INVISIBLE);
            refreshIB.setVisibility(View.INVISIBLE);
        } else if(size < 1)
        {
            showCheckBoxes = false;
            firstActivity.setEditInvisible();
        } else {
            int dTID = downloadTaskIDs.get(0);
            int currentStatus = db.getCurrentStatusOfDT(dTID);

            if(currentStatus == 2 || currentStatus == 1 || currentStatus == 6)
            {
                if(!downloadingOrStartedSelectedTasksArrayList.contains(dTID))
                {
                    downloadingOrStartedSelectedTasksArrayList.add(dTID);
                }
            } else {
                if(!pausedOrErrorSelectedTasksArrayList.contains(dTID))
                {
                    pausedOrErrorSelectedTasksArrayList.add(dTID);
                    try {
                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        if(notificationManager != null)
                        {
                            notificationManager.cancel(dTID);
                        }
                    } catch (Exception ignored) {}
                }
            }

            propertiesIB.setVisibility(View.VISIBLE);
            if(db.getPauseResumeSupported(dTID) == 0)
            {
                refreshIB.setVisibility(View.INVISIBLE);
            } else {
                refreshIB.setVisibility(View.VISIBLE);
                singleTaskPos = 0;
            }
        }

        recyclerView.removeAllViews();
        notifyDataSetChanged();
        firstActivity.setCountTVText(size);
    }

    void deselectAllTasks()
    {
        propertiesIB.setVisibility(View.INVISIBLE);
        refreshIB.setVisibility(View.INVISIBLE);
        final ArrayList<Integer> tempTasks = new ArrayList<>();

        try {
            tempTasks.addAll(downloadingOrStartedSelectedTasksArrayList);
            tempTasks.addAll(pausedOrErrorSelectedTasksArrayList);

            if (downloadingOrStartedSelectedTasksArrayList.size() > 0)
            {
                downloadingOrStartedSelectedTasksArrayList.clear();
            }

            if (pausedOrErrorSelectedTasksArrayList.size() > 0)
            {
                pausedOrErrorSelectedTasksArrayList.clear();
            }
        } finally {
            for (Integer dId : tempTasks)
            {
                notifyItemChanged(downloadTaskIDs.indexOf(dId));
            }

            firstActivity.setCountTVText(0);
        }
    }

    void showDeleteDialog()
    {
        if(downloadingOrStartedSelectedTasksArrayList.size() >= 1 || pausedOrErrorSelectedTasksArrayList.size() >= 1)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = inflater.inflate(R.layout.popup_delete_multiple_download_tasks,mainContainer,false);

            final TextView selectedTasksCountTVDeleteMultiplePopup = view.findViewById(R.id.selectedTasksCountTVDeleteMultiplePopup);
            final CheckBox deleteSourceFileCB = view.findViewById(R.id.deleteSourceFileCB);
            final MaterialButton noBtn,yesBtn;
            noBtn = view.findViewById(R.id.noBtn);
            yesBtn = view.findViewById(R.id.yesBtn);

            selectedTasksCountTVDeleteMultiplePopup.setText(String.valueOf(downloadingOrStartedSelectedTasksArrayList.size() +
                    pausedOrErrorSelectedTasksArrayList.size()));

            builder.setView(view);
            dialog = builder.create();

            noBtn.setOnClickListener(view1 -> {
                dialog.dismiss();
                hideSelectCheckbox();
            });

            yesBtn.setOnClickListener(view12 -> {
                dialog.dismiss();
                final androidx.appcompat.app.AlertDialog innerDialog = createDeleteMessagePopup();
                if(downloadingOrStartedSelectedTasksArrayList.size() > 0)
                {
                    //We need to pause currently active tasks of downloader that we want to delete
                    Intent intent = new Intent(context,DownloadingService.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("dStatus","deleteNow");
                    bundle.putIntegerArrayList("dSelectedArrayList",downloadingOrStartedSelectedTasksArrayList);
                    intent.putExtras(bundle);
                    context.startService(intent);

                    deletionWork = new DeleteTasks(deleteSourceFileCB.isChecked(),innerDialog);
                } else {
                    new DeleteTasks(deleteSourceFileCB.isChecked(),innerDialog).start();
                }
            });

            dialog.setOnDismissListener(dialogInterface -> firstActivity.resumeAd());
            firstActivity.removeAndPauseAd();


            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
        }
        else if(downloadTaskIDs.size() == 0)
        {
            final androidx.appcompat.app.AlertDialog innerDialog = createNormalMessagePopup(R.string.no_tasks_present);
            hideSelectCheckbox();
            new Handler(Looper.getMainLooper()).postDelayed(innerDialog::dismiss,500);
        } else {
            final androidx.appcompat.app.AlertDialog innerDialog = createNormalMessagePopup(R.string.no_selected_tasks);
            hideSelectCheckbox();
            new Handler(Looper.getMainLooper()).postDelayed(innerDialog::dismiss,500);
        }
    }

    private androidx.appcompat.app.AlertDialog createNormalMessagePopup(final int resId)
    {
        androidx.appcompat.app.AlertDialog.Builder innerDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(context);
        View view = firstActivity.getLayoutInflater().inflate(R.layout.popup_action_message,mainContainer,false);

        popupActionMessageTV = view.findViewById(R.id.popupActionMessage);
        popupActionMessageTV.setText(resId);

        innerDialogBuilder.setView(view);
        final androidx.appcompat.app.AlertDialog innerDialog = innerDialogBuilder.create();
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

        return innerDialog;
    }

    private androidx.appcompat.app.AlertDialog createDeleteMessagePopup() {
        androidx.appcompat.app.AlertDialog.Builder innerDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(context);
        View view = firstActivity.getLayoutInflater().inflate(R.layout.popup_action_message,mainContainer,false);

        popupActionMessageTV = view.findViewById(R.id.popupActionMessage);
        popupActionMessageTV.setText(R.string.deleting);

        innerDialogBuilder.setView(view);
        final androidx.appcompat.app.AlertDialog innerDialog = innerDialogBuilder.create();
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

        innerDialog.setOnDismissListener(dialogInterface -> {
            showCheckBoxes = false;

            if (downloadingOrStartedSelectedTasksArrayList.size() > 0)
            {
                downloadingOrStartedSelectedTasksArrayList.clear();
            }

            if (pausedOrErrorSelectedTasksArrayList.size() > 0)
            {
                pausedOrErrorSelectedTasksArrayList.clear();
            }

            try {
                analyzeCurrentQueuedTasks();
            } finally {
                firstActivity.setQueuedCount(downloadTaskIDs.size());
                firstActivity.setCompletedTasksCount();
                firstActivity.resumeBRAfterDeletion();
            }
        });

        innerDialog.setCanceledOnTouchOutside(false);
        innerDialog.setCancelable(false);
        innerDialog.show();

        return innerDialog;
    }

    private class DeleteTasks extends Thread
    {
        private final boolean deleteSourceFileOrNot;
        private final androidx.appcompat.app.AlertDialog innerDialog;

        private DeleteTasks(boolean deleteSourceFileOrNot, androidx.appcompat.app.AlertDialog innerDialog)
        {
            this.deleteSourceFileOrNot = deleteSourceFileOrNot;
            this.innerDialog = innerDialog;
            firstActivity.pauseBroadCastReceiver();
        }

        @Override
        public void run() {
            super.run();

            try {
                if(pausedOrErrorSelectedTasksArrayList.size() > 0)
                {
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    for (Integer dTID : pausedOrErrorSelectedTasksArrayList)
                    {
                        PartialHalfTask downloadTask = db.getHalfDownloadTask(dTID);
                        if(deleteSourceFileOrNot)
                        {
                            try {
                                //delete original source file from storage

                                DocumentFile pickedDir = DocumentFile.fromTreeUri(context,Uri.parse(downloadTask.getDirPath()));

                                if(pickedDir != null)
                                {
                                    DocumentFile[] files = pickedDir.listFiles();

                                    ArrayList<String> fileNames = new ArrayList<>();

                                    for(DocumentFile file : files)
                                    {
                                        fileNames.add(file.getName());
                                    }

                                    if(fileNames.contains(downloadTask.getFileName()))
                                    {
                                        DocumentFile fileToBeDeleted = files[fileNames.indexOf(downloadTask.getFileName())];
                                        if(fileToBeDeleted != null)
                                        {
                                            fileToBeDeleted.delete();
                                        }
                                    }
                                }

                            } catch (Exception ignored) {}
                        }

                        db.deleteDownloadTask(downloadTask.getKeyId());

                        try {
                            notificationManager.cancel(downloadTask.getKeyId());
                        } catch (Exception ignored) {}

                        firstActivity.runOnUiThread(() -> {
                            int pos = downloadTaskIDs.indexOf(dTID);
                            downloadTaskIDs.remove(dTID);
                            notifyItemRemoved(pos);
                        });

                    }
                }
            } catch (Exception ignored) {}
            finally {
                if(downloadingOrStartedSelectedTasksArrayList.size() > 0)
                {
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    for (Integer dTID : downloadingOrStartedSelectedTasksArrayList)
                    {
                        PartialHalfTask downloadTask = db.getHalfDownloadTask(dTID);
                        if(deleteSourceFileOrNot)
                        {
                            try {
                                //delete original source file from storage

                                DocumentFile pickedDir = DocumentFile.fromTreeUri(context,Uri.parse(downloadTask.getDirPath()));

                                if(pickedDir != null)
                                {
                                    DocumentFile[] files = pickedDir.listFiles();

                                    ArrayList<String> fileNames = new ArrayList<>();

                                    for(DocumentFile file : files)
                                    {
                                        fileNames.add(file.getName());
                                    }

                                    if(fileNames.contains(downloadTask.getFileName()))
                                    {
                                        DocumentFile fileToBeDeleted = files[fileNames.indexOf(downloadTask.getFileName())];
                                        if(fileToBeDeleted != null)
                                        {
                                            fileToBeDeleted.delete();
                                        }
                                    }
                                }

                            } catch (Exception ignored) {}
                        }

                        db.deleteDownloadTask(downloadTask.getKeyId());

                        try {
                            notificationManager.cancel(downloadTask.getKeyId());
                        } catch (Exception ignored) {}

                        firstActivity.runOnUiThread(() -> {
                            int pos = downloadTaskIDs.indexOf(dTID);
                            downloadTaskIDs.remove(dTID);
                            notifyItemRemoved(pos);
                        });

                    }
                }

                firstActivity.runOnUiThread(() -> {
                    popupActionMessageTV.setText(R.string.task_successfully_deleted);
                    innerDialog.dismiss();
                });
            }
        }
    }

}
