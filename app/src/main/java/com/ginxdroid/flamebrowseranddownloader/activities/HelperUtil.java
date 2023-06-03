package com.ginxdroid.flamebrowseranddownloader.activities;

import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.webkit.MimeTypeMap;

import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelperUtil {
    //Regex used to parse content-disposition headers
    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile("attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1\\s*$",Pattern.CASE_INSENSITIVE);

    static String parseContentDisposition(String contentDisposition)
    {
        try {
            Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if(m.find())
            {
                return m.group(2);
            }
        } catch (IllegalStateException ex)
        {
            //this function is defined as returning null when it cannot parse the header
        }

        return null;
    }

    public static String chooseExtensionFromMimeType(String mimeType, boolean useDefaults, String url)
    {
        String extension = null;
        try {
            if(mimeType != null)
            {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                if(extension != null)
                {
                    if(extension.equals("bin")){
                        extension = null;
                    } else {
                        extension = "."+extension;
                    }
                }
            }
        } catch (Exception e)
        {
            extension = null;
        }

        if(extension == null)
        {
            if(mimeType != null && mimeType.toLowerCase().startsWith("text/"))
            {
                if(mimeType.equalsIgnoreCase("text/html"))
                {
                    extension = ".html";
                } else if (useDefaults) {
                    extension = ".txt";
                }
            } else if (useDefaults) {
                extension = MimeTypeMap.getFileExtensionFromUrl(url);

                if(extension == null)
                {
                    extension = ".unknown";
                }
            }
        }

        if(extension != null)
        {
            extension = extension.replaceAll("\\s+","");
        }

        return extension;
    }

    public static String chooseExtensionFromFileName(String mimeType,String fileName,int dotIndex,String url)
    {
        String extension = null;
        try {
            if(mimeType != null)
            {
                //compare lat segment of the extension against the mime type.
                //if there is mismatch then we wil discard the entire extension
                int lastDotIndex = fileName.lastIndexOf('.');
                String typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substring(lastDotIndex + 1));
                if(typeFromExt == null || !typeFromExt.equalsIgnoreCase(mimeType))
                {
                    extension = chooseExtensionFromMimeType(mimeType,false,url);
                }

            }
        } catch (Exception ignored) {}

        if(extension == null)
        {
            extension = fileName.substring(dotIndex);
        }

        extension = extension.replaceAll("\\s+","");
        return extension;
    }

    public static String chooseFileName(String url, String contentDisposition,String contentLocation)
    {
        String fileName = null;

        try {

            try {
                if(contentDisposition != null)
                {
                    fileName = parseContentDisposition(contentDisposition);
                    if(fileName != null)
                    {
                        int index = fileName.lastIndexOf('/') + 1;
                        if(index > 0)
                        {
                            fileName = fileName.substring(index);
                        }
                    }
                }
            } catch (Exception e){fileName = null;}

            try {
                if(fileName == null && contentLocation != null)
                {
                    String decodedContentLocation = Uri.decode(contentLocation);
                    if(decodedContentLocation != null && !decodedContentLocation.endsWith("/") &&
                            decodedContentLocation.indexOf('?') < 0)
                    {
                        int index = decodedContentLocation.lastIndexOf('/') + 1;
                        if(index > 0)
                        {
                            fileName = decodedContentLocation.substring(index);
                        } else {
                            fileName = decodedContentLocation;
                        }
                    }
                }
            } catch (Exception ignored) {}

            try {
                if(fileName == null)
                {
                    UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(url);
                    fileName = sanitizer.getValue("title");
                }
            } catch (Exception ignored){}

            try {
                if(fileName == null)
                {
                    if(contentDisposition != null)
                    {
                        fileName = Uri.decode(contentDisposition);
                        if(fileName != null)
                        {
                            fileName = fileName.replaceAll("[^a-zA-Z0-9.\\-]+"," ");
                            fileName = fileName.substring(fileName.indexOf("filename")).replace("filename","").replaceAll("UTF-8","");

                            int index = fileName.lastIndexOf('/') + 1;
                            if(index > 0)
                            {
                                fileName = fileName.substring(index);
                            }
                        }
                    }
                }
            } catch (Exception e){ fileName = null;}

            try {
                if(fileName == null)
                {
                    String decodedUrl = Uri.decode(url);
                    if(decodedUrl != null)
                    {
                        int queryIndex = decodedUrl.indexOf('?');

                        if(queryIndex > 0)
                        {
                            decodedUrl = decodedUrl.substring(0,queryIndex);
                        }

                        if(!decodedUrl.endsWith("/"))
                        {
                            int index = decodedUrl.lastIndexOf('/') + 1;

                            if(index > 0)
                            {
                                fileName = decodedUrl.substring(index);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            try {
                if(fileName == null)
                {
                    String decodedUrl = Uri.decode(url);
                    if(decodedUrl != null)
                    {
                        int queryIndex = decodedUrl.indexOf('?');
                        if(queryIndex > 0)
                        {
                            decodedUrl = decodedUrl.substring(queryIndex);
                        }

                        if(!decodedUrl.endsWith("/"))
                        {
                            int index = decodedUrl.lastIndexOf('/') + 1;
                            if(index > 0)
                            {
                                fileName = decodedUrl.substring(index);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            //finally if couldn't get fileName from url, get a generic fileName

            if(!HelperTextUtility.isNotEmpty(fileName))
            {
                fileName = "unknown";
            }

            fileName = fileName.replaceAll("[^a-zA-Z0-9.\\-]+"," ");

            return fileName;

        }catch (Exception e)
        {
            return "unknown";
        }
    }
}
