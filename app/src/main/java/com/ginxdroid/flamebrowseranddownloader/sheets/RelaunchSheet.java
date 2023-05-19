package com.ginxdroid.flamebrowseranddownloader.sheets;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ginxdroid.flamebrowseranddownloader.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class RelaunchSheet extends BottomSheetDialogFragment {

    private BottomSheetListener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_relaunch, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;

            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if(bottomSheet != null)
            {
                BottomSheetBehavior.from(bottomSheet).setPeekHeight(bottomSheet.getHeight());
            }
        });

        return dialog;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final MaterialButton cancelBtn,restartBtn;
        cancelBtn = view.findViewById(R.id.cancelBtn);
        restartBtn = view.findViewById(R.id.restartBtn);

        cancelBtn.setOnClickListener(view12 -> RelaunchSheet.this.dismiss());

        restartBtn.setOnClickListener(view1 -> {
            RelaunchSheet.this.dismiss();
            listener.relaunchNow();
        });

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (BottomSheetListener) context;
        }catch (ClassCastException e)
        {
            throw new ClassCastException(context + " must implement BottomSheetListener");
        }
    }


    public interface BottomSheetListener {
        void relaunchNow();
    }
}