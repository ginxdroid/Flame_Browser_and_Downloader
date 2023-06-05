package com.ginxdroid.flamebrowseranddownloader.sheets;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class OptimizationsSheet extends BottomSheetDialogFragment {
    private CheckBox doNotShowCB;
    private BottomSheetListener listener;
    private Toast toast = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_optimizations, container, false);
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

        final MaterialButton navigateButton,closeBtn;
        navigateButton = view.findViewById(R.id.navigateButton);
        closeBtn = view.findViewById(R.id.closeBtn);
        doNotShowCB = view.findViewById(R.id.doNotShowCB);

        navigateButton.setOnClickListener(view1 -> {
            try {
                if(doNotShowCB.isChecked())
                {
                    try {
                        DatabaseHandler.getInstance(getContext()).updateShowOptimizationStatus();
                    } catch (Exception e){
                        showToast(R.string.oops_general_message,getContext());
                    }

                }

                OptimizationsSheet.this.dismiss();
                //noinspection ConstantConditions
                getActivity().startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (ActivityNotFoundException e1)
            {
                showToast(R.string.battery_optimization_activity_not_found,getContext());
            } catch (Exception e2)
            {
                showToast(R.string.oops_general_message,getContext());
            }
        });

        closeBtn.setOnClickListener(view12 -> {
            if(doNotShowCB.isChecked())
            {
                try {
                    DatabaseHandler.getInstance(getContext()).updateShowOptimizationStatus();
                } catch (Exception e){
                    showToast(R.string.oops_general_message,getContext());
                }

            }
            OptimizationsSheet.this.dismiss();
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

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if(doNotShowCB.isChecked())
        {
            try {
                DatabaseHandler.getInstance(getContext()).updateShowOptimizationStatus();
            } catch (Exception e){
                showToast(R.string.oops_general_message,getContext());
            }
        }
        listener.optimizationSheetDismissed();
        super.onDismiss(dialog);
    }

    public interface BottomSheetListener {
        void optimizationSheetDismissed();
    }

    private void showToast(int resID,Context context)
    {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, resID, Toast.LENGTH_SHORT);
        toast.show();
    }
}
