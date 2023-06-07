package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
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
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialCompletedTask;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialHalfTask;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

public class CompletedRecyclerViewAdapter extends RecyclerView.Adapter<CompletedRecyclerViewAdapter.ViewHolder> {
    private final Context context;
    private final ConstraintLayout mainContainer;
    private final FirstActivity firstActivity;
    private final RelativeLayout tabsRL;
    private final LinearLayout editLL;
    private final MaterialButton propertiesIB;
    private final MaterialButton refreshIB;

    private RecyclerView recyclerView;
    private final ArrayList<Integer> completedTaskIDs;
    private final DatabaseHandler db;
    private final LayoutInflater inflater;

    private final ArrayList<Integer> selectedTasksArrayList;
    private CheckBox deleteMultipleSourceFilesCheckBox;
    private android.app.AlertDialog dialog;
    private TextView popupActionMessageTV;
    private boolean showCheckBoxes = false;


    public CompletedRecyclerViewAdapter(Context context, FirstActivity firstActivity,ConstraintLayout mainContainer, RelativeLayout tabsRL, LinearLayout editLL,
                                        MaterialButton propertiesIB, MaterialButton refreshIB) {
        this.context = context;
        this.mainContainer = mainContainer;
        this.firstActivity = firstActivity;
        this.tabsRL = tabsRL;
        this.editLL = editLL;
        this.propertiesIB = propertiesIB;
        this.refreshIB = refreshIB;

        db = DatabaseHandler.getInstance(context);

        completedTaskIDs = new ArrayList<>();
        selectedTasksArrayList = new ArrayList<>();
        inflater = LayoutInflater.from(context);
    }



    void showProperties()
    {
       try {
            Intent intent = new Intent(context,FileDetailsActivity.class);
            intent.putExtra("id",String.valueOf(selectedTasksArrayList.get(0)));
            context.startActivity(intent);
        } catch (Exception e)
        {
            Toast.makeText(context, R.string.oops_general_message, Toast.LENGTH_SHORT).show();
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
            firstActivity.setEditInvisible();

            showCheckBoxes = false;

            for(Integer dTID : selectedTasksArrayList)
            {
                notifyItemChanged(completedTaskIDs.indexOf(dTID));
            }

        } finally {
            if(selectedTasksArrayList.size() > 0)
            {
                selectedTasksArrayList.clear();
            }

            firstActivity.setCountTVText(0);

            try {
                firstActivity.setCompletedCount(completedTaskIDs.size());
            } finally {
                firstActivity.resumeBroadcastReceiver();
            }
        }
    }

    void insertCompletedTask(Integer dTID)
    {
        completedTaskIDs.add(0,dTID);
        notifyItemInserted(0);
        recyclerView.scrollToPosition(0);
    }

