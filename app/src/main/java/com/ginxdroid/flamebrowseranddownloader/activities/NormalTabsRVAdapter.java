package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.google.android.material.bottomappbar.BottomAppBar;

import java.util.ArrayList;

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

        viewPool = new RecyclerView.RecycledViewPool();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                isScrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
            }
        });

        db = DatabaseHandler.getInstance(context);

        inflater = LayoutInflater.from(context);
        customHorizontalManager.setNormalTabsRVAdapter(NormalTabsRVAdapter.this);

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
        String urlString = urlsAL.get(position);

        holder.quickLinksRV.setRecycledViewPool(viewPool);

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

        private final CoordinatorLayout homePageCL;
        private final RecyclerView quickLinksRV;
        private final QuickLinksRVHomePageAdapter quickLinksRVHomePageAdapter;
        private final GridLayoutManager gridLayoutManager;

        private final RelativeLayout webViewContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            emptyFrameRL = itemView.findViewById(R.id.emptyFrameRL);
            emptyFrameLRL = emptyFrameRL.findViewById(R.id.emptyFrameLRL);
            tabPreviewIB = itemView.findViewById(R.id.tabPreviewIB);
            emptyCV = itemView.findViewById(R.id.emptyCV);


            ImageButton closeTabIB = itemView.findViewById(R.id.closeTabIB);
            closeTabIB.setOnClickListener(ViewHolder.this);

            emptyFrameRL.setVisibility(View.INVISIBLE);

            bottomToolbarCL = itemView.findViewById(R.id.bottomToolbarCL);
            reloadIB = bottomToolbarCL.findViewById(R.id.reloadIB);
            searchIB = bottomToolbarCL.findViewById(R.id.searchIB);
            showMoreIB = bottomToolbarCL.findViewById(R.id.showMoreIB);
            homePageIB = bottomToolbarCL.findViewById(R.id.homePageIB);
            tabsCountChildTVHPRL = bottomToolbarCL.findViewById(R.id.tabsCountChildTVHPRL);
            tabsCountChildTVHP = tabsCountChildTVHPRL.findViewById(R.id.tabsCountChildTVHP);

            homePageCL = itemView.findViewById(R.id.homePageCL);
            quickLinksRV = homePageCL.findViewById(R.id.quickLinksRV);
            quickLinksRVHomePageAdapter = new QuickLinksRVHomePageAdapter(db,context,NormalTabsRVAdapter.this,
                    ViewHolder.this,mainActivity,inflater);
            gridLayoutManager = new GridLayoutManager(context, 4);

            quickLinksRV.setLayoutManager(gridLayoutManager);
            quickLinksRV.setAdapter(quickLinksRVHomePageAdapter);
            setQL();

            webViewContainer = itemView.findViewById(R.id.webViewContainer);


            reloadIB.setOnClickListener(ViewHolder.this);
            searchIB.setOnClickListener(ViewHolder.this);
            showMoreIB.setOnClickListener(ViewHolder.this);
            homePageIB.setOnClickListener(ViewHolder.this);
            tabsCountChildTVHPRL.setOnClickListener(ViewHolder.this);


            itemView.post(this::doGestureWork);

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
                        customHorizontalManager.minimalSelect(getBindingAdapterPosition(), itemView, emptyCV, () -> emptyFrameRL.setVisibility(View.INVISIBLE));
                    }
                }

                @Override
                public boolean canSwipe() {
                    return !isScrolling && customHorizontalManager.ifNotScrolling() && isEmptyFrameLRLVisible;
                }
            }));
        }


        @Override
        public void onClick(View view) {
            int id = view.getId();
            if(id == R.id.tabsCountChildTVHPRL)
            {
                customHorizontalManager.minimalDeselectItem(() -> emptyFrameRL.setVisibility(View.VISIBLE));
            }else if(id == R.id.closeTabIB)
            {
                if(customHorizontalManager.ifNotScrolling())
                {
                    isSwiping = true;
                    itemView.animate().translationY(-recyclerViewContainerHeight)
                            .withEndAction(() -> mainActivity.runOnUiThread(() -> {
                                removeTab(getBindingAdapterPosition(), itemView);
                                isSwiping = false;
                            })).setDuration(175).start();
                }
            }else if(id == R.id.reloadIB)
            {

            }else if(id == R.id.searchIB)
            {

            }else if(id == R.id.showMoreIB)
            {

            }else if(id == R.id.homePageIB)
            {

            }
        }
    }
}
