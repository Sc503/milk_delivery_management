package com.example.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.bumptech.glide.Glide;
import com.example.databinding.DialogImagePreviewBinding;

public class ImagePreviewDialog extends AppCompatDialogFragment {

    private DialogImagePreviewBinding binding;
    private String imageUri;

    public ImagePreviewDialog(String imageUri){
        this.imageUri=imageUri;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        binding=DialogImagePreviewBinding.inflate(getLayoutInflater());

        Dialog dialog=new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(binding.getRoot());

        Glide.with(requireContext())
                .load(imageUri)
                .into(binding.previewImage);

        binding.btnClose.setOnClickListener(v->dismiss());

        return dialog;
    }
}