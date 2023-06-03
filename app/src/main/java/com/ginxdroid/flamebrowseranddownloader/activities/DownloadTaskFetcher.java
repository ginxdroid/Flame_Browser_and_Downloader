package com.ginxdroid.flamebrowseranddownloader.activities;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.models.UserPreferences;
import com.ginxdroid.flamebrowseranddownloader.sheets.AddNewDTaskSheet;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DownloadTaskFetcher extends Thread {
    private final String url;
    private final String userAgent;
    private final String contentDisposition;
    private String mimeType;
    private int chunkMode;
    private String pauseResumeSupported;
    private int isPauseResumeSupported;
    private String fName,originalName;
    private final String pageURL;
    private long contentLength;
    private int defaultSegments;

    private final String name;
    private final NormalTabsRVAdapter normalTabsRVAdapter;
    private final DatabaseHandler db;
    private final Context context;
    private final AppCompatActivity activity;

    public DownloadTaskFetcher(String url, String userAgent, String contentDisposition, String mimeType, String pageURL, long contentLength,
                               String name, NormalTabsRVAdapter normalTabsRVAdapter, DatabaseHandler db, Context context,
                               AppCompatActivity activity) {
        this.url = url;
        this.userAgent = userAgent;
        this.contentDisposition = contentDisposition;
        this.mimeType = mimeType;
        this.pageURL = pageURL;
        this.contentLength = contentLength;
        this.name = name;
        this.normalTabsRVAdapter = normalTabsRVAdapter;
        this.db = db;
        this.context = context;
        this.activity = activity;
    }

    @Override
    public void run() {
        super.run();
        try {
            int s = 0;
            String extension = null;
            if(!normalTabsRVAdapter.isNetworkUrl(url))
            {
                if(url.startsWith("data:"))
                {
                    try {
                        chunkMode = 1;
                        pauseResumeSupported = "Unresumable";
                        isPauseResumeSupported = 0;
                        contentLength = -1;

                        String fileName;
                        if(!TextUtils.isEmpty(name))
                        {
                            fileName = name;
                        } else {
                            fileName = "download";
                        }

                        if(TextUtils.isEmpty(mimeType))
                        {
                            mimeType = url.substring(url.indexOf(":") + 1, url.indexOf(";"));
                        }

                        String fileRoot;
                        fileRoot = fileName;
                        extension = "."+ url.substring(url.indexOf("/") + 1, url.indexOf(";"));

                        fileName = fileRoot + extension;
                        originalName = fileName;

                        UserPreferences userPreferences = db.getHalfUserPreferences();

                        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, Uri.parse(userPreferences.getDownloadPath()));

                        int cnt = 1;

                        ArrayList<String> fileNamesInDb = db.getAllDownloadTaskNames();


                        boolean search = true;
                        if(pickedDir != null)
                        {
                            DocumentFile[] files = pickedDir.listFiles();
                            ArrayList<String> fileNames = new ArrayList<>();

                            for(DocumentFile documentFile: files)
                            {
                                fileNames.add(documentFile.getName());
                            }

                            fileNames.addAll(fileNamesInDb);

                            if(fileNames.contains(fileName))
                            {
                                while (search) {
                                    fName = fileRoot + "(" + cnt + ")" + extension;
                                    if (!fileNames.contains(fName)) {
                                        search = false;
                                    } else {
                                        cnt++;
                                    }
                                }
                            } else {
                                fName = fileName;
                            }

                            defaultSegments = db.getDefaultSegments();
                            s = 1;
                        }
                    } finally {
                        final int finalS = s;
                        final String finalExtension = extension;

                        activity.runOnUiThread(() -> {
                            if(finalS == 1)
                            {
                                createPopupDialog(url,userAgent,contentLength,pauseResumeSupported,fName,chunkMode,pageURL,
                                        mimeType,defaultSegments,finalExtension,originalName,isPauseResumeSupported);
                            } else {
                                Toast.makeText(context, R.string.oops_general_message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    activity.runOnUiThread(() -> Toast.makeText(context, R.string.cannot_download_file_from_such_url, Toast.LENGTH_SHORT).show());
                }
            } else {
                HttpURLConnection connectionRange = null;
                try {
                    if(contentLength == -1 || contentLength == 0)
                    {
                        chunkMode = 1;
                    } else {
                        chunkMode = 0;
                    }

                    String bytesRange = 0 + "-";
                    URL urlConnect = new URL(url);
                    connectionRange = (HttpURLConnection) urlConnect.openConnection();
                    connectionRange.setRequestProperty("Range","bytes="+bytesRange);
                    connectionRange.setReadTimeout(45000);
                    connectionRange.setConnectTimeout(45000);
                    connectionRange.setRequestProperty("User-Agent",userAgent);

                    if(connectionRange.getResponseCode() == HttpURLConnection.HTTP_PARTIAL)
                    {
                        pauseResumeSupported = "Resumable";
                        isPauseResumeSupported = 1;
                    } else {
                        pauseResumeSupported = "Unresumable";
                        isPauseResumeSupported = 0;
                    }

                    String contentLocation = connectionRange.getHeaderField("Content-Location");

                    String fileName;
                    if(!TextUtils.isEmpty(name))
                    {
                        fileName = name;
                    } else {
                        fileName = HelperUtil.chooseFileName(url,contentDisposition,contentLocation);
                    }

                    int dotIndex = fileName.indexOf('.');
                    String fileRoot;

                    if(dotIndex < 0)
                    {
                        fileRoot = fileName;
                        extension = HelperUtil.chooseExtensionFromMimeType(mimeType,true,url);
                        if(!HelperTextUtility.isNotEmpty(extension))
                        {
                            extension = ".unknown";
                        }
                    } else {
                        extension = HelperUtil.chooseExtensionFromFileName(mimeType,fileName,dotIndex,url);
                        fileRoot = fileName.substring(0,dotIndex);
                    }

                    fileName = fileRoot + extension;
                    originalName = fileName;

                    UserPreferences userPreferences = db.getHalfUserPreferences();

                    DocumentFile pickedDir = DocumentFile.fromTreeUri(context, Uri.parse(userPreferences.getDownloadPath()));

                    int cnt = 1;

                    ArrayList<String> fileNamesInDb = db.getAllDownloadTaskNames();

                    boolean search = true;
                    if(pickedDir != null)
                    {
                        DocumentFile[] files = pickedDir.listFiles();
                        ArrayList<String> fileNames = new ArrayList<>();

                        for(DocumentFile documentFile: files)
                        {
                            fileNames.add(documentFile.getName());
                        }

                        fileNames.addAll(fileNamesInDb);

                        if(fileNames.contains(fileName))
                        {
                            while (search) {
                                fName = fileRoot + "(" + cnt + ")" + extension;
                                if (!fileNames.contains(fName)) {
                                    search = false;
                                } else {
                                    cnt++;
                                }
                            }
                        } else {
                            fName = fileName;
                        }

                        defaultSegments = db.getDefaultSegments();
                        s = 1;
                }
            } catch (Exception ignored) {}
                finally {
                    try {
                        if(connectionRange != null)
                        {
                            connectionRange.getInputStream().close();
                            connectionRange.disconnect();
                        }
                    } catch (Exception ignored){
                    }

                    final int finalS = s;
                    final String finalExtension = extension;
                    activity.runOnUiThread(() -> {
                        if(finalS == 1)
                        {
                            createPopupDialog(url,userAgent,contentLength,pauseResumeSupported,fName,chunkMode,pageURL,
                                    mimeType,defaultSegments,finalExtension,originalName,isPauseResumeSupported);
                        } else {
                            Toast.makeText(context, R.string.oops_general_message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        } catch (Exception e)
        {
            Toast.makeText(context, R.string.oops_general_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void createPopupDialog(final String url,final String userAgent,final long contentLength,final String pauseResumeSupported, final String fileName,
                                   final int chunkMode, final String pageURL, final String mimeType, final int defaultSegments,final String extension,
                                   final String originalName,final int isPauseResumeSupported)
    {
        try {
            AddNewDTaskSheet addNewDTaskSheet = new AddNewDTaskSheet();
            Bundle bundle = new Bundle();
            bundle.putString("url",url);
            bundle.putString("userAgent",userAgent);
            bundle.putLong("contentLength",contentLength);
            bundle.putString("pauseResumeSupported",pauseResumeSupported);
            bundle.putString("fileName",fileName);
            bundle.putInt("chunkMode",chunkMode);
            bundle.putString("pageURL",pageURL);
            bundle.putString("mimeType",mimeType);
            bundle.putInt("defaultSegments",defaultSegments);
            bundle.putString("extension",extension);
            bundle.putString("originalName",originalName);
            bundle.putInt("isPauseResumeSupported",isPauseResumeSupported);

            addNewDTaskSheet.setArguments(bundle);
            addNewDTaskSheet.show(activity.getSupportFragmentManager(),"addNewDTaskSheet");
        } catch (Exception ignored) {}
    }
}
