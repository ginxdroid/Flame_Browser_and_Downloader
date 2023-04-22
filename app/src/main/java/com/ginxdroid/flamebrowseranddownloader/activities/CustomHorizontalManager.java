package com.ginxdroid.flamebrowseranddownloader.activities;
import android.content.Context;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.R;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CustomHorizontalManager extends RecyclerView.LayoutManager {
    private NormalTabsRVAdapter normalTabsRVAdapter;
    private boolean scrolling = false;

    private int layDownType = -1;

    private int itemCount, currentActivePos = -1, lastPos;

    private RecyclerView.Recycler hereRecycler;

    private final float minimizeScale = 0.60f;
    private int width,height = 0;
    private float maxBoundary, invertedMaxBoundary, previousTrans,nextTrans;
    private float differ, invertedWidth;

    private View currentFSView = null;

    private final CoordinatorLayout recyclerViewContainer;

    private boolean noMoreLeft = false;
    private boolean noMoreRight = false;
    private boolean isScrollUnlocked = true;

    private boolean isDeleting = false;

    private final int two, six, zero;

    private final float maxElevation, maxOpenElevation;

    private final BottomAppBar appBarRL;

    private final FloatingActionButton fabAdd;

    private int deletePos;


    public CustomHorizontalManager(CoordinatorLayout recyclerViewContainer, Context context,
                                   BottomAppBar appBarRL, FloatingActionButton fabAdd) {
        this.recyclerViewContainer = recyclerViewContainer;
        this.appBarRL = appBarRL;
        this.fabAdd = fabAdd;

        zero = context.getResources().getDimensionPixelSize(R.dimen.zero);
        two = context.getResources().getDimensionPixelSize(R.dimen.two);
        six = context.getResources().getDimensionPixelSize(R.dimen.six);

        //normal elevation will be 2, maximum elevation for maximized tab will be 2 + 1f
        //and tab opener elevation will be maximum elevation + 1f so that
        //when user opens a new tab we can animate that newly opened tab nicely!
        maxElevation = two + 1f;
        maxOpenElevation = maxElevation + 1f;
    }

    void setDeletePos(int deletePos)
    {
        this.deletePos = deletePos;
    }

    void setNormalTabsRVAdapter(NormalTabsRVAdapter normalTabsRVAdapter) {
        this.normalTabsRVAdapter = normalTabsRVAdapter;
    }

    void setLayDownType(int layDownType)
    {
        this.layDownType = layDownType;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT,RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            if(!scrolling)
            {
                if (-1 == layDownType)
                {

                    if(currentFSView != null)
                    {
                        //We will run default layout related code
                    }

                } else {
                    itemCount = getItemCount();

                    hereRecycler = recycler;
                    lastPos = itemCount - 1;

                    if (height == 0)
                    {
                        try {
                            height = recyclerViewContainer.getHeight();
                            width = recyclerViewContainer.getWidth();
                            differ = (width * 0.36f);
                            invertedWidth = -width;
                            maxBoundary = ((2 * width) - (2 * differ));
                            invertedMaxBoundary = -maxBoundary;
                            nextTrans = width - differ;
                            previousTrans = -width + differ;
                        }finally {
                            horizontalLayDown(itemCount);
                        }
                    } else {
                        horizontalLayDown(itemCount);
                    }
                }
            }
        } catch (Exception ignored) {}

    }

    private void horizontalLayDown(int itemCount)
    {
        try {
            switch (layDownType) {
                case 0:
                {
                    try {
                        isDeleting = true;
                        noMoreLeft = false;
                        noMoreRight = false;
                        layDownType = -1;

                        detachAndScrapAttachedViews(hereRecycler);

                        int previousPos = deletePos - 1;
                        int nextPos = deletePos + 1;

                        if (deletePos < itemCount) {
                            try {
                                //next item
                                if (nextPos < itemCount) {
                                    {
                                        //add view
                                        final View view = hereRecycler.getViewForPosition(nextPos);
                                        addView(view);
                                        measureChild(view, 0, 0);
                                        layoutDecorated(view, 0, 0, width, height);

                                        view.post(() -> view.animate().scaleX(minimizeScale).scaleX(minimizeScale).translationX(nextTrans)
                                                .setDuration(75).start());
                                    }

                                    {
                                        //                                        run next loop
                                        for (int i = nextPos + 1, cnt = 2; i < itemCount; i++, cnt++) {
                                            final View view = hereRecycler.getViewForPosition(i);
                                            view.setTranslationX(getViewTranslation(cnt));
                                            view.setScaleX(minimizeScale);
                                            view.setScaleY(minimizeScale);

                                            addView(view);
                                            measureChild(view, 0, 0);
                                            layoutDecorated(view, 0, 0, width, height);

                                            detachAndScrapView(view, hereRecycler);
                                        }
                                    }
                                }

                                //previous items
                                if (previousPos >= 0) {
                                    {
                                        //add previous item
                                        final View view = hereRecycler.getViewForPosition(previousPos);
                                        addView(view);
                                        measureChild(view, 0, 0);
                                        layoutDecorated(view, 0, 0, width, height);

                                        view.post(() -> view.animate().scaleX(minimizeScale).scaleY(minimizeScale).translationX(previousTrans)
                                                .setDuration(75).start());
                                    }

                                    //run previous loop
                                    for (int i = previousPos - 1, cnt = -2; i >= 0; i--, cnt--) {
                                        final View view = hereRecycler.getViewForPosition(i);
                                        view.setTranslationX(getViewTranslation(cnt));
                                        view.setScaleX(minimizeScale);
                                        view.setScaleY(minimizeScale);

                                        addView(view);
                                        measureChild(view, 0, 0);
                                        layoutDecorated(view, 0, 0, width, height);

                                        detachAndScrapView(view, hereRecycler);
                                    }
                                }

                            } finally {
                                //add 1 next item
                                final View view = hereRecycler.getViewForPosition(deletePos);
                                addView(view);
                                measureChild(view, 0, 0);
                                layoutDecorated(view, 0, 0, width, height);

                                view.post(() -> view.animate().scaleX(minimizeScale).scaleY(minimizeScale).translationX(0f)
                                        .setDuration(75).withEndAction(() -> {
                                            deletePos = -1;
                                            isDeleting = false;
                                        }).start());
                            }
                        } else if (previousPos >= 0) {
                            try {
                                {
                                    int secondLastPos = previousPos - 1;

                                    if (secondLastPos >= 0) {
                                        {
                                            // add 1 previous item
                                            final View view = hereRecycler.getViewForPosition(secondLastPos);
                                            addView(view);
                                            measureChild(view, 0, 0);
                                            layoutDecorated(view, 0, 0, width, height);

                                            view.post(() -> view.animate().scaleY(minimizeScale).scaleY(minimizeScale).translationX(previousTrans)
                                                    .setDuration(75).start());
                                        }

                                        //run previous loop
                                        for (int i = secondLastPos - 1, cnt = -2; i >= 0; i--, cnt--) {
                                            final View view = hereRecycler.getViewForPosition(i);
                                            view.setTranslationX(getViewTranslation(cnt));
                                            view.setScaleX(minimizeScale);
                                            view.setScaleY(minimizeScale);

                                            addView(view);
                                            measureChild(view, 0, 0);
                                            layoutDecorated(view, 0, 0, width, height);

                                            detachAndScrapView(view, hereRecycler);
                                        }
                                    }
                                }
                            } finally {
                                //add 1 middle item
                                final View view = hereRecycler.getViewForPosition(previousPos);
                                addView(view);
                                measureChild(view, 0, 0);
                                layoutDecorated(view, 0, 0, width, height);

                                view.post(() -> view.animate().scaleX(minimizeScale).scaleY(minimizeScale).translationX(0f)
                                        .setDuration(75).withEndAction(() -> {
                                            deletePos = -1;
                                            isDeleting = false;
                                        }).start());
                            }
                        } else {
                            deletePos = -1;
                            isDeleting = false;
                        }

                    } catch (Exception e) {
                        deletePos = -1;
                        isDeleting = false;
                    }
                    break;
            }
                case 1:

                    break;
                case 2:

                    break;
                case 3:

                    break;
                case 4:
                    //It will be new tab case
                    noMoreLeft = false;
                    noMoreRight = false;
                    layDownType = -1;
                    scrolling = true;
                    openBlockedPopupHorizontal();
                    break;
            }


        } catch (Exception e)
        {
            layDownType = -1;
            scrolling = false;
        }
    }

    private void openBlockedPopupHorizontal()
    {
        if (currentFSView != null)
        {
          //write code for current full screen tab also
        } else {
            isScrollUnlocked = false;
            currentActivePos = lastPos;
            currentFSView = hereRecycler.getViewForPosition(lastPos);
            setFSViewPropertiesHorizontal(currentFSView);
        }
    }

    private void setFSViewPropertiesHorizontal(final View view)
    {
        CustomMCV materialCardView = currentFSView.findViewById(R.id.emptyCV);

        materialCardView.setRadius(zero);

        addView(view);
        measureChild(view,0,0);

        layoutDecorated(view, 0, 0, width, height);

        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setTranslationX(0f);

        materialCardView.setCardElevation(maxElevation);

        view.post(() -> {
            view.animate().scaleY(1f).scaleX(1f).setDuration(175).withEndAction(() -> {
                //This runnable will gets called when animation is finished
//                        materialCardView.findViewById(R.id.emptyFrameRL).setVisibility(View.INVISIBLE);
                scrolling = false;
            }).start();

            hideButtons();
        });

    }

    interface SetterCallback
    {
        void setNow();
    }

    void minimalDeselectItem(final SetterCallback callback)
    {
        scrolling = true;
        noMoreRight = false;
        noMoreLeft = false;
        currentFSView = null;

        try {
            detachAndScrapAttachedViews(hereRecycler);
            int previousPos = currentActivePos - 1;
            int nextPos = currentActivePos + 1;

            if(nextPos<itemCount)
            {
                //add 1 next item
                final View view = hereRecycler.getViewForPosition(nextPos);
                view.setScaleX(minimizeScale);
                view.setScaleY(minimizeScale);
                view.setTranslationX(nextTrans);

                addView(view);
                measureChild(view,0,0);
                layoutDecorated(view,0,0,width,height);
            }

            if(previousPos>=0)
            {
                //add 1 previous item
                final View view = hereRecycler.getViewForPosition(previousPos);
                view.setScaleX(minimizeScale);
                view.setScaleY(minimizeScale);
                view.setTranslationX(previousTrans);

                addView(view);
                measureChild(view,0,0);
                layoutDecorated(view,0,0,width,height);
            }

            for(int i = previousPos - 1,cnt = -2; i >= 0;i--,cnt--)
            {
                final View view = hereRecycler.getViewForPosition(i);
                view.setTranslationX(getViewTranslation(cnt));

                view.setScaleX(minimizeScale);
                view.setScaleY(minimizeScale);
                addView(view);
                measureChild(view,0,0);
                layoutDecorated(view,0,0,width,height);

                detachAndScrapView(view, hereRecycler);
            }

            for(int i = nextPos + 1, cnt = 2;i < itemCount;i++,cnt++)
            {
                final View view = hereRecycler.getViewForPosition(i);
                view.setTranslationX(getViewTranslation(cnt));

                view.setScaleX(minimizeScale);
                view.setScaleY(minimizeScale);
                addView(view);
                measureChild(view,0,0);
                layoutDecorated(view,0,0,width,height);

                detachAndScrapView(view, hereRecycler);
            }

        }finally {
            final View view = hereRecycler.getViewForPosition(currentActivePos);
            addView(view);
            measureChild(view,0,0);
            layoutDecorated(view,0,0,width,height);

            setCVElevation(view);

            view.animate().scaleX(minimizeScale).scaleY(minimizeScale).translationX(0f).setDuration(175)
                    .withEndAction(() -> {
                        showButtons();
                        callback.setNow();

                        isScrollUnlocked = true;
                        currentActivePos = -1;
                        scrolling = false;
                    }).start();

        }
    }

    interface OnMaximizedCallBack
    {
        void onMaximized();
    }

    void minimalSelect(int position, View view, CustomMCV materialCardView,OnMaximizedCallBack callBack)
    {
        scrolling = true;
        isScrollUnlocked = false;
        hideButtons();
        currentActivePos = position;

        materialCardView.setRadius(zero);
        materialCardView.setCardElevation(maxElevation);

        view.animate().scaleX(1.0f).scaleY(1.0f).translationX(0f).setDuration(175)
                .withEndAction(() -> {
                    try {
                        currentFSView = view;
                        callBack.onMaximized();
                    }finally {
                        scrolling = false;
                    }
                }).start();
    }

    private void setCVElevation(View view)
    {
        CustomMCV materialCardView = view.findViewById(R.id.emptyCV);
        materialCardView.setCardElevation(two);
        materialCardView.setRadius(six);
    }

    private float getViewTranslation(int position)
    {
        return ((position * width) - (position * differ));
    }

    @Override
    public boolean canScrollHorizontally() {
        return isScrollUnlocked && !isDeleting && !scrolling;
    }

    @Override
    public boolean canScrollVertically() {
        return false;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            if(itemCount < 2)
            {
                return 0;
            }

            if(dx < 0)
            {
                if(!noMoreRight)
                {
                    scrollRight(Math.abs(dx));
                    return dx;
                }
            } else {
                if(!noMoreLeft)
                {
                    scrollLeft(dx);
                    return dx;
                }
            }
        }catch (Exception e)
        {
            return 0;
        }

        return 0;
    }

    private void scrollRight(int dx)
    {
        try{
            boolean tempRight = noMoreRight;
            try{
                noMoreLeft = false;
                scrolling = true;

                detachAndScrapAttachedViews(hereRecycler);
                try{
                    for(int i = lastPos; i > 0;i--)
                    {
                        View view = hereRecycler.getViewForPosition(i);
                        float translation = view.getTranslationX() + dx;

                        translation = Math.min(getViewTranslation(i), translation);
                        view.setTranslationX(translation);

                        if(ifNotInRange(translation))
                        {
                            hereRecycler.recycleView(view);
                        } else
                        {
                            addView(view);
                        }
                    }
                } finally {
                    View view = hereRecycler.getViewForPosition(0);
                    float translation = view.getTranslationX() + dx;
                    translation = Math.min(0f,translation);
                    view.setTranslationX(translation);

                    if(ifNotInRange(translation))
                    {
                        hereRecycler.recycleView(view);
                    } else {
                        addView(view);
                    }

                    if(translation == 0f)
                    {
                        noMoreRight = true;
                    }

                    scrolling = false;
                }
            } catch (Exception e)
            {
                try {
                    noMoreRight = tempRight;
                    scrolling = false;
                } catch (Exception e1)
                {
                    try{
                        scrolling = false;
                    }catch (Exception ignored){}
                }
            }

        }catch (Exception ignored){}
    }

    private void scrollLeft(int dx)
    {
        try{
           boolean tempLeft = noMoreLeft;
           try {
               noMoreRight = false;
               scrolling = true;

               detachAndScrapAttachedViews(hereRecycler);

               try {
                   for(int i=0;i<lastPos;i++)
                   {
                       View view = hereRecycler.getViewForPosition(i);
                       float translation = view.getTranslationX() - dx;
                       translation = Math.max(-getViewTranslation(lastPos - i), translation);
                       view.setTranslationX(translation);

                       if(ifNotInRange(translation))
                       {
                           hereRecycler.recycleView(view);
                       }else {
                           addView(view);
                       }
                   }
               }finally {
                   View view = hereRecycler.getViewForPosition(lastPos);
                   float translation = view.getTranslationX() - dx;
                   translation = Math.max(0f, translation);
                   view.setTranslationX(translation);

                   if(ifNotInRange(translation))
                   {
                       hereRecycler.recycleView(view);
                   }else {
                       addView(view);
                   }

                   if(translation == 0f)
                   {
                       noMoreLeft = true;
                   }

                   scrolling = false;
               }
           } catch (Exception e)
           {
               try {
                   noMoreLeft = tempLeft;
                   scrolling = false;
               }catch (Exception e1)
               {
                   try{
                       scrolling = false;
                   }catch (Exception ignored){}
               }
           }
        } catch (Exception ignored){}
    }

    boolean ifNotScrolling()
    {
        return !scrolling && !isDeleting;
    }

    private boolean ifNotInRange(float translation)
    {
        return translation > maxBoundary || translation < invertedMaxBoundary;
    }

    private void hideButtons()
    {
        //hiding our bottom app bar and fab
        appBarRL.performHide();
        fabAdd.hide();
    }

    private void showButtons()
    {
        //will become handy when we want to show buttons once again
        appBarRL.performShow();
        fabAdd.show();
    }
}
