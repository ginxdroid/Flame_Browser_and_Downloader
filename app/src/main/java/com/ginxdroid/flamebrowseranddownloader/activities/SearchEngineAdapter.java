package com.ginxdroid.flamebrowseranddownloader.activities;

import static com.ginxdroid.flamebrowseranddownloader.classes.ResourceFinder.getResId;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.models.SearchEngineItem;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;

public class SearchEngineAdapter extends RecyclerView.Adapter<SearchEngineAdapter.SEViewHolder> {
    private final ArrayList<Integer> searchEngineItems;
    private final ImageView innerFaviconView;
    private final DatabaseHandler db;
    private final Context context;
    private final NormalTabsRVAdapter normalTabsRVAdapter;
    private final NormalTabsRVAdapter.ViewHolder viewHolder;
    private final LayoutInflater inflater;

    public SearchEngineAdapter(ImageView innerFaviconView, DatabaseHandler db, Context context, NormalTabsRVAdapter normalTabsRVAdapter,
                               NormalTabsRVAdapter.ViewHolder viewHolder, LayoutInflater inflater) {
        this.innerFaviconView = innerFaviconView;
        this.db = db;
        this.context = context;
        this.normalTabsRVAdapter = normalTabsRVAdapter;
        this.viewHolder = viewHolder;
        this.inflater = inflater;

        searchEngineItems = new ArrayList<>();
    }

    @SuppressLint("NotifyDataSetChanged")
    void setDefaultSearchEngineItems()
    {
        if(searchEngineItems.size() > 0)
        {
            searchEngineItems.clear();
        }

        searchEngineItems.addAll(db.getAllSearchEngineItemIDs());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchEngineAdapter.SEViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.search_engine_item_row_hp,parent,false);
        return new SEViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchEngineAdapter.SEViewHolder holder, int position) {
        SearchEngineItem searchEngineItem = db.getSearchEngineItem(searchEngineItems.get(position));
        holder.seTitleTV.setText(searchEngineItem.getSEItemTitle());

        holder.searchEngineIVInner.setImageResource(0);
        try {
            if(searchEngineItem.getSEIsDefault() == 1)
            {
                int resId = getResId(context,searchEngineItem.getSEItemTitle().toLowerCase(),"drawable",context.getPackageName());

                if(resId != 0)
                {
                    holder.searchEngineIVInner.setImageResource(resId);
                } else {
                    throw new Exception();
                }
            } else {
                holder.searchEngineIVInner.setImageResource(R.drawable.round_search_24);
            }
        } catch (Exception e)
        {
            holder.searchEngineIVInner.setImageResource(R.drawable.round_search_24);
        }

        holder.seRadioBtn.setChecked(searchEngineItem.getSEItemIsCurrent() == 1);
    }

    @Override
    public int getItemCount() {
        return searchEngineItems.size();
    }

    public class SEViewHolder extends RecyclerView.ViewHolder {
        private final ImageView searchEngineIVInner;
        private final RadioButton seRadioBtn;
        private final MaterialTextView seTitleTV;

        public SEViewHolder(@NonNull View itemView) {
            super(itemView);
            searchEngineIVInner = itemView.findViewById(R.id.searchEngineIVInner);
            seRadioBtn = itemView.findViewById(R.id.seRadioBtn);
            seTitleTV = itemView.findViewById(R.id.seTitleTV);

            @SuppressLint("NotifyDataSetChanged") View.OnClickListener onClickListener = view -> {
                db.updateOldSeId();
                int id = searchEngineItems.get(getBindingAdapterPosition());
                SearchEngineItem searchEngineItem = db.getSearchEngineItem(id);
                String newSeURL = searchEngineItem.getSEItemURL();
                db.updateSearchEngineURL(newSeURL);
                db.updateSEIsCurrent(id);
                notifyDataSetChanged();

                if(normalTabsRVAdapter != null)
                {
                    normalTabsRVAdapter.setSearchEngineURL(newSeURL);
                    int seFavResId = R.drawable.round_search_24;
                    try {
                        if(searchEngineItem.getSEIsDefault() == 1)
                        {
                            seFavResId = getResId(context,searchEngineItem.getSEItemTitle().toLowerCase(),"drawable",context.getPackageName());

                            if(seFavResId != 0)
                            {
                                normalTabsRVAdapter.setSeFavResId(seFavResId);
                            } else {
                                throw new Exception();
                            }
                        } else {
                            normalTabsRVAdapter.setSeFavResId(seFavResId);
                        }
                    } catch (Exception e)
                    {
                        normalTabsRVAdapter.setSeFavResId(seFavResId);
                    }finally {


                        if(innerFaviconView != null)
                        {
                            innerFaviconView.setImageResource(0);
                            innerFaviconView.setImageResource(seFavResId);
                        }

                        if(viewHolder != null)
                        {
                            if(viewHolder.isHPCVisible)
                            {
                                viewHolder.connectionInformationIBInner.setImageResource(0);
                                viewHolder.connectionInformationIBInner.setImageResource(seFavResId);
                            }
                        }
                    }
                }
            };

            seRadioBtn.setOnClickListener(onClickListener);
            searchEngineIVInner.setOnClickListener(onClickListener);
            seTitleTV.setOnClickListener(onClickListener);

        }
    }
}
