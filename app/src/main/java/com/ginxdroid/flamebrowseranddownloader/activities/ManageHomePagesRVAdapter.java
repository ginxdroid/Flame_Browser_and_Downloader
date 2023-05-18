package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.TextDrawable;
import com.ginxdroid.flamebrowseranddownloader.models.HomePageItem;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;

public class ManageHomePagesRVAdapter extends RecyclerView.Adapter<ManageHomePagesRVAdapter.ViewHolder> {
    private final DatabaseHandler db;
    private final Context context;
    private final ArrayList<Integer> homePageItems;
    private final AppCompatActivity activity;
    private final LayoutInflater inflater;
    private final String dumpPath;

    public ManageHomePagesRVAdapter(Context context, AppCompatActivity activity) {
        this.context = context;
        this.activity = activity;
        db=DatabaseHandler.getInstance(context);
        homePageItems = new ArrayList<>();
        inflater = LayoutInflater.from(context);
        dumpPath = context.getFilesDir().getAbsolutePath()+ File.separator+ "favicon" + File.separator + "no_file_ABC_XYZ";
    }

    @SuppressLint("NotifyDataSetChanged")
    void setHomePages()
    {
        if(homePageItems.size() > 0)
        {
            homePageItems.clear();
        }

        homePageItems.addAll(db.getAllHomePageItemsIDs());
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    void setSearchedHomePages(String title)
    {
        if(homePageItems.size() > 0)
        {
            homePageItems.clear();
        }

        homePageItems.addAll(db.getAllHomePageItemsIDsWithTitle(title));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ManageHomePagesRVAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.home_page_item_row,parent,false);
        return new ManageHomePagesRVAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageHomePagesRVAdapter.ViewHolder holder, int position) {
        int itemId = homePageItems.get(position);
        HomePageItem homePageItem = db.getHomePageItem(itemId);
        holder.hpFaviconIV.setBackground(null);
        holder.hpFaviconIV.setImageBitmap(null);
        holder.hpFaviconIV.setBackgroundResource(0);

        if(homePageItem.getHpFaviconPath() != null && homePageItem.getHpFaviconPath().equals("R.drawable"))
        {
            holder.hpFaviconIV.setBackgroundResource(R.drawable.splash_logo_light);
        } else {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                File file = new File(homePageItem.getHpFaviconPath());
                if(file.exists() && !homePageItem.getHpFaviconPath().equals(dumpPath))
                {
                    Bitmap bitmap = BitmapFactory.decodeFile(homePageItem.getHpFaviconPath(), options);
                    holder.hpFaviconIV.setImageBitmap(bitmap);
                } else
                {
                    if(homePageItem.getHpTitle().equals("No title"))
                    {
                        holder.hpFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                    } else {
                        String firstC = homePageItem.getHpTitle().substring(0,1);
                        holder.hpFaviconIV.setBackground(new TextDrawable(context,firstC));
                    }
                }
            } catch (Exception e)
            {
                if(homePageItem.getHpTitle().equals("No title"))
                {
                    holder.hpFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                } else {
                    String firstC = homePageItem.getHpTitle().substring(0,1);
                    holder.hpFaviconIV.setBackground(new TextDrawable(context,firstC));
                }
            }
        }

        holder.hpRadioBtn.setChecked(db.getHomePageURL().equals(homePageItem.getHpURL()));


        if(homePageItem.getHpURL().equals("NewTab"))
        {
            holder.hpDeleteIB.setVisibility(View.INVISIBLE);
        } else {
            holder.hpDeleteIB.setVisibility(View.VISIBLE);
        }

