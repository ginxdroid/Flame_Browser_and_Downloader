package com.ginxdroid.flamebrowseranddownloader.sheets;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ginxdroid.flamebrowseranddownloader.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class MessageSheet extends BottomSheetDialogFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_message_dialog, container, false);
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

        final TextView messageTV = view.findViewById(R.id.messageTV);
        final Bundle bundle = getArguments();

        //noinspection ConstantConditions
        messageTV.setText(bundle.getInt("message"));

        MaterialButton closeButton;
        closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> MessageSheet.this.dismiss());

    }
}
