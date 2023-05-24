package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.models.ThemeModel;
import com.ginxdroid.flamebrowseranddownloader.sheets.ThemesSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ThemesRVAdapter extends RecyclerView.Adapter<ThemesRVAdapter.ViewHolder> {
    private final DatabaseHandler db;
    private final Context context;
    private final LayoutInflater inflater;
    private final List<Integer> themeIDs;
    private final int isDarkRV;
    private final MainActivity mainActivity;
    private final ThemesSheet themesSheet;
    private final CoordinatorLayout recyclerViewContainer;
    private final int currentThemeID;

    public ThemesRVAdapter(Context context, int isDarkRV, MainActivity mainActivity, ThemesSheet themesSheet, CoordinatorLayout recyclerViewContainer, int currentThemeID) {
        this.context = context;
        this.isDarkRV = isDarkRV;
        this.mainActivity = mainActivity;
        this.themesSheet = themesSheet;
        this.recyclerViewContainer = recyclerViewContainer;
        this.currentThemeID = currentThemeID;

        db = DatabaseHandler.getInstance(context);
        this.themeIDs = new ArrayList<>();
        inflater = LayoutInflater.from(context);
    }

    @SuppressLint("NotifyDataSetChanged")
    void setThemes(List<Integer> list)
    {
        if(themeIDs.size() > 0)
        {
            themeIDs.clear();
        }

        themeIDs.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ThemesRVAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = inflater.inflate(R.layout.theme_item_row, parent, false);
        return new ThemesRVAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThemesRVAdapter.ViewHolder holder, int position) {
        int themeId = themeIDs.get(position);
        ThemeModel themeModel = db.getThemeModel(themeId);
        holder.mcv.setCardBackgroundColor(Color.parseColor(themeModel.getThemeAccentColor()));

        if(currentThemeID == themeId)
        {
            holder.checkedIV.setVisibility(View.VISIBLE);
            holder.checkedIV.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(themeModel.getThemeAccentTextColor())));
        } else {
            holder.checkedIV.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return themeIDs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView mcv;
        private final ImageView checkedIV;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mcv = itemView.findViewById(R.id.mcv);
            checkedIV = itemView.findViewById(R.id.checkedIV);

            mcv.setOnClickListener(view -> {
                themesSheet.dismiss();

                int itemId = themeIDs.get(getBindingAdapterPosition());
                db.changeTheme(isDarkRV);
                db.updateCurrentThemeID(itemId);
                mainActivity.setRecreating();

                if(isDarkRV == 1)
                {
                    if(db.getDarkWebUI() == 1)
                    {
                        //don't prompt user to set Dark Web UI
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        mainActivity.recreate();
                    }
                    else {
                        //We will prompt user to whether user wants to set dark web ui or not
                        showDarkWebUIChooser();
                    }
                }else {
                    if(db.getDarkWebUI() == 1)
                    {
                        //we want to prompt user to disable dark web ui
                        showDarkWebUIDisablerDialog();
                    }
                    else{
                        //then we will directly set dark theme for our app
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        mainActivity.recreate();
                    }
                }

            });
        }

        private void showDarkWebUIChooser()
        {
            try {
                //Show alert dialog
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

                final View view = mainActivity.getLayoutInflater().inflate(R.layout.popup_dark_web_ui_chooser,recyclerViewContainer,false);

                dialogBuilder.setView(view);
                AlertDialog dialog = dialogBuilder.create();

                //Getting views
                MaterialButton noBtn, yesBtn;
                noBtn = view.findViewById(R.id.noBtn);
                yesBtn = view.findViewById(R.id.yesBtn);

                noBtn.setOnClickListener(view1 -> {
                    dialog.dismiss();
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    mainActivity.recreate();
                });

                yesBtn.setOnClickListener(view12 -> {
                    dialog.dismiss();
                    db.updateIsDarkWebUI(1);
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    mainActivity.recreate();
                });

                dialog.setCanceledOnTouchOutside(false);
                dialog.setCancelable(false);
                dialog.show();
            }catch (Exception ignored)
            {}
        }

        private void showDarkWebUIDisablerDialog()
        {
            try {
                //Show alert dialog
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

                final View view = mainActivity.getLayoutInflater().inflate(R.layout.popup_dark_web_ui_disabler,recyclerViewContainer,false);

                dialogBuilder.setView(view);
                AlertDialog dialog = dialogBuilder.create();

                //Getting views
                MaterialButton noBtn, yesBtn;
                noBtn = view.findViewById(R.id.noBtn);
                yesBtn = view.findViewById(R.id.yesBtn);

                noBtn.setOnClickListener(view1 -> {
                    dialog.dismiss();
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    mainActivity.recreate();
                });

                yesBtn.setOnClickListener(view12 -> {
                    dialog.dismiss();
                    db.updateIsDarkWebUI(0);
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    mainActivity.recreate();
                });

                dialog.setCanceledOnTouchOutside(false);
                dialog.setCancelable(false);
                dialog.show();
            }catch (Exception ignored)
            {}
        }
    }
}
