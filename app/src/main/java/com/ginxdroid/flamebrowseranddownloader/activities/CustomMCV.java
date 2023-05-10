package com.ginxdroid.flamebrowseranddownloader.activities;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.card.MaterialCardView;

public class CustomMCV extends MaterialCardView {
    private SetterListener listener;

    public CustomMCV(Context context) {
        super(context);
    }

    public CustomMCV(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomMCV(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    interface SetterListener
    {

        void miniSetNow();

        void selectNow();

        void startSelect();


        void makeTitleBarVisible();

        void makeTitleBarInvisible();

    }


    void setListener(SetterListener listener)
    {
        this.listener = listener;
    }

    void callMiniSetNow()
    {
        listener.miniSetNow();
    }

    void callSelectNow()
    {
        listener.selectNow();
    }

    void callMakeTitleBarVisible()
    {
        listener.makeTitleBarVisible();
    }

    void callMakeTitleBarInvisible()
    {
        listener.makeTitleBarInvisible();
    }

    void callStartSelect()
    {
        listener.startSelect();
    }
}
