package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialCompletedTask;
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
        inflater = LayoutInflater.from(context);
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
}
