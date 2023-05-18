package com.ginxdroid.flamebrowseranddownloader.classes;

import static android.content.ClipDescription.MIMETYPE_TEXT_HTML;
import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;

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

    public static String getClipString(Context context) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);


            // If it does contain data, decide if you can handle the data.

            ClipDescription clipDescription;
            if (clipboard != null) {
                clipDescription = clipboard.getPrimaryClipDescription();
                if (clipDescription != null && (clipDescription.hasMimeType(MIMETYPE_TEXT_PLAIN) ||
                        clipDescription.hasMimeType(MIMETYPE_TEXT_HTML))) {

                    ClipData clipData = clipboard.getPrimaryClip();
                    if (clipData != null) {
                        ClipData.Item item = clipData.getItemAt(0);

                        if (item != null) {
                            CharSequence clipValue = item.getText();
                            if(isNotEmpty(clipValue))
                            {
                                return clipValue.toString();
                            }
                            else
                            {
                                return null;
                            }

                        }
                        else {
                            return null;
                        }
                    }
                    else {
                        return null;
                    }

                }
                else {
                    return null;
                }
            }
            else {
                return null;
            }

        }catch (Exception ignored)
        {
            return null;
        }

    }
}
