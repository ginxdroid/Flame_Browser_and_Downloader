package com.ginxdroid.flamebrowseranddownloader.sheets;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ginxdroid.flamebrowseranddownloader.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class LicenseSheet extends BottomSheetDialogFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_view_license, container, false);
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
        try {
            final TextView licenseTV = view.findViewById(R.id.licenseTV);
            final Bundle bundle = getArguments();
            //noinspection ConstantConditions
            String licenseText = bundle.getString("licenseText");

            if(!TextUtils.isEmpty(licenseText))
            {
                licenseTV.setText(licenseText);
            } else {
                Toast.makeText(view.getContext(), R.string.oops_general_message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e)
        {
            Toast.makeText(view.getContext(), R.string.oops_general_message, Toast.LENGTH_SHORT).show();
        }
    }
}
