package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.CustomEditText;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;

public class ManageBookmarks extends BaseActivity implements View.OnClickListener {
    private RelativeLayout searchBookmarksLL,bookmarksLL;
    private CustomEditText searchBookmarksEditText;

    private ManageBookmarksRVAdapter manageBookmarksRVAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_bookmarks);

        RecyclerView bookmarksRV = findViewById(R.id.bookmarksRV);
        manageBookmarksRVAdapter = new ManageBookmarksRVAdapter(ManageBookmarks.this,ManageBookmarks.this);
        manageBookmarksRVAdapter.setHasStableIds(false);
        bookmarksRV.setLayoutManager(new LinearLayoutManager(ManageBookmarks.this));
        bookmarksRV.setAdapter(manageBookmarksRVAdapter);

        searchBookmarksLL = findViewById(R.id.searchBookmarksLL);
        bookmarksLL = findViewById(R.id.bookmarksLL);
        searchBookmarksEditText = searchBookmarksLL.findViewById(R.id.searchBookmarksEditText);

        final ImageButton backButtonSearchLL, backIBRL, searchBookmarksIB;

        searchBookmarksIB = bookmarksLL.findViewById(R.id.searchBookmarksIB);
        backIBRL = bookmarksLL.findViewById(R.id.backIB);
        backButtonSearchLL = searchBookmarksLL.findViewById(R.id.backButtonSearchLL);

        searchBookmarksIB.setOnClickListener(ManageBookmarks.this);
        backIBRL.setOnClickListener(ManageBookmarks.this);
        backButtonSearchLL.setOnClickListener(ManageBookmarks.this);

        searchBookmarksEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Editable editable = searchBookmarksEditText.getText();

                if(HelperTextUtility.isNotEmpty(editable))
                {
                    manageBookmarksRVAdapter.setSearchedBookmarks(editable.toString());
                } else {
                    manageBookmarksRVAdapter.setBookmarks();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        manageBookmarksRVAdapter.setBookmarks();
    }

    @Override
    public void onBackPressed() {
        if(searchBookmarksLL.getVisibility() == View.VISIBLE)
        {
            searchBookmarksLL.findViewById(R.id.backButtonSearchLL).callOnClick();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.backIB)
        {
            finish();
        } else if(id == R.id.backButtonSearchLL)
        {
            searchBookmarksLL.setVisibility(View.GONE);
            bookmarksLL.setVisibility(View.VISIBLE);
            manageBookmarksRVAdapter.setBookmarks();
            Editable editable = searchBookmarksEditText.getText();
            if(editable != null)
            {
                editable.clear();
            }
        } else if(id ==R.id.searchBookmarksIB)
        {
            searchBookmarksLL.setVisibility(View.VISIBLE);
            bookmarksLL.setVisibility(View.GONE);
        }

    }
}