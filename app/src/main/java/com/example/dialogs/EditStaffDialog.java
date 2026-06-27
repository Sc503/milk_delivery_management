package com.example.dialogs;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.databinding.DialogEditStaffBinding;
import com.example.models.Staff;

public class EditStaffDialog extends DialogFragment {

    public interface Listener {
        void onSave(Staff staff);
    }

    private final Staff staff;
    private final Listener listener;

    private DialogEditStaffBinding binding;

    private Uri selectedImageUri;

    private ActivityResultLauncher<PickVisualMediaRequest> imagePickerLauncher;

    public EditStaffDialog(Staff staff, Listener listener) {
        this.staff = staff;
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.PickVisualMedia(),
                        uri -> {
                            if (uri != null) {
                                selectedImageUri = uri;

                                if (binding != null) {
                                    binding.imgDocument.setImageURI(uri);
                                }
                            }
                        }
                );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        binding = DialogEditStaffBinding.inflate(
                LayoutInflater.from(requireContext())
        );

        // ---------------- SET DATA ----------------
        binding.etName.setText(staff.getName());
        binding.etMobile1.setText(staff.getMobile1());
        binding.etMobile2.setText(staff.getMobile2());
        binding.etAddress.setText(staff.getAddress());

        if (staff.getDocumentPath() != null &&
                !staff.getDocumentPath().isEmpty()) {
            binding.imgDocument.setImageURI(Uri.parse(staff.getDocumentPath()));
        }

        // 🔥 STEP 3: IMAGE CLICK LISTENER
        binding.imgDocument.setOnClickListener(v -> {
            imagePickerLauncher.launch(
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE
                            )
                            .build()
            );
        });

        return new AlertDialog.Builder(requireContext())
                .setTitle("Edit Staff")
                .setView(binding.getRoot())

                .setPositiveButton("Save", (dialog, which) -> {

                    staff.setName(binding.etName.getText().toString());
                    staff.setMobile1(binding.etMobile1.getText().toString());
                    staff.setMobile2(binding.etMobile2.getText().toString());
                    staff.setAddress(binding.etAddress.getText().toString());

                    if (selectedImageUri != null) {
                        staff.setDocumentPath(selectedImageUri.toString());
                    }

                    android.util.Log.d(
                            "STAFF_EDIT",
                            "Saved Path = " + staff.getDocumentPath()
                    );

                    listener.onSave(staff);
                })

                .setNegativeButton("Cancel", null)
                .create();
    }
}