        holder.hpTitle.setText(homePageItem.getHpTitle());
        holder.hpUrl.setText(homePageItem.getHpURL());
    }

    @Override
    public int getItemCount() {
        return homePageItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView hpTitle,hpUrl;
        private final ImageView hpFaviconIV;
        private final ImageButton hpDeleteIB;
        private final RadioButton hpRadioBtn;

        @SuppressLint("NotifyDataSetChanged")
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            hpTitle = itemView.findViewById(R.id.hpTitle);
            hpUrl = itemView.findViewById(R.id.hpUrl);
            hpFaviconIV = itemView.findViewById(R.id.hpFaviconIV);
            hpDeleteIB = itemView.findViewById(R.id.hpDeleteIB);
            hpRadioBtn = itemView.findViewById(R.id.hpRadioBtn);

            hpRadioBtn.setOnClickListener(view -> {
                HomePageItem homePageItem = db.getHomePageItem(homePageItems.get(getBindingAdapterPosition()));
                db.updateHomePageURL(homePageItem.getHpURL());
                Toast.makeText(context, homePageItem.getHpTitle() + " is your homepage from now on.", Toast.LENGTH_SHORT).show();
                notifyDataSetChanged();
            });

            hpDeleteIB.setOnClickListener(view -> deleteBMI(homePageItems.get(getBindingAdapterPosition()),(ViewGroup) itemView, getBindingAdapterPosition()));
        }

        @SuppressLint("NotifyDataSetChanged")
        private void deleteBMI(final int id, ViewGroup itemView, final int position)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            View popupView = activity.getLayoutInflater().inflate(R.layout.popup_delete_quick_link, itemView, false);
            builder.setView(popupView);
            final AlertDialog dialog = builder.create();

            Window window = dialog.getWindow();
            if(window != null)
            {
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                layoutParams.windowAnimations = R.style.PopupWindowAnimationStyleSmallPopupWindow;
                window.setAttributes(layoutParams);
            }

            ImageView qlFaviconIV = popupView.findViewById(R.id.qlFaviconIV);
            TextView qlTitle = popupView.findViewById(R.id.qlTitle);
            MaterialButton cancelBtn,removeBtn;
            cancelBtn = popupView.findViewById(R.id.cancelBtn);
            removeBtn = popupView.findViewById(R.id.removeBtn);

            final HomePageItem homePageItem = db.getHomePageItem(id);

            qlFaviconIV.setBackground(null);
            qlFaviconIV.setImageBitmap(null);
            qlFaviconIV.setBackgroundResource(0);

            if(homePageItem.getHpFaviconPath() != null && homePageItem.getHpFaviconPath().equals("R.drawable"))
            {
                qlFaviconIV.setBackgroundResource(R.drawable.splash_logo_light);
            } else {
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    File file = new File(homePageItem.getHpFaviconPath());
                    if(file.exists() && !homePageItem.getHpFaviconPath().equals(dumpPath))
                    {
                        Bitmap bitmap = BitmapFactory.decodeFile(homePageItem.getHpFaviconPath(), options);
                        qlFaviconIV.setImageBitmap(bitmap);
                    } else
                    {
                        if(homePageItem.getHpTitle().equals("No title"))
                        {
                            qlFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                        } else {
                            String firstC = homePageItem.getHpTitle().substring(0,1);
                            qlFaviconIV.setBackground(new TextDrawable(context,firstC));
                        }
                    }
                } catch (Exception e)
                {
                    if(homePageItem.getHpTitle().equals("No title"))
                    {
                        qlFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                    } else {
                        String firstC = homePageItem.getHpTitle().substring(0,1);
                        qlFaviconIV.setBackground(new TextDrawable(context,firstC));
                    }
                }
            }

            qlTitle.setText(homePageItem.getHpTitle());

            cancelBtn.setOnClickListener(view -> dialog.dismiss());

            removeBtn.setOnClickListener(view -> {
                new Thread(() -> {
                    final String faviconPath = homePageItem.getHpFaviconPath();
                    if(db.checkNotContainsFaviconInQuickLinks(faviconPath)
                    && db.checkNotContainsFaviconInHistory(faviconPath)
                        && db.checkNotContainsFaviconInBookmarks(faviconPath))
                    {
                        File file = new File(faviconPath);
                        if(file.exists())
                        {
                            //noinspection ResultOfMethodCallIgnored
                            file.delete();
                        }
                    }
                }).start();
                db.deleteHomePageItem(id);

                if(db.getHomePageURL().equals(homePageItem.getHpURL()))
                {
                    db.updateHomePageURL("NewTab");
                }

                homePageItems.remove(position);
                notifyDataSetChanged();
                dialog.dismiss();

            });

            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        }
    }
}
