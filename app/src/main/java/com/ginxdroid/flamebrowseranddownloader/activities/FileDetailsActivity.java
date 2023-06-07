package com.ginxdroid.flamebrowseranddownloader.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.classes.HumanReadableFormat;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialDetailsTask;
import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;

public class FileDetailsActivity extends BaseActivity {
    private DatabaseHandler db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_details);
        db = DatabaseHandler.getInstance(FileDetailsActivity.this);
        setUpUI();
    }

    private void setUpUI()
    {
        Bundle bundle = getIntent().getExtras();
        if(bundle != null)
        {
            String dId = bundle.getString("id");

            if(dId != null)
            {
                final ImageButton backIB = findViewById(R.id.backIB);
                backIB.setOnClickListener(view -> finish());

                final TextView fileNameTV,fileSizeTV,segmentsForDownloadTaskTV,pauseResumeSupportedTV,currentStatusTV,
                        storageLocationTV,fileURLTV,pageURLTV;
                final MaterialButton copyFileURLBtn,copyPageURLBtn;

                fileNameTV = findViewById(R.id.fileNameTV);
                fileSizeTV = findViewById(R.id.fileSizeTV);
                segmentsForDownloadTaskTV = findViewById(R.id.segmentsForDownloadTaskTV);
                pauseResumeSupportedTV = findViewById(R.id.pauseResumeSupportedTV);
                currentStatusTV = findViewById(R.id.currentStatusTV);
                storageLocationTV = findViewById(R.id.storageLocationTV);
                fileURLTV = findViewById(R.id.fileURLTV);
                pageURLTV = findViewById(R.id.pageURLTV);

                copyFileURLBtn = findViewById(R.id.copyFileURLBtn);
                copyPageURLBtn = findViewById(R.id.copyPageURLBtn);


                final PartialDetailsTask downloadTask = db.getDownloadTaskDetails(Integer.parseInt(dId));

                fileNameTV.setText(downloadTask.getFileName());
                fileURLTV.setText(downloadTask.getUrl());

                if(HelperTextUtility.isNotEmpty(downloadTask.getPageURL()))
                {
                    pageURLTV.setText(downloadTask.getPageURL());
                }

                storageLocationTV.setText(downloadTask.getDirPath());
                pauseResumeSupportedTV.setText(downloadTask.getPauseResumeSupported());

                DecimalFormat dec = new DecimalFormat("0.##");

                final String finalText;
                if(downloadTask.getChunkMode() == 1)
                {
                    finalText = HumanReadableFormat.calculateHumanReadableSize(downloadTask.getDownloadedBytes(),dec) + " downloaded out of unknown";
                } else {
                    finalText = HumanReadableFormat.calculateHumanReadableSize(downloadTask.getDownloadedBytes(),dec) + " downloaded out of " +
                        HumanReadableFormat.calculateHumanReadableSize(downloadTask.getTotalBytes(),dec);
                }

                fileSizeTV.setText(finalText);

                switch (downloadTask.getCurrentStatus())
                {
                    case 0:
                        currentStatusTV.setText(R.string.deleting);
                        break;
                    case 1:
                        currentStatusTV.setText(R.string.just_started);
                        break;
                    case 2:
                        currentStatusTV.setText(R.string.downloading);
                        break;
                    case 3:
                        currentStatusTV.setText(R.string.resumed);
                        break;
                    case 4:
                        currentStatusTV.setText(R.string.paused);
                        break;
                    case 5:
                        currentStatusTV.setText(R.string.error);
                        break;
                    case 6:
                        currentStatusTV.setText(R.string.waiting);
                        break;
                    case 7:
                        currentStatusTV.setText(R.string.complete);
                        break;

                }

                segmentsForDownloadTaskTV.setText(String.valueOf(downloadTask.getSegmentsForDownloadTask()));

                copyFileURLBtn.setOnClickListener(view -> {
                    if(HelperTextUtility.isNotEmpty(downloadTask.getUrl()))
                    {
                        ClipboardManager clipboardManager = (ClipboardManager) FileDetailsActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText("File URL",downloadTask.getUrl());
                        if(clipboardManager != null)
                        {
                            clipboardManager.setPrimaryClip(clipData);
                        }

                        Toast.makeText(FileDetailsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(FileDetailsActivity.this, R.string.cannot_copy_empty_link_text, Toast.LENGTH_SHORT).show();
                    }
                });

                copyPageURLBtn.setOnClickListener(view -> {
                    if(HelperTextUtility.isNotEmpty(downloadTask.getPageURL()))
                    {
                        ClipboardManager clipboardManager = (ClipboardManager) FileDetailsActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText("Page URL",downloadTask.getPageURL());
                        if(clipboardManager != null)
                        {
                            clipboardManager.setPrimaryClip(clipData);
                        }

                        Toast.makeText(FileDetailsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(FileDetailsActivity.this, R.string.cannot_copy_empty_link_text, Toast.LENGTH_SHORT).show();
                    }
                });

            } else
            {
                finish();
            }
        } else {
            finish();
        }
    }
}