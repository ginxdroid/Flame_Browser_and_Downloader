package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.sheets.OptimizationsSheet;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textview.MaterialTextView;

public class FirstActivity extends BaseActivity implements View.OnClickListener, DownloadingFragment.DFListener,
        CompletedFragment.CFListener, OptimizationsSheet.BottomSheetListener {

    private Toast toast = null;
    private ConstraintLayout mainContainer;
    private DatabaseHandler db;

    private RelativeLayout tabsRL;

    private ViewPager2 viewPager;
    private TabLayout tabs;

    private String[] tabTitles = new String[] {"Downloading","Completed"};

    private RecyclerViewAdapter recyclerViewAdapter;
    private CompletedRecyclerViewAdapter completedRecyclerViewAdapter;

    private LinearLayout editLL;

    private MaterialTextView loadingMTV;

    private MaterialButton backButtonDeleteIB,selectAllIB,deselectAllIB,deleteIB;
    private MaterialTextView countTV;

    private BroadcastReceiver broadcastReceiver;
    private final String DOWNLOADING_INTENT = "dI";
    private final String DOWNLOAD_PAUSED_INTENT = "dPI";
    private final String ERROR_OCCURRED_INTENT = "eCI";
    private final String RESUMING_INTENT = "rI";
    private final String DOWNLOAD_COMPLETE_INTENT = "dCI";
    private final String DO_DELETION_WORK_INTENT = "dDWI";

    void resumeBroadcastReceiver()
    {

    }

    private void setUpBroadcastReceiver()
    {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    String action = intent.getAction();
                    if(action != null)
                    {
                        switch (action)
                        {
                            case DOWNLOADING_INTENT:
                            case RESUMING_INTENT:
                            case ERROR_OCCURRED_INTENT:
                            case DOWNLOAD_PAUSED_INTENT:
                            {
                                recyclerViewAdapter.reflectChange(intent.getIntExtra("dId",0));
                                break;
                            }
                            case DOWNLOAD_COMPLETE_INTENT:
                            {
                                LocalBroadcastManager.getInstance(FirstActivity.this).unregisterReceiver(broadcastReceiver);
                                //todo recyclerViewAdapter.setTasksCompletion();
                                break;
                            }
                            case DO_DELETION_WORK_INTENT:
                            {
                                //todo recyclerViewAdapter.doDeletionWork();
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        };
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        initiate();
    }

    private void initiate()
    {
        mainContainer = findViewById(R.id.mainContainer);
        db = DatabaseHandler.getInstance(FirstActivity.this);


        editLL = mainContainer.findViewById(R.id.editLL);

        final MaterialButton refreshIB,propertiesIB;

        refreshIB = editLL.findViewById(R.id.refreshIB);
        propertiesIB = editLL.findViewById(R.id.propertiesIB);
        refreshIB.setOnClickListener(FirstActivity.this);
        propertiesIB.setOnClickListener(FirstActivity.this);

        tabsRL = mainContainer.findViewById(R.id.tabsRL);


        viewPager = mainContainer.findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(1);

        recyclerViewAdapter = new RecyclerViewAdapter(FirstActivity.this,FirstActivity.this,mainContainer,tabsRL,editLL,propertiesIB,refreshIB);
        completedRecyclerViewAdapter = new CompletedRecyclerViewAdapter(FirstActivity.this,FirstActivity.this,
                mainContainer,tabsRL,editLL,propertiesIB,refreshIB);

        recyclerViewAdapter.setHasStableIds(false);
        completedRecyclerViewAdapter.setHasStableIds(false);

        viewPager.setAdapter(new DownloadsPagerAdapter(FirstActivity.this));

        tabs = tabsRL.findViewById(R.id.tabs);

        new TabLayoutMediator(tabs, viewPager, (tab, position) -> tab.setText(tabTitles[position])).attach();

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if(editLL.getVisibility() == View.VISIBLE)
                {
                    //todo doHideWork(tab.getPosition());
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


    }

    void setQueuedCount(int count)
    {
        try {
            TabLayout.Tab tab = tabs.getTabAt(0);
            if(tab != null)
            {
                BadgeDrawable badgeDrawable = tab.getOrCreateBadge();
                badgeDrawable.setNumber(count);
            }
        } catch (Exception ignored) {}
    }

    void setCompletedCount(int count)
    {
        try {
            TabLayout.Tab tab = tabs.getTabAt(1);
            if(tab != null)
            {
                BadgeDrawable badgeDrawable = tab.getOrCreateBadge();
                badgeDrawable.setNumber(count);
            }
        } catch (Exception ignored) {}
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.downloaderSettingsIB)
        {
            startActivity(new Intent(FirstActivity.this, DownloaderSettings.class));
        } else if(id == R.id.backButtonDeleteIB)
        {

        } else if(id == R.id.refreshIB)
        {

        } else if(id == R.id.propertiesIB)
        {

        } else if(id == R.id.selectAllIB)
        {

        } else if(id == R.id.deselectAllIB)
        {

        } else if(id == R.id.deleteIB)
        {

        }
    }

    @Override
    public void showCFFragment(View view) {
        final RecyclerView downloaderRV = view.findViewById(R.id.downloaderRV);
        completedRecyclerViewAdapter.setRecyclerView(downloaderRV);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(FirstActivity.this);
        linearLayoutManager.setItemPrefetchEnabled(true);
        downloaderRV.setLayoutManager(linearLayoutManager);

        SimpleItemAnimator simpleItemAnimator = (SimpleItemAnimator) downloaderRV.getItemAnimator();
        if(simpleItemAnimator != null)
        {
            simpleItemAnimator.setSupportsChangeAnimations(false);
        }
        downloaderRV.setAdapter(completedRecyclerViewAdapter);

        loadingMTV = mainContainer.findViewById(R.id.loadingMTV);
        loadingMTV.postDelayed(() -> {
            setUpBroadcastReceiver();
            recyclerViewAdapter.setTasks();
            completedRecyclerViewAdapter.setTasks();
            loadingMTV.setVisibility(View.GONE);
            checkOptimizations(FirstActivity.this);
        },5);


        backButtonDeleteIB = editLL.findViewById(R.id.backButtonDeleteIB);
        countTV = editLL.findViewById(R.id.countTV);

        selectAllIB = editLL.findViewById(R.id.selectAllIB);
        deselectAllIB = editLL.findViewById(R.id.deselectAllIB);
        deleteIB = editLL.findViewById(R.id.deleteIB);

        backButtonDeleteIB.setOnClickListener(FirstActivity.this);

        selectAllIB.setOnClickListener(FirstActivity.this);
        deselectAllIB.setOnClickListener(FirstActivity.this);
        deleteIB.setOnClickListener(FirstActivity.this);

        final ImageButton downloaderSettingsIB = tabsRL.findViewById(R.id.downloaderSettingsIB);
        downloaderSettingsIB.setOnClickListener(FirstActivity.this);

    }

    @Override
    public void showDFFragment(View view) {
        final RecyclerView downloaderRV = view.findViewById(R.id.downloaderRV);
        downloaderRV.setItemViewCacheSize(0);
        recyclerViewAdapter.setRecyclerView(downloaderRV);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(FirstActivity.this);
        linearLayoutManager.setItemPrefetchEnabled(true);
        downloaderRV.setLayoutManager(linearLayoutManager);

        SimpleItemAnimator simpleItemAnimator = (SimpleItemAnimator) downloaderRV.getItemAnimator();
        if(simpleItemAnimator != null)
        {
            simpleItemAnimator.setSupportsChangeAnimations(false);
        }
        downloaderRV.setAdapter(recyclerViewAdapter);
    }

    @Override
    public void optimizationSheetDismissed() {
        showNotificationPermissionPrompt();
    }

    final ActivityResultLauncher<String> postNotifications = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if(isGranted)
        {
            //notification permission is granted
            showToast(R.string.permission_granted);
        } else {
            //no notification permission granted
            if(ActivityCompat.shouldShowRequestPermissionRationale(FirstActivity.this, Manifest.permission.POST_NOTIFICATIONS))
            {
                //User selected never ask again option
                showToast(R.string.notifications_turned_off);
            } else {
                showToast(R.string.permission_denied);
            }
        }
    });

    private void showNotificationPermissionPrompt()
    {
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                if(ContextCompat.checkSelfPermission(FirstActivity.this,Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED)
                {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(FirstActivity.this,Manifest.permission.POST_NOTIFICATIONS))
                    {
                        //Explain to the user why we needed this permission
                        AlertDialog.Builder builder = new AlertDialog.Builder(FirstActivity.this);
                        View view = FirstActivity.this.getLayoutInflater().inflate(R.layout.popup_permission_needed,
                                mainContainer,false);

                        TextView whyNeededTV = view.findViewById(R.id.whyNeededTV);
                        whyNeededTV.setText(R.string.why_need_notification_permission);

                        builder.setView(view);
                        final AlertDialog dialog = builder.create();

                        MaterialButton grantPermissionDialogBtn,closePermissionDialogBtn;
                        grantPermissionDialogBtn = view.findViewById(R.id.grantPermissionDialogBtn);
                        closePermissionDialogBtn = view.findViewById(R.id.closePermissionDialogBtn);

                        grantPermissionDialogBtn.setOnClickListener(view1 -> {
                            dialog.dismiss();
                            postNotifications.launch(Manifest.permission.POST_NOTIFICATIONS);
                        });

                        closePermissionDialogBtn.setOnClickListener(view12 -> dialog.dismiss());

                        dialog.setCancelable(true);
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.show();
                    } else {
                        postNotifications.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                }
            }
        } catch (Exception e)
        {
            showToast(R.string.maybe_notifications_turned_off);
        }
    }

    private void checkOptimizations(Context context)
    {
        try {
            if(db.isShowOptimization())
            {
                final Context applicationContext = context.getApplicationContext();

                PowerManager powerManager = (PowerManager) applicationContext.getSystemService(Context.POWER_SERVICE);
                String name = applicationContext.getPackageName();
                boolean result = powerManager.isIgnoringBatteryOptimizations(name);
                if(!result)
                {
                    new OptimizationsSheet().show(FirstActivity.this.getSupportFragmentManager(),"optimizationsSheet");
                } else {
                    showNotificationPermissionPrompt();
                }
            } else {
                showNotificationPermissionPrompt();
            }
        } catch (Exception ignored) {}
    }

    void showToast(int resID)
    {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(FirstActivity.this, resID, Toast.LENGTH_SHORT);
        toast.show();
    }

    private static class DownloadsPagerAdapter extends FragmentStateAdapter
    {
        public DownloadsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if(position == 0)
            {
                return new DownloadingFragment();
            } else {
                return new CompletedFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}