package com.ginxdroid.flamebrowseranddownloader.activities;

import static android.system.OsConstants.SEEK_SET;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStatVfs;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.classes.HumanReadableFormat;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.DownloadTask;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadingService extends Service {
    private DatabaseHandler db;
    private ExecutorService mainExecutorService;
    private LocalBroadcastManager localBroadcastManager;

    private final String downloadingIntent = "dI";
    private final String downloadingPausedIntent = "dPI";
    private final String downloadCompleteIntent = "dCI";
    private final String errorOccurredIntent = "eCI";

    private final ArrayList<Integer> submittedTasks = new ArrayList<>();

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final AtomicInteger starting = new AtomicInteger(1);
    private final AtomicInteger isForeground = new AtomicInteger(-1);

    private DecimalFormat dec,speedDec;
    private NotificationManagerCompat notificationManager;

    public DownloadingService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = NotificationManagerCompat.from(this);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel("FDD");
            if(notificationChannel == null)
            {
                notificationChannel = new NotificationChannel("FDD","Flame Downloader", NotificationManager.IMPORTANCE_LOW);
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        dec = new DecimalFormat("0.##");
        speedDec = new DecimalFormat("0.#");

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        connectivityManager = (ConnectivityManager) DownloadingService.this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest.Builder req = new NetworkRequest.Builder();
        req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        req.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        db = DatabaseHandler.getInstance(this);

        mainExecutorService = Executors.newFixedThreadPool(db.getSimultaneousTasks());

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                int size = submittedTasks.size();
                if(starting.get() == 0)
                {
                    try {
                        if(mainExecutorService != null)
                        {
                            if(!mainExecutorService.isShutdown())
                            {
                                mainExecutorService.shutdownNow();
                            }
                            mainExecutorService = null;
                        }

                        mainExecutorService = Executors.newFixedThreadPool(db.getSimultaneousTasks());

                        for(int i = 0; i < size; i++)
                        {
                            try {
                                notificationManager.cancel(submittedTasks.get(i));
                            } catch (Exception ignored) {}
                        }

                        isForeground.set(-1);
                        stopForeground(true);
                    } finally {
                        for(int i = 0; i < size; i++)
                        {
                            int dTID = submittedTasks.get(i);
                            db.updateDownloadTaskNA(dTID,3,"Queued","-");
                            sendIntent(downloadingIntent,dTID);
                            mainExecutorService.execute(new DownloadsExecutor(dTID));
                        }
                    }
                }

            }
        };
    }


    private void sendIntent(String action, Integer dTID)
    {
        localBroadcastManager.sendBroadcast(new Intent(action).putExtra("dId",dTID));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Bundle b = intent.getExtras();

            String status = null;
            if(b != null)
            {
                status = b.getString("dStatus");
            }

            if(status != null)
            {
                switch (status)
                {
                    case "Pause":
                    {
                        int dId = b.getInt("dId");
                        pause(dId);
                        break;
                    }
                    case "ResumeFromRv":
                    {
                        starting.set(1);
                        int dId = b.getInt("dId");
                        resumeFromRv(dId);
                        break;
                    }
                    case "ResumeFromN":
                    {
                        starting.set(1);
                        int dId = b.getInt("dId");
                        resumeFromN(dId);
                        break;
                    }
                    case "downloadNow":
                    {
                        starting.set(1);
                        int dId = b.getInt("dId");
                        downloadNow(dId);
                        break;
                    }
                    case "deleteNow":
                    {
                        ArrayList<Integer> selectedArrayList = b.getIntegerArrayList("dSelectedArrayList");
                        if(selectedArrayList != null)
                        {
                            deleteNow(selectedArrayList);
                        }
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        return START_STICKY;
    }

    private void downloadNow(int dId)
    {
        DownloadsExecutor runnable = new DownloadsExecutor(dId);
        mainExecutorService.submit(runnable);
        try {
            submittedTasks.add(dId);
        } catch (Exception ignored) {}

        try {
            notificationManager.cancel(dId);
        } catch (Exception ignored) {}

        if(isForeground.get() == -1)
        {
            isForeground.set(dId);

            CustomNotificationGenerator customNotificationGenerator = new CustomNotificationGenerator(DownloadingService.this,notificationManager);
            NotificationCompat.Builder builder = customNotificationGenerator.getRawBuilder();
            DownloadTask downloadTask = db.getDownloadTask(dId);
            if(downloadTask.getChunkMode() == 0)
            {
                startForeground(dId,customNotificationGenerator.createNotification(downloadTask,builder).build());
            } else {
                startForeground(dId,new CustomNotificationGenerator(DownloadingService.this,notificationManager)
                        .createChunkNotification(downloadTask,builder).build());
            }
        }
    }

    private void resumeFromN(int dId)
    {
        db.updateDownloadTaskNA(dId,3,"Queued","-");

        try {
            notificationManager.cancel(dId);
        } catch (Exception ignored) {}

        String resumingIntent = "rI";
        sendIntent(resumingIntent,dId);

        DownloadsExecutor runnable = new DownloadsExecutor(dId);
        mainExecutorService.submit(runnable);

        try {
            submittedTasks.add(dId);
        } catch (Exception ignored) {}

        try {
            notificationManager.cancel(dId);
        } catch (Exception ignored) {}

        if(isForeground.get() == -1)
        {
            isForeground.set(dId);

            CustomNotificationGenerator customNotificationGenerator = new CustomNotificationGenerator(DownloadingService.this,notificationManager);
            NotificationCompat.Builder builder = customNotificationGenerator.getRawBuilder();

            DownloadTask downloadTask = db.getDownloadTask(dId);
            if(downloadTask.getChunkMode() == 0)
            {
                startForeground(dId,customNotificationGenerator.createNotification(downloadTask,builder).build());
            } else {
                startForeground(dId,new CustomNotificationGenerator(DownloadingService.this,notificationManager)
                        .createChunkNotification(downloadTask,builder).build());
            }
        }
    }

    private void resumeFromRv(int dId)
    {
        DownloadsExecutor runnable = new DownloadsExecutor(dId);
        mainExecutorService.submit(runnable);

        try {
            submittedTasks.add(dId);
        } catch (Exception ignored) {}

        try {
            notificationManager.cancel(dId);
        } catch (Exception ignored) {}

        if(isForeground.get() == -1)
        {
            isForeground.set(dId);

            CustomNotificationGenerator customNotificationGenerator = new CustomNotificationGenerator(DownloadingService.this,notificationManager);
            NotificationCompat.Builder builder = customNotificationGenerator.getRawBuilder();

            DownloadTask downloadTask = db.getDownloadTask(dId);
            if(downloadTask.getChunkMode() == 0)
            {
                startForeground(dId,customNotificationGenerator.createNotification(downloadTask,builder).build());
            } else {
                startForeground(dId,new CustomNotificationGenerator(DownloadingService.this,notificationManager)
                        .createChunkNotification(downloadTask,builder).build());
            }
        }

    }

    private void pause(int dId)
    {
        db.initPauseOfDownloadTask(dId);

        try {
            submittedTasks.remove((Integer)dId);
        } catch (Exception ignored) {}
        finally {
            doStopWork(dId);
            final DownloadTask downloadTask = db.getDownloadTask(dId);
            downloadTask.setTimeLeft("Paused");

            if(downloadTask.getChunkMode() == 0)
            {
                downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSize(downloadTask.getDownloadedBytes(),dec)
                        + "/" + HumanReadableFormat.calculateHumanReadableSize(downloadTask.getTotalBytes(),dec)) ;
            } else {
                downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSize(downloadTask.getDownloadedBytes(),dec));
            }

            db.updateDownloadTaskTimeSpeed(downloadTask);

            sendIntent(downloadingPausedIntent, dId);

            CustomNotificationGenerator customNotificationGenerator = new CustomNotificationGenerator(DownloadingService.this,notificationManager);
            customNotificationGenerator.createPausedNotification(downloadTask,customNotificationGenerator.getRawBuilder());

        }
    }

    private void doStopWork(int dId)
    {
        int size = submittedTasks.size();

        if(size == 0)
        {
            if(!mainExecutorService.isShutdown())
            {
                mainExecutorService.shutdownNow();
            }

            isForeground.set(-1);
            stopForeground(true);
            stopSelf();
        } else if(size < db.getSimultaneousTasks() && dId == isForeground.get())
        {
            int dTID = submittedTasks.get(0);
            isForeground.set(dTID);
            CustomNotificationGenerator customNotificationGenerator = new CustomNotificationGenerator(DownloadingService.this,notificationManager);
            NotificationCompat.Builder builder = customNotificationGenerator.getRawBuilder();

            DownloadTask downloadTask = db.getDownloadTask(dTID);

            if(downloadTask.getChunkMode() == 0)
            {
                startForeground(dTID,customNotificationGenerator.createNotification(downloadTask,builder).build());
            } else {
                startForeground(dTID,customNotificationGenerator.createChunkNotification(downloadTask,builder).build());
            }
        } else if(dId == isForeground.get())
        {
            isForeground.set(-1);
            stopForeground(true);
        }
    }

    private void deleteNow(ArrayList<Integer> selectedArrayList)
    {
        //Pause all tasks first
        try {
            isForeground.set(-1);

            stopForeground(true);

            for(int i = 0;i < selectedArrayList.size();i++)
            {
                final Integer dTID = selectedArrayList.get(i);
                db.updateDownloadTaskStatus(dTID,0);

                try {
                    submittedTasks.remove(dTID);
                } catch (Exception ignored) {}

                try {
                    notificationManager.cancel(dTID);
                } catch (Exception ignored) {}
            }
        } finally {
            String doDeletionWorkIntent = "dDWI";
            sendIntent(doDeletionWorkIntent,0);

            int size = submittedTasks.size();
            if(size == 0) {
                if (!mainExecutorService.isShutdown()) {
                    mainExecutorService.shutdownNow();
                }
                stopSelf();
            } else if(size < db.getSimultaneousTasks())
            {
                int dTID = submittedTasks.get(0);
                isForeground.set(dTID);
                CustomNotificationGenerator customNotificationGenerator = new CustomNotificationGenerator(DownloadingService.this,notificationManager);
                NotificationCompat.Builder builder = customNotificationGenerator.getRawBuilder();

                DownloadTask downloadTask = db.getDownloadTask(dTID);

                if(downloadTask.getChunkMode() == 0)
                {
                    startForeground(dTID,customNotificationGenerator.createNotification(downloadTask,builder).build());
                } else {
                    startForeground(dTID,customNotificationGenerator.createChunkNotification(downloadTask,builder).build());
                }
            }

        }
    }

    @Override
    public void onDestroy() {
        try {
            if(!submittedTasks.isEmpty())
            {
                submittedTasks.clear();
            }
        } catch (Exception ignored) {}

        try {
            if(networkCallback != null)
            {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception ignored) {}
        finally {
            super.onDestroy();
        }
    }

    private final class DownloadsExecutor implements Runnable {

        private DownloadTask downloadTask;
        private long sizeOfEachSegment;
        private long bytesDone;

        private AtomicLong tZero,tOne,tTwo,tThree,tFour,tFive,tSix,tSeven,tEight,tNine,tTen,tEleven,tTwelve,tThirteen,tFourteen,tFifteen,
        tSixteen,tSeventeen,tEighteen,tNineteen,tTwenty,tTwentyOne,tTwentyTwo,tTwentyThree,tTwentyFour,tTwentyFive,tTwentySix,tTwentySeven,
                tTwentyEight,tTwentyNine,tThirty,tThirtyOne;

        private AtomicInteger pZero,pOne,pTwo,pThree,pFour,pFive,pSix,pSeven,pEight,pNine,pTen,pEleven,pTwelve,pThirteen,pFourteen,pFifteen,
                pSixteen,pSeventeen,pEighteen,pNineteen,pTwenty,pTwentyOne,pTwentyTwo,pTwentyThree,pTwentyFour,pTwentyFive,pTwentySix,pTwentySeven,
                pTwentyEight,pTwentyNine,pThirty,pThirtyOne;

        private long totalSize;
        private long previousBytes;
        private long bytesDelta;
        private int segmentsPerDownload;

        private ExecutorService executorService;

        private AtomicInteger chunkDownloading;
        private final int dTID;
        private Uri newFileUri;
        private AtomicInteger downloadOrException;
        private final CustomNotificationGenerator customNotificationGenerator;
        private final NotificationCompat.Builder builder;
        private long previousRemainingTime = 0L;
        private int retryTime = 0;
        private final Object monitor = new Object();

        DownloadsExecutor(Integer dTID)
        {
            this.dTID = dTID;
            customNotificationGenerator = new CustomNotificationGenerator(DownloadingService.this,notificationManager);
            builder = customNotificationGenerator.getRawBuilder();
        }


        @Override
        public void run() {
            if(submittedTasks.contains(dTID))
            {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                downloadTask = db.getDownloadTask(dTID);

                if(downloadTask.getChunkMode() == 0)
                {
                    if(isForeground.get() == -1)
                    {
                        isForeground.set(dTID);
                        startForeground(dTID,customNotificationGenerator.createNotification(downloadTask,builder).build());
                    }

                    segmentsPerDownload = downloadTask.getSegmentsForDownloadTask();
                    totalSize = downloadTask.getTotalBytes();
                    sizeOfEachSegment = totalSize / segmentsPerDownload;
                    downloadOrException = new AtomicInteger();
                    chunkDownloading = new AtomicInteger();
                    chunkDownloading.set(0);
                    downloadOrException.set(0);
                    executorService = Executors.newFixedThreadPool(segmentsPerDownload);
                    previousBytes = downloadTask.getDownloadedBytes();
                    bytesDone = previousBytes;
                    bytesDelta = 0L;

                    try {
                        try {
                            DocumentFile pickedDir = DocumentFile.fromTreeUri(DownloadingService.this,Uri.parse(downloadTask.getDirPath()));
                            String dirName = null;
                            if(pickedDir != null)
                            {
                                dirName = pickedDir.getName();
                            }

                            if(dirName == null)
                            {
                                throw new Exception();
                            } else {
                                DocumentFile downloadFile = pickedDir.findFile(downloadTask.getFileName());
                                if(downloadFile == null)
                                {
                                    //file not found, create new one
                                    DocumentFile newlyCreated = pickedDir.createFile(downloadTask.getMimeType(),downloadTask.getFileName());

                                    if(newlyCreated != null)
                                    {
                                        newFileUri = newlyCreated.getUri();
                                        startAsFreshDownload();
                                    } else if (downloadOrException.get() == 0) {
                                        downloadOrException.set(6);
                                        exceptionOccurred();
                                    }
                                } else {
                                    //file found
                                    newFileUri = downloadFile.getUri();
                                    if(downloadTask.getIsPauseResumeSupported() == 1)
                                    {
                                        //append at the end
                                        resumeDownload();
                                    } else {
                                        startAsFreshDownload();
                                    }
                                }
                            }
                        } finally {
                            HandlerThread handlerThread = new HandlerThread(String.valueOf(dTID));
                            handlerThread.start();
                            final Handler handler = new Handler(handlerThread.getLooper());
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (monitor)
                                    {
                                        monitor.notify();
                                        handler.postDelayed(this,1000);
                                    }
                                }
                            };
                            handler.postDelayed(runnable,1000);

                            switch (segmentsPerDownload)
                            {
                                case 1:
                                    oneSegment(handlerThread,handler,runnable);
                                    break;
                                case 2:
                                    twoSegments(handlerThread,handler,runnable);
                                    break;
                                case 4:
                                    fourSegments(handlerThread,handler,runnable);
                                    break;
                                case 6:
                                    sixSegments(handlerThread,handler,runnable);
                                    break;
                                case 8:
                                    eightSegments(handlerThread,handler,runnable);
                                    break;
                                case 16:
                                    sixteenSegments(handlerThread,handler,runnable);
                                    break;
                                case 32:
                                    thirtyTwoSegments(handlerThread,handler,runnable);
                                    break;
                            }
                        }
                    } catch (Exception e)
                    {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                            exceptionOccurred();
                        }
                    }

                } else {
                    //chunk mode
                    if(isForeground.get() == -1)
                    {
                        isForeground.set(dTID);
                        startForeground(dTID,customNotificationGenerator.createChunkNotification(downloadTask,builder).build());
                    }

                    segmentsPerDownload = downloadTask.getSegmentsForDownloadTask();
                    downloadOrException = new AtomicInteger();
                    chunkDownloading = new AtomicInteger();
                    chunkDownloading.set(0);
                    downloadOrException.set(0);
                    executorService = Executors.newFixedThreadPool(segmentsPerDownload);

                    previousBytes = downloadTask.getDownloadedBytes();
                    bytesDone = previousBytes;
                    bytesDelta = 0L;

                    try {
                        try {
                            DocumentFile pickedDir = DocumentFile.fromTreeUri(DownloadingService.this,Uri.parse(downloadTask.getDirPath()));
                            String dirName = null;
                            if(pickedDir != null)
                            {
                                dirName = pickedDir.getName();
                            }

                            if(dirName == null)
                            {
                                throw new Exception();
                            } else {
                                DocumentFile downloadFile = pickedDir.findFile(downloadTask.getFileName());
                                if(downloadFile == null)
                                {
                                    //file not found, create new one
                                    DocumentFile newlyCreated = pickedDir.createFile(downloadTask.getMimeType(),downloadTask.getFileName());

                                    if(newlyCreated != null)
                                    {
                                        newFileUri = newlyCreated.getUri();
                                        startAsChunkFreshDownload();
                                    } else if (downloadOrException.get() == 0) {
                                        downloadOrException.set(6);
                                        exceptionOccurredChunk();
                                    }
                                } else {
                                    //file found
                                    newFileUri = downloadFile.getUri();
                                    if(downloadTask.getIsPauseResumeSupported() == 1)
                                    {
                                        //append at the end
                                        resumeChunkDownload();
                                    } else {
                                        startAsChunkFreshDownload();
                                    }
                                }
                            }
                        } finally {
                            HandlerThread handlerThread = new HandlerThread(String.valueOf(dTID));
                            handlerThread.start();
                            final Handler handler = new Handler(handlerThread.getLooper());
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (monitor)
                                    {
                                        monitor.notify();
                                        handler.postDelayed(this,1000);
                                    }
                                }
                            };
                            handler.postDelayed(runnable,1000);

                            chunkOneSegment(handlerThread,handler,runnable);
                        }
                    } catch (Exception e)
                    {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                            exceptionOccurredChunk();
                        }
                    }
                }
            }

            if(Thread.currentThread().isAlive())
            {
                Thread.currentThread().interrupt();
            }
        }

        private void chunkOneSegment(HandlerThread handlerThread, Handler handler, Runnable runnable)
        {
            tZero = new AtomicLong();
            tZero.set(downloadTask.getDownloadedBytes());

            pZero = new AtomicInteger();
            pZero.set(downloadTask.getCurrentProgress());

            while (chunkDownloading.get() == 0)
            {
                synchronized (monitor)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                handler.removeCallbacks(runnable);
                                downloadOrException.set(1);
                                exceptionOccurredChunk();

                                break;
                            } else {
                                try {
                                    bytesDone = tZero.get();
                                    bytesDelta = bytesDone - previousBytes;
                                    previousBytes = bytesDone;
                                    downloadTask.setCurrentStatus(2);
                                    downloadTask.setDownloadedBytes(bytesDone);
                                    downloadTask.setCurrentProgress(0);
                                    try {
                                        String downloadSpeed;
                                        String timeLeft;

                                        if(bytesDelta > 0)
                                        {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(bytesDelta, speedDec);
                                            timeLeft = HumanReadableFormat.calculateHumanReadableSizeChunkDM(bytesDone,dec);
                                            retryTime = 0;
                                        } else {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec);
                                            if(retryTime > 20)
                                            {
                                                timeLeft = "Retrying("+(retryTime-20)+")";
                                            } else {
                                                timeLeft = HumanReadableFormat.calculateHumanReadableSizeChunkDM(bytesDone,dec);
                                            }

                                            retryTime++;
                                        }

                                        downloadTask.setDownloadSpeed(downloadSpeed);
                                        downloadTask.setTimeLeft(timeLeft);
                                    } catch (Exception e)
                                    {
                                        try {
                                            downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec));

                                            if(retryTime > 20)
                                            {
                                                String timeLeft = "Retrying("+(retryTime-20)+")";
                                                downloadTask.setTimeLeft(timeLeft);
                                            } else {
                                                downloadTask.setTimeLeft(HumanReadableFormat.calculateHumanReadableSizeChunkDM(bytesDone,dec));
                                            }

                                            retryTime++;
                                        } catch (Exception e1)
                                        {
                                            downloadTask.setDownloadSpeed("");
                                            downloadTask.setTimeLeft("Connecting");
                                        }
                                    }

                                    db.updateChunkDownloadTaskPartial(downloadTask);
                                    customNotificationGenerator.updateChunkNotification(downloadTask,builder);
                                    sendIntent(downloadingIntent,dTID);

                                    if(retryTime > 180)
                                    {
                                        downloadOrException.set(3);
                                    }
                                } catch (Exception ignored) {}

                                monitor.wait();
                            }
                        } else {
                            handler.removeCallbacks(runnable);
                            exceptionOccurredChunk();
                            break;
                        }
                    } catch (Exception e)
                    {
                        handler.removeCallbacks(runnable);
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                downloadOrException.set(1);
                            } else {
                                downloadOrException.set(8);
                            }
                        }
                        exceptionOccurredChunk();
                        break;
                    }
                }
            }

            handlerThread.quit();
            if(chunkDownloading.get() == 1 && downloadOrException.get() == 0)
            {
                //we completed download without any exception
                downloadTask = db.getBindDownloadTaskCompleteDM(dTID);
                chunkDownloadComplete(downloadTask);
            }
        }

        private void oneSegment(HandlerThread handlerThread, Handler handler, Runnable runnable)
        {
            tZero = new AtomicLong();
            tZero.set(downloadTask.getDownloadedBytes());

            pZero = new AtomicInteger();
            pZero.set(downloadTask.getCurrentProgress());

            while (bytesDone < totalSize)
            {
                synchronized (monitor)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                handler.removeCallbacks(runnable);
                                downloadOrException.set(1);
                                exceptionOccurred();

                                break;
                            } else {
                                try {
                                    bytesDone = tZero.get();
                                    downloadTask.setCurrentProgress(pZero.get());

                                    try {
                                        bytesDelta = bytesDone - previousBytes;
                                        String downloadSpeed;
                                        String timeLeft;

                                        long remaining = Math.max(0L,totalSize - bytesDone);
                                        if(bytesDelta > 0)
                                        {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(bytesDelta,speedDec);
                                            long remainingTime = (remaining / bytesDelta);
                                            previousRemainingTime = remainingTime;
                                            timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,remainingTime,dec);
                                            retryTime = 0;
                                        } else {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec);
                                            if(retryTime > 20)
                                            {
                                                timeLeft = "Retrying("+(retryTime - 20)+")";
                                            } else {
                                                previousRemainingTime = previousRemainingTime + 30;
                                                timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec);
                                            }
                                            retryTime++;
                                        }
                                        downloadTask.setDownloadSpeed(downloadSpeed);
                                        downloadTask.setTimeLeft(timeLeft);
                                    } catch (Exception e)
                                    {
                                        try {
                                            downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec));
                                            if(retryTime > 20)
                                            {
                                                String timeLeft = "Retrying("+(retryTime - 20)+")";
                                                downloadTask.setTimeLeft(timeLeft);
                                            } else {
                                                long remaining = Math.max(0L,totalSize - bytesDone);
                                                previousRemainingTime = previousRemainingTime + 30;
                                                downloadTask.setTimeLeft(HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec));
                                            }

                                            retryTime++;
                                        } catch (Exception e1)
                                        {
                                            downloadTask.setDownloadSpeed("");
                                            downloadTask.setTimeLeft("Connecting");
                                        }
                                    }

                                    previousBytes = bytesDone;

                                    downloadTask.setCurrentStatus(2);
                                    downloadTask.setDownloadedBytes(bytesDone);
                                    db.updateDownloadTaskOnePartial(downloadTask);

                                    customNotificationGenerator.updateNotification(downloadTask,builder);
                                    sendIntent(downloadingIntent,dTID);

                                    if(retryTime > 180)
                                    {
                                        downloadOrException.set(3);
                                    }

                                } catch (Exception ignored) {}
                                monitor.wait();
                            }
                        } else {
                            handler.removeCallbacks(runnable);
                            exceptionOccurred();
                            break;
                        }
                    } catch (Exception e)
                    {
                        handler.removeCallbacks(runnable);
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                downloadOrException.set(1);
                            } else {
                                downloadOrException.set(8);
                            }
                        }
                        exceptionOccurred();
                        break;
                    }
                }
            }

            handlerThread.quit();

            if(bytesDone >= downloadTask.getTotalBytes() && downloadOrException.get() == 0)
            {
                //we completed downloading of downloadTask
                downloadTask = db.getDownloadTask(dTID);
                downloadComplete(downloadTask);
            }
        }

        private void twoSegments(HandlerThread handlerThread, Handler handler, Runnable runnable)
        {

            tZero = new AtomicLong();
            tOne = new AtomicLong();

            tZero.set(downloadTask.getTSS1());
            tOne.set(downloadTask.getTSS2());


            pZero = new AtomicInteger();
            pOne = new AtomicInteger();

            pZero.set(downloadTask.getTPB1());
            pOne.set(downloadTask.getTPB2());

            long segOne,segTwo;

            while (bytesDone < totalSize)
            {
                synchronized (monitor)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                handler.removeCallbacks(runnable);
                                downloadOrException.set(1);
                                exceptionOccurred();

                                break;
                            } else {
                                try {

                                    segOne = tZero.get();
                                    segTwo = tOne.get();
                                    downloadTask.setTPB1(pZero.get());
                                    downloadTask.setTPB2(pOne.get());

                                    downloadTask.setTSS1(segOne);
                                    downloadTask.setTSS2(segTwo);

                                    bytesDone = segOne + segTwo;

                                    try {
                                        bytesDelta = bytesDone - previousBytes;
                                        String downloadSpeed;
                                        String timeLeft;

                                        long remaining = Math.max(0L,totalSize - bytesDone);
                                        if(bytesDelta > 0)
                                        {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(bytesDelta,speedDec);
                                            long remainingTime = (remaining / bytesDelta);
                                            previousRemainingTime = remainingTime;
                                            timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,remainingTime,dec);
                                            retryTime = 0;
                                        } else {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec);
                                            if(retryTime > 20)
                                            {
                                                timeLeft = "Retrying("+(retryTime - 20)+")";
                                            } else {
                                                previousRemainingTime = previousRemainingTime + 30;
                                                timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec);
                                            }
                                            retryTime++;
                                        }
                                        downloadTask.setDownloadSpeed(downloadSpeed);
                                        downloadTask.setTimeLeft(timeLeft);
                                    } catch (Exception e)
                                    {
                                        try {
                                            downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec));
                                            if(retryTime > 20)
                                            {
                                                String timeLeft = "Retrying("+(retryTime - 20)+")";
                                                downloadTask.setTimeLeft(timeLeft);
                                            } else {
                                                long remaining = Math.max(0L,totalSize - bytesDone);
                                                previousRemainingTime = previousRemainingTime + 30;
                                                downloadTask.setTimeLeft(HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec));
                                            }

                                            retryTime++;
                                        } catch (Exception e1)
                                        {
                                            downloadTask.setDownloadSpeed("");
                                            downloadTask.setTimeLeft("Connecting");
                                        }
                                    }

                                    previousBytes = bytesDone;

                                    downloadTask.setCurrentStatus(2);
                                    downloadTask.setDownloadedBytes(bytesDone);
                                    downloadTask.setCurrentProgress((int)((bytesDone * 100)/totalSize));
                                    db.updateDownloadTaskTwoPartial(downloadTask);

                                    customNotificationGenerator.updateNotification(downloadTask,builder);
                                    sendIntent(downloadingIntent,dTID);

                                    if(retryTime > 180)
                                    {
                                        downloadOrException.set(3);
                                    }

                                } catch (Exception ignored) {}
                                monitor.wait();
                            }
                        } else {
                            handler.removeCallbacks(runnable);
                            exceptionOccurred();
                            break;
                        }
                    } catch (Exception e)
                    {
                        handler.removeCallbacks(runnable);
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                downloadOrException.set(1);
                            } else {
                                downloadOrException.set(8);
                            }
                        }
                        exceptionOccurred();
                        break;
                    }
                }
            }

            handlerThread.quit();

            if(bytesDone >= downloadTask.getTotalBytes() && downloadOrException.get() == 0)
            {
                //we completed downloading of downloadTask
                downloadTask = db.getDownloadTask(dTID);
                downloadComplete(downloadTask);
            }
        }

        private void fourSegments(HandlerThread handlerThread, Handler handler, Runnable runnable)
        {

            tZero = new AtomicLong();
            tOne = new AtomicLong();
            tTwo = new AtomicLong();
            tThree = new AtomicLong();

            tZero.set(downloadTask.getTSS1());
            tOne.set(downloadTask.getTSS2());
            tTwo.set(downloadTask.getTSS3());
            tThree.set(downloadTask.getTSS4());

            pZero = new AtomicInteger();
            pOne = new AtomicInteger();
            pTwo = new AtomicInteger();
            pThree = new AtomicInteger();

            pZero.set(downloadTask.getTPB1());
            pOne.set(downloadTask.getTPB2());
            pTwo.set(downloadTask.getTPB3());
            pThree.set(downloadTask.getTPB4());

            long segOne,segTwo,segThree,segFour;

            while (bytesDone < totalSize)
            {
                synchronized (monitor)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                handler.removeCallbacks(runnable);
                                downloadOrException.set(1);
                                exceptionOccurred();

                                break;
                            } else {
                                try {

                                    segOne = tZero.get();
                                    segTwo = tOne.get();
                                    segThree = tTwo.get();
                                    segFour = tThree.get();
                                    downloadTask.setTPB1(pZero.get());
                                    downloadTask.setTPB2(pOne.get());
                                    downloadTask.setTPB3(pTwo.get());
                                    downloadTask.setTPB4(pThree.get());

                                    downloadTask.setTSS1(segOne);
                                    downloadTask.setTSS2(segTwo);
                                    downloadTask.setTSS3(segThree);
                                    downloadTask.setTSS4(segFour);

                                    bytesDone = segOne + segTwo + segThree + segFour;

                                    try {
                                        bytesDelta = bytesDone - previousBytes;
                                        String downloadSpeed;
                                        String timeLeft;

                                        long remaining = Math.max(0L,totalSize - bytesDone);
                                        if(bytesDelta > 0)
                                        {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(bytesDelta,speedDec);
                                            long remainingTime = (remaining / bytesDelta);
                                            previousRemainingTime = remainingTime;
                                            timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,remainingTime,dec);
                                            retryTime = 0;
                                        } else {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec);
                                            if(retryTime > 20)
                                            {
                                                timeLeft = "Retrying("+(retryTime - 20)+")";
                                            } else {
                                                previousRemainingTime = previousRemainingTime + 30;
                                                timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec);
                                            }
                                            retryTime++;
                                        }
                                        downloadTask.setDownloadSpeed(downloadSpeed);
                                        downloadTask.setTimeLeft(timeLeft);
                                    } catch (Exception e)
                                    {
                                        try {
                                            downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec));
                                            if(retryTime > 20)
                                            {
                                                String timeLeft = "Retrying("+(retryTime - 20)+")";
                                                downloadTask.setTimeLeft(timeLeft);
                                            } else {
                                                long remaining = Math.max(0L,totalSize - bytesDone);
                                                previousRemainingTime = previousRemainingTime + 30;
                                                downloadTask.setTimeLeft(HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec));
                                            }

                                            retryTime++;
                                        } catch (Exception e1)
                                        {
                                            downloadTask.setDownloadSpeed("");
                                            downloadTask.setTimeLeft("Connecting");
                                        }
                                    }

                                    previousBytes = bytesDone;

                                    downloadTask.setCurrentStatus(2);
                                    downloadTask.setDownloadedBytes(bytesDone);
                                    downloadTask.setCurrentProgress((int)((bytesDone * 100)/totalSize));
                                    db.updateDownloadTaskFourPartial(downloadTask);

                                    customNotificationGenerator.updateNotification(downloadTask,builder);
                                    sendIntent(downloadingIntent,dTID);

                                    if(retryTime > 180)
                                    {
                                        downloadOrException.set(3);
                                    }

                                } catch (Exception ignored) {}
                                monitor.wait();
                            }
                        } else {
                            handler.removeCallbacks(runnable);
                            exceptionOccurred();
                            break;
                        }
                    } catch (Exception e)
                    {
                        handler.removeCallbacks(runnable);
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                downloadOrException.set(1);
                            } else {
                                downloadOrException.set(8);
                            }
                        }
                        exceptionOccurred();
                        break;
                    }
                }
            }

            handlerThread.quit();

            if(bytesDone >= downloadTask.getTotalBytes() && downloadOrException.get() == 0)
            {
                //we completed downloading of downloadTask
                downloadTask = db.getDownloadTask(dTID);
                downloadComplete(downloadTask);
            }
        }

        private void sixSegments(HandlerThread handlerThread, Handler handler, Runnable runnable)
        {

            tZero = new AtomicLong();
            tOne = new AtomicLong();
            tTwo = new AtomicLong();
            tThree = new AtomicLong();
            tFour = new AtomicLong();
            tFive = new AtomicLong();

            tZero.set(downloadTask.getTSS1());
            tOne.set(downloadTask.getTSS2());
            tTwo.set(downloadTask.getTSS3());
            tThree.set(downloadTask.getTSS4());
            tFour.set(downloadTask.getTSS5());
            tFive.set(downloadTask.getTSS6());

            pZero = new AtomicInteger();
            pOne = new AtomicInteger();
            pTwo = new AtomicInteger();
            pThree = new AtomicInteger();
            pFour = new AtomicInteger();
            pFive = new AtomicInteger();

            pZero.set(downloadTask.getTPB1());
            pOne.set(downloadTask.getTPB2());
            pTwo.set(downloadTask.getTPB3());
            pThree.set(downloadTask.getTPB4());
            pFour.set(downloadTask.getTPB5());
            pFive.set(downloadTask.getTPB6());

            long segOne,segTwo,segThree,segFour,segFive,segSix;

            while (bytesDone < totalSize)
            {
                synchronized (monitor)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                handler.removeCallbacks(runnable);
                                downloadOrException.set(1);
                                exceptionOccurred();

                                break;
                            } else {
                                try {

                                    segOne = tZero.get();
                                    segTwo = tOne.get();
                                    segThree = tTwo.get();
                                    segFour = tThree.get();
                                    segFive = tFour.get();
                                    segSix = tFive.get();
                                    downloadTask.setTPB1(pZero.get());
                                    downloadTask.setTPB2(pOne.get());
                                    downloadTask.setTPB3(pTwo.get());
                                    downloadTask.setTPB4(pThree.get());
                                    downloadTask.setTPB5(pFour.get());
                                    downloadTask.setTPB6(pFive.get());

                                    downloadTask.setTSS1(segOne);
                                    downloadTask.setTSS2(segTwo);
                                    downloadTask.setTSS3(segThree);
                                    downloadTask.setTSS4(segFour);
                                    downloadTask.setTSS5(segFive);
                                    downloadTask.setTSS6(segSix);

                                    bytesDone = segOne + segTwo + segThree + segFour + segFive + segSix;

                                    try {
                                        bytesDelta = bytesDone - previousBytes;
                                        String downloadSpeed;
                                        String timeLeft;

                                        long remaining = Math.max(0L,totalSize - bytesDone);
                                        if(bytesDelta > 0)
                                        {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(bytesDelta,speedDec);
                                            long remainingTime = (remaining / bytesDelta);
                                            previousRemainingTime = remainingTime;
                                            timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,remainingTime,dec);
                                            retryTime = 0;
                                        } else {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec);
                                            if(retryTime > 20)
                                            {
                                                timeLeft = "Retrying("+(retryTime - 20)+")";
                                            } else {
                                                previousRemainingTime = previousRemainingTime + 30;
                                                timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec);
                                            }
                                            retryTime++;
                                        }
                                        downloadTask.setDownloadSpeed(downloadSpeed);
                                        downloadTask.setTimeLeft(timeLeft);
                                    } catch (Exception e)
                                    {
                                        try {
                                            downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec));
                                            if(retryTime > 20)
                                            {
                                                String timeLeft = "Retrying("+(retryTime - 20)+")";
                                                downloadTask.setTimeLeft(timeLeft);
                                            } else {
                                                long remaining = Math.max(0L,totalSize - bytesDone);
                                                previousRemainingTime = previousRemainingTime + 30;
                                                downloadTask.setTimeLeft(HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec));
                                            }

                                            retryTime++;
                                        } catch (Exception e1)
                                        {
                                            downloadTask.setDownloadSpeed("");
                                            downloadTask.setTimeLeft("Connecting");
                                        }
                                    }

                                    previousBytes = bytesDone;

                                    downloadTask.setCurrentStatus(2);
                                    downloadTask.setDownloadedBytes(bytesDone);
                                    downloadTask.setCurrentProgress((int)((bytesDone * 100)/totalSize));
                                    db.updateDownloadTaskSixPartial(downloadTask);

                                    customNotificationGenerator.updateNotification(downloadTask,builder);
                                    sendIntent(downloadingIntent,dTID);

                                    if(retryTime > 180)
                                    {
                                        downloadOrException.set(3);
                                    }

                                } catch (Exception ignored) {}
                                monitor.wait();
                            }
                        } else {
                            handler.removeCallbacks(runnable);
                            exceptionOccurred();
                            break;
                        }
                    } catch (Exception e)
                    {
                        handler.removeCallbacks(runnable);
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                downloadOrException.set(1);
                            } else {
                                downloadOrException.set(8);
                            }
                        }
                        exceptionOccurred();
                        break;
                    }
                }
            }

            handlerThread.quit();

            if(bytesDone >= downloadTask.getTotalBytes() && downloadOrException.get() == 0)
            {
                //we completed downloading of downloadTask
                downloadTask = db.getDownloadTask(dTID);
                downloadComplete(downloadTask);
            }
        }

        private void eightSegments(HandlerThread handlerThread, Handler handler, Runnable runnable)
        {

            tZero = new AtomicLong();
            tOne = new AtomicLong();
            tTwo = new AtomicLong();
            tThree = new AtomicLong();
            tFour = new AtomicLong();
            tFive = new AtomicLong();
            tSix = new AtomicLong();
            tSeven = new AtomicLong();

            tZero.set(downloadTask.getTSS1());
            tOne.set(downloadTask.getTSS2());
            tTwo.set(downloadTask.getTSS3());
            tThree.set(downloadTask.getTSS4());
            tFour.set(downloadTask.getTSS5());
            tFive.set(downloadTask.getTSS6());
            tSix.set(downloadTask.getTSS7());
            tSeven.set(downloadTask.getTSS8());

            pZero = new AtomicInteger();
            pOne = new AtomicInteger();
            pTwo = new AtomicInteger();
            pThree = new AtomicInteger();
            pFour = new AtomicInteger();
            pFive = new AtomicInteger();
            pSix = new AtomicInteger();
            pSeven = new AtomicInteger();

            pZero.set(downloadTask.getTPB1());
            pOne.set(downloadTask.getTPB2());
            pTwo.set(downloadTask.getTPB3());
            pThree.set(downloadTask.getTPB4());
            pFour.set(downloadTask.getTPB5());
            pFive.set(downloadTask.getTPB6());
            pSix.set(downloadTask.getTPB7());
            pSeven.set(downloadTask.getTPB8());

            long segOne,segTwo,segThree,segFour,segFive,segSix,segSeven,segEight;

            while (bytesDone < totalSize)
            {
                synchronized (monitor)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                handler.removeCallbacks(runnable);
                                downloadOrException.set(1);
                                exceptionOccurred();

                                break;
                            } else {
                                try {

                                    segOne = tZero.get();
                                    segTwo = tOne.get();
                                    segThree = tTwo.get();
                                    segFour = tThree.get();
                                    segFive = tFour.get();
                                    segSix = tFive.get();
                                    segSeven = tSix.get();
                                    segEight = tSeven.get();
                                    downloadTask.setTPB1(pZero.get());
                                    downloadTask.setTPB2(pOne.get());
                                    downloadTask.setTPB3(pTwo.get());
                                    downloadTask.setTPB4(pThree.get());
                                    downloadTask.setTPB5(pFour.get());
                                    downloadTask.setTPB6(pFive.get());
                                    downloadTask.setTPB7(pSix.get());
                                    downloadTask.setTPB8(pSeven.get());

                                    downloadTask.setTSS1(segOne);
                                    downloadTask.setTSS2(segTwo);
                                    downloadTask.setTSS3(segThree);
                                    downloadTask.setTSS4(segFour);
                                    downloadTask.setTSS5(segFive);
                                    downloadTask.setTSS6(segSix);
                                    downloadTask.setTSS7(segSeven);
                                    downloadTask.setTSS8(segEight);

                                    bytesDone = segOne + segTwo + segThree + segFour + segFive + segSix + segSeven + segEight;

                                    try {
                                        bytesDelta = bytesDone - previousBytes;
                                        String downloadSpeed;
                                        String timeLeft;

                                        long remaining = Math.max(0L,totalSize - bytesDone);
                                        if(bytesDelta > 0)
                                        {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(bytesDelta,speedDec);
                                            long remainingTime = (remaining / bytesDelta);
                                            previousRemainingTime = remainingTime;
                                            timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,remainingTime,dec);
                                            retryTime = 0;
                                        } else {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec);
                                            if(retryTime > 20)
                                            {
                                                timeLeft = "Retrying("+(retryTime - 20)+")";
                                            } else {
                                                previousRemainingTime = previousRemainingTime + 30;
                                                timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec);
                                            }
                                            retryTime++;
                                        }
                                        downloadTask.setDownloadSpeed(downloadSpeed);
                                        downloadTask.setTimeLeft(timeLeft);
                                    } catch (Exception e)
                                    {
                                        try {
                                            downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec));
                                            if(retryTime > 20)
                                            {
                                                String timeLeft = "Retrying("+(retryTime - 20)+")";
                                                downloadTask.setTimeLeft(timeLeft);
                                            } else {
                                                long remaining = Math.max(0L,totalSize - bytesDone);
                                                previousRemainingTime = previousRemainingTime + 30;
                                                downloadTask.setTimeLeft(HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec));
                                            }

                                            retryTime++;
                                        } catch (Exception e1)
                                        {
                                            downloadTask.setDownloadSpeed("");
                                            downloadTask.setTimeLeft("Connecting");
                                        }
                                    }

                                    previousBytes = bytesDone;

                                    downloadTask.setCurrentStatus(2);
                                    downloadTask.setDownloadedBytes(bytesDone);
                                    downloadTask.setCurrentProgress((int)((bytesDone * 100)/totalSize));
                                    db.updateDownloadTaskEightPartial(downloadTask);

                                    customNotificationGenerator.updateNotification(downloadTask,builder);
                                    sendIntent(downloadingIntent,dTID);

                                    if(retryTime > 180)
                                    {
                                        downloadOrException.set(3);
                                    }

                                } catch (Exception ignored) {}
                                monitor.wait();
                            }
                        } else {
                            handler.removeCallbacks(runnable);
                            exceptionOccurred();
                            break;
                        }
                    } catch (Exception e)
                    {
                        handler.removeCallbacks(runnable);
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                downloadOrException.set(1);
                            } else {
                                downloadOrException.set(8);
                            }
                        }
                        exceptionOccurred();
                        break;
                    }
                }
            }

            handlerThread.quit();

            if(bytesDone >= downloadTask.getTotalBytes() && downloadOrException.get() == 0)
            {
                //we completed downloading of downloadTask
                downloadTask = db.getDownloadTask(dTID);
                downloadComplete(downloadTask);
            }
        }

        private void sixteenSegments(HandlerThread handlerThread, Handler handler, Runnable runnable)
        {

            tZero = new AtomicLong();
            tOne = new AtomicLong();
            tTwo = new AtomicLong();
            tThree = new AtomicLong();
            tFour = new AtomicLong();
            tFive = new AtomicLong();
            tSix = new AtomicLong();
            tSeven = new AtomicLong();

            tEight = new AtomicLong();
            tNine = new AtomicLong();
            tTen = new AtomicLong();
            tEleven = new AtomicLong();
            tTwelve = new AtomicLong();
            tThirteen = new AtomicLong();
            tFourteen = new AtomicLong();
            tFifteen = new AtomicLong();

            tZero.set(downloadTask.getTSS1());
            tOne.set(downloadTask.getTSS2());
            tTwo.set(downloadTask.getTSS3());
            tThree.set(downloadTask.getTSS4());
            tFour.set(downloadTask.getTSS5());
            tFive.set(downloadTask.getTSS6());
            tSix.set(downloadTask.getTSS7());
            tSeven.set(downloadTask.getTSS8());

            tEight.set(downloadTask.getTSS9());
            tNine.set(downloadTask.getTSS10());
            tTen.set(downloadTask.getTSS11());
            tEleven.set(downloadTask.getTSS12());
            tTwelve.set(downloadTask.getTSS13());
            tThirteen.set(downloadTask.getTSS14());
            tFourteen.set(downloadTask.getTSS15());
            tFifteen.set(downloadTask.getTSS16());

            pZero = new AtomicInteger();
            pOne = new AtomicInteger();
            pTwo = new AtomicInteger();
            pThree = new AtomicInteger();
            pFour = new AtomicInteger();
            pFive = new AtomicInteger();
            pSix = new AtomicInteger();
            pSeven = new AtomicInteger();

            pEight = new AtomicInteger();
            pNine = new AtomicInteger();
            pTen = new AtomicInteger();
            pEleven = new AtomicInteger();
            pTwelve = new AtomicInteger();
            pThirteen = new AtomicInteger();
            pFourteen = new AtomicInteger();
            pFifteen = new AtomicInteger();

            pZero.set(downloadTask.getTPB1());
            pOne.set(downloadTask.getTPB2());
            pTwo.set(downloadTask.getTPB3());
            pThree.set(downloadTask.getTPB4());
            pFour.set(downloadTask.getTPB5());
            pFive.set(downloadTask.getTPB6());
            pSix.set(downloadTask.getTPB7());
            pSeven.set(downloadTask.getTPB8());

            pEight.set(downloadTask.getTPB9());
            pNine.set(downloadTask.getTPB10());
            pTen.set(downloadTask.getTPB11());
            pEleven.set(downloadTask.getTPB12());
            pTwelve.set(downloadTask.getTPB13());
            pThirteen.set(downloadTask.getTPB14());
            pFourteen.set(downloadTask.getTPB15());
            pFifteen.set(downloadTask.getTPB16());

            long segOne,segTwo,segThree,segFour,segFive,segSix,segSeven,segEight,
                    segNine,segTen,segEleven,segTwelve,segThirteen,segFourteen,segFifteen,segSixteen;

            while (bytesDone < totalSize)
            {
                synchronized (monitor)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                handler.removeCallbacks(runnable);
                                downloadOrException.set(1);
                                exceptionOccurred();

                                break;
                            } else {
                                try {

                                    segOne = tZero.get();
                                    segTwo = tOne.get();
                                    segThree = tTwo.get();
                                    segFour = tThree.get();
                                    segFive = tFour.get();
                                    segSix = tFive.get();
                                    segSeven = tSix.get();
                                    segEight = tSeven.get();

                                    segNine = tEight.get();
                                    segTen = tNine.get();
                                    segEleven = tTen.get();
                                    segTwelve = tEleven.get();
                                    segThirteen = tTwelve.get();
                                    segFourteen = tThirteen.get();
                                    segFifteen = tFourteen.get();
                                    segSixteen = tFifteen.get();

                                    downloadTask.setTPB1(pZero.get());
                                    downloadTask.setTPB2(pOne.get());
                                    downloadTask.setTPB3(pTwo.get());
                                    downloadTask.setTPB4(pThree.get());
                                    downloadTask.setTPB5(pFour.get());
                                    downloadTask.setTPB6(pFive.get());
                                    downloadTask.setTPB7(pSix.get());
                                    downloadTask.setTPB8(pSeven.get());

                                    downloadTask.setTPB9(pEight.get());
                                    downloadTask.setTPB10(pNine.get());
                                    downloadTask.setTPB11(pTen.get());
                                    downloadTask.setTPB12(pEleven.get());
                                    downloadTask.setTPB13(pTwelve.get());
                                    downloadTask.setTPB14(pThirteen.get());
                                    downloadTask.setTPB15(pFourteen.get());
                                    downloadTask.setTPB16(pFifteen.get());

                                    downloadTask.setTSS1(segOne);
                                    downloadTask.setTSS2(segTwo);
                                    downloadTask.setTSS3(segThree);
                                    downloadTask.setTSS4(segFour);
                                    downloadTask.setTSS5(segFive);
                                    downloadTask.setTSS6(segSix);
                                    downloadTask.setTSS7(segSeven);
                                    downloadTask.setTSS8(segEight);

                                    downloadTask.setTSS9(segNine);
                                    downloadTask.setTSS10(segTen);
                                    downloadTask.setTSS11(segEleven);
                                    downloadTask.setTSS12(segTwelve);
                                    downloadTask.setTSS13(segThirteen);
                                    downloadTask.setTSS14(segFourteen);
                                    downloadTask.setTSS15(segFifteen);
                                    downloadTask.setTSS16(segSixteen);

                                    bytesDone = segOne + segTwo + segThree + segFour + segFive + segSix + segSeven + segEight +
                                        segNine + segTen + segEleven + segTwelve + segThirteen + segFourteen + segFifteen + segSixteen;

                                    try {
                                        bytesDelta = bytesDone - previousBytes;
                                        String downloadSpeed;
                                        String timeLeft;

                                        long remaining = Math.max(0L,totalSize - bytesDone);
                                        if(bytesDelta > 0)
                                        {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(bytesDelta,speedDec);
                                            long remainingTime = (remaining / bytesDelta);
                                            previousRemainingTime = remainingTime;
                                            timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,remainingTime,dec);
                                            retryTime = 0;
                                        } else {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec);
                                            if(retryTime > 20)
                                            {
                                                timeLeft = "Retrying("+(retryTime - 20)+")";
                                            } else {
                                                previousRemainingTime = previousRemainingTime + 30;
                                                timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec);
                                            }
                                            retryTime++;
                                        }
                                        downloadTask.setDownloadSpeed(downloadSpeed);
                                        downloadTask.setTimeLeft(timeLeft);
                                    } catch (Exception e)
                                    {
                                        try {
                                            downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec));
                                            if(retryTime > 20)
                                            {
                                                String timeLeft = "Retrying("+(retryTime - 20)+")";
                                                downloadTask.setTimeLeft(timeLeft);
                                            } else {
                                                long remaining = Math.max(0L,totalSize - bytesDone);
                                                previousRemainingTime = previousRemainingTime + 30;
                                                downloadTask.setTimeLeft(HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec));
                                            }

                                            retryTime++;
                                        } catch (Exception e1)
                                        {
                                            downloadTask.setDownloadSpeed("");
                                            downloadTask.setTimeLeft("Connecting");
                                        }
                                    }

                                    previousBytes = bytesDone;

                                    downloadTask.setCurrentStatus(2);
                                    downloadTask.setDownloadedBytes(bytesDone);
                                    downloadTask.setCurrentProgress((int)((bytesDone * 100)/totalSize));
                                    db.updateDownloadTaskSixteenPartial(downloadTask);

                                    customNotificationGenerator.updateNotification(downloadTask,builder);
                                    sendIntent(downloadingIntent,dTID);

                                    if(retryTime > 180)
                                    {
                                        downloadOrException.set(3);
                                    }

                                } catch (Exception ignored) {}
                                monitor.wait();
                            }
                        } else {
                            handler.removeCallbacks(runnable);
                            exceptionOccurred();
                            break;
                        }
                    } catch (Exception e)
                    {
                        handler.removeCallbacks(runnable);
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                downloadOrException.set(1);
                            } else {
                                downloadOrException.set(8);
                            }
                        }
                        exceptionOccurred();
                        break;
                    }
                }
            }

            handlerThread.quit();

            if(bytesDone >= downloadTask.getTotalBytes() && downloadOrException.get() == 0)
            {
                //we completed downloading of downloadTask
                downloadTask = db.getDownloadTask(dTID);
                downloadComplete(downloadTask);
            }
        }

        private void thirtyTwoSegments(HandlerThread handlerThread, Handler handler, Runnable runnable)
        {

            tZero = new AtomicLong();
            tOne = new AtomicLong();
            tTwo = new AtomicLong();
            tThree = new AtomicLong();
            tFour = new AtomicLong();
            tFive = new AtomicLong();
            tSix = new AtomicLong();
            tSeven = new AtomicLong();

            tEight = new AtomicLong();
            tNine = new AtomicLong();
            tTen = new AtomicLong();
            tEleven = new AtomicLong();
            tTwelve = new AtomicLong();
            tThirteen = new AtomicLong();
            tFourteen = new AtomicLong();
            tFifteen = new AtomicLong();

            tSixteen = new AtomicLong();
            tSeventeen = new AtomicLong();
            tEighteen = new AtomicLong();
            tNineteen = new AtomicLong();
            tTwenty = new AtomicLong();
            tTwentyOne = new AtomicLong();
            tTwentyTwo = new AtomicLong();
            tTwentyThree = new AtomicLong();

            tTwentyFour = new AtomicLong();
            tTwentyFive = new AtomicLong();
            tTwentySix = new AtomicLong();
            tTwentySeven = new AtomicLong();
            tTwentyEight = new AtomicLong();
            tTwentyNine = new AtomicLong();
            tThirty = new AtomicLong();
            tThirtyOne = new AtomicLong();

            tZero.set(downloadTask.getTSS1());
            tOne.set(downloadTask.getTSS2());
            tTwo.set(downloadTask.getTSS3());
            tThree.set(downloadTask.getTSS4());
            tFour.set(downloadTask.getTSS5());
            tFive.set(downloadTask.getTSS6());
            tSix.set(downloadTask.getTSS7());
            tSeven.set(downloadTask.getTSS8());

            tEight.set(downloadTask.getTSS9());
            tNine.set(downloadTask.getTSS10());
            tTen.set(downloadTask.getTSS11());
            tEleven.set(downloadTask.getTSS12());
            tTwelve.set(downloadTask.getTSS13());
            tThirteen.set(downloadTask.getTSS14());
            tFourteen.set(downloadTask.getTSS15());
            tFifteen.set(downloadTask.getTSS16());

            tSixteen.set(downloadTask.getTSS1());
            tSeventeen.set(downloadTask.getTSS2());
            tEighteen.set(downloadTask.getTSS3());
            tNineteen.set(downloadTask.getTSS4());
            tTwenty.set(downloadTask.getTSS5());
            tTwentyOne.set(downloadTask.getTSS6());
            tTwentyTwo.set(downloadTask.getTSS7());
            tTwentyThree.set(downloadTask.getTSS8());

            tTwentyFour.set(downloadTask.getTSS9());
            tTwentyFive.set(downloadTask.getTSS10());
            tTwentySix.set(downloadTask.getTSS11());
            tTwentySeven.set(downloadTask.getTSS12());
            tTwentyEight.set(downloadTask.getTSS13());
            tTwentyNine.set(downloadTask.getTSS14());
            tThirty.set(downloadTask.getTSS15());
            tThirtyOne.set(downloadTask.getTSS16());

            pZero = new AtomicInteger();
            pOne = new AtomicInteger();
            pTwo = new AtomicInteger();
            pThree = new AtomicInteger();
            pFour = new AtomicInteger();
            pFive = new AtomicInteger();
            pSix = new AtomicInteger();
            pSeven = new AtomicInteger();

            pEight = new AtomicInteger();
            pNine = new AtomicInteger();
            pTen = new AtomicInteger();
            pEleven = new AtomicInteger();
            pTwelve = new AtomicInteger();
            pThirteen = new AtomicInteger();
            pFourteen = new AtomicInteger();
            pFifteen = new AtomicInteger();

            pSixteen = new AtomicInteger();
            pSeventeen = new AtomicInteger();
            pEighteen = new AtomicInteger();
            pNineteen = new AtomicInteger();
            pTwenty = new AtomicInteger();
            pTwentyOne = new AtomicInteger();
            pTwentyTwo = new AtomicInteger();
            pTwentyThree = new AtomicInteger();

            pTwentyFour = new AtomicInteger();
            pTwentyFive = new AtomicInteger();
            pTwentySix = new AtomicInteger();
            pTwentySeven = new AtomicInteger();
            pTwentyEight = new AtomicInteger();
            pTwentyNine = new AtomicInteger();
            pThirty = new AtomicInteger();
            pThirtyOne = new AtomicInteger();

            pZero.set(downloadTask.getTPB1());
            pOne.set(downloadTask.getTPB2());
            pTwo.set(downloadTask.getTPB3());
            pThree.set(downloadTask.getTPB4());
            pFour.set(downloadTask.getTPB5());
            pFive.set(downloadTask.getTPB6());
            pSix.set(downloadTask.getTPB7());
            pSeven.set(downloadTask.getTPB8());

            pEight.set(downloadTask.getTPB9());
            pNine.set(downloadTask.getTPB10());
            pTen.set(downloadTask.getTPB11());
            pEleven.set(downloadTask.getTPB12());
            pTwelve.set(downloadTask.getTPB13());
            pThirteen.set(downloadTask.getTPB14());
            pFourteen.set(downloadTask.getTPB15());
            pFifteen.set(downloadTask.getTPB16());

            pSixteen.set(downloadTask.getTPB17());
            pSeventeen.set(downloadTask.getTPB18());
            pEighteen.set(downloadTask.getTPB19());
            pNineteen.set(downloadTask.getTPB20());
            pTwenty.set(downloadTask.getTPB21());
            pTwentyOne.set(downloadTask.getTPB22());
            pTwentyTwo.set(downloadTask.getTPB23());
            pTwentyThree.set(downloadTask.getTPB24());

            pTwentyFour.set(downloadTask.getTPB25());
            pTwentyFive.set(downloadTask.getTPB26());
            pTwentySix.set(downloadTask.getTPB27());
            pTwentySeven.set(downloadTask.getTPB28());
            pTwentyEight.set(downloadTask.getTPB29());
            pTwentyNine.set(downloadTask.getTPB30());
            pThirty.set(downloadTask.getTPB31());
            pThirtyOne.set(downloadTask.getTPB32());

            long segOne,segTwo,segThree,segFour,segFive,segSix,segSeven,segEight,
                    segNine,segTen,segEleven,segTwelve,segThirteen,segFourteen,segFifteen,segSixteen,
            segSeventeen,segEighteen,segNineteen,segTwenty,segTwentyOne,segTwentyTwo,segTwentyThree,segTwentyFour,
                    segTwentyFive,segTwentySix,segTwentySeven,segTwentyEight,segTwentyNine,segThirty,segThirtyOne,segThirtyTwo;

            while (bytesDone < totalSize)
            {
                synchronized (monitor)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                handler.removeCallbacks(runnable);
                                downloadOrException.set(1);
                                exceptionOccurred();

                                break;
                            } else {
                                try {

                                    segOne = tZero.get();
                                    segTwo = tOne.get();
                                    segThree = tTwo.get();
                                    segFour = tThree.get();
                                    segFive = tFour.get();
                                    segSix = tFive.get();
                                    segSeven = tSix.get();
                                    segEight = tSeven.get();

                                    segNine = tEight.get();
                                    segTen = tNine.get();
                                    segEleven = tTen.get();
                                    segTwelve = tEleven.get();
                                    segThirteen = tTwelve.get();
                                    segFourteen = tThirteen.get();
                                    segFifteen = tFourteen.get();
                                    segSixteen = tFifteen.get();

                                    segSeventeen = tSixteen.get();
                                    segEighteen = tSeventeen.get();
                                    segNineteen = tEighteen.get();
                                    segTwenty = tNineteen.get();
                                    segTwentyOne = tTwenty.get();
                                    segTwentyTwo = tTwentyOne.get();
                                    segTwentyThree = tTwentyTwo.get();
                                    segTwentyFour = tTwentyThree.get();

                                    segTwentyFive = tTwentyFour.get();
                                    segTwentySix = tTwentyFive.get();
                                    segTwentySeven = tTwentySix.get();
                                    segTwentyEight = tTwentySeven.get();
                                    segTwentyNine = tTwentyEight.get();
                                    segThirty = tTwentyNine.get();
                                    segThirtyOne = tThirty.get();
                                    segThirtyTwo = tThirtyOne.get();

                                    downloadTask.setTPB1(pZero.get());
                                    downloadTask.setTPB2(pOne.get());
                                    downloadTask.setTPB3(pTwo.get());
                                    downloadTask.setTPB4(pThree.get());
                                    downloadTask.setTPB5(pFour.get());
                                    downloadTask.setTPB6(pFive.get());
                                    downloadTask.setTPB7(pSix.get());
                                    downloadTask.setTPB8(pSeven.get());

                                    downloadTask.setTPB9(pEight.get());
                                    downloadTask.setTPB10(pNine.get());
                                    downloadTask.setTPB11(pTen.get());
                                    downloadTask.setTPB12(pEleven.get());
                                    downloadTask.setTPB13(pTwelve.get());
                                    downloadTask.setTPB14(pThirteen.get());
                                    downloadTask.setTPB15(pFourteen.get());
                                    downloadTask.setTPB16(pFifteen.get());

                                    downloadTask.setTPB17(pSixteen.get());
                                    downloadTask.setTPB18(pSeventeen.get());
                                    downloadTask.setTPB19(pEighteen.get());
                                    downloadTask.setTPB20(pNineteen.get());
                                    downloadTask.setTPB21(pTwenty.get());
                                    downloadTask.setTPB22(pTwentyOne.get());
                                    downloadTask.setTPB23(pTwentyTwo.get());
                                    downloadTask.setTPB24(pTwentyThree.get());

                                    downloadTask.setTPB25(pTwentyFour.get());
                                    downloadTask.setTPB26(pTwentyFive.get());
                                    downloadTask.setTPB27(pTwentySix.get());
                                    downloadTask.setTPB28(pTwentySeven.get());
                                    downloadTask.setTPB29(pTwentyEight.get());
                                    downloadTask.setTPB30(pTwentyNine.get());
                                    downloadTask.setTPB31(pThirty.get());
                                    downloadTask.setTPB32(pThirtyOne.get());

                                    downloadTask.setTSS1(segOne);
                                    downloadTask.setTSS2(segTwo);
                                    downloadTask.setTSS3(segThree);
                                    downloadTask.setTSS4(segFour);
                                    downloadTask.setTSS5(segFive);
                                    downloadTask.setTSS6(segSix);
                                    downloadTask.setTSS7(segSeven);
                                    downloadTask.setTSS8(segEight);

                                    downloadTask.setTSS9(segNine);
                                    downloadTask.setTSS10(segTen);
                                    downloadTask.setTSS11(segEleven);
                                    downloadTask.setTSS12(segTwelve);
                                    downloadTask.setTSS13(segThirteen);
                                    downloadTask.setTSS14(segFourteen);
                                    downloadTask.setTSS15(segFifteen);
                                    downloadTask.setTSS16(segSixteen);

                                    downloadTask.setTSS17(segSeventeen);
                                    downloadTask.setTSS18(segEighteen);
                                    downloadTask.setTSS19(segNineteen);
                                    downloadTask.setTSS20(segTwenty);
                                    downloadTask.setTSS21(segTwentyOne);
                                    downloadTask.setTSS22(segTwentyTwo);
                                    downloadTask.setTSS23(segTwentyThree);
                                    downloadTask.setTSS24(segTwentyFour);

                                    downloadTask.setTSS25(segTwentyFive);
                                    downloadTask.setTSS26(segTwentySix);
                                    downloadTask.setTSS27(segTwentySeven);
                                    downloadTask.setTSS28(segTwentyEight);
                                    downloadTask.setTSS29(segTwentyNine);
                                    downloadTask.setTSS30(segThirty);
                                    downloadTask.setTSS31(segThirtyOne);
                                    downloadTask.setTSS32(segThirtyTwo);

                                    bytesDone = segOne + segTwo + segThree + segFour + segFive + segSix + segSeven + segEight +
                                            segNine + segTen + segEleven + segTwelve + segThirteen + segFourteen + segFifteen + segSixteen +
                                            segSeventeen + segEighteen + segNineteen + segTwenty + segTwentyOne + segTwentyTwo + segTwentyThree + segTwentyFour +
                                            segTwentyFive + segTwentySix + segTwentySeven + segTwentyEight + segTwentyNine + segThirty + segThirtyOne + segThirtyTwo;

                                    try {
                                        bytesDelta = bytesDone - previousBytes;
                                        String downloadSpeed;
                                        String timeLeft;

                                        long remaining = Math.max(0L,totalSize - bytesDone);
                                        if(bytesDelta > 0)
                                        {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(bytesDelta,speedDec);
                                            long remainingTime = (remaining / bytesDelta);
                                            previousRemainingTime = remainingTime;
                                            timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,remainingTime,dec);
                                            retryTime = 0;
                                        } else {
                                            downloadSpeed = HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec);
                                            if(retryTime > 20)
                                            {
                                                timeLeft = "Retrying("+(retryTime - 20)+")";
                                            } else {
                                                previousRemainingTime = previousRemainingTime + 30;
                                                timeLeft = HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec);
                                            }
                                            retryTime++;
                                        }
                                        downloadTask.setDownloadSpeed(downloadSpeed);
                                        downloadTask.setTimeLeft(timeLeft);
                                    } catch (Exception e)
                                    {
                                        try {
                                            downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSizeDm(0,speedDec));
                                            if(retryTime > 20)
                                            {
                                                String timeLeft = "Retrying("+(retryTime - 20)+")";
                                                downloadTask.setTimeLeft(timeLeft);
                                            } else {
                                                long remaining = Math.max(0L,totalSize - bytesDone);
                                                previousRemainingTime = previousRemainingTime + 30;
                                                downloadTask.setTimeLeft(HumanReadableFormat.calculateHumanReadableTimeDM(remaining,previousRemainingTime,dec));
                                            }

                                            retryTime++;
                                        } catch (Exception e1)
                                        {
                                            downloadTask.setDownloadSpeed("");
                                            downloadTask.setTimeLeft("Connecting");
                                        }
                                    }

                                    previousBytes = bytesDone;

                                    downloadTask.setCurrentStatus(2);
                                    downloadTask.setDownloadedBytes(bytesDone);
                                    downloadTask.setCurrentProgress((int)((bytesDone * 100)/totalSize));
                                    db.updateDownloadTaskThirtyTwoPartial(downloadTask);

                                    customNotificationGenerator.updateNotification(downloadTask,builder);
                                    sendIntent(downloadingIntent,dTID);

                                    if(retryTime > 180)
                                    {
                                        downloadOrException.set(3);
                                    }

                                } catch (Exception ignored) {}
                                monitor.wait();
                            }
                        } else {
                            handler.removeCallbacks(runnable);
                            exceptionOccurred();
                            break;
                        }
                    } catch (Exception e)
                    {
                        handler.removeCallbacks(runnable);
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isTaskPaused(dTID))
                            {
                                downloadOrException.set(1);
                            } else {
                                downloadOrException.set(8);
                            }
                        }
                        exceptionOccurred();
                        break;
                    }
                }
            }

            handlerThread.quit();

            if(bytesDone >= downloadTask.getTotalBytes() && downloadOrException.get() == 0)
            {
                //we completed downloading of downloadTask
                downloadTask = db.getDownloadTask(dTID);
                downloadComplete(downloadTask);
            }
        }

        private void downloadComplete(DownloadTask downloadTask)
        {
            downloadTask.setTimeLeft("Complete");
            downloadTask.setCurrentStatus(7);
            downloadTask.setDownloadedBytes(bytesDone);
            downloadTask.setCurrentProgress((int)((bytesDone * 100) / downloadTask.getTotalBytes()));

            try {
                downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSize(downloadTask.getTotalBytes(),dec));
            } catch (Exception e)
            {
                downloadTask.setDownloadSpeed("");
            }

            db.updateDownloadTaskCompletePartial(downloadTask);
            db.addCompletedTaskID(dTID);

            try {
                submittedTasks.remove((Integer)dTID);
            } catch (Exception ignored) {}
            finally {
                sendIntent(downloadCompleteIntent, dTID);
                completeStop(dTID,downloadTask);
            }
        }

        private void chunkDownloadComplete(DownloadTask downloadTask)
        {
            bytesDone = tZero.get();
            downloadTask.setTimeLeft("Complete");
            downloadTask.setCurrentStatus(7);
            downloadTask.setDownloadedBytes(bytesDone);
            downloadTask.setCurrentProgress(100);

            try {
                downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSize(bytesDone,dec));
            } catch (Exception e)
            {
                downloadTask.setDownloadSpeed("");
            }

            db.updateDownloadTaskChunkComplete(downloadTask);
            db.addCompletedTaskID(dTID);

            try {
                submittedTasks.remove((Integer)dTID);
            } catch (Exception ignored) {}
            finally {
                sendIntent(downloadCompleteIntent, dTID);
                completeStop(dTID,downloadTask);
            }
        }

        private void completeStop(int dId, DownloadTask downloadTask)
        {
            int size = submittedTasks.size();
            if(size == 0)
            {
                if(!mainExecutorService.isShutdown())
                {
                    mainExecutorService.shutdownNow();
                }

                isForeground.set(-1);
                stopForeground(true);
                customNotificationGenerator.createNotificationAfterComplete(downloadTask,builder);
                stopSelf();
            } else if (size < db.getSimultaneousTasks() && dId == isForeground.get()) {
                int dTID = submittedTasks.get(0);
                isForeground.set(dTID);

                if(downloadTask.getChunkMode() == 0)
                {
                    startForeground(dTID,customNotificationGenerator.createNotification(downloadTask,builder).build());
                } else {
                    startForeground(dTID,customNotificationGenerator.createChunkNotification(downloadTask,builder).build());
                }

                customNotificationGenerator.createNotificationAfterComplete(downloadTask,builder);
            } else if (dId == isForeground.get()) {
                isForeground.set(-1);
                stopForeground(true);
                customNotificationGenerator.createNotificationAfterComplete(downloadTask,builder);
            } else {
                customNotificationGenerator.createNotificationAfterComplete(downloadTask,builder);
            }
        }

        private void startAsFreshDownload()
        {
            downloadTask.setDownloadedBytes(0L);
            downloadTask.setCurrentStatus(1);
            downloadTask.setCurrentProgress(0);
            downloadTask.setDownloadSpeed("Queued");
            downloadTask.setTimeLeft("-");
            downloadTask.setWhichError("NotAny");

            downloadTask.setTPB1(0);
            downloadTask.setTPB2(0);
            downloadTask.setTPB3(0);
            downloadTask.setTPB4(0);
            downloadTask.setTPB5(0);
            downloadTask.setTPB6(0);
            downloadTask.setTPB7(0);
            downloadTask.setTPB8(0);
            downloadTask.setTPB9(0);
            downloadTask.setTPB10(0);
            downloadTask.setTPB11(0);
            downloadTask.setTPB12(0);
            downloadTask.setTPB13(0);
            downloadTask.setTPB14(0);
            downloadTask.setTPB15(0);
            downloadTask.setTPB16(0);
            downloadTask.setTPB17(0);
            downloadTask.setTPB18(0);
            downloadTask.setTPB19(0);
            downloadTask.setTPB20(0);
            downloadTask.setTPB21(0);
            downloadTask.setTPB22(0);
            downloadTask.setTPB23(0);
            downloadTask.setTPB24(0);
            downloadTask.setTPB25(0);
            downloadTask.setTPB26(0);
            downloadTask.setTPB27(0);
            downloadTask.setTPB28(0);
            downloadTask.setTPB29(0);
            downloadTask.setTPB30(0);
            downloadTask.setTPB31(0);
            downloadTask.setTPB32(0);

            downloadTask.setTSS1(0L);
            downloadTask.setTSS2(0L);
            downloadTask.setTSS3(0L);
            downloadTask.setTSS4(0L);
            downloadTask.setTSS5(0L);
            downloadTask.setTSS6(0L);
            downloadTask.setTSS7(0L);
            downloadTask.setTSS8(0L);
            downloadTask.setTSS9(0L);
            downloadTask.setTSS10(0L);
            downloadTask.setTSS11(0L);
            downloadTask.setTSS12(0L);
            downloadTask.setTSS13(0L);
            downloadTask.setTSS14(0L);
            downloadTask.setTSS15(0L);
            downloadTask.setTSS16(0L);
            downloadTask.setTSS17(0L);
            downloadTask.setTSS18(0L);
            downloadTask.setTSS19(0L);
            downloadTask.setTSS20(0L);
            downloadTask.setTSS21(0L);
            downloadTask.setTSS22(0L);
            downloadTask.setTSS23(0L);
            downloadTask.setTSS24(0L);
            downloadTask.setTSS25(0L);
            downloadTask.setTSS26(0L);
            downloadTask.setTSS27(0L);
            downloadTask.setTSS28(0L);
            downloadTask.setTSS29(0L);
            downloadTask.setTSS30(0L);
            downloadTask.setTSS31(0L);
            downloadTask.setTSS32(0L);

            db.updateDownloadTaskNormalFreshPartial(downloadTask);
            downloadTask = db.getDownloadTask(dTID);

            for(int i = 0; i < segmentsPerDownload; i++)
            {
                long startAt = (sizeOfEachSegment * i);
                long finishAt;
                long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                if(i == (segmentsPerDownload - 1))
                {
                    //Take all remaining bytes as end
                    finishAt = totalSize;
                } else {
                    finishAt = ((i + 1) * sizeOfEachSegment);
                }

                if(startAt < finishAt)
                {
                    switch (i)
                    {
                        case 0:
                            executorService.execute(new DownloadSegmentZero(startAt,finishAt,totalDownloaded));
                            break;
                        case 1:
                            executorService.execute(new DownloadSegmentOne(startAt,finishAt,totalDownloaded));
                            break;
                        case 2:
                            executorService.execute(new DownloadSegmentTwo(startAt,finishAt,totalDownloaded));
                            break;
                        case 3:
                            executorService.execute(new DownloadSegmentThree(startAt,finishAt,totalDownloaded));
                            break;
                        case 4:
                            executorService.execute(new DownloadSegmentFour(startAt,finishAt,totalDownloaded));
                            break;
                        case 5:
                            executorService.execute(new DownloadSegmentFive(startAt,finishAt,totalDownloaded));
                            break;
                        case 6:
                            executorService.execute(new DownloadSegmentSix(startAt,finishAt,totalDownloaded));
                            break;
                        case 7:
                            executorService.execute(new DownloadSegmentSeven(startAt,finishAt,totalDownloaded));
                            break;
                        case 8:
                            executorService.execute(new DownloadSegmentEight(startAt,finishAt,totalDownloaded));
                            break;
                        case 9:
                            executorService.execute(new DownloadSegmentNine(startAt,finishAt,totalDownloaded));
                            break;
                        case 10:
                            executorService.execute(new DownloadSegmentTen(startAt,finishAt,totalDownloaded));
                            break;
                        case 11:
                            executorService.execute(new DownloadSegmentEleven(startAt,finishAt,totalDownloaded));
                            break;
                        case 12:
                            executorService.execute(new DownloadSegmentTwelve(startAt,finishAt,totalDownloaded));
                            break;
                        case 13:
                            executorService.execute(new DownloadSegmentThirteen(startAt,finishAt,totalDownloaded));
                            break;
                        case 14:
                            executorService.execute(new DownloadSegmentFourteen(startAt,finishAt,totalDownloaded));
                            break;
                        case 15:
                            executorService.execute(new DownloadSegmentFifteen(startAt,finishAt,totalDownloaded));
                            break;
                        case 16:
                            executorService.execute(new DownloadSegmentSixteen(startAt,finishAt,totalDownloaded));
                            break;
                        case 17:
                            executorService.execute(new DownloadSegmentSeventeen(startAt,finishAt,totalDownloaded));
                            break;
                        case 18:
                            executorService.execute(new DownloadSegmentEighteen(startAt,finishAt,totalDownloaded));
                            break;
                        case 19:
                            executorService.execute(new DownloadSegmentNineteen(startAt,finishAt,totalDownloaded));
                            break;
                        case 20:
                            executorService.execute(new DownloadSegmentTwenty(startAt,finishAt,totalDownloaded));
                            break;
                        case 21:
                            executorService.execute(new DownloadSegmentTwentyOne(startAt,finishAt,totalDownloaded));
                            break;
                        case 22:
                            executorService.execute(new DownloadSegmentTwentyTwo(startAt,finishAt,totalDownloaded));
                            break;
                        case 23:
                            executorService.execute(new DownloadSegmentTwentyThree(startAt,finishAt,totalDownloaded));
                            break;
                        case 24:
                            executorService.execute(new DownloadSegmentTwentyFour(startAt,finishAt,totalDownloaded));
                            break;
                        case 25:
                            executorService.execute(new DownloadSegmentTwentyFive(startAt,finishAt,totalDownloaded));
                            break;
                        case 26:
                            executorService.execute(new DownloadSegmentTwentySix(startAt,finishAt,totalDownloaded));
                            break;
                        case 27:
                            executorService.execute(new DownloadSegmentTwentySeven(startAt,finishAt,totalDownloaded));
                            break;
                        case 28:
                            executorService.execute(new DownloadSegmentTwentyEight(startAt,finishAt,totalDownloaded));
                            break;
                        case 29:
                            executorService.execute(new DownloadSegmentTwentyNine(startAt,finishAt,totalDownloaded));
                            break;
                        case 30:
                            executorService.execute(new DownloadSegmentThirty(startAt,finishAt,totalDownloaded));
                            break;
                        case 31:
                            executorService.execute(new DownloadSegmentThirtyOne(startAt,finishAt,totalDownloaded));
                            break;
                    }
                }
            }
        }

        private void resumeDownload()
        {
            downloadTask = db.getDownloadTask(dTID);

            for(int i = 0; i < segmentsPerDownload; i++)
            {
                switch (i)
                {
                    case 0:
                    {
                        long start;
                        if(segmentsPerDownload == 1)
                        {
                            start = downloadTask.getDownloadedBytes();
                        } else {
                            start = downloadTask.getTSS1();
                        }

                        long startAt = start;
                        long finishAt;
                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentZero(startAt,finishAt,(startAt)));
                        }

                        break;
                    }
                    case 1:
                    {
                        long start = downloadTask.getTSS2();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentOne(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 2:
                    {
                        long start = downloadTask.getTSS3();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwo(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 3:
                    {
                        long start = downloadTask.getTSS4();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentThree(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 4:
                    {
                        long start = downloadTask.getTSS5();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentFour(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 5:
                    {
                        long start = downloadTask.getTSS6();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentFive(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 6:
                    {
                        long start = downloadTask.getTSS7();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentSix(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 7:
                    {
                        long start = downloadTask.getTSS8();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentSeven(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 8:
                    {
                        long start = downloadTask.getTSS9();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentEight(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 9:
                    {
                        long start = downloadTask.getTSS10();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentNine(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 10:
                    {
                        long start = downloadTask.getTSS11();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTen(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 11:
                    {
                        long start = downloadTask.getTSS12();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentEleven(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 12:
                    {
                        long start = downloadTask.getTSS13();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwelve(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 13:
                    {
                        long start = downloadTask.getTSS14();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentThirteen(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 14:
                    {
                        long start = downloadTask.getTSS15();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentFourteen(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 15:
                    {
                        long start = downloadTask.getTSS16();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentFifteen(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 16:
                    {
                        long start = downloadTask.getTSS17();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentSixteen(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 17:
                    {
                        long start = downloadTask.getTSS18();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentSeventeen(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 18:
                    {
                        long start = downloadTask.getTSS19();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentEighteen(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 19:
                    {
                        long start = downloadTask.getTSS20();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentNineteen(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 20:
                    {
                        long start = downloadTask.getTSS21();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwenty(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 21:
                    {
                        long start = downloadTask.getTSS22();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwentyOne(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 22:
                    {
                        long start = downloadTask.getTSS23();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwentyTwo(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 23:
                    {
                        long start = downloadTask.getTSS24();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwentyThree(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 24:
                    {
                        long start = downloadTask.getTSS25();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwentyFour(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 25:
                    {
                        long start = downloadTask.getTSS26();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwentyFive(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 26:
                    {
                        long start = downloadTask.getTSS27();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwentySix(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 27:
                    {
                        long start = downloadTask.getTSS28();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwentySeven(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 28:
                    {
                        long start = downloadTask.getTSS29();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwentyEight(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 29:
                    {
                        long start = downloadTask.getTSS30();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentTwentyNine(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 30:
                    {
                        long start = downloadTask.getTSS31();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentThirty(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                    case 31:
                    {
                        long start = downloadTask.getTSS32();
                        long startAt = ((sizeOfEachSegment * i)) + start;
                        long finishAt;
                        long totalDownloaded = (startAt - (sizeOfEachSegment * i));

                        if(i == (segmentsPerDownload - 1))
                        {
                            //take all remaining bytes as end
                            finishAt = totalSize;
                        } else {
                            finishAt = ((i + 1) * sizeOfEachSegment);
                        }

                        if(startAt < finishAt)
                        {
                            executorService.execute(new DownloadSegmentThirtyOne(startAt,finishAt,totalDownloaded));
                        }

                        break;

                    }
                }
            }

        }

        private void startAsChunkFreshDownload()
        {
            downloadTask.setDownloadedBytes(0L);
            downloadTask.setCurrentStatus(1);
            downloadTask.setCurrentProgress(0);
            downloadTask.setDownloadSpeed("Queued");
            downloadTask.setTimeLeft("-");
            downloadTask.setWhichError("NotAny");

            downloadTask.setTPB1(0);
            downloadTask.setTPB2(0);
            downloadTask.setTPB3(0);
            downloadTask.setTPB4(0);
            downloadTask.setTPB5(0);
            downloadTask.setTPB6(0);
            downloadTask.setTPB7(0);
            downloadTask.setTPB8(0);
            downloadTask.setTPB9(0);
            downloadTask.setTPB10(0);
            downloadTask.setTPB11(0);
            downloadTask.setTPB12(0);
            downloadTask.setTPB13(0);
            downloadTask.setTPB14(0);
            downloadTask.setTPB15(0);
            downloadTask.setTPB16(0);
            downloadTask.setTPB17(0);
            downloadTask.setTPB18(0);
            downloadTask.setTPB19(0);
            downloadTask.setTPB20(0);
            downloadTask.setTPB21(0);
            downloadTask.setTPB22(0);
            downloadTask.setTPB23(0);
            downloadTask.setTPB24(0);
            downloadTask.setTPB25(0);
            downloadTask.setTPB26(0);
            downloadTask.setTPB27(0);
            downloadTask.setTPB28(0);
            downloadTask.setTPB29(0);
            downloadTask.setTPB30(0);
            downloadTask.setTPB31(0);
            downloadTask.setTPB32(0);

            downloadTask.setTSS1(0L);
            downloadTask.setTSS2(0L);
            downloadTask.setTSS3(0L);
            downloadTask.setTSS4(0L);
            downloadTask.setTSS5(0L);
            downloadTask.setTSS6(0L);
            downloadTask.setTSS7(0L);
            downloadTask.setTSS8(0L);
            downloadTask.setTSS9(0L);
            downloadTask.setTSS10(0L);
            downloadTask.setTSS11(0L);
            downloadTask.setTSS12(0L);
            downloadTask.setTSS13(0L);
            downloadTask.setTSS14(0L);
            downloadTask.setTSS15(0L);
            downloadTask.setTSS16(0L);
            downloadTask.setTSS17(0L);
            downloadTask.setTSS18(0L);
            downloadTask.setTSS19(0L);
            downloadTask.setTSS20(0L);
            downloadTask.setTSS21(0L);
            downloadTask.setTSS22(0L);
            downloadTask.setTSS23(0L);
            downloadTask.setTSS24(0L);
            downloadTask.setTSS25(0L);
            downloadTask.setTSS26(0L);
            downloadTask.setTSS27(0L);
            downloadTask.setTSS28(0L);
            downloadTask.setTSS29(0L);
            downloadTask.setTSS30(0L);
            downloadTask.setTSS31(0L);
            downloadTask.setTSS32(0L);

            db.updateDownloadTaskChunkFreshPartial(downloadTask);
            downloadTask = db.getDownloadTask(dTID);
            executorService.execute(new ChunkDownloadSegment(0L,0L));

        }

        private void resumeChunkDownload()
        {
            downloadTask = db.getDownloadTask(dTID);
            long start = downloadTask.getDownloadedBytes();
            executorService.execute(new ChunkDownloadSegment(start,start));
        }

        private void exceptionOccurredChunk()
        {
            downloadTask = db.getDownloadTask(dTID);
            downloadTask.setDownloadedBytes(bytesDone);
            downloadTask.setCurrentProgress(0);

            switch (downloadOrException.get())
            {
                case 1:
                    switch (downloadTask.getCurrentStatus()){
                        case 0:
                            if(dTID == isForeground.get())
                            {
                                isForeground.set(-1);
                                stopForeground(true);
                            }

                            try {
                                notificationManager.cancel(dTID);
                            } catch (Exception ignored) {}
                            break;
                        case 4:
                            pausedChunkDownload(downloadTask);
                            break;
                        case 2:
                        case 3:
                        case 1:
                            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                            boolean connected = false;
                            if(networkCapabilities != null)
                            {
                                connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                            }

                            if(!connected)
                            {
                                starting.set(0);
                                connectionUnavailableChunk(downloadTask);
                            } else {
                                elseOtherErrorChunk(downloadTask);
                            }
                            break;
                    }

                    break;
                case 3:
                    malformedURLExceptionOccurred(downloadTask);
                    break;
                case 4:
                    serverTemporarilyUnavailableOccurred(downloadTask);
                    break;
                case 5:
                    connectionTimedOutOrSeveralRetries(downloadTask,"ConnectionTimedOutException");
                    break;
                case 6:
                    directoryNotFoundExceptionOccurred(downloadTask);
                    break;
                case 7:
                    connectionTimedOutOrSeveralRetries(downloadTask,"SeveralRetriesException");
                    break;
                case 8:
                    unknownError(downloadTask);
                    break;
            }
        }

        private void exceptionOccurred()
        {
            downloadTask = db.getDownloadTask(dTID);

            switch (downloadTask.getSegmentsForDownloadTask())
            {
                case 1:
                {
                    bytesDone = tZero.get();
                    downloadTask.setCurrentProgress(pZero.get());
                    break;
                }
                case 2:
                {
                    long segOne,segTwo;
                    segOne = tZero.get();
                    segTwo = tOne.get();
                    downloadTask.setTPB1(pZero.get());
                    downloadTask.setTPB2(pOne.get());

                    downloadTask.setTSS1(segOne);
                    downloadTask.setTSS2(segTwo);
                    bytesDone = segOne + segTwo;
                    break;
                }
                case 4:
                {
                    long segOne,segTwo,segThree,segFour;
                    segOne = tZero.get();
                    segTwo = tOne.get();
                    segThree = tTwo.get();
                    segFour = tThree.get();
                    downloadTask.setTPB1(pZero.get());
                    downloadTask.setTPB2(pOne.get());
                    downloadTask.setTPB3(pTwo.get());
                    downloadTask.setTPB4(pThree.get());

                    downloadTask.setTSS1(segOne);
                    downloadTask.setTSS2(segTwo);
                    downloadTask.setTSS3(segThree);
                    downloadTask.setTSS4(segFour);

                    bytesDone = segOne + segTwo + segThree + segFour;
                    break;
                }
                case 6:
                {
                    long segOne,segTwo,segThree,segFour,segFive,segSix;
                    segOne = tZero.get();
                    segTwo = tOne.get();
                    segThree = tTwo.get();
                    segFour = tThree.get();
                    segFive = tFour.get();
                    segSix = tFive.get();
                    downloadTask.setTPB1(pZero.get());
                    downloadTask.setTPB2(pOne.get());
                    downloadTask.setTPB3(pTwo.get());
                    downloadTask.setTPB4(pThree.get());
                    downloadTask.setTPB5(pFour.get());
                    downloadTask.setTPB6(pFive.get());

                    downloadTask.setTSS1(segOne);
                    downloadTask.setTSS2(segTwo);
                    downloadTask.setTSS3(segThree);
                    downloadTask.setTSS4(segFour);
                    downloadTask.setTSS5(segFive);
                    downloadTask.setTSS6(segSix);

                    bytesDone = segOne + segTwo + segThree + segFour + segFive + segSix;

                    break;
                }
                case 8:
                {
                    long segOne,segTwo,segThree,segFour,segFive,segSix,segSeven,segEight;
                    segOne = tZero.get();
                    segTwo = tOne.get();
                    segThree = tTwo.get();
                    segFour = tThree.get();
                    segFive = tFour.get();
                    segSix = tFive.get();
                    segSeven = tSix.get();
                    segEight = tSeven.get();
                    downloadTask.setTPB1(pZero.get());
                    downloadTask.setTPB2(pOne.get());
                    downloadTask.setTPB3(pTwo.get());
                    downloadTask.setTPB4(pThree.get());
                    downloadTask.setTPB5(pFour.get());
                    downloadTask.setTPB6(pFive.get());
                    downloadTask.setTPB7(pSix.get());
                    downloadTask.setTPB8(pSeven.get());

                    downloadTask.setTSS1(segOne);
                    downloadTask.setTSS2(segTwo);
                    downloadTask.setTSS3(segThree);
                    downloadTask.setTSS4(segFour);
                    downloadTask.setTSS5(segFive);
                    downloadTask.setTSS6(segSix);
                    downloadTask.setTSS7(segSeven);
                    downloadTask.setTSS8(segEight);

                    bytesDone = segOne + segTwo + segThree + segFour + segFive + segSix + segSeven + segEight;

                    break;
                }
                case 16:
                {
                    long segOne,segTwo,segThree,segFour,segFive,segSix,segSeven,segEight,
                            segNine,segTen,segEleven,segTwelve,segThirteen,segFourteen,segFifteen,segSixteen;

                    segOne = tZero.get();
                    segTwo = tOne.get();
                    segThree = tTwo.get();
                    segFour = tThree.get();
                    segFive = tFour.get();
                    segSix = tFive.get();
                    segSeven = tSix.get();
                    segEight = tSeven.get();

                    segNine = tEight.get();
                    segTen = tNine.get();
                    segEleven = tTen.get();
                    segTwelve = tEleven.get();
                    segThirteen = tTwelve.get();
                    segFourteen = tThirteen.get();
                    segFifteen = tFourteen.get();
                    segSixteen = tFifteen.get();

                    downloadTask.setTPB1(pZero.get());
                    downloadTask.setTPB2(pOne.get());
                    downloadTask.setTPB3(pTwo.get());
                    downloadTask.setTPB4(pThree.get());
                    downloadTask.setTPB5(pFour.get());
                    downloadTask.setTPB6(pFive.get());
                    downloadTask.setTPB7(pSix.get());
                    downloadTask.setTPB8(pSeven.get());

                    downloadTask.setTPB9(pEight.get());
                    downloadTask.setTPB10(pNine.get());
                    downloadTask.setTPB11(pTen.get());
                    downloadTask.setTPB12(pEleven.get());
                    downloadTask.setTPB13(pTwelve.get());
                    downloadTask.setTPB14(pThirteen.get());
                    downloadTask.setTPB15(pFourteen.get());
                    downloadTask.setTPB16(pFifteen.get());

                    downloadTask.setTSS1(segOne);
                    downloadTask.setTSS2(segTwo);
                    downloadTask.setTSS3(segThree);
                    downloadTask.setTSS4(segFour);
                    downloadTask.setTSS5(segFive);
                    downloadTask.setTSS6(segSix);
                    downloadTask.setTSS7(segSeven);
                    downloadTask.setTSS8(segEight);

                    downloadTask.setTSS9(segNine);
                    downloadTask.setTSS10(segTen);
                    downloadTask.setTSS11(segEleven);
                    downloadTask.setTSS12(segTwelve);
                    downloadTask.setTSS13(segThirteen);
                    downloadTask.setTSS14(segFourteen);
                    downloadTask.setTSS15(segFifteen);
                    downloadTask.setTSS16(segSixteen);

                    bytesDone = segOne + segTwo + segThree + segFour + segFive + segSix + segSeven + segEight +
                            segNine + segTen + segEleven + segTwelve + segThirteen + segFourteen + segFifteen + segSixteen;

                    break;
                }
                case 32:
                {
                    long segOne,segTwo,segThree,segFour,segFive,segSix,segSeven,segEight,
                            segNine,segTen,segEleven,segTwelve,segThirteen,segFourteen,segFifteen,segSixteen,
                            segSeventeen,segEighteen,segNineteen,segTwenty,segTwentyOne,segTwentyTwo,segTwentyThree,segTwentyFour,
                            segTwentyFive,segTwentySix,segTwentySeven,segTwentyEight,segTwentyNine,segThirty,segThirtyOne,segThirtyTwo;

                    segOne = tZero.get();
                    segTwo = tOne.get();
                    segThree = tTwo.get();
                    segFour = tThree.get();
                    segFive = tFour.get();
                    segSix = tFive.get();
                    segSeven = tSix.get();
                    segEight = tSeven.get();

                    segNine = tEight.get();
                    segTen = tNine.get();
                    segEleven = tTen.get();
                    segTwelve = tEleven.get();
                    segThirteen = tTwelve.get();
                    segFourteen = tThirteen.get();
                    segFifteen = tFourteen.get();
                    segSixteen = tFifteen.get();

                    segSeventeen = tSixteen.get();
                    segEighteen = tSeventeen.get();
                    segNineteen = tEighteen.get();
                    segTwenty = tNineteen.get();
                    segTwentyOne = tTwenty.get();
                    segTwentyTwo = tTwentyOne.get();
                    segTwentyThree = tTwentyTwo.get();
                    segTwentyFour = tTwentyThree.get();

                    segTwentyFive = tTwentyFour.get();
                    segTwentySix = tTwentyFive.get();
                    segTwentySeven = tTwentySix.get();
                    segTwentyEight = tTwentySeven.get();
                    segTwentyNine = tTwentyEight.get();
                    segThirty = tTwentyNine.get();
                    segThirtyOne = tThirty.get();
                    segThirtyTwo = tThirtyOne.get();

                    downloadTask.setTPB1(pZero.get());
                    downloadTask.setTPB2(pOne.get());
                    downloadTask.setTPB3(pTwo.get());
                    downloadTask.setTPB4(pThree.get());
                    downloadTask.setTPB5(pFour.get());
                    downloadTask.setTPB6(pFive.get());
                    downloadTask.setTPB7(pSix.get());
                    downloadTask.setTPB8(pSeven.get());

                    downloadTask.setTPB9(pEight.get());
                    downloadTask.setTPB10(pNine.get());
                    downloadTask.setTPB11(pTen.get());
                    downloadTask.setTPB12(pEleven.get());
                    downloadTask.setTPB13(pTwelve.get());
                    downloadTask.setTPB14(pThirteen.get());
                    downloadTask.setTPB15(pFourteen.get());
                    downloadTask.setTPB16(pFifteen.get());

                    downloadTask.setTPB17(pSixteen.get());
                    downloadTask.setTPB18(pSeventeen.get());
                    downloadTask.setTPB19(pEighteen.get());
                    downloadTask.setTPB20(pNineteen.get());
                    downloadTask.setTPB21(pTwenty.get());
                    downloadTask.setTPB22(pTwentyOne.get());
                    downloadTask.setTPB23(pTwentyTwo.get());
                    downloadTask.setTPB24(pTwentyThree.get());

                    downloadTask.setTPB25(pTwentyFour.get());
                    downloadTask.setTPB26(pTwentyFive.get());
                    downloadTask.setTPB27(pTwentySix.get());
                    downloadTask.setTPB28(pTwentySeven.get());
                    downloadTask.setTPB29(pTwentyEight.get());
                    downloadTask.setTPB30(pTwentyNine.get());
                    downloadTask.setTPB31(pThirty.get());
                    downloadTask.setTPB32(pThirtyOne.get());

                    downloadTask.setTSS1(segOne);
                    downloadTask.setTSS2(segTwo);
                    downloadTask.setTSS3(segThree);
                    downloadTask.setTSS4(segFour);
                    downloadTask.setTSS5(segFive);
                    downloadTask.setTSS6(segSix);
                    downloadTask.setTSS7(segSeven);
                    downloadTask.setTSS8(segEight);

                    downloadTask.setTSS9(segNine);
                    downloadTask.setTSS10(segTen);
                    downloadTask.setTSS11(segEleven);
                    downloadTask.setTSS12(segTwelve);
                    downloadTask.setTSS13(segThirteen);
                    downloadTask.setTSS14(segFourteen);
                    downloadTask.setTSS15(segFifteen);
                    downloadTask.setTSS16(segSixteen);

                    downloadTask.setTSS17(segSeventeen);
                    downloadTask.setTSS18(segEighteen);
                    downloadTask.setTSS19(segNineteen);
                    downloadTask.setTSS20(segTwenty);
                    downloadTask.setTSS21(segTwentyOne);
                    downloadTask.setTSS22(segTwentyTwo);
                    downloadTask.setTSS23(segTwentyThree);
                    downloadTask.setTSS24(segTwentyFour);

                    downloadTask.setTSS25(segTwentyFive);
                    downloadTask.setTSS26(segTwentySix);
                    downloadTask.setTSS27(segTwentySeven);
                    downloadTask.setTSS28(segTwentyEight);
                    downloadTask.setTSS29(segTwentyNine);
                    downloadTask.setTSS30(segThirty);
                    downloadTask.setTSS31(segThirtyOne);
                    downloadTask.setTSS32(segThirtyTwo);

                    bytesDone = segOne + segTwo + segThree + segFour + segFive + segSix + segSeven + segEight +
                            segNine + segTen + segEleven + segTwelve + segThirteen + segFourteen + segFifteen + segSixteen +
                            segSeventeen + segEighteen + segNineteen + segTwenty + segTwentyOne + segTwentyTwo + segTwentyThree + segTwentyFour +
                            segTwentyFive + segTwentySix + segTwentySeven + segTwentyEight + segTwentyNine + segThirty + segThirtyOne + segThirtyTwo;

                    break;
                }

            }

            downloadTask.setDownloadedBytes(bytesDone);
            downloadTask.setCurrentProgress((int)((bytesDone * 100) / downloadTask.getTotalBytes()));

            switch (downloadOrException.get())
            {
                case 1:
                    switch (downloadTask.getCurrentStatus()){
                        case 0:
                            if(dTID == isForeground.get())
                            {
                                isForeground.set(-1);
                                stopForeground(true);
                            }

                            try {
                                notificationManager.cancel(dTID);
                            } catch (Exception ignored) {}
                            break;
                        case 4:
                            pausedNormalDownload(downloadTask);
                            break;
                        case 2:
                        case 3:
                        case 1:
                            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                            boolean connected = false;
                            if(networkCapabilities != null)
                            {
                                connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                            }

                            if(!connected)
                            {
                                starting.set(0);
                                connectionUnavailableNormal(downloadTask);
                            } else {
                                elseOtherErrorNormal(downloadTask);
                            }
                            break;
                    }
                    break;
                case 3:
                    malformedURLExceptionOccurred(downloadTask);
                    break;
                case 4:
                    serverTemporarilyUnavailableOccurred(downloadTask);
                    break;
                case 5:
                    connectionTimedOutOrSeveralRetries(downloadTask,"ConnectionTimedOutException");
                    break;
                case 6:
                    directoryNotFoundExceptionOccurred(downloadTask);
                    break;
                case 7:
                    connectionTimedOutOrSeveralRetries(downloadTask,"SeveralRetriesException");
                    break;
                case 8:
                    unknownError(downloadTask);
                    break;
            }
        }

        private void unknownError(DownloadTask downloadTask)
        {
            downloadTask.setTimeLeft("Error");
            downloadTask.setDownloadSpeed("");
            downloadTask.setCurrentStatus(5);
            downloadTask.setWhichError("UnknownError");

            db.updateDownloadTaskTSSE(downloadTask);
            sendIntent(errorOccurredIntent,dTID);

            try {
                submittedTasks.remove((Integer)dTID);
            } catch (Exception ignored){}
            finally {
                doStopWork(dTID);
                customNotificationGenerator.createErrorNotification(downloadTask,builder);
            }
        }

        private void connectionTimedOutOrSeveralRetries(DownloadTask downloadTask, String exceptionString)
        {
            downloadTask.setTimeLeft("Error");
            downloadTask.setDownloadSpeed("");
            downloadTask.setCurrentStatus(5);
            downloadTask.setWhichError(exceptionString);

            db.updateDownloadTaskTSSE(downloadTask);
            sendIntent(errorOccurredIntent,dTID);

            try {
                submittedTasks.remove((Integer)dTID);
            } catch (Exception ignored){}
            finally {
                doStopWork(dTID);
                customNotificationGenerator.createErrorNotification(downloadTask,builder);
            }
        }

        private void directoryNotFoundExceptionOccurred(DownloadTask downloadTask)
        {
            downloadTask.setTimeLeft("Error");
            downloadTask.setDownloadSpeed("");
            downloadTask.setCurrentStatus(5);
            downloadTask.setWhichError("DirectoryNotFoundException");

            db.updateDownloadTaskTSSE(downloadTask);
            sendIntent(errorOccurredIntent,dTID);

            try {
                submittedTasks.remove((Integer)dTID);
            } catch (Exception ignored){}
            finally {
                doStopWork(dTID);
                customNotificationGenerator.createErrorNotification(downloadTask,builder);
            }
        }

        private void malformedURLExceptionOccurred(DownloadTask downloadTask)
        {
            downloadTask.setTimeLeft("Error");
            downloadTask.setDownloadSpeed("");
            downloadTask.setCurrentStatus(5);
            downloadTask.setWhichError("MalformedURLException");

            db.updateDownloadTaskTSSE(downloadTask);
            sendIntent(errorOccurredIntent,dTID);

            try {
                submittedTasks.remove((Integer)dTID);
            } catch (Exception ignored){}
            finally {
                doStopWork(dTID);
                customNotificationGenerator.createErrorNotification(downloadTask,builder);
            }
        }

        private void serverTemporarilyUnavailableOccurred(DownloadTask downloadTask)
        {
            downloadTask.setTimeLeft("Error");
            downloadTask.setDownloadSpeed("");
            downloadTask.setCurrentStatus(5);
            downloadTask.setWhichError("ServerTemporarilyUnavailable");

            db.updateDownloadTaskTSSE(downloadTask);
            sendIntent(errorOccurredIntent,dTID);

            try {
                submittedTasks.remove((Integer)dTID);
            } catch (Exception ignored){}
            finally {
                doStopWork(dTID);
                customNotificationGenerator.createErrorNotification(downloadTask,builder);
            }
        }

        private void connectionUnavailableNormal(DownloadTask downloadTask)
        {

            if(db.getAutoResumeStatus() == 1)
            {
                if(downloadTask.getIsPauseResumeSupported() == 1)
                {
                    //update downloadTask as waiting
                    downloadTask.setTimeLeft("Waiting for network");
                    downloadTask.setDownloadSpeed("");
                    downloadTask.setCurrentStatus(6);
                    downloadTask.setWhichError("NotAny");

                    db.updateDownloadTaskTSSE(downloadTask);
                    customNotificationGenerator.updateNotificationWaitingNormal(downloadTask,builder);
                    sendIntent(downloadingIntent,dTID);
                } else {
                    downloadTask.setTimeLeft("Error");
                    downloadTask.setDownloadSpeed("");
                    downloadTask.setCurrentStatus(5);
                    downloadTask.setWhichError("NetworkInterruptedAndPRNOException");

                    db.updateDownloadTaskTSSE(downloadTask);
                    sendIntent(errorOccurredIntent,dTID);
                    try {
                        submittedTasks.remove((Integer)dTID);
                    } catch (Exception ignored){}
                    finally {
                        doStopWork(dTID);
                        customNotificationGenerator.createErrorNotification(downloadTask,builder);
                    }
                }
            } else {
                if(downloadTask.getIsPauseResumeSupported() == 1)
                {
                    //download interrupted due to network not available and autoResume is off
                    downloadTask.setTimeLeft("No network");
                    downloadTask.setCurrentStatus(4);
                    try {
                        downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSize(bytesDone,dec) + "/" +
                                HumanReadableFormat.calculateHumanReadableSize(downloadTask.getTotalBytes(),dec));
                    } catch (Exception e)
                    {
                        downloadTask.setDownloadSpeed("");
                    }

                    db.updateDownloadTaskNA(downloadTask.getKeyId(),downloadTask.getCurrentStatus(),downloadTask.getDownloadSpeed(),
                            downloadTask.getTimeLeft());
                    sendIntent(downloadingPausedIntent,dTID);

                    try {
                        submittedTasks.remove((Integer)dTID);
                    } catch (Exception ignored){}
                    finally {
                        doStopWork(dTID);
                        customNotificationGenerator.createPausedNotification(downloadTask,builder);
                    }
                } else {
                    //update error info and send broadcast
                    downloadTask.setTimeLeft("Error");
                    downloadTask.setDownloadSpeed("");
                    downloadTask.setCurrentStatus(5);
                    downloadTask.setWhichError("NetworkInterruptedAndPRNOException");

                    db.updateDownloadTaskTSSE(downloadTask);
                    sendIntent(errorOccurredIntent,dTID);
                    try {
                        submittedTasks.remove((Integer)dTID);
                    } catch (Exception ignored){}
                    finally {
                        doStopWork(dTID);
                        customNotificationGenerator.createErrorNotification(downloadTask,builder);
                    }
                }
            }
        }

        private void connectionUnavailableChunk(DownloadTask downloadTask)
        {
            if(db.getAutoResumeStatus() == 1)
            {
                if(downloadTask.getIsPauseResumeSupported() == 1)
                {
                    //update downloadTask as waiting
                    downloadTask.setTimeLeft("Waiting for network");
                    downloadTask.setDownloadSpeed("");
                    downloadTask.setCurrentStatus(6);
                    downloadTask.setWhichError("NotAny");

                    db.updateDownloadTaskTSSE(downloadTask);
                    customNotificationGenerator.updateNotificationWaitingChunk(downloadTask,builder);
                    sendIntent(downloadingIntent,dTID);
                } else {
                    downloadTask.setTimeLeft("Error");
                    downloadTask.setDownloadSpeed("");
                    downloadTask.setCurrentStatus(5);
                    downloadTask.setWhichError("NetworkInterruptedAndPRNOException");

                    db.updateDownloadTaskTSSE(downloadTask);
                    sendIntent(errorOccurredIntent,dTID);
                    try {
                        submittedTasks.remove((Integer)dTID);
                    } catch (Exception ignored){}
                    finally {
                        doStopWork(dTID);
                        customNotificationGenerator.createErrorNotification(downloadTask,builder);
                    }
                }
            } else {
                if(downloadTask.getIsPauseResumeSupported() == 1)
                {
                    //download interrupted due to network not available and autoResume is off
                    downloadTask.setTimeLeft("No network");
                    downloadTask.setCurrentStatus(4);
                    try {
                        downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSize(bytesDone,dec));
                    } catch (Exception e)
                    {
                        downloadTask.setDownloadSpeed("");
                    }

                    db.updateDownloadTaskNA(downloadTask.getKeyId(),downloadTask.getCurrentStatus(),downloadTask.getDownloadSpeed(),
                            downloadTask.getTimeLeft());
                    sendIntent(downloadingPausedIntent,dTID);

                    try {
                        submittedTasks.remove((Integer)dTID);
                    } catch (Exception ignored){}
                    finally {
                        doStopWork(dTID);
                        customNotificationGenerator.createPausedNotification(downloadTask,builder);
                    }
                } else {
                    //update error info and send broadcast
                    downloadTask.setTimeLeft("Error");
                    downloadTask.setDownloadSpeed("");
                    downloadTask.setCurrentStatus(5);
                    downloadTask.setWhichError("NetworkInterruptedAndPRNOException");

                    db.updateDownloadTaskTSSE(downloadTask);
                    sendIntent(errorOccurredIntent,dTID);
                    try {
                        submittedTasks.remove((Integer)dTID);
                    } catch (Exception ignored){}
                    finally {
                        doStopWork(dTID);
                        customNotificationGenerator.createErrorNotification(downloadTask,builder);
                    }
                }
            }
        }

        private void elseOtherErrorNormal(DownloadTask downloadTask)
        {
            //Checking available space
            ParcelFileDescriptor parcelFileDescriptor = null;
            try {
                FileDescriptor fileDescriptor = null;
                parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                if(parcelFileDescriptor != null)
                {
                    fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                }

                if(fileDescriptor != null)
                {
                    StructStatVfs structStatVfs = Os.fstatvfs(fileDescriptor);
                    long blockSize = structStatVfs.f_frsize;
                    long availableBlocks = structStatVfs.f_bavail;

                    if(blockSize * availableBlocks == 0)
                    {
                        try {
                            downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSize(bytesDone,dec) + "/" +
                                    HumanReadableFormat.calculateHumanReadableSize(downloadTask.getTotalBytes(),dec));
                        } catch (Exception e)
                        {
                            downloadTask.setDownloadSpeed("");
                        }

                        downloadTask.setTimeLeft("Unknown error");
                        downloadTask.setCurrentStatus(5);
                        downloadTask.setWhichError("OutOfSpaceException");

                        db.updateDownloadTaskTSSE(downloadTask);
                        sendIntent(errorOccurredIntent,dTID);

                        try {
                            submittedTasks.remove((Integer)dTID);
                        } catch (Exception ignored){}
                        finally {
                            doStopWork(dTID);
                            customNotificationGenerator.createErrorNotification(downloadTask,builder);
                        }
                    }
                }
            } catch (Exception ignored) {}
            finally {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}
            }
        }

        private void elseOtherErrorChunk(DownloadTask downloadTask)
        {
            //Checking available space
            ParcelFileDescriptor parcelFileDescriptor = null;
            try {
                FileDescriptor fileDescriptor = null;
                parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                if(parcelFileDescriptor != null)
                {
                    fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                }

                if(fileDescriptor != null)
                {
                    StructStatVfs structStatVfs = Os.fstatvfs(fileDescriptor);
                    long blockSize = structStatVfs.f_frsize;
                    long availableBlocks = structStatVfs.f_bavail;

                    if(blockSize * availableBlocks == 0)
                    {
                        try {
                            downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSize(bytesDone,dec));
                        } catch (Exception e)
                        {
                            downloadTask.setDownloadSpeed("");
                        }

                        downloadTask.setTimeLeft("Unknown error");
                        downloadTask.setCurrentStatus(5);
                        downloadTask.setWhichError("OutOfSpaceException");

                        db.updateDownloadTaskTSSE(downloadTask);
                        sendIntent(errorOccurredIntent,dTID);

                        try {
                            submittedTasks.remove((Integer)dTID);
                        } catch (Exception ignored){}
                        finally {
                            doStopWork(dTID);
                            customNotificationGenerator.createErrorNotification(downloadTask,builder);
                        }
                    }
                }
            } catch (Exception ignored) {}
            finally {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}
            }
        }

        private void pausedChunkDownload(DownloadTask downloadTask)
        {
            downloadTask.setTimeLeft("Paused");
            try {
                downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSize(bytesDone,dec));
            } catch (Exception e) {
                downloadTask.setDownloadSpeed("");
            }
            db.updateDownloadTaskTS(downloadTask);

            sendIntent(downloadingPausedIntent,dTID);

            doStopWork(dTID);

            customNotificationGenerator.createPausedNotification(downloadTask,builder);
        }

        private void pausedNormalDownload(DownloadTask downloadTask)
        {
            downloadTask.setTimeLeft("Paused");
            try {
                downloadTask.setDownloadSpeed(HumanReadableFormat.calculateHumanReadableSize(bytesDone,dec) + "/" +
                        HumanReadableFormat.calculateHumanReadableSize(downloadTask.getTotalBytes(),dec));
            } catch (Exception e) {
                downloadTask.setDownloadSpeed("");
            }
            db.updateDownloadTaskTS(downloadTask);

            sendIntent(downloadingPausedIntent,dTID);

            doStopWork(dTID);

            customNotificationGenerator.createPausedNotification(downloadTask,builder);
        }

        private final class ChunkDownloadSegment implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start;
            private long totalDownloaded;
            private int malformedRetryCount = 0;

            private ChunkDownloadSegment(long start, long totalDownloaded)
            {
                this.start = start;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeChunkCT()
            {
                start = tZero.get();
                totalDownloaded = start;
                downloadFile();
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd,byte[] bytes,int byteOffset,int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private boolean isNetworkUrl(String url)
            {
                int length = url.length();
                return (((length > 7) && url.substring(0, 7).equalsIgnoreCase("http://"))
                        || ((length > 8) && url.substring(0, 8).equalsIgnoreCase("https://")));
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                OutputStream os = null;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);
                    downloadTask = db.getDownloadTask(dTID);

                    if(!isNetworkUrl(downloadTask.getUrl()))
                    {
                        if(downloadTask.getUrl().startsWith("data:"))
                        {
                            FileDescriptor fileDescriptor = null;
                            parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                            if(parcelFileDescriptor != null)
                            {
                                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                            }

                            if(fileDescriptor != null)
                            {
                                byte[] decodedBytes = Base64.decode(downloadTask.getUrl().substring(downloadTask.getUrl().indexOf(",") + 1),Base64.DEFAULT);
                                os = new FileOutputStream(fileDescriptor);
                                os.write(decodedBytes);
                                totalDownloaded += decodedBytes.length;
                                tZero.set(totalDownloaded);
                            }
                        }

                        if(chunkDownloading.get() == 0)
                        {
                            chunkDownloading.set(1);
                        }
                    } else {
                        URL lastUrl = new URL(downloadTask.getUrl());
                        connection = (HttpURLConnection) lastUrl.openConnection();

                        if(downloadTask.getIsPauseResumeSupported() == 1)
                        {
                            connection.setRequestProperty("Range","bytes="+start+"-");
                        }

                        connection.setReadTimeout(10000);
                        connection.setConnectTimeout(10000);
                        connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                        int code = connection.getResponseCode();
                        int dividedByHundred = (code / 100);
                        if(dividedByHundred != 2)
                        {
                            if(dividedByHundred == 5)
                            {
                                try {
                                    if(downloadOrException.get() == 0)
                                    {
                                        if(downloadTask.getIsPauseResumeSupported() == 1)
                                        {
                                            closeAllSecurely(null,null);
                                            resumeChunkCT();
                                        } else {
                                            downloadOrException.set(4);
                                        }
                                    }
                                } catch (Exception ignored) {}
                            } else {
                                try {
                                    if(downloadOrException.get() == 0)
                                    {
                                        if(malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0)
                                        {
                                            downloadOrException.set(3);
                                        } else {
                                            malformedRetryCount++;
                                            closeAllSecurely(null,null);
                                            resumeChunkCT();
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                        } else {
                            //All is well we can download our file from server because we have response code which is within success range
                            malformedRetryCount = 0;
                            inputStream = new BufferedInputStream(connection.getInputStream());
                            byte[] data = new byte[32768];
                            int count;

                            FileDescriptor fileDescriptor = null;
                            parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                            if(parcelFileDescriptor != null)
                            {
                                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                            }

                            if(fileDescriptor != null)
                            {
                                Os.lseek(fileDescriptor,start,SEEK_SET);

                                while((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                                {
                                    write(fileDescriptor,data,0,count);
                                    totalDownloaded += count;
                                    tZero.set(totalDownloaded);
                                }
                            }

                            if(chunkDownloading.get() == 0)
                            {
                                chunkDownloading.set(1);
                            }
                        }
                    }

                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor,os);
                                resumeChunkCT();
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor,os);
                                resumeChunkCT();
                            }
                        }
                    } catch (Exception ignored) {}
                } catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor,os);
                                    resumeChunkCT();
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                } catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor,os);
                                resumeChunkCT();
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(os != null)
                        {
                            os.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor,OutputStream os)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(os != null)
                    {
                        os.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }

        }


        private final class DownloadSegmentZero implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentZero(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(0 == (segmentsPerDownload - 1))
                {
                    end = totalSize;
                } else {
                    end = sizeOfEachSegment;
                }

                totalDownloaded = (start);
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tZero.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tZero.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pZero.set(currentProgress);
                                tZero.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tZero.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tZero.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tZero.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tZero.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentOne implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentOne(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(1 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment) + start);
                    end = (2 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tOne.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tOne.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pOne.set(currentProgress);
                                tOne.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tOne.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tOne.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tOne.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tOne.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwo implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwo(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(2 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 2) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 2) + start);
                    end = (3 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 2));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwo.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwo.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwo.set(currentProgress);
                                tTwo.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwo.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwo.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwo.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwo.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentThree implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentThree(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(3 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 3) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 3) + start);
                    end = (4 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 3));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tThree.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tThree.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pThree.set(currentProgress);
                                tThree.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThree.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThree.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tThree.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThree.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentFour implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentFour(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(4 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 4) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 4) + start);
                    end = (5 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 4));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tFour.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tFour.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pFour.set(currentProgress);
                                tFour.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFour.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFour.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tFour.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFour.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentFive implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentFive(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(5 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 5) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 5) + start);
                    end = (6 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 5));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tFive.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tFive.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pFive.set(currentProgress);
                                tFive.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFive.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFive.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tFive.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFive.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentSix implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentSix(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(6 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 6) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 6) + start);
                    end = (7 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 6));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tSix.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tSix.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pSix.set(currentProgress);
                                tSix.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSix.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSix.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tSix.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSix.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentSeven implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentSeven(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(7 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 7) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 7) + start);
                    end = (8 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 7));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tSeven.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tSeven.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pSeven.set(currentProgress);
                                tSeven.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSeven.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSeven.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tSeven.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSeven.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentEight implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentEight(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(8 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 8) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 8) + start);
                    end = (9 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 8));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tEight.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tEight.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pEight.set(currentProgress);
                                tEight.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tEight.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tEight.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tEight.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tEight.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentNine implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentNine(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(9 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 9) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 9) + start);
                    end = (10 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 9));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tNine.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tNine.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pNine.set(currentProgress);
                                tNine.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tNine.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tNine.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tNine.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tNine.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTen implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTen(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(10 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 10) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 10) + start);
                    end = (11 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 10));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTen.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTen.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTen.set(currentProgress);
                                tTen.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTen.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTen.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentEleven implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentEleven(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(11 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 11) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 11) + start);
                    end = (12 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 11));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tEleven.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tEleven.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pEleven.set(currentProgress);
                                tEleven.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tEleven.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tEleven.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tEleven.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tEleven.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwelve implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwelve(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(12 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 12) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 12) + start);
                    end = (13 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 12));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwelve.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwelve.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwelve.set(currentProgress);
                                tTwelve.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwelve.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwelve.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwelve.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwelve.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentThirteen implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentThirteen(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(13 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 13) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 13) + start);
                    end = (14 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 13));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tThirteen.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tThirteen.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pThirteen.set(currentProgress);
                                tThirteen.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThirteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThirteen.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tThirteen.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThirteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentFourteen implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentFourteen(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(14 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 14) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 14) + start);
                    end = (15 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 14));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tFourteen.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tFourteen.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pFourteen.set(currentProgress);
                                tFourteen.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFourteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFourteen.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tFourteen.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFourteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentFifteen implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentFifteen(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(15 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 15) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 15) + start);
                    end = (16 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 15));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tFifteen.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tFifteen.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pFifteen.set(currentProgress);
                                tFifteen.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFifteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFifteen.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tFifteen.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tFifteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentSixteen implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentSixteen(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(16 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 16) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 16) + start);
                    end = (17 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 16));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tSixteen.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tSixteen.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pSixteen.set(currentProgress);
                                tSixteen.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSixteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSixteen.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tSixteen.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSixteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentSeventeen implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentSeventeen(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(17 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 17) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 17) + start);
                    end = (18 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 17));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tSeventeen.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tSeventeen.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pSeventeen.set(currentProgress);
                                tSeventeen.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSeventeen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSeventeen.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tSeventeen.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tSeventeen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentEighteen implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentEighteen(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(18 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 18) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 18) + start);
                    end = (19 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 18));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tEighteen.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tEighteen.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pEighteen.set(currentProgress);
                                tEighteen.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tEighteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tEighteen.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tEighteen.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tEighteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentNineteen implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentNineteen(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(19 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 19) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 19) + start);
                    end = (20 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 19));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tNineteen.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tNineteen.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pNineteen.set(currentProgress);
                                tNineteen.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tNineteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tNineteen.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tNineteen.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tNineteen.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwenty implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwenty(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(20 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 20) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 20) + start);
                    end = (21 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 20));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwenty.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwenty.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwenty.set(currentProgress);
                                tTwenty.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwenty.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwenty.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwenty.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwenty.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwentyOne implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwentyOne(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(21 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 21) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 21) + start);
                    end = (22 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 21));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyOne.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyOne.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwentyOne.set(currentProgress);
                                tTwentyOne.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyOne.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyOne.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwentyOne.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyOne.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwentyTwo implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwentyTwo(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(22 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 22) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 22) + start);
                    end = (23 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 22));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyTwo.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyTwo.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwentyTwo.set(currentProgress);
                                tTwentyTwo.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyTwo.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyTwo.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwentyTwo.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyTwo.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwentyThree implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwentyThree(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(23 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 23) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 23) + start);
                    end = (24 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 23));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyThree.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyThree.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwentyThree.set(currentProgress);
                                tTwentyThree.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyThree.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyThree.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwentyThree.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyThree.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwentyFour implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwentyFour(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(24 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 24) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 24) + start);
                    end = (25 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 24));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyFour.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyFour.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwentyFour.set(currentProgress);
                                tTwentyFour.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyFour.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyFour.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwentyFour.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyFour.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwentyFive implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwentyFive(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(25 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 25) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 25) + start);
                    end = (26 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 25));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyFive.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyFive.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwentyFive.set(currentProgress);
                                tTwentyFive.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyFive.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyFive.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwentyFive.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyFive.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwentySix implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwentySix(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(26 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 26) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 26) + start);
                    end = (27 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 26));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentySix.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentySix.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwentySix.set(currentProgress);
                                tTwentySix.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentySix.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentySix.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwentySix.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentySix.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwentySeven implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwentySeven(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(27 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 27) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 27) + start);
                    end = (28 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 27));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentySeven.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentySeven.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwentySeven.set(currentProgress);
                                tTwentySeven.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentySeven.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentySeven.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwentySeven.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentySeven.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwentyEight implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwentyEight(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(28 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 28) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 28) + start);
                    end = (29 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 28));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyEight.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyEight.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwentyEight.set(currentProgress);
                                tTwentyEight.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyEight.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyEight.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwentyEight.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyEight.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentTwentyNine implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentTwentyNine(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(29 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 29) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 29) + start);
                    end = (30 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 29));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyNine.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tTwentyNine.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pTwentyNine.set(currentProgress);
                                tTwentyNine.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyNine.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyNine.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tTwentyNine.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tTwentyNine.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentThirty implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentThirty(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(30 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 30) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 30) + start);
                    end = (31 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 30));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tThirty.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tThirty.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pThirty.set(currentProgress);
                                tThirty.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThirty.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThirty.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tThirty.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThirty.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }

        private final class DownloadSegmentThirtyOne implements Runnable {
            private BufferedInputStream inputStream = null;
            private HttpURLConnection connection = null;

            private long start,end,totalDownloaded;
            private int malformedRetryCount = 0;

            private DownloadSegmentThirtyOne(long start, long end, long totalDownloaded)
            {
                this.start = start;
                this.end = end;
                this.totalDownloaded = totalDownloaded;
            }

            @Override
            public void run() {
                downloadFile();
            }

            private void resumeCurrentThread(long startHere)
            {
                start = startHere;

                if(31 == (segmentsPerDownload - 1))
                {
                    start = ((sizeOfEachSegment * 31) + start);
                    end = totalSize;
                } else {
                    start = ((sizeOfEachSegment * 31) + start);
                    end = (32 * sizeOfEachSegment);
                }

                totalDownloaded = (start - (sizeOfEachSegment * 31));
                if(start < end)
                {
                    downloadFile();
                }
            }

            public void checkOffsetAndCount(int arrayLength, int offset, int count)
            {
                if((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count)
                {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            public void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
                checkOffsetAndCount(bytes.length,byteOffset,byteCount);

                if(byteCount == 0)
                {
                    return;
                }
                try {
                    while (byteCount > 0)
                    {
                        int bytesWritten = Os.write(fd,bytes,byteOffset,byteCount);
                        byteCount -= bytesWritten;
                        byteOffset += bytesWritten;
                    }
                } catch (ErrnoException errnoException)
                {
                    throw new IOException(errnoException.getMessage(),errnoException.getCause());
                }
            }

            private void downloadFile()
            {
                ParcelFileDescriptor parcelFileDescriptor = null;
                DownloadTask downloadTask;
                try {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                    sendIntent(downloadingIntent,dTID);

                    downloadTask = db.getDownloadTask(dTID);

                    URL lastUrl = new URL(downloadTask.getUrl());
                    connection = (HttpURLConnection) lastUrl.openConnection();

                    if(downloadTask.getIsPauseResumeSupported() == 1)
                    {
                        String bytesRange;
                        bytesRange = start + "-" + end;
                        connection.setRequestProperty("Range","bytes="+bytesRange);
                    }

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("User-Agent",downloadTask.getUserAgentString());

                    int code = connection.getResponseCode();
                    int dividedByHundred = (code / 100);
                    if(dividedByHundred != 2) {
                        if (dividedByHundred == 5) {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (downloadTask.getIsPauseResumeSupported() == 1) {
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tThirtyOne.get());
                                    } else {
                                        downloadOrException.set(4);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            try {
                                if (downloadOrException.get() == 0) {
                                    if (malformedRetryCount > 4 || downloadTask.getIsPauseResumeSupported() == 0) {
                                        downloadOrException.set(3);
                                    } else {
                                        malformedRetryCount++;
                                        closeAllSecurely(null);
                                        resumeCurrentThread(tThirtyOne.get());

                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        //All is well we can download file!

                        malformedRetryCount = 0;
                        inputStream = new BufferedInputStream(connection.getInputStream());
                        byte[] data = new byte[32768];
                        int count;

                        FileDescriptor fileDescriptor = null;
                        parcelFileDescriptor = DownloadingService.this.getContentResolver().openFileDescriptor(newFileUri,"rw");

                        if(parcelFileDescriptor != null)
                        {
                            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        }

                        if(fileDescriptor != null)
                        {
                            Os.lseek(fileDescriptor,start,SEEK_SET);

                            while ((count = inputStream.read(data)) > 0 && downloadOrException.get() == 0)
                            {
                                write(fileDescriptor,data,0,count);
                                totalDownloaded += count;
                                int currentProgress = (int) (totalDownloaded * 100 / sizeOfEachSegment);

                                pThirtyOne.set(currentProgress);
                                tThirtyOne.set(totalDownloaded);
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThirtyOne.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (MalformedURLException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(malformedRetryCount > 4 || !db.isPauseResumeSupported(dTID))
                            {
                                downloadOrException.set(3);
                            } else {
                                malformedRetryCount++;
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThirtyOne.get());
                            }
                        }
                    } catch (Exception ignored) {}
                }catch (FileNotFoundException | ErrnoException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            downloadOrException.set(6);
                        }
                    } catch (Exception ignored) {}
                } catch (IOException e)
                {
                    try {
                        if(downloadOrException.get() == 0)
                        {

                            if(db.getAutoResumeStatus() == 1 && db.isPauseResumeSupported(dTID))
                            {
                                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                                boolean connected = false;
                                if(networkCapabilities != null)
                                {
                                    connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                }

                                if(!connected)
                                {
                                    downloadOrException.set(1);
                                } else {
                                    closeAllSecurely(parcelFileDescriptor);
                                    resumeCurrentThread(tThirtyOne.get());
                                }

                            } else {
                                downloadOrException.set(1);
                            }

                        }
                    } catch (Exception ignored) {}

                }
                catch (Exception e) {
                    try {
                        if(downloadOrException.get() == 0)
                        {
                            if(db.isPauseResumeSupported(dTID))
                            {
                                closeAllSecurely(parcelFileDescriptor);
                                resumeCurrentThread(tThirtyOne.get());
                            } else {
                                downloadOrException.set(5);
                            }
                        }
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if(parcelFileDescriptor != null)
                        {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception ignored) {}


                    try {
                        if(inputStream != null)
                        {
                            inputStream.close();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if(connection != null)
                        {
                            connection.disconnect();
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (Thread.currentThread().isAlive())
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            private void closeAllSecurely(ParcelFileDescriptor parcelFileDescriptor)
            {
                try {
                    if(parcelFileDescriptor != null)
                    {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }
                } catch (Exception ignored) {}

                try {
                    if(connection != null)
                    {
                        connection.disconnect();
                    }
                } catch (Exception ignored) {}
            }
        }


    }


}
