package com.ginxdroid.flamebrowseranddownloader.classes;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import java.util.ArrayList;

public class URLEditText extends AppCompatEditText {
    private final ArrayList<EditTextListener> listeners;

    public URLEditText(@NonNull Context context) {
        super(context);
        listeners = new ArrayList<>();
    }

    public URLEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        listeners = new ArrayList<>();
    }

    public URLEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        listeners = new ArrayList<>();
    }

    public void addListener(EditTextListener listener) {
        try {
            listeners.add(listener);
        } catch (NullPointerException ignored) {

        }
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        boolean consumed = super.onTextContextMenuItem(id);
        if (id == android.R.id.paste) {
            onTextPaste();
        }
        return consumed;
    }

    private void onTextPaste(){
        for (EditTextListener listener : listeners) {
            listener.onUpdate();
        }
    }

    public interface EditTextListener {
        void onUpdate();
    }

}
