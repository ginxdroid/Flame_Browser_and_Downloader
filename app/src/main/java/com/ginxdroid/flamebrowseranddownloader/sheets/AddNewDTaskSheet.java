package com.ginxdroid.flamebrowseranddownloader.sheets;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.activities.DownloadingService;
import com.ginxdroid.flamebrowseranddownloader.classes.FileNameEditText;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.ginxdroid.flamebrowseranddownloader.classes.HumanReadableFormat;
import com.ginxdroid.flamebrowseranddownloader.models.UserPreferences;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.DownloadTask;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class AddNewDTaskSheet extends BottomSheetDialogFragment {
    private Toast toast = null;
    private BottomSheetListener listener;

    private void showToast(int resId, final Context context)
    {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_add_new_download_task, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;

            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if(bottomSheet != null)
            {
                BottomSheetBehavior.from(bottomSheet).setPeekHeight(bottomSheet.getHeight());
            }
        });

        return dialog;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle bundle = getArguments();

        @SuppressWarnings("ConstantConditions")
        final String url = bundle.getString("url");
        final String userAgent = bundle.getString("userAgent");
        final long contentLength = bundle.getLong("contentLength");
        final String pauseResumeSupported = bundle.getString("pauseResumeSupported");
        final String fileName = bundle.getString("fileName");
        final int chunkMode = bundle.getInt("chunkMode");
        final String pageURL = bundle.getString("pageURL");
        final String mimeType = bundle.getString("mimeType");
        final int defaultSegments = bundle.getInt("defaultSegments");
        final String extension = bundle.getString("extension");
        final int isPauseResumeSupported = bundle.getInt("isPauseResumeSupported");

        final Context context = getContext();
        final DatabaseHandler db = DatabaseHandler.getInstance(context);
        final FragmentActivity activity = getActivity();


        final TextView urlTV,fileSizeTV,pauseResumeSupportedTV,statusTextView,segmentsForTaskValueTV;
        final FileNameEditText fileNameET;
        final LinearLayout segmentsForDTaskLL;
        final Slider segmentsSliderNewTaskPopup;
        final MaterialButton downloadBtn;
        final ImageButton shareIB,copyIB;

        urlTV = view.findViewById(R.id.urlTV);
        fileSizeTV = view.findViewById(R.id.fileSizeTV);
        pauseResumeSupportedTV = view.findViewById(R.id.pauseResumeSupportedTV);
        statusTextView = view.findViewById(R.id.statusTextView);
        fileNameET = view.findViewById(R.id.fileNameET);
        segmentsForDTaskLL = view.findViewById(R.id.segmentsForDTaskLL);
        segmentsSliderNewTaskPopup = segmentsForDTaskLL.findViewById(R.id.segmentsSliderNewTaskPopup);
        segmentsForTaskValueTV = segmentsForDTaskLL.findViewById(R.id.segmentsForTaskValueTV);

        downloadBtn = view.findViewById(R.id.downloadBtn);
        shareIB = view.findViewById(R.id.shareIB);
        copyIB = view.findViewById(R.id.copyIB);

        if(HelperTextUtility.isNotEmpty(extension))
        {
            final LinearLayout extensionLL = view.findViewById(R.id.extensionLL);
            extensionLL.setVisibility(View.VISIBLE);
            final MaterialTextView extensionTV = extensionLL.findViewById(R.id.extensionTV);
            extensionTV.setText(extension);
        }

        urlTV.setText(url);

        if(chunkMode == 1 || isPauseResumeSupported == 0)
        {
            segmentsForDTaskLL.setVisibility(View.GONE);
        } else {
                segmentsSliderNewTaskPopup.setValue(defaultSegments);

                switch (defaultSegments)
                {
                    case 0:
                        segmentsForTaskValueTV.setText(String.valueOf(1));
                        break;
                    case 1:
                        segmentsForTaskValueTV.setText(String.valueOf(2));
                        break;
                    case 2:
                        segmentsForTaskValueTV.setText(String.valueOf(4));
                        break;
                    case 3:
                        segmentsForTaskValueTV.setText(String.valueOf(6));
                        break;
                    case 4:
                        segmentsForTaskValueTV.setText(String.valueOf(8));
                        break;
                    case 5:
                        segmentsForTaskValueTV.setText(String.valueOf(16));
                        break;
                    case 6:
                        segmentsForTaskValueTV.setText(String.valueOf(32));
                        break;
                }

                segmentsSliderNewTaskPopup.addOnChangeListener((slider, value, fromUser) -> {
                    switch ((int)value)
                    {
                        case 0:
                            segmentsForTaskValueTV.setText(String.valueOf(1));
                            break;
                        case 1:
                            segmentsForTaskValueTV.setText(String.valueOf(2));
                            break;
                        case 2:
                            segmentsForTaskValueTV.setText(String.valueOf(4));
                            break;
                        case 3:
                            segmentsForTaskValueTV.setText(String.valueOf(6));
                            break;
                        case 4:
                            segmentsForTaskValueTV.setText(String.valueOf(8));
                            break;
                        case 5:
                            segmentsForTaskValueTV.setText(String.valueOf(16));
                            break;
                        case 6:
                            segmentsForTaskValueTV.setText(String.valueOf(32));
                            break;

                    }
                });

                fileNameET.setText(fileName);
                //noinspection ConstantConditions
                if(chunkMode == 1)
                {
                    fileSizeTV.setText(context.getString(R.string.unknown));
                } else {
                    fileSizeTV.setText(HumanReadableFormat.calculateHumanReadableSize(contentLength,new DecimalFormat("0.##")));
                }

                pauseResumeSupportedTV.setText(pauseResumeSupported);

                fileNameET.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if(statusTextView.getVisibility() == View.VISIBLE)
                        {
                            statusTextView.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                fileNameET.setOnFocusChangeListener((view1, hasFocus) -> {
                    if(hasFocus)
                    {
                        try {
                            if(statusTextView.getVisibility() == View.VISIBLE)
                            {
                                statusTextView.setVisibility(View.GONE);
                            }
                        }catch (Exception ignored) {}
                    }
                });

                View.OnClickListener onClickListener = view12 -> {
                    int id = view12.getId();
                    if(id == R.id.shareIB)
                    {
                        try {
                            Intent share = new Intent(Intent.ACTION_SEND);
                            share.setTypeAndNormalize("text/plain");
                            share.putExtra(Intent.EXTRA_TEXT,url);

                            //noinspection ConstantConditions
                            activity.startActivity(Intent.createChooser(share,context.getString(R.string.share_via)));
                        } catch (Exception e)
                        {
                            showToast(R.string.app_not_found,context);
                        }
                    } else if(id == R.id.copyIB)
                    {
                        if(url != null)
                        {
                            //noinspection ConstantConditions
                            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clipData = ClipData.newPlainText("file URL",url);

                            if(clipboardManager != null)
                            {
                                clipboardManager.setPrimaryClip(clipData);
                                showToast(R.string.copied_to_clipboard,context);
                            } else {
                                showToast(R.string.oops_general_message,context);
                            }
                        }
                    } else if(id == R.id.downloadBtn)
                    {
                        try {
                            UserPreferences userPreferences = db.getHalfUserPreferences();

                            final Editable editable = fileNameET.getText();

                            if(HelperTextUtility.isNotEmpty(editable))
                            {
                                String downloadPath = userPreferences.getDownloadPath();
                                String editableString = editable.toString();
                                ArrayList<String> fileNamesInDB = db.getAllDownloadTaskNames();

                                if(fileNamesInDB.contains(editableString))
                                {
                                    statusTextView.setText(R.string.download_task_with);
                                    statusTextView.setVisibility(View.VISIBLE);
                                }
                                else {
                                    //noinspection ConstantConditions
                                    DocumentFile pickedDir = DocumentFile.fromTreeUri(context, Uri.parse(downloadPath));
                                    if(pickedDir != null)
                                    {
                                        DocumentFile[] files = pickedDir.listFiles();
                                        ArrayList<String> fileNames = new ArrayList<>();

                                        for(DocumentFile documentFile : files)
                                        {
                                            fileNames.add(documentFile.getName());
                                        }

                                        if(fileNames.contains(editableString))
                                        {
                                            statusTextView.setText(R.string.file_with);
                                            statusTextView.setVisibility(View.VISIBLE);
                                        } else {

                                            if(chunkMode == 0)
                                            {

                                                int segmentsForDownloadTask = 1;

                                                        switch (pauseResumeSupported) {
                                                            case "Unresumable":
                                                                break;
                                                            case "Resumable":
                                                                switch (((int) segmentsSliderNewTaskPopup.getValue())) {
                                                                    case 0:
                                                                        break;
                                                                    case 1:
                                                                        segmentsForDownloadTask = 2;
                                                                        break;
                                                                    case 2:
                                                                        segmentsForDownloadTask = 4;
                                                                        break;
                                                                    case 3:
                                                                        segmentsForDownloadTask = 6;
                                                                        break;
                                                                    case 4:
                                                                        segmentsForDownloadTask = 8;
                                                                        break;
                                                                    case 5:
                                                                        segmentsForDownloadTask = 16;
                                                                        break;
                                                                    case 6:
                                                                        segmentsForDownloadTask = 32;
                                                                        break;
                                                                }
                                                        }

                                                        if(contentLength < segmentsForDownloadTask)
                                                        {
                                                            statusTextView.setText(R.string.segments_for_download_task_are_less);
                                                            statusTextView.setVisibility(View.VISIBLE);
                                                        } else {

                                                            int finalSegmentsForDownloadTask = segmentsForDownloadTask;

                                                            new Thread(() -> {
                                                                int recentTaskId = -1;

                                                                try {
                                                                    //noinspection ConstantConditions
                                                                    activity.runOnUiThread(() -> showToast(R.string.creating_new_task,context));

                                                                    DownloadTask downloadTask = new DownloadTask();
                                                                    downloadTask.setSegmentsForDownloadTask(finalSegmentsForDownloadTask);
                                                                    downloadTask.setFileName(editableString);
                                                                    downloadTask.setUrl(url);
                                                                    downloadTask.setTotalBytes(contentLength);
                                                                    downloadTask.setDirPath(downloadPath);
                                                                    downloadTask.setDownloadedBytes(0L);
                                                                    downloadTask.setCurrentStatus(1);
                                                                    downloadTask.setCurrentProgress(0);
                                                                    downloadTask.setDownloadSpeed("Queued");
                                                                    downloadTask.setTimeLeft("-");
                                                                    downloadTask.setPauseResumeSupported(pauseResumeSupported);
                                                                    downloadTask.setWhichError("NotAny");
                                                                    downloadTask.setUserAgentString(userAgent);
                                                                    downloadTask.setPageURL(pageURL);
                                                                    downloadTask.setMimeType(mimeType);
                                                                    downloadTask.setIsPauseResumeSupported(isPauseResumeSupported);
                                                                    downloadTask.setChunkMode(chunkMode);

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

                                                                    DocumentFile pickedDir1 = DocumentFile.fromTreeUri(context,Uri.parse(downloadTask.getDirPath()));

                                                                    if(pickedDir1 != null)
                                                                    {
                                                                        pickedDir1.createFile(downloadTask.getMimeType(),downloadTask.getFileName());
                                                                    }

                                                                    db.addTask(downloadTask);
                                                                    recentTaskId = db.getRecentTaskID();

                                                                } catch (Exception ignored){}
                                                                finally {
                                                                    int finalRecentTaskId = recentTaskId;
                                                                    if(finalRecentTaskId != -1)
                                                                    {
                                                                        //data saved download now
                                                                        activity.runOnUiThread(() -> {
                                                                            Intent intent = new Intent(context, DownloadingService.class);
                                                                            Bundle bundle1 = new Bundle();
                                                                            bundle1.putInt("dId",finalRecentTaskId);
                                                                            bundle1.putString("dStatus","downloadNow");
                                                                            intent.putExtras(bundle1);
                                                                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                                                            {
                                                                                activity.startForegroundService(intent);
                                                                            } else {
                                                                                activity.startService(intent);
                                                                            }
                                                                            showToast(R.string.new_task_added,context);
                                                                        });
                                                                    }
                                                                }

                                                            }).start();

                                                            listener.addTaskSheetDismissed();
                                                            AddNewDTaskSheet.this.dismiss();

                                                        }
                                            } else {

                                                new Thread(() -> {
                                                    int recentTaskId = -1;

                                                    try {
                                                        //noinspection ConstantConditions
                                                        activity.runOnUiThread(() -> showToast(R.string.creating_new_task,context));

                                                        DownloadTask downloadTask = new DownloadTask();
                                                        downloadTask.setSegmentsForDownloadTask(1);
                                                        downloadTask.setFileName(editableString);
                                                        downloadTask.setUrl(url);
                                                        downloadTask.setTotalBytes(contentLength);
                                                        downloadTask.setDirPath(downloadPath);
                                                        downloadTask.setDownloadedBytes(0L);
                                                        downloadTask.setCurrentStatus(1);
                                                        downloadTask.setCurrentProgress(0);
                                                        downloadTask.setDownloadSpeed("Queued");
                                                        downloadTask.setTimeLeft("-");
                                                        downloadTask.setPauseResumeSupported(pauseResumeSupported);
                                                        downloadTask.setWhichError("NotAny");
                                                        downloadTask.setUserAgentString(userAgent);
                                                        downloadTask.setPageURL(pageURL);
                                                        downloadTask.setMimeType(mimeType);
                                                        downloadTask.setIsPauseResumeSupported(isPauseResumeSupported);
                                                        downloadTask.setChunkMode(chunkMode);

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

                                                        DocumentFile pickedDir1 = DocumentFile.fromTreeUri(context,Uri.parse(downloadTask.getDirPath()));

                                                        if(pickedDir1 != null)
                                                        {
                                                            pickedDir1.createFile(downloadTask.getMimeType(),downloadTask.getFileName());
                                                        }

                                                        db.addTask(downloadTask);
                                                        recentTaskId = db.getRecentTaskID();

                                                    } catch (Exception ignored){}
                                                    finally {
                                                        int finalRecentTaskId = recentTaskId;
                                                        if(finalRecentTaskId != -1)
                                                        {
                                                            //data saved download now
                                                            activity.runOnUiThread(() -> {
                                                                Intent intent = new Intent(context, DownloadingService.class);
                                                                Bundle bundle1 = new Bundle();
                                                                bundle1.putInt("dId",finalRecentTaskId);
                                                                bundle1.putString("dStatus","downloadNow");
                                                                intent.putExtras(bundle1);
                                                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                                                {
                                                                    activity.startForegroundService(intent);
                                                                } else {
                                                                    activity.startService(intent);
                                                                }
                                                                showToast(R.string.new_task_added,context);
                                                            });
                                                        }
                                                    }

                                                }).start();

                                                listener.addTaskSheetDismissed();
                                                AddNewDTaskSheet.this.dismiss();

                                            }


                                        }

                                    } else {
                                        statusTextView.setText(R.string.set_download_path);
                                        statusTextView.setVisibility(View.VISIBLE);
                                    }
                                }
                            } else {
                                statusTextView.setText(R.string.enter_file_name);
                                statusTextView.setVisibility(View.VISIBLE);
                            }


                    } catch (Exception e)
                    {
                        showToast(R.string.oops_general_message,context);
                    }
                }
            };

                fileNameET.setOnClickListener(onClickListener);
                downloadBtn.setOnClickListener(onClickListener);
                copyIB.setOnClickListener(onClickListener);
                shareIB.setOnClickListener(onClickListener);

        }

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (BottomSheetListener) context;
        }catch (ClassCastException e)
        {
            throw new ClassCastException(context + " must implement BottomSheetListener");
        }
    }


    public interface BottomSheetListener {
        void addTaskSheetDismissed();
    }
}