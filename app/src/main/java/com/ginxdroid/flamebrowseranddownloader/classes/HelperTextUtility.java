package com.ginxdroid.flamebrowseranddownloader.classes;

public class HelperTextUtility {

    public static boolean isNotEmpty(CharSequence str) {
        if (str!=null && str.length() > 0)
        {
            String trimmed = str.toString().trim();
            return trimmed.length() > 0;
        }else
        {
            return false;
        }
    }
}
