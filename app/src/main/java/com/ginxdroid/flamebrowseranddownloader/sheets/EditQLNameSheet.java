package com.ginxdroid.flamebrowseranddownloader.sheets;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.ginxdroid.flamebrowseranddownloader.DatabaseHandler;
import com.ginxdroid.flamebrowseranddownloader.R;
import com.ginxdroid.flamebrowseranddownloader.classes.FileNameEditText;
import com.ginxdroid.flamebrowseranddownloader.classes.HelperTextUtility;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class EditQLNameSheet extends BottomSheetDialogFragment {
    private Toast toast = null;
    private BottomSheetListener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_edit_ql_name_sheet, container, false);
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

        @SuppressWarnings("ConstantConditions")
        final int qlKeyId = bundle.getInt("qlKeyId");
        final String existingQLName = bundle.getString("existingQLName");
        final int bindingAdapterPosition = bundle.getInt("bindingAdapterPosition");

        final Context context = view.getContext();
        final FragmentActivity activity = getActivity();

        final TextView statusTV;
        final FileNameEditText fileNameET;
        final MaterialButton changeNameBtn;

        statusTV = view.findViewById(R.id.statusTextViewNewTaskPopup);
        fileNameET = view.findViewById(R.id.fileNameEditTextNewTaskPopup);
        changeNameBtn = view.findViewById(R.id.changeNameBtn);

        fileNameET.setText(existingQLName);

        fileNameET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(statusTV.getVisibility() == View.VISIBLE)
                {
                    statusTV.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        fileNameET.setOnFocusChangeListener((view1, hasFocus) -> {
            if(hasFocus)
            {
                try {
                    if(statusTV.getVisibility() == View.VISIBLE)
                    {
                        statusTV.setVisibility(View.GONE);
                    }
                } catch (Exception ignored) {}
            }
        });

        View.OnClickListener onClickListener = view12 -> {
            int id = view12.getId();
            if(id == R.id.changeNameBtn)
            {
                try {
                    final Editable editable = fileNameET.getText();
                    if(HelperTextUtility.isNotEmpty(editable))
                    {
                        String editableString = editable.toString();
                        try {
                            if(existingQLName.equals(editableString))
                            {
                                statusTV.setText(R.string.quick_link_already_has_this_name_please_change_it);
                                statusTV.setVisibility(View.VISIBLE);
                            } else {
                                final DatabaseHandler db = DatabaseHandler.getInstance(context);
                                db.updateQLName(editableString,qlKeyId);
                                listener.changeQLNameSetQLNow(bindingAdapterPosition);

                                //noinspection ConstantConditions
                                activity.runOnUiThread(() -> showToast(R.string.success,context));

                                EditQLNameSheet.this.dismiss();
                            }
                        } catch (Exception e)
                        {
                            //noinspection ConstantConditions
                            activity.runOnUiThread(() -> showToast(R.string.oops_general_message,context));
                        }
                    } else {
                        statusTV.setText(R.string.enter_quick_link_name);
                        statusTV.setVisibility(View.VISIBLE);
                    }
                }catch (Exception e)
                {
                    //noinspection ConstantConditions
                    activity.runOnUiThread(() -> showToast(R.string.oops_general_message,context));
                }

            }
        };

        fileNameET.setOnClickListener(onClickListener);
        changeNameBtn.setOnClickListener(onClickListener);

    }

    private void showToast(int res, Context context)
    {
        if(toast != null)
        {
            toast.cancel();
        }

        toast = Toast.makeText(context,res,Toast.LENGTH_SHORT);
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
        void changeQLNameSetQLNow(int bindingAdapterPosition);
    }
}
