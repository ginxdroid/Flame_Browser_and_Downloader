package com.ginxdroid.flamebrowseranddownloader.classes;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ginxdroid.flamebrowseranddownloader.R;

public class TextDrawable extends Drawable {
    private final Paint mPaint;
    private final CharSequence mText;

    public TextDrawable(Context context, CharSequence mText) {
        this.mText = mText;
        mPaint = new Paint(Paint.FAKE_BOLD_TEXT_FLAG);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.primaryText,typedValue,true);
        int color = typedValue.data;

        mPaint.setColor(color);

        mPaint.setTextAlign(Paint.Align.CENTER);
        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,20,context.getResources().getDisplayMetrics());
        mPaint.setTextSize(textSize);

    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        canvas.drawText(mText, 0, mText.length(),(float) bounds.centerX(),
                (float)bounds.centerY() - ((mPaint.descent() + mPaint.ascent()) / 2), mPaint);
    }

    @Override
    public void setAlpha(int i) {
        mPaint.setAlpha(i);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }
}
