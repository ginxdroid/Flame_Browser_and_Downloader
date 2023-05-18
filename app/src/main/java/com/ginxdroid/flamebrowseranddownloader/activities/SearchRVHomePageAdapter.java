package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.CustomEditText;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.google.common.net.InternetDomainName;

import java.net.URL;
import java.util.ArrayList;

public class SearchRVHomePageAdapter extends RecyclerView.Adapter<SearchRVHomePageAdapter.ViewHolder> {
    private final ArrayList<String> searchItems;
    private final CustomEditText searchETHP;
    private String title = null;
    private final Dialog dialog;
    private final ConstraintLayout cBMetaDataRL;
    private final ImageButton voiceLauncherIB, closeSearchETHP;
    private final DatabaseHandler db;
    private final NormalTabsRVAdapter.ViewHolder viewHolder;
    private final NormalTabsRVAdapter normalTabsRVAdapter;
    private final LayoutInflater inflater;

    public SearchRVHomePageAdapter(CustomEditText searchETHP, Dialog dialog, ConstraintLayout cBMetaDataRL, ImageButton voiceLauncherIB, ImageButton closeSearchETHP, DatabaseHandler db,
                                   NormalTabsRVAdapter.ViewHolder viewHolder, NormalTabsRVAdapter normalTabsRVAdapter) {
        this.searchETHP = searchETHP;
        this.dialog = dialog;
        this.cBMetaDataRL = cBMetaDataRL;
        this.voiceLauncherIB = voiceLauncherIB;
        this.closeSearchETHP = closeSearchETHP;
        this.db = db;
        this.viewHolder = viewHolder;
        this.normalTabsRVAdapter = normalTabsRVAdapter;
        inflater = normalTabsRVAdapter.getLayoutInflater();
        searchItems = new ArrayList<>();
    }

    @SuppressLint("NotifyDataSetChanged")
    void setDefaultSearchItems()
    {
        try {
            this.title = null;
            if(searchItems.size() > 0)
            {
                searchItems.clear();
            }

            searchItems.addAll(db.getAllSearchItems());
            notifyDataSetChanged();
        } catch (Exception ignored) {}
    }

    @SuppressLint("NotifyDataSetChanged")
    void setSearchItems(String title)
    {
        try {
            this.title = title;
            if(searchItems.size() > 0)
            {
                searchItems.clear();
            }

            searchItems.addAll(db.getAllSearchItemsWithTitle(title));
            notifyDataSetChanged();
        } catch (Exception ignored) {}
    }

    @SuppressLint("NotifyDataSetChanged")
    void setSearchResult(ArrayList<String> searchResults, String title)
    {
        try {
            if(searchItems.size() > 0)
            {
                searchItems.clear();
            }
            this.title = title;
            searchItems.addAll(db.getAllSearchItemsWithTitle(title));
            searchItems.addAll(searchResults);
            notifyDataSetChanged();
        }catch (Exception ignored) {}
    }



    @NonNull
    @Override
    public SearchRVHomePageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.search_item_row,parent,false);
        return new SearchRVHomePageAdapter.ViewHolder(view);
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

    private void setFavicon(ImageButton faviconIB, String keyWord)
    {
        try {
            faviconIB.setImageResource(0);
            new URL(keyWord);

            if(normalTabsRVAdapter.isNetworkUrl(keyWord))
            {
                faviconIB.setImageResource(R.drawable.public_earth_bg);
            } else {
                faviconIB.setImageResource(R.drawable.round_search_24);
            }
        } catch (Exception e)
        {
            try {
                @SuppressWarnings("UnstableApiUsage") InternetDomainName internetDomainName = InternetDomainName.from(keyWord);
                //noinspection UnstableApiUsage
                if(internetDomainName.hasPublicSuffix() && internetDomainName.hasParent())
                {
                    faviconIB.setImageResource(R.drawable.public_earth_bg);
                } else //noinspection UnstableApiUsage
                    if(internetDomainName.isTopPrivateDomain())
                {
                    faviconIB.setImageResource(R.drawable.public_earth_bg);
                } else {
                    faviconIB.setImageResource(R.drawable.round_search_24);
                }

            } catch (Exception e1)
            {
                try {
                    faviconIB.setImageResource(R.drawable.round_search_24);
                }catch (Exception ignored){}
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SearchRVHomePageAdapter.ViewHolder holder, int position) {
        try {
            final String value = searchItems.get(position);
            holder.searchItemTitle.setText(value);
            setHighLightedText(holder.searchItemTitle,title);
            setFavicon(holder.faviconIB,value);
        } catch (Exception ignored) {}
    }

    @Override
    public int getItemCount() {
        return searchItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView searchItemTitle;
        private final ImageButton faviconIB;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            searchItemTitle = itemView.findViewById(R.id.searchItemTitle);
            faviconIB = itemView.findViewById(R.id.faviconIB);

            ImageButton enterSearchItemIB = itemView.findViewById(R.id.enterSearchItemIB);
            RelativeLayout searchItemRL = itemView.findViewById(R.id.searchItemRL);

            final View.OnClickListener onClickListener = view ->
                    viewHolder.holderUtility.checkAndLoad(searchItems.get(getBindingAdapterPosition()),dialog);

            searchItemRL.setOnClickListener(onClickListener);
            searchItemTitle.setOnClickListener(onClickListener);
            faviconIB.setOnClickListener(onClickListener);

            enterSearchItemIB.setOnClickListener(view -> {
                try {
                    String value = searchItems.get(getBindingAdapterPosition());
                    searchETHP.setText(value);
                    searchETHP.setSelection(value.length());
                    cBMetaDataRL.setVisibility(View.GONE);
                    voiceLauncherIB.setVisibility(View.INVISIBLE);
                    closeSearchETHP.setVisibility(View.VISIBLE);
                } catch (Exception ignored) {}
            });

        }
    }
}