    void hideSelectCheckbox()
    {
        showCheckBoxes = false;
        try {
            for(Integer dTID : selectedTasksArrayList)
            {
                notifyItemChanged(completedTaskIDs.indexOf(dTID));
            }
        } finally {
            if (selectedTasksArrayList.size() > 0)
            {
                selectedTasksArrayList.clear();
            }

            firstActivity.setCountTVText(0);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    void selectAllCompletedTasks()
    {
        int size = completedTaskIDs.size();
        if(size > 1)
        {
            for(int i = 0; i < size; i++)
            {
                int dTID = completedTaskIDs.get(i);
                if(!selectedTasksArrayList.contains(dTID))
                {
                    selectedTasksArrayList.add(dTID);
                }
            }
            propertiesIB.setVisibility(View.INVISIBLE);
            refreshIB.setVisibility(View.INVISIBLE);
        } else if(size < 1)
        {
            showCheckBoxes = false;
            firstActivity.setEditInvisible();
        } else {
            int dTID = completedTaskIDs.get(0);
            if(!selectedTasksArrayList.contains(dTID))
            {
                selectedTasksArrayList.add(dTID);
            }

            propertiesIB.setVisibility(View.VISIBLE);
            refreshIB.setVisibility(View.INVISIBLE);
        }

        recyclerView.removeAllViews();
        notifyDataSetChanged();
        firstActivity.setCountTVText(size);
    }

    void deselectAllCompletedTasks()
    {
        propertiesIB.setVisibility(View.INVISIBLE);
        refreshIB.setVisibility(View.INVISIBLE);
        final ArrayList<Integer> tempTasks = new ArrayList<>();

        try {
            tempTasks.addAll(selectedTasksArrayList);

            if (selectedTasksArrayList.size() > 0)
            {
                selectedTasksArrayList.clear();
            }

        } finally {
            for (Integer dId : tempTasks)
            {
                notifyItemChanged(completedTaskIDs.indexOf(dId));
            }

            firstActivity.setCountTVText(0);
        }
    }

    void setRecyclerView(RecyclerView recyclerView)
    {
        this.recyclerView = recyclerView;
    }

    @SuppressLint("NotifyDataSetChanged")
    void setTasks()
    {
        try {
            if(completedTaskIDs.size() > 0)
            {
                completedTaskIDs.clear();
            }
            completedTaskIDs.addAll(db.getCompletedDownloadTaskIDs());
            recyclerView.removeAllViews();
            notifyDataSetChanged();
            firstActivity.setCompletedCount(completedTaskIDs.size());
        } finally {
            firstActivity.resumeBroadcastReceiver();
        }
    }

    @NonNull
    @Override
    public CompletedRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.completed_item_row,parent,false);
        return new CompletedRecyclerViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompletedRecyclerViewAdapter.ViewHolder holder, int position) {
        int id = completedTaskIDs.get(position);
        PartialCompletedTask downloadTask = db.getBindDownloadTaskComplete(id);

        if(showCheckBoxes)
        {
            holder.mcv.setChecked(selectedTasksArrayList.contains(id));
        } else {
            holder.mcv.setChecked(false);
        }


        holder.fileName.setText(downloadTask.getFileName());
        holder.downloadStatus.setText(downloadTask.getTimeLeft());
        holder.fileSize.setText(downloadTask.getDownloadSpeed());
    }

    @Override
    public int getItemCount() {
        return completedTaskIDs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView fileName,fileSize,downloadStatus;
        private final MaterialCardView mcv;
        private Toast toast = null;



        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mcv = itemView.findViewById(R.id.mcv);
            fileName = mcv.findViewById(R.id.fileName);
            fileSize = mcv.findViewById(R.id.fileSize);
            downloadStatus = mcv.findViewById(R.id.downloadStatus);

            mcv.setOnClickListener(view -> {
                final int taskID = completedTaskIDs.get(getBindingAdapterPosition());

                if(showCheckBoxes)
                {

                    if(!mcv.isChecked())
                    {
                        mcv.setChecked(true);
                        selectedTasksArrayList.add(taskID);

                    } else {
                        mcv.setChecked(false);
                        selectedTasksArrayList.remove((Integer) taskID);
                    }

                    final int size = selectedTasksArrayList.size();

                    firstActivity.setCountTVText(size);

                    if(size > 1)
                    {
                        propertiesIB.setVisibility(View.INVISIBLE);
                        refreshIB.setVisibility(View.INVISIBLE);
                    } else if(size < 1) {
                        showCheckBoxes = false;
                        firstActivity.setEditInvisible();
                    } else {
                        propertiesIB.setVisibility(View.VISIBLE);
                        refreshIB.setVisibility(View.INVISIBLE);
                    }


                }
                else {
                    //We will show file opening helper
                    try {
                        PartialHalfTask downloadTask = db.getHalfDownloadTask(taskID);

                        DocumentFile documentFile = DocumentFile.fromTreeUri(context,Uri.parse(downloadTask.getDirPath()));
                        if(documentFile != null)
                        {
                            DocumentFile innerDocumentFile =  documentFile.findFile(downloadTask.getFileName());
                            if(innerDocumentFile != null)
                            {
                                final String mimeType = innerDocumentFile.getType();

                                if(HelperTextUtility.isNotEmpty(mimeType))
                                {
                                    if(mimeType.equals("application/vnd.android.package-archive"))
                                    {
                                        showToast(R.string.open_this_apk_file_from_file_manager_of_your_device);
                                    } else {
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setDataAndType(innerDocumentFile.getUri(),mimeType);
                                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        firstActivity.startActivity(intent);
                                    }

                                } else {
                                    showToast(R.string.unknown_file_type);
                                }
                            } else {
                                showToast(R.string.unable_to_locate_file);
                            }

                        } else {
                            showToast(R.string.unable_to_locate_file);
                        }
                    } catch (Exception e)
                    {
                        showToast(R.string.no_app_found_that_will_open_this_type_of_file);
                    }
                }
            });

            mcv.setOnLongClickListener(view -> {
                if(!mcv.isChecked())
                {
                    showCheckBoxes = true;
                    editLL.setVisibility(View.VISIBLE);
                    tabsRL.setVisibility(View.GONE);
                    propertiesIB.setVisibility(View.VISIBLE);
                    refreshIB.setVisibility(View.INVISIBLE);

                    mcv.setChecked(true);
                    selectedTasksArrayList.add(completedTaskIDs.get(getBindingAdapterPosition()));
                    firstActivity.setCountTVText(selectedTasksArrayList.size());

                }

                return true;
            });
        }

