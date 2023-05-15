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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.TextDrawable;
import com.ginxdroid.flamebrowseranddownloader.models.BookmarkItem;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;

public class ManageBookmarksRVAdapter extends RecyclerView.Adapter<ManageBookmarksRVAdapter.ViewHolder>{

    private final DatabaseHandler db;
    private final Context context;
    private final ArrayList<Integer> bookmarkItems;
    private final AppCompatActivity activity;
    private final LayoutInflater inflater;
    private final String dumpPath;

    public ManageBookmarksRVAdapter(Context context, AppCompatActivity activity) {
        this.context = context;
        this.activity = activity;
        db=DatabaseHandler.getInstance(context);
        bookmarkItems = new ArrayList<>();
        inflater = LayoutInflater.from(context);
        dumpPath = context.getFilesDir().getAbsolutePath()+ File.separator+ "favicon" + File.separator + "no_file_ABC_XYZ";
    }

    @SuppressLint("NotifyDataSetChanged")
    void setBookmarks()
    {
        if(bookmarkItems.size() > 0)
        {
            bookmarkItems.clear();
        }

        bookmarkItems.addAll(db.getAllBookmarkItemsIDs());
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    void setSearchedBookmarks(String title)
    {
        if(bookmarkItems.size() > 0)
        {
            bookmarkItems.clear();
        }

        bookmarkItems.addAll(db.getAllBookmarkIDsWithTitle(title));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ManageBookmarksRVAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.bookmark_item_row,parent,false);
        return new ManageBookmarksRVAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageBookmarksRVAdapter.ViewHolder holder, int position) {
        int itemId = bookmarkItems.get(position);
        BookmarkItem bookmarkItem = db.getBookmarkItem(itemId);
        holder.bFaviconIV.setBackground(null);
        holder.bFaviconIV.setImageBitmap(null);
        holder.bFaviconIV.setBackgroundResource(0);

        if(bookmarkItem.getBFaviconPath() != null && bookmarkItem.getBFaviconPath().equals("R.drawable"))
        {
            holder.bFaviconIV.setBackgroundResource(R.drawable.splash_logo_light);
        } else {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                File file = new File(bookmarkItem.getBFaviconPath());
                if(file.exists() && !bookmarkItem.getBFaviconPath().equals(dumpPath))
                {
                    Bitmap bitmap = BitmapFactory.decodeFile(bookmarkItem.getBFaviconPath(), options);
                    holder.bFaviconIV.setImageBitmap(bitmap);
                } else
                {
                    if(bookmarkItem.getBTitle().equals("No title"))
                    {
                        holder.bFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                    } else {
                        String firstC = bookmarkItem.getBTitle().substring(0,1);
                        holder.bFaviconIV.setBackground(new TextDrawable(context,firstC));
                    }
                }
            } catch (Exception e)
            {
                if(bookmarkItem.getBTitle().equals("No title"))
                {
                    holder.bFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                } else {
                    String firstC = bookmarkItem.getBTitle().substring(0,1);
                    holder.bFaviconIV.setBackground(new TextDrawable(context,firstC));
                }
            }
        }



        holder.bookmarkTitle.setText(bookmarkItem.getBTitle());
        holder.bookmarkUrl.setText(bookmarkItem.getBURL());
    }

    @Override
    public int getItemCount() {
        return bookmarkItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView bookmarkTitle,bookmarkUrl;
        private final ImageView bFaviconIV;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            bookmarkTitle = itemView.findViewById(R.id.bookmarkTitle);
            bookmarkUrl = itemView.findViewById(R.id.bookmarkUrl);
            bFaviconIV = itemView.findViewById(R.id.bFaviconIV);
            ImageButton bDeleteIB = itemView.findViewById(R.id.bDeleteIB);

            bDeleteIB.setOnClickListener(view -> deleteBMI(bookmarkItems.get(getBindingAdapterPosition()),(ViewGroup) itemView, getBindingAdapterPosition()));
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

            final BookmarkItem bookmarkItem = db.getBookmarkItem(id);

            qlFaviconIV.setBackground(null);
            qlFaviconIV.setImageBitmap(null);
            qlFaviconIV.setBackgroundResource(0);

            if(bookmarkItem.getBFaviconPath() != null && bookmarkItem.getBFaviconPath().equals("R.drawable"))
            {
                qlFaviconIV.setBackgroundResource(R.drawable.splash_logo_light);
            } else {
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    File file = new File(bookmarkItem.getBFaviconPath());
                    if(file.exists() && !bookmarkItem.getBFaviconPath().equals(dumpPath))
                    {
                        Bitmap bitmap = BitmapFactory.decodeFile(bookmarkItem.getBFaviconPath(), options);
                        qlFaviconIV.setImageBitmap(bitmap);
                    } else
                    {
                        if(bookmarkItem.getBTitle().equals("No title"))
                        {
                            qlFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                        } else {
                            String firstC = bookmarkItem.getBTitle().substring(0,1);
                            qlFaviconIV.setBackground(new TextDrawable(context,firstC));
                        }
                    }
                } catch (Exception e)
                {
                    if(bookmarkItem.getBTitle().equals("No title"))
                    {
                        qlFaviconIV.setBackgroundResource(R.drawable.public_earth_bg);
                    } else {
                        String firstC = bookmarkItem.getBTitle().substring(0,1);
                        qlFaviconIV.setBackground(new TextDrawable(context,firstC));
                    }
                }
            }

            qlTitle.setText(bookmarkItem.getBTitle());

            cancelBtn.setOnClickListener(view -> dialog.dismiss());

            removeBtn.setOnClickListener(view -> {
                new Thread(() -> {
                    final String faviconPath = bookmarkItem.getBFaviconPath();
                    
                    if(db.checkNotContainsFaviconInQuickLinks(faviconPath)
                            && db.checkNotContainsFaviconInHistory(faviconPath)
                            && db.checkNotContainsFaviconInHomePages(faviconPath))
                    {
                        File file = new File(faviconPath);
                        if(file.exists())
                        {
                            //noinspection ResultOfMethodCallIgnored
                            file.delete();
                        }
                    }
                }).start();
                db.deleteBookmarkItem(id);


                bookmarkItems.remove(position);
                notifyDataSetChanged();
                dialog.dismiss();

            });

            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        }
    }
}
