package com.ginxdroid.flamebrowseranddownloader.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ginxdroid.flamebrowseranddownloader.R;

public class DownloadingFragment extends Fragment {
    private DFListener listener;


    public interface DFListener
    {
        void showDFFragment(View view);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (DFListener) context;
        } catch (ClassCastException e)
        {
            throw new ClassCastException(context + " must implement DFListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloader,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listener.showDFFragment(view);
    }


}
