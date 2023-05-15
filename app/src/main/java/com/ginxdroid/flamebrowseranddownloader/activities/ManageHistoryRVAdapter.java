package com.ginxdroid.flamebrowseranddownloader.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.TextDrawable;
import com.ginxdroid.flamebrowseranddownloader.models.HistoryItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ManageHistoryRVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final DatabaseHandler db;
    private final ArrayList<Integer> historyItems;
    private final Context context;
    private final LayoutInflater inflater;
    private final String dumpPath;

    public ManageHistoryRVAdapter(Context context) {
        this.context = context;
        db = DatabaseHandler.getInstance(context);
        historyItems = new ArrayList<>();
        inflater = LayoutInflater.from(context);
        dumpPath = context.getFilesDir().getAbsolutePath()+ File.separator+ "favicon" + File.separator + "no_file_ABC_XYZ";
    }



    @SuppressLint("NotifyDataSetChanged")
    void setHistory()
    {
        if(historyItems.size() > 0)
        {
            historyItems.clear();
        }

        historyItems.addAll(db.getAllHistoryItemsIDs());
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    void setSearchedHistory(String title)
    {
        if(historyItems.size() > 0)
        {
            historyItems.clear();
        }

        historyItems.addAll(db.getAllHistoryItemsIDsWithTitle(title));
        notifyDataSetChanged();
    }

    int clearAllHistory()
    {
        int size = historyItems.size();
        if(size > 0)
        {
            historyItems.clear();
        }

        return size;
    }

    void removeItem(int adapterPosition)
    {
        try {
            HistoryItem historyItem = null;
            try {
                final Integer hId = historyItems.get(adapterPosition);
                historyItem = db.getHistoryItem(hId);

                final HistoryItem finalHistoryItem = historyItem;
                new Thread(() -> {
                    final String faviconPath = finalHistoryItem.getHiFaviconPath();
                    if(db.checkNotContainsFaviconInBookmarks(faviconPath) && db.checkNotContainsFaviconInQuickLinks(faviconPath)&&
                    db.checkNotContainsFaviconInHomePages(faviconPath) && db.checkNotContainsFaviconInHistoryMore(faviconPath,hId))
                    {
                        File file = new File(faviconPath);
                        if(file.exists())
                        {
                            //noinspection ResultOfMethodCallIgnored
                            file.delete();
                        }
                    }
                }).start();

                db.deleteHistoryItem(hId);
            } finally {
                historyItems.remove(adapterPosition);
                notifyItemRemoved(adapterPosition);

                if(historyItem != null)
                {
                    if(db.getHistoryItemsByDateSize(historyItem.getHiDate()) == 0)
                    {
                        int id = db.getHistoryItemIdWithDate(historyItem.getHiDate());
                        db.deleteHistoryItem(id);
                        setHistory();
                    }
                }
            }
        } catch (Exception e)
        {
            setHistory();
        }
    }

    @Override
    public int getItemViewType(int position) {
        try {
            return db.getHistoryItemType(historyItems.get(position));
        } catch (Exception e)
        {
            return -1;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
       switch (viewType)
       {
           case 0:
           {
               final View view = inflater.inflate(R.layout.history_date_row,parent,false);
               return new DateViewHolder(view);
           }
           case 1:
           {
               final View view = inflater.inflate(R.layout.history_item_row,parent,false);
               return new ManageHistoryRVAdapter.HistoryViewHolder(view);
           }
           default:
           {
               final View view = inflater.inflate(R.layout.empty_row,parent,false);
               return new ManageHistoryRVAdapter.ViewHolderEmpty(view);
           }

       }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int itemId = historyItems.get(position);
        HistoryItem historyItem = db.getHistoryItem(itemId);
        switch (holder.getItemViewType())
        {
            case 0:
                if(new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(new Date(System.currentTimeMillis())).equals(historyItem.getHiDate()))
                {
                    String finalString = "Today - "+historyItem.getHiDate();
                    ((DateViewHolder)holder).hiDate.setText(finalString);
                }else if(new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(new Date(System.currentTimeMillis() - (1000 * 60 * 60 *24)))
                        .equals(historyItem.getHiDate()))
                {
                    String finalString = "Yesterday - "+historyItem.getHiDate();
                    ((DateViewHolder)holder).hiDate.setText(finalString);
                } else {
                    ((DateViewHolder)holder).hiDate.setText(historyItem.getHiDate());
                }
                break;
            case 1:
                ManageHistoryRVAdapter.HistoryViewHolder viewHolder = (ManageHistoryRVAdapter.HistoryViewHolder)holder;
                viewHolder.hFaviconIV.setBackground(null);
                viewHolder.hFaviconIV.setImageBitmap(null);
                viewHolder.hFaviconIV.setBackgroundResource(0);

                if(historyItem.getHiFaviconPath() != null && historyItem.getHiFaviconPath().equals("R.drawable"))
                {
                    viewHolder.hFaviconIV.setBackgroundResource(R.drawable.splash_logo_light);
                } else {
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.RGB_565;
                        File file = new File(historyItem.getHiFaviconPath());
                        if(file.exists() && !historyItem.getHiFaviconPath().equals(dumpPath))
                        {
                            Bitmap bitmap = BitmapFactory.decodeFile(historyItem.getHiFaviconPath(), options);
                            viewHolder.hFaviconIV.setImageBitmap(bitmap);
                        } else
                        {
                            if(historyItem.getHiTitle().equals("No title"))
                            {
                                viewHolder.hFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                            } else {
                                String firstC = historyItem.getHiTitle().substring(0,1);
                                viewHolder.hFaviconIV.setBackground(new TextDrawable(context,firstC));
                            }
                        }
                    } catch (Exception e)
                    {
                        if(historyItem.getHiTitle().equals("No title"))
                        {
                            viewHolder.hFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                        } else {
                            String firstC = historyItem.getHiTitle().substring(0,1);
                            viewHolder.hFaviconIV.setBackground(new TextDrawable(context,firstC));
                        }
                    }
                }

                viewHolder.hTitle.setText(historyItem.getHiTitle());
                viewHolder.hUrl.setText(historyItem.getHiURL());
                break;
        }
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    static class ViewHolderEmpty extends RecyclerView.ViewHolder {
        public ViewHolderEmpty(@NonNull View itemView) {
            super(itemView);
        }
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        
        private final TextView hTitle,hUrl;
        private final ImageView hFaviconIV;
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            hTitle = itemView.findViewById(R.id.hTitle);
            hUrl = itemView.findViewById(R.id.hUrl);
            hFaviconIV = itemView.findViewById(R.id.hFaviconIV);
            
            final ImageButton hDeleteIB = itemView.findViewById(R.id.hDeleteIB);
            hDeleteIB.setOnClickListener(view -> removeItem(getBindingAdapterPosition()));
        }
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        private final TextView hiDate;
        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            hiDate = itemView.findViewById(R.id.hiDate);
        }
    }
}
