package com.ginxdroid.flamebrowseranddownloader.classes;

import android.annotation.SuppressLint;
import android.content.Context;

public class ResourceFinder {

    @SuppressLint("DiscouragedApi")
    public static int getResId(Context context, String variableName, String resourceName, String packageName) {
        try {
            return context.getResources().getIdentifier(variableName, resourceName, packageName);
        } catch (Exception e) {
            throw new RuntimeException("No resource ID found", e);
        }
    }

}
