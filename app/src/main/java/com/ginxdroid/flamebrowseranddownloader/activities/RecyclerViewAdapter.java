package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialBindDownloadTask;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialEight;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialFour;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialOne;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialSix;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialSixteen;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialThirtyTwo;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialTwo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

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
        inflater = LayoutInflater.from(context);
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
}
