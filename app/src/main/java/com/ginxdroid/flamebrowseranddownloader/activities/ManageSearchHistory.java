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

public class ManageSearchHistory extends BaseActivity implements View.OnClickListener {
    private RelativeLayout searchSearchHistoryLL,searchHistoryLL;
    private CustomEditText searchSearchHistoryEditText;

    private ManageSHAdapter manageSHAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_search_history);

        RecyclerView searchHistoryRV = findViewById(R.id.searchHistoryRV);
        manageSHAdapter = new ManageSHAdapter(ManageSearchHistory.this);
        manageSHAdapter.setHasStableIds(false);
        searchHistoryRV.setLayoutManager(new LinearLayoutManager(ManageSearchHistory.this));
        searchHistoryRV.setAdapter(manageSHAdapter);

        searchSearchHistoryLL = findViewById(R.id.searchSearchHistoryLL);
        searchHistoryLL = findViewById(R.id.searchHistoryLL);
        searchSearchHistoryEditText = searchSearchHistoryLL.findViewById(R.id.searchSearchHistoryEditText);

        final ImageButton backButtonSearchLL, backIBRL, searchSearchHistoryIB;

        searchSearchHistoryIB = searchHistoryLL.findViewById(R.id.searchSearchHistoryIB);
        backIBRL = searchHistoryLL.findViewById(R.id.backIB);
        backButtonSearchLL = searchSearchHistoryLL.findViewById(R.id.backButtonSearchLL);

        searchSearchHistoryIB.setOnClickListener(ManageSearchHistory.this);
        backIBRL.setOnClickListener(ManageSearchHistory.this);
        backButtonSearchLL.setOnClickListener(ManageSearchHistory.this);

        searchSearchHistoryEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Editable editable = searchSearchHistoryEditText.getText();

                if(HelperTextUtility.isNotEmpty(editable))
                {
                    manageSHAdapter.setSearchedSearchHistory(editable.toString());
                } else {
                    manageSHAdapter.setSearchHistory();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        manageSHAdapter.setSearchHistory();
    }

    @Override
    public void onBackPressed() {
        if(searchSearchHistoryLL.getVisibility() == View.VISIBLE)
        {
            searchSearchHistoryLL.findViewById(R.id.backButtonSearchLL).callOnClick();
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
            searchSearchHistoryLL.setVisibility(View.GONE);
            searchHistoryLL.setVisibility(View.VISIBLE);
            manageSHAdapter.setSearchHistory();
            Editable editable = searchSearchHistoryEditText.getText();
            if(editable != null)
            {
                editable.clear();
            }
        } else if(id ==R.id.searchSearchHistoryIB)
        {
            searchSearchHistoryLL.setVisibility(View.VISIBLE);
            searchHistoryLL.setVisibility(View.GONE);
        }

    }
}