        private void showToast(int resID)
        {
            if (toast != null) {
                toast.cancel();
            }
            toast = Toast.makeText(context, resID, Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    void showDeleteDialog()
    {
        if(selectedTasksArrayList.size() >= 1)
        {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            View view = inflater.inflate(R.layout.popup_delete_multiple_download_tasks,mainContainer,false);

            final TextView selectedTasksCountTVDeleteMultiplePopup = view.findViewById(R.id.selectedTasksCountTVDeleteMultiplePopup);
            deleteMultipleSourceFilesCheckBox = view.findViewById(R.id.deleteSourceFileCB);
            final MaterialButton noBtn,yesBtn;
            noBtn = view.findViewById(R.id.noBtn);
            yesBtn = view.findViewById(R.id.yesBtn);

            selectedTasksCountTVDeleteMultiplePopup.setText(String.valueOf(selectedTasksArrayList.size()));

            builder.setView(view);
            dialog = builder.create();

            noBtn.setOnClickListener(view1 -> {
                dialog.dismiss();
                hideSelectCheckbox();
            });

            yesBtn.setOnClickListener(view12 -> {
                dialog.dismiss();
                final androidx.appcompat.app.AlertDialog innerDialog = createDeleteMessagePopup();
                new DeleteTasks(deleteMultipleSourceFilesCheckBox.isChecked(),innerDialog).start();
            });

            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
        }
        else if(completedTaskIDs.size() == 0)
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

            if (selectedTasksArrayList.size() > 0)
            {
                selectedTasksArrayList.clear();
            }

            try {
                firstActivity.analyzeCurrentQueuedTasks();
            } finally {
                firstActivity.setQueuedTasksCount();
                firstActivity.setCompletedCount(completedTaskIDs.size());
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

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                for (Integer dTID : selectedTasksArrayList)
                {
                    PartialHalfTask downloadTask = db.getHalfDownloadTask(dTID);
                    if(deleteSourceFileOrNot)
                    {
                        try {
                            //delete original source file from storage

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

                        } catch (Exception ignored) {}
                    }

                    db.deleteDownloadTask(downloadTask.getKeyId());
                    db.deleteDownloadTaskFromCompletedTable(downloadTask.getKeyId());

                    try {
                        notificationManager.cancel(downloadTask.getKeyId());
                    } catch (Exception ignored) {}

                    firstActivity.runOnUiThread(() -> {
                        int pos = completedTaskIDs.indexOf(dTID);
                        completedTaskIDs.remove(dTID);
                        notifyItemRemoved(pos);
                    });

                }
            } catch (Exception ignored) {}
            finally {
                firstActivity.runOnUiThread(() -> {
                    popupActionMessageTV.setText(R.string.task_successfully_deleted);
                    innerDialog.dismiss();
                });
            }
        }
    }
}
