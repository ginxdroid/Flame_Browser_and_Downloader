package com.ginxdroid.flamebrowseranddownloader.activities;

import androidx.appcompat.app.AppCompatActivity;
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

public class ManageHomePages extends AppCompatActivity implements View.OnClickListener {

    private RelativeLayout searchHomePagesLL,homePagesLL;
    private CustomEditText searchHomePagesEditText;

    private ManageHomePagesRVAdapter homePagesRVAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_home_pages);

        RecyclerView homePagesRV = findViewById(R.id.homePagesRV);
        homePagesRVAdapter = new ManageHomePagesRVAdapter(ManageHomePages.this,ManageHomePages.this);
        homePagesRVAdapter.setHasStableIds(false);
        homePagesRV.setLayoutManager(new LinearLayoutManager(ManageHomePages.this));
        homePagesRV.setAdapter(homePagesRVAdapter);

        searchHomePagesLL = findViewById(R.id.searchHomePagesLL);
        homePagesLL = findViewById(R.id.homePagesLL);
        searchHomePagesEditText = searchHomePagesLL.findViewById(R.id.searchHomePagesEditText);

        final ImageButton backButtonSearchLL, backIBRL, searchHomePagesIB;

        searchHomePagesIB = homePagesLL.findViewById(R.id.searchHomePagesIB);
        backIBRL = homePagesLL.findViewById(R.id.backIB);
        backButtonSearchLL = searchHomePagesLL.findViewById(R.id.backButtonSearchLL);

        searchHomePagesIB.setOnClickListener(ManageHomePages.this);
        backIBRL.setOnClickListener(ManageHomePages.this);
        backButtonSearchLL.setOnClickListener(ManageHomePages.this);

        searchHomePagesEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Editable editable = searchHomePagesEditText.getText();

                if(HelperTextUtility.isNotEmpty(editable))
                {
                    homePagesRVAdapter.setSearchedHomePages(editable.toString());
                } else {
                    homePagesRVAdapter.setHomePages();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        homePagesRVAdapter.setHomePages();
    }

    @Override
    public void onBackPressed() {
        if(searchHomePagesLL.getVisibility() == View.VISIBLE)
        {
            searchHomePagesLL.findViewById(R.id.backButtonSearchLL).callOnClick();
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
            searchHomePagesLL.setVisibility(View.GONE);
            homePagesLL.setVisibility(View.VISIBLE);
            homePagesRVAdapter.setHomePages();
            Editable editable = searchHomePagesEditText.getText();
            if(editable != null)
            {
                editable.clear();
            }
        } else if(id ==R.id.searchHomePagesIB)
        {
            searchHomePagesLL.setVisibility(View.VISIBLE);
            homePagesLL.setVisibility(View.GONE);
        }

    }
}