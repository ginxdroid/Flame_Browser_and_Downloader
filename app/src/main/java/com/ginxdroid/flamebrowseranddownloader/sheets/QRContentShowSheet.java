package com.ginxdroid.flamebrowseranddownloader.sheets;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class QRContentShowSheet extends BottomSheetDialogFragment {
    private Toast toast = null;
    private BottomSheetListener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_qr_content, container, false);
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

        final Bundle bundle = getArguments();
        @SuppressWarnings("ConstantConditions") final String content = bundle.getString("content");

        final TextView contentTV = view.findViewById(R.id.contentTV);

        boolean contentNotEmpty = false;
        if(HelperTextUtility.isNotEmpty(content))
        {
            contentTV.setText(content);
            contentNotEmpty = true;
        }

        final MaterialButton shareBtn,copyBtn,loadBtn;
        shareBtn = view.findViewById(R.id.shareBtn);
        copyBtn = view.findViewById(R.id.copyBtn);
        loadBtn = view.findViewById(R.id.loadBtn);

        boolean finalContentNotEmpty = contentNotEmpty;
        View.OnClickListener onClickListener = view1 -> {
            final int id = view1.getId();
            if(id == R.id.shareBtn)
            {
                try {
                    if(finalContentNotEmpty)
                    {
                        QRContentShowSheet.this.dismiss();

                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setTypeAndNormalize("text/plain");
                        share.putExtra(Intent.EXTRA_TEXT,content);

                        //noinspection ConstantConditions
                        getActivity().startActivity(Intent.createChooser(share,getContext().getString(R.string.share_via)));



                    } else {
                        showToast(getContext(),R.string.cannot_share_empty_content);
                    }
                } catch (Exception e)
                {
                    showToast(getContext(),R.string.oops_general_message);
                }
            } else if(id == R.id.copyBtn)
            {
                try {
                    if(finalContentNotEmpty)
                    {
                        ClipboardManager clipboardManager = (ClipboardManager) view1.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText("Content",content);
                        if(clipboardManager != null)
                        {
                            clipboardManager.setPrimaryClip(clipData);
                            showToast(getContext(),R.string.copied_to_clipboard);
                        } else {
                            showToast(getContext(),R.string.oops_general_message);
                        }
                    }
                } catch (Exception e) {
                    showToast(getContext(),R.string.oops_general_message);
                }
            } else if(id == R.id.loadBtn)
            {
                try {
                    if(finalContentNotEmpty)
                    {
                        QRContentShowSheet.this.dismiss();
                        listener.loadQRData(content);
                    } else {
                        showToast(getContext(),R.string.cannot_load_empty_content);
                    }
                } catch (Exception e) {
                    showToast(getContext(),R.string.oops_general_message);
                }
            }
        };

        shareBtn.setOnClickListener(onClickListener);
        copyBtn.setOnClickListener(onClickListener);
        loadBtn.setOnClickListener(onClickListener);
    }

    private void showToast(Context context, int resId)
    {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        toast.show();
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
        void loadQRData(String content);
    }
}