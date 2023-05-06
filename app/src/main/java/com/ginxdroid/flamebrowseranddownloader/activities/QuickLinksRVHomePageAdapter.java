package com.ginxdroid.flamebrowseranddownloader.activities;

import static com.ginxdroid.flamebrowseranddownloader.classes.ResourceFinder.getResId;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.TextDrawable;
import com.ginxdroid.flamebrowseranddownloader.models.QuickLinkModel;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;

public class QuickLinksRVHomePageAdapter extends RecyclerView.Adapter<QuickLinksRVHomePageAdapter.ViewHolder> {
    private final ArrayList<Integer> quickLinksItems;
    private final DatabaseHandler db;
    private final Context context;
    private final NormalTabsRVAdapter normalTabsRVAdapter;
    private final NormalTabsRVAdapter.ViewHolder viewHolder;
    private final MainActivity mainActivity;
    private final LayoutInflater inflater;
    private final String dumpPath;

    public QuickLinksRVHomePageAdapter(DatabaseHandler db, Context context, NormalTabsRVAdapter normalTabsRVAdapter,
                                       NormalTabsRVAdapter.ViewHolder viewHolder, MainActivity mainActivity, LayoutInflater inflater) {
        this.db = db;
        this.context = context;
        this.normalTabsRVAdapter = normalTabsRVAdapter;
        this.viewHolder = viewHolder;
        this.mainActivity = mainActivity;
        this.inflater = inflater;

        quickLinksItems = new ArrayList<>();
        dumpPath = context.getFilesDir().getAbsolutePath() + File.separator + "favicon" + File.separator + "no_file_ABC_XYZ";
    }

    void setQuickLinks()
    {
        ArrayList<Integer> newList = db.getAllQuickLinkItemsIDs();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MyDiffUtilCallBack(newList, quickLinksItems));

        if(quickLinksItems.size() > 0)
        {
            quickLinksItems.clear();
        }

        quickLinksItems.addAll(newList);

        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public QuickLinksRVHomePageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.quick_link_item_row_hp, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuickLinksRVHomePageAdapter.ViewHolder holder, int position) {
        int itemId = quickLinksItems.get(position);
        QuickLinkModel quickLinkModel = db.getQuickLinkModel(itemId);


        holder.qlFaviconIV.setBackground(null);
        holder.qlFaviconIV.setImageBitmap(null);

        if(quickLinkModel.getQlFaviconPath() != null && quickLinkModel.getQlFaviconPath().equals("R.drawable"))
        {
            //that means that it is our default quick link
            try {
                int resId = getResId(context,quickLinkModel.getQlTitle().toLowerCase(),"drawable",context.getPackageName());

                if(resId != 0)
                {
                    holder.qlFaviconIV.setBackgroundResource(resId);
                }else {
                    throw new Exception();
                }
            } catch (Exception e)
            {
                if(quickLinkModel.getQlTitle().equals("No title"))
                {
                    holder.qlFaviconIV.setBackgroundResource(R.drawable.cyclone_bg);
                } else {
                    String firstC = quickLinkModel.getQlTitle().substring(0,1);
                    holder.qlFaviconIV.setBackground(new TextDrawable(context, firstC));
                }
            }
        }


        holder.qlTitle.setText(quickLinkModel.getQlTitle());
    }

    @Override
    public int getItemCount() {
        return quickLinksItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView qlTitle;
        private final ImageView qlFaviconIV;

        private final MaterialCardView qlCV;



        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            qlCV = itemView.findViewById(R.id.qlCV);
            qlTitle = itemView.findViewById(R.id.qlTitle);
            qlFaviconIV = qlCV.findViewById(R.id.qlFaviconIV);

            View.OnClickListener onClickListener = view -> {
                try{
                    int itemId = quickLinksItems.get(getBindingAdapterPosition());
                    QuickLinkModel quickLinkModel = db.getQuickLinkModel(itemId);

                    viewHolder.webView.evaluateJavascript("javascript:document.open();document.close();",null);
                    viewHolder.homePageCL.setVisibility(View.INVISIBLE);
                    viewHolder.isHPCVisible = false;
                    viewHolder.webViewContainer.setVisibility(View.VISIBLE);

                    viewHolder.setClearHistory();
                    viewHolder.webView.loadUrl(quickLinkModel.getQlURL());

                    if(!viewHolder.isProgressBarVisible)
                    {
                        viewHolder.makeProgressBarVisible();
                    }

                    normalTabsRVAdapter.setDecorations(quickLinkModel.getQlURL(),viewHolder);
                }catch (Exception ignored){}
            };

            qlTitle.setOnClickListener(onClickListener);
            qlCV.setOnClickListener(onClickListener);

        }
    }
}
