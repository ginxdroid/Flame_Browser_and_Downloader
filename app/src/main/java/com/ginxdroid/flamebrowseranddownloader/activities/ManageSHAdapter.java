package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class ManageSHAdapter extends RecyclerView.Adapter<ManageSHAdapter.ViewHolder> {
    private final ArrayList<Integer> searchItems;
    private final Context context;
    private final DatabaseHandler db;
    private final LayoutInflater inflater;
    private String title;

    public ManageSHAdapter(Context context) {
        this.context = context;
        searchItems = new ArrayList<>();
        db = DatabaseHandler.getInstance(context);
        inflater = LayoutInflater.from(context);
    }

    @SuppressLint("NotifyDataSetChanged")
    void setSearchHistory()
    {
        try {
            this.title = null;
            if(searchItems.size() > 0)
            {
                searchItems.clear();
            }

            searchItems.addAll(db.getAllSearchHistoryItemsIDs());
            notifyDataSetChanged();
        } catch (Exception ignored) {}
    }

    @SuppressLint("NotifyDataSetChanged")
    void setSearchedSearchHistory(String title)
    {
        try {
            this.title = title;
            if(searchItems.size() > 0)
            {
                searchItems.clear();
            }

            searchItems.addAll(db.getAllSearchItemsIDsWithTitleAs(title));
            notifyDataSetChanged();
        } catch (Exception ignored) {}
    }

    private void setHighLightedText(TextView tv, String textToHighlight)
    {
        try {
            if(HelperTextUtility.isNotEmpty(textToHighlight))
            {
                final StyleSpan span = new StyleSpan(Typeface.BOLD);
                String tvt = tv.getText().toString();
                int ofe = tvt.indexOf(textToHighlight);
                Spannable wordToSpan = new SpannableString(tv.getText());
                for(int ofs = 0; ofs < tvt.length() && ofe != -1; ofs = ofe + 1)
                {
                    ofe = tvt.indexOf(textToHighlight,ofs);
                    if(ofe == -1)
                    {
                        break;
                    }else {
                        //set color
                        wordToSpan.setSpan(span,ofe,ofe + textToHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        tv.setText(wordToSpan,TextView.BufferType.SPANNABLE);
                    }
                }

                ofe = tvt.indexOf(textToHighlight);
                for(int ofs = 0;ofs < tvt.length() && ofe != -1;ofs = ofe + 1)
                {
                    ofe = tvt.indexOf(textToHighlight, ofs);
                    if(ofe == -1)
                    {
                        break;
                    } else {
                        wordToSpan.setSpan(span,ofe,ofe + textToHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        tv.setText(wordToSpan,TextView.BufferType.SPANNABLE);
                    }
                }
            }
        } catch (Exception ignored) {}
    }


    @NonNull
    @Override
    public ManageSHAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.manage_sh_item_row,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageSHAdapter.ViewHolder holder, int position) {
        holder.siTitle.setText(db.getSearchItemTitle(searchItems.get(position)));
        setHighLightedText(holder.siTitle,title);
    }

    @Override
    public int getItemCount() {
        return searchItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView siTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            siTitle = itemView.findViewById(R.id.siTitle);
            final ImageButton siDeleteIB = itemView.findViewById(R.id.siDeleteIB);
            siDeleteIB.setOnClickListener(view -> deleteSI(searchItems.get(getBindingAdapterPosition()),(ViewGroup)itemView,getBindingAdapterPosition()));
        }

        private void deleteSI(int id, ViewGroup itemView,int position)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = inflater.inflate(R.layout.popup_delete_search_item, itemView,false);
            builder.setView(view);
            final AlertDialog dialog = builder.create();

            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                layoutParams.windowAnimations = R.style.PopupWindowAnimationStyleSmallPopupWindow;
                window.setAttributes(layoutParams);
            }

            final TextView siTitle = view.findViewById(R.id.siTitle);
            MaterialButton cancelBtn, deleteBtn;
            cancelBtn = view.findViewById(R.id.cancelBtn);
            deleteBtn = view.findViewById(R.id.deleteBtn);

            siTitle.setText(db.getSearchItemTitle(id));

            cancelBtn.setOnClickListener(view1 -> dialog.dismiss());

            deleteBtn.setOnClickListener(view12 -> {
                db.deleteSearchItem(id);
                searchItems.remove(position);
                notifyItemRemoved(position);
                dialog.dismiss();
            });

            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();

        }
    }
}
