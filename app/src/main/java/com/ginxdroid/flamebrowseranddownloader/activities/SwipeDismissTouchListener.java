package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.recyclerview.widget.RecyclerView;

public class SwipeDismissTouchListener implements View.OnTouchListener{
    //cache viewConfiguration system-wide values
    private final int mSlop;

    //fixed properties
    private final View mView;
    private final DismissCallbacks mCallbacks;

    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private VelocityTracker mVelocityTracker;
    private float mTranslationY = 0f;
    private final RecyclerView recyclerView;

    private final int mMinFlingVelocity;
    private boolean isFound = false;
    private int mViewHeight;
    private final NormalTabsRVAdapter normalTabsRVAdapter;

    public SwipeDismissTouchListener(Context context, RecyclerView recyclerView, NormalTabsRVAdapter normalTabsRVAdapter,
                                     View view, DismissCallbacks callbacks) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mView = view;
        mCallbacks = callbacks;
        this.recyclerView = recyclerView;
        this.normalTabsRVAdapter = normalTabsRVAdapter;
    }

    private boolean isAClick(float startX, float endX, float startY, float endY)
    {
        float differenceX = Math.abs(startX - endX);
        float differenceY = Math.abs(startY - endY);

        return !(differenceX > mSlop/* =5 */ || differenceY > mSlop);
    }

    public interface DismissCallbacks {
        //Called when the user has indicated that he would like to dismiss the view
        void onDismiss();

        //gets called when there is onClick on view
        void onClick();

        //It is used to determine whether user can swipe or not the view
        boolean canSwipe();
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        if(mCallbacks.canSwipe())
        {
            //offset because the view is translated during swipe
            motionEvent.offsetLocation(0f,mTranslationY);

            if(!isFound)
            {
                isFound = true;
                initiateVariables();
            }

            switch (motionEvent.getActionMasked())
            {
                case MotionEvent.ACTION_DOWN:
                {
                    mDownX = motionEvent.getRawX();
                    mDownY = motionEvent.getRawY();

                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(motionEvent);

                    return true;
                }
                case MotionEvent.ACTION_UP:
                {
                    if(mVelocityTracker == null)
                    {
                        break;
                    }

                    float endX = motionEvent.getRawX();
                    float endY = motionEvent.getRawY();

                    if(isAClick(mDownX,endX,mDownY,endY))
                    {
                        mCallbacks.onClick();
                        normalTabsRVAdapter.setIsSwiping(false);
                    }else {
                        float deltaY = endY - mDownY;
                        mVelocityTracker.addMovement(motionEvent);
                        mVelocityTracker.computeCurrentVelocity(1000);
                        float velocityY = mVelocityTracker.getYVelocity();
                        float absVelocityY = Math.abs(velocityY);

                        boolean dismiss = false;
                        boolean dismissBottom = false;

                        if(Math.abs(deltaY) > (mViewHeight * 0.5f) && mSwiping)
                        {
                            dismiss = true;
                            dismissBottom = deltaY > 0;
                        } else if (absVelocityY >= mMinFlingVelocity && mSwiping) {
                            //dismiss only if flinging in the same direction as dragging
                            dismiss = (velocityY < 0) == (deltaY < 0);
                            dismissBottom = velocityY > 0;
                        }

                        if(dismiss)
                        {
                            //dismiss
                            mView.animate().translationY(dismissBottom ? mViewHeight : -mViewHeight)
                                    .setDuration(175).withEndAction(() -> {
                                        mCallbacks.onDismiss();
                                        normalTabsRVAdapter.setIsSwiping(false);
                                    }).start();
                        }else if(mSwiping)
                        {
                            //cancel
                            mView.animate().translationY(0f).setDuration(175).withEndAction(() -> normalTabsRVAdapter.setIsSwiping(false)).start();

                        }

                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                        isFound = false;
                        mTranslationY = 0;
                        mDownX = 0;
                        mDownY = 0;
                        mSwiping = false;

                        break;
                    }
                }
                case MotionEvent.ACTION_CANCEL:
                {
                    if(mVelocityTracker == null)
                    {
                        break;
                    }

                    //cancel our dismiss
                    mView.animate().translationY(0f).setDuration(175).withEndAction(() -> normalTabsRVAdapter.setIsSwiping(false)).start();

                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    isFound = false;
                    mTranslationY = 0;
                    mDownX = 0;
                    mDownY = 0;
                    mSwiping = false;

                    break;
                }
                case MotionEvent.ACTION_MOVE:
                {
                    if(mVelocityTracker == null)
                    {
                        break;
                    }

                    mVelocityTracker.addMovement(motionEvent);
                    float deltaX = motionEvent.getRawX() - mDownX;
                    float deltaY = motionEvent.getRawY() - mDownY;

                    if(Math.abs(deltaY) > mSlop && Math.abs(deltaX) < Math.abs((deltaY) / 2))
                    {
                        mSwiping = true;
                        recyclerView.requestDisallowInterceptTouchEvent(true);
                        //Cancel view's touch
                        MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL | (motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        mView.onTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                    }

                    if(mSwiping)
                    {
                        normalTabsRVAdapter.setIsSwiping(true);
                        mTranslationY = deltaY;
                        mView.setTranslationY(deltaY);

                        return true;
                    }

                    break;

                }
            }

            return false;
        }

        return false;
    }

    private void initiateVariables()
    {
        mViewHeight = mView.getHeight();
    }
}
