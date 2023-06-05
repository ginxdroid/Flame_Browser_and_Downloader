package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.DownloadTask;

class CustomNotificationGenerator {
    private final Context context;
    private final NotificationManagerCompat notificationManager;

    public CustomNotificationGenerator(Context context, NotificationManagerCompat notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
    }

    private PendingIntent getPausePI(int dTID)
    {
        Intent intent = new Intent(context, DownloadingService.class);
        Bundle bundle = new Bundle();
        bundle.putString("dStatus","Pause");
        bundle.putInt("dId",dTID);
        intent.putExtras(bundle);

        return PendingIntent.getService(context,dTID,intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent getResumePI(int dTID)
    {
        Intent intent = new Intent(context, DownloadingService.class);
        Bundle bundle = new Bundle();
        bundle.putString("dStatus","ResumeFromN");
        bundle.putInt("dId",dTID);
        intent.putExtras(bundle);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            return PendingIntent.getForegroundService(context,dTID,intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getService(context,dTID,intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
    }

    private String getErrorDetails(DownloadTask downloadTask)
    {
        try {
            if(HelperTextUtility.isNotEmpty(downloadTask.getWhichError()))
            {
                switch (downloadTask.getWhichError())
                {
                    case "MalformedURLException":
                        return context.getString(R.string.file_not_found_on_url);
                    case "NetworkInterruptedAndPRNOException":
                    case "PauseResumeNotSupportedException":
                        return context.getString(R.string.pause_resume_not_supported);
                    case "OutOfSpaceException":
                        return context.getString(R.string.out_of_storage_space);
                    case "ConnectionTimedOutException":
                        return context.getString(R.string.connection_timed_out);
                    case "ServerTemporarilyUnavailable":
                        return context.getString(R.string.server_temporarily_unavailable);
                    case "DirectoryNotFoundException":
                        return context.getString(R.string.directory_not_found);
                    case "SeveralRetriesException":
                        return context.getString(R.string.failed_after_several_tries);
                    default:
                        return context.getString(R.string.unknown_error);
                }
            } else {
                return context.getString(R.string.unknown_error);
            }
        } catch (Exception e)
        {
            return context.getString(R.string.unknown_error);
        }
    }

    NotificationCompat.Builder getRawBuilder()
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,"FDD");
        builder.setContentTitle(context.getString(R.string.initializing));
        builder.setPriority(NotificationCompat.PRIORITY_LOW);
        builder.setSmallIcon(R.drawable.ic_notification_icon);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.fetching)));
        builder.setSubText(context.getString(R.string.started));

        Intent notificationIntent = new Intent(context, FirstActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingContentIntent = PendingIntent.getActivity(context,0,notificationIntent,PendingIntent.FLAG_IMMUTABLE);

        builder.setContentIntent(pendingContentIntent);
        return builder;
    }

    NotificationCompat.Builder createNotification(DownloadTask downloadTask, NotificationCompat.Builder builder)
    {
        try {
            int actionRes;
            String actionName;
            if (downloadTask.getIsPauseResumeSupported() == 1) {
                actionRes = R.drawable.pause_icon_noti;
                actionName = context.getString(R.string.pause);
            } else {
                actionRes = R.drawable.stop_icon_noti;
                actionName = context.getString(R.string.stop);
            }

            return createNotificationFromContent(builder, downloadTask, 100, downloadTask.getCurrentProgress(),
                    false, actionRes, actionName, getPausePI(downloadTask.getKeyId()));
        }catch (Exception e)
        {
            return createNotification(downloadTask,builder);
        }
    }

    NotificationCompat.Builder createNotificationFromContent(NotificationCompat.Builder builder,DownloadTask downloadTask,
                                                             int progressMax, int currentProgress, boolean isIndeterminate, int actionResId,
                                                             String actionName, PendingIntent pendingIntent)
    {
        builder.setContentTitle(downloadTask.getFileName());
        builder.setPriority(NotificationCompat.PRIORITY_LOW);
        builder.setSmallIcon(R.drawable.ic_notification_icon);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(downloadTask.getTimeLeft()));
        builder.setSubText(downloadTask.getDownloadSpeed());
        builder.setProgress(progressMax,currentProgress,isIndeterminate);

        builder.clearActions();
        builder.addAction(actionResId,actionName,pendingIntent);

        Intent notificationIntent = new Intent(context, FirstActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingContentIntent = PendingIntent.getActivity(context,0,notificationIntent,PendingIntent.FLAG_IMMUTABLE);

        builder.setContentIntent(pendingContentIntent);
        return builder;
    }

