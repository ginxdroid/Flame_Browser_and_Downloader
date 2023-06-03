package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.models.UserPreferences;
import com.ginxdroid.flamebrowseranddownloader.sheets.MessageSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textview.MaterialTextView;

public class DownloaderSettings extends BaseActivity implements View.OnClickListener {

    private DatabaseHandler db;
    private TextView downloadLocationValue;
    private LinearLayout mainContainer;
    private AlertDialog chooseDownloadLocationAD;
    private Spinner simultaneousTasksSpinner;
    private Toast toast = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloader_settings);

        db = DatabaseHandler.getInstance(DownloaderSettings.this);
        mainContainer = findViewById(R.id.mainContainer);
        setUp();
    }

    private void setUp()
    {
        final ImageButton backIB = mainContainer.findViewById(R.id.backIB);
        backIB.setOnClickListener(DownloaderSettings.this);

        final LinearLayout downloadPathLL = mainContainer.findViewById(R.id.downloadPathLL);
        downloadPathLL.setOnClickListener(DownloaderSettings.this);
        downloadLocationValue = downloadPathLL.findViewById(R.id.downloadLocationValue);

        final SwitchMaterial autoResumeSwitchDownloaderSettings = mainContainer.findViewById(R.id.autoResumeSwitchDownloaderSettings);

        final LinearLayout simultaneousTasksLL = mainContainer.findViewById(R.id.simultaneousTasksLL);
        simultaneousTasksLL.setOnClickListener(DownloaderSettings.this);
        simultaneousTasksSpinner = simultaneousTasksLL.findViewById(R.id.simultaneousTasksSpinner);

        final LinearLayout segmentsForDownloadLL = mainContainer.findViewById(R.id.segmentsForDownloadLL);
        final Slider segmentsForDownloadSlider = segmentsForDownloadLL.findViewById(R.id.segmentsForDownloadSlider);
        final TextView segmentsForDownloadTV = segmentsForDownloadLL.findViewById(R.id.segmentsForDownloadTV);

        UserPreferences userPreferences = db.getDTSettingsUP();
        int segmentsSBProgress = userPreferences.getDefaultSegments();
        segmentsForDownloadSlider.setValue(segmentsSBProgress);

        switch (segmentsSBProgress)
        {
            case 0:
                segmentsForDownloadTV.setText(String.valueOf(1));
                break;
            case 1:
                segmentsForDownloadTV.setText(String.valueOf(2));
                break;
            case 2:
                segmentsForDownloadTV.setText(String.valueOf(4));
                break;
            case 3:
                segmentsForDownloadTV.setText(String.valueOf(6));
                break;
            case 4:
                segmentsForDownloadTV.setText(String.valueOf(8));
                break;
            case 5:
                segmentsForDownloadTV.setText(String.valueOf(16));
                break;
            case 6:
                segmentsForDownloadTV.setText(String.valueOf(32));
                break;
        }

        segmentsForDownloadSlider.addOnChangeListener((slider, value, fromUser) -> {
            int finalValue = (int) value;
            switch (finalValue)
            {
                case 0:
                    segmentsForDownloadTV.setText(String.valueOf(1));
                    break;
                case 1:
                    segmentsForDownloadTV.setText(String.valueOf(2));
                    break;
                case 2:
                    segmentsForDownloadTV.setText(String.valueOf(4));
                    break;
                case 3:
                    segmentsForDownloadTV.setText(String.valueOf(6));
                    break;
                case 4:
                    segmentsForDownloadTV.setText(String.valueOf(8));
                    break;
                case 5:
                    segmentsForDownloadTV.setText(String.valueOf(16));
                    break;
                case 6:
                    segmentsForDownloadTV.setText(String.valueOf(32));
                    break;
            }

            db.updateDefaultSegments(finalValue);
        });

        try {
            downloadLocationValue.setText(userPreferences.getDownloadPath());
        } catch (Exception e)
        {
            downloadLocationValue.setText(DownloaderSettings.this.getString(R.string.not_found));
        }

        int autoResumeStatus = userPreferences.getAutoResumeStatus();
        autoResumeSwitchDownloaderSettings.setChecked(autoResumeStatus == 1);

        String[] tasks = {"1","2","3","4"};
        simultaneousTasksSpinner.setAdapter(new SpinnerAdapter(tasks));
        simultaneousTasksSpinner.setSelection(userPreferences.getSimultaneousTasks() - 1,false);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                db.updateSimultaneousTasks(position + 1);
                showMessageDialog();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };

        simultaneousTasksSpinner.setOnItemSelectedListener(listener);

        autoResumeSwitchDownloaderSettings.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked)
            {
                db.updateAutoResumeStatus(1);
                showToast(R.string.auto_resume_turned_on);
            } else {
                db.updateAutoResumeStatus(0);
                showToast(R.string.auto_resume_turned_off);
            }
        });
    }

    private void showMessageDialog()
    {
        try{
            MessageSheet messageSheet = new MessageSheet();
            Bundle bundle = new Bundle();
            bundle.putInt("message",R.string.will_only_take_effect);
            messageSheet.setArguments(bundle);
            messageSheet.show(DownloaderSettings.this
                    .getSupportFragmentManager(), "messageSheet");
        }catch(Exception ignored)
        {}
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.backIB)
        {
            finish();
        } else if (id == R.id.downloadPathLL) {
            openChooseLocationPopup();
        } else if(id == R.id.simultaneousTasksLL)
        {
            simultaneousTasksSpinner.performClick();
        }
    }

    private class SpinnerAdapter extends BaseAdapter
    {
        private final String[] mCounting;
        private final LayoutInflater inflater;

        public SpinnerAdapter(String[] mCounting) {
            this.mCounting = mCounting;
            this.inflater = LayoutInflater.from(DownloaderSettings.this);
        }

        @Override
        public int getCount() {
            return mCounting.length;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(view == null)
            {
                view = inflater.inflate(R.layout.spinner_item_row_downloader_settings,viewGroup,false);
            }

            MaterialTextView valueTV = view.findViewById(R.id.valueTV);
            valueTV.setText(mCounting[i]);

            return view;
        }
    }


    final ActivityResultLauncher<Intent> selectDownloadPathLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    try {
                        if(result.getResultCode() == Activity.RESULT_OK)
                        {
                            Intent data = result.getData();
                            if(data != null)
                            {
                                Uri treeUri = data.getData();
                                if(treeUri != null)
                                {
                                    db.updateDownloadAddress(treeUri.toString());
                                    getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    downloadLocationValue.setText(treeUri.toString());
                                    showToast(R.string.updated_successfully);
                                }
                                else {
                                    showToast(R.string.please_set_download_directory_again);
                                }
                            }
                        }
                    } catch (Exception e)
                    {
                        showToast(R.string.please_set_download_directory_again);
                    }
                }
            });

    private void openChooseLocationPopup()
    {
        //Show storagePathNotChosenDialog
        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(DownloaderSettings.this);
        View view = DownloaderSettings.this.getLayoutInflater().
                inflate(R.layout.popup_storage_path_not_choosen,mainContainer,false);

        final MaterialButton closeBtn,chooseBtn;
        closeBtn = view.findViewById(R.id.closeBtn);
        chooseBtn = view.findViewById(R.id.chooseBtn);

        closeBtn.setOnClickListener(view1 -> chooseDownloadLocationAD.dismiss());

        chooseBtn.setOnClickListener(view12 -> {
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                selectDownloadPathLauncher.launch(intent);
                chooseDownloadLocationAD.dismiss();
            } catch (Exception e1)
            {
                showToast(R.string.maybe_you_have_disabled);
            }
        });

        dialogBuilder.setView(view);
        chooseDownloadLocationAD = dialogBuilder.create();
        Window window = chooseDownloadLocationAD.getWindow();
        WindowManager.LayoutParams layoutParams;
        if(window != null)
        {
            layoutParams = window.getAttributes();
            if(layoutParams != null)
            {
                layoutParams.windowAnimations = R.style.PopupWindowAnimationStyleSmallPopupWindow;
                window.setAttributes(layoutParams);
            }
        }

        chooseDownloadLocationAD.show();
    }

    void showToast(int resID)
    {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(DownloaderSettings.this, resID, Toast.LENGTH_SHORT);
        toast.show();
    }
}