    @SuppressLint("MissingPermission")
    void updateNotification(DownloadTask downloadTask, NotificationCompat.Builder builder)
    {
        try {
            int actionRes;
            String actionName;
            if (downloadTask.getIsPauseResumeSupported() == 1) {
                actionRes = R.drawable.pause_icon_noti;
                actionName = context.getString(R.string.pause);
            } else {
                actionRes = R.drawable.stop_icon_noti;
                actionName = context.getString(R.string.stop);
            }

            builder.setContentTitle(downloadTask.getFileName());
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
            builder.setSmallIcon(R.drawable.ic_notification_icon);
            builder.setAutoCancel(false);
            builder.setOngoing(true);
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(downloadTask.getTimeLeft()));
            builder.setSubText(downloadTask.getDownloadSpeed());
            builder.setProgress(100,downloadTask.getCurrentProgress(),false);

            builder.clearActions();
            builder.addAction(actionRes,actionName,getPausePI(downloadTask.getKeyId()));

            Intent notificationIntent = new Intent(context, FirstActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingContentIntent = PendingIntent.getActivity(context,0,notificationIntent,PendingIntent.FLAG_IMMUTABLE);

            builder.setContentIntent(pendingContentIntent);

            notificationManager.notify(downloadTask.getKeyId(),builder.build());
        } catch (Exception ignored) {}
    }

    @SuppressLint("MissingPermission")
    void updateNotificationWaitingNormal(DownloadTask downloadTask, NotificationCompat.Builder builder)
    {
        try {
            int actionRes;
            String actionName;
            if (downloadTask.getIsPauseResumeSupported() == 1) {
                actionRes = R.drawable.pause_icon_noti;
                actionName = context.getString(R.string.pause);
            } else {
                actionRes = R.drawable.stop_icon_noti;
                actionName = context.getString(R.string.stop);
            }

            builder.setContentTitle(downloadTask.getFileName());
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
            builder.setSmallIcon(R.drawable.ic_notification_icon);
            builder.setAutoCancel(false);
            builder.setOngoing(true);
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(downloadTask.getTimeLeft()));
            builder.setSubText(context.getString(R.string.waiting));
            builder.setProgress(100,downloadTask.getCurrentProgress(),false);

            builder.clearActions();
            builder.addAction(actionRes,actionName,getPausePI(downloadTask.getKeyId()));

            Intent notificationIntent = new Intent(context, FirstActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingContentIntent = PendingIntent.getActivity(context,0,notificationIntent,PendingIntent.FLAG_IMMUTABLE);

            builder.setContentIntent(pendingContentIntent);

            notificationManager.notify(downloadTask.getKeyId(),builder.build());
        } catch (Exception ignored) {}
    }

    @SuppressLint("MissingPermission")
    void createPausedNotification(DownloadTask downloadTask, NotificationCompat.Builder builder)
    {
        try {
            int actionRes = R.drawable.resume_icon_noti;
            String actionName;
            if (downloadTask.getIsPauseResumeSupported() == 1) {
                actionName = context.getString(R.string.resume);
            } else {
                actionName = context.getString(R.string.start);
            }

            int progressMax;
            int currentProgress;
            if(downloadTask.getChunkMode() == 1)
            {
                progressMax = 0;
                currentProgress = 0;
            } else {
                progressMax = 100;
                currentProgress = downloadTask.getCurrentProgress();
            }

            builder.setContentTitle(downloadTask.getFileName());
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
            builder.setSmallIcon(R.drawable.ic_notification_icon);
            builder.setAutoCancel(false);
            builder.setOngoing(false);
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(downloadTask.getTimeLeft()));
            builder.setSubText(downloadTask.getDownloadSpeed());
            builder.setProgress(progressMax,currentProgress,false);

            builder.clearActions();
            builder.addAction(actionRes,actionName,getResumePI(downloadTask.getKeyId()));

            Intent notificationIntent = new Intent(context, FirstActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingContentIntent = PendingIntent.getActivity(context,0,notificationIntent,PendingIntent.FLAG_IMMUTABLE);

            builder.setContentIntent(pendingContentIntent);

            notificationManager.notify(downloadTask.getKeyId(),builder.build());
        } catch (Exception ignored) {}
    }

    @SuppressLint("MissingPermission")
    void createErrorNotification(DownloadTask downloadTask, NotificationCompat.Builder builder)
    {
        try {
            int actionRes = R.drawable.resume_icon_noti;
            String actionName;
            if (downloadTask.getIsPauseResumeSupported() == 1) {
                actionName = context.getString(R.string.resume);
            } else {
                actionName = context.getString(R.string.start);
            }

            int progressMax;
            int currentProgress;
            if(downloadTask.getChunkMode() == 1)
            {
                progressMax = 0;
                currentProgress = 0;
            } else {
                progressMax = 100;
                currentProgress = downloadTask.getCurrentProgress();
            }

            builder.setContentTitle(downloadTask.getFileName());
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
            builder.setSmallIcon(R.drawable.ic_notification_icon);
            builder.setAutoCancel(false);
            builder.setOngoing(false);
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(getErrorDetails(downloadTask)));
            builder.setSubText(context.getString(R.string.error_occurred));
            builder.setProgress(progressMax,currentProgress,false);

            builder.clearActions();
            builder.addAction(actionRes,actionName,getResumePI(downloadTask.getKeyId()));

            Intent notificationIntent = new Intent(context, FirstActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingContentIntent = PendingIntent.getActivity(context,0,notificationIntent,PendingIntent.FLAG_IMMUTABLE);

            builder.setContentIntent(pendingContentIntent);

            notificationManager.notify(downloadTask.getKeyId(),builder.build());
        } catch (Exception ignored) {}
    }

    @SuppressLint("MissingPermission")
    void createNotificationAfterComplete(DownloadTask downloadTask, NotificationCompat.Builder builder)
    {
        try {
            builder.setContentTitle(downloadTask.getFileName());
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
            builder.setSmallIcon(R.drawable.ic_notification_icon);
            builder.setAutoCancel(false);
            builder.setOngoing(false);
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(downloadTask.getDownloadSpeed()));
            builder.setSubText(context.getString(R.string.complete));
            builder.setProgress(0,0,false);

            builder.clearActions();

            Intent notificationIntent = new Intent(context, FirstActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingContentIntent = PendingIntent.getActivity(context,0,notificationIntent,PendingIntent.FLAG_IMMUTABLE);

            builder.setContentIntent(pendingContentIntent);

            notificationManager.notify(downloadTask.getKeyId(),builder.build());
        } catch (Exception ignored) {}
    }

    NotificationCompat.Builder createChunkNotification(DownloadTask downloadTask, NotificationCompat.Builder builder)
    {
        try {
            int actionRes;
            String actionName;
            if (downloadTask.getIsPauseResumeSupported() == 1) {
                actionRes = R.drawable.pause_icon_noti;
                actionName = context.getString(R.string.pause);
            } else {
                actionRes = R.drawable.stop_icon_noti;
                actionName = context.getString(R.string.stop);
            }

            return createNotificationFromContent(builder, downloadTask, 0, 0,
                    true, actionRes, actionName, getPausePI(downloadTask.getKeyId()));
        }catch (Exception e)
        {
            return createChunkNotification(downloadTask,builder);
        }
    }

    @SuppressLint("MissingPermission")
    void updateChunkNotification(DownloadTask downloadTask, NotificationCompat.Builder builder)
    {
        try {
            int actionRes;
            String actionName;
            if (downloadTask.getIsPauseResumeSupported() == 1) {
                actionRes = R.drawable.pause_icon_noti;
                actionName = context.getString(R.string.pause);
            } else {
                actionRes = R.drawable.stop_icon_noti;
                actionName = context.getString(R.string.stop);
            }

            builder.setContentTitle(downloadTask.getFileName());
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
            builder.setSmallIcon(R.drawable.ic_notification_icon);
            builder.setAutoCancel(false);
            builder.setOngoing(true);
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(downloadTask.getTimeLeft()));
            builder.setSubText(downloadTask.getDownloadSpeed());
            builder.setProgress(0,0,true);

            builder.clearActions();
            builder.addAction(actionRes,actionName,getPausePI(downloadTask.getKeyId()));

            Intent notificationIntent = new Intent(context, FirstActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingContentIntent = PendingIntent.getActivity(context,0,notificationIntent,PendingIntent.FLAG_IMMUTABLE);

            builder.setContentIntent(pendingContentIntent);

            notificationManager.notify(downloadTask.getKeyId(),builder.build());
        } catch (Exception ignored) {}
    }

    @SuppressLint("MissingPermission")
    void updateNotificationWaitingChunk(DownloadTask downloadTask, NotificationCompat.Builder builder)
    {
        try {
            int actionRes;
            String actionName;
            if (downloadTask.getIsPauseResumeSupported() == 1) {
                actionRes = R.drawable.pause_icon_noti;
                actionName = context.getString(R.string.pause);
            } else {
                actionRes = R.drawable.stop_icon_noti;
                actionName = context.getString(R.string.stop);
            }

            builder.setContentTitle(downloadTask.getFileName());
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
            builder.setSmallIcon(R.drawable.ic_notification_icon);
            builder.setAutoCancel(false);
            builder.setOngoing(true);
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(downloadTask.getTimeLeft()));
            builder.setSubText(context.getString(R.string.waiting));
            builder.setProgress(0,0,true);

            builder.clearActions();
            builder.addAction(actionRes,actionName,getPausePI(downloadTask.getKeyId()));

            Intent notificationIntent = new Intent(context, FirstActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingContentIntent = PendingIntent.getActivity(context,0,notificationIntent,PendingIntent.FLAG_IMMUTABLE);

            builder.setContentIntent(pendingContentIntent);

            notificationManager.notify(downloadTask.getKeyId(),builder.build());
        } catch (Exception ignored) {}
    }
}
