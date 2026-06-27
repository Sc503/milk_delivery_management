package com.example.dialogs;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.databinding.FragmentAddStaffBinding;
import com.example.models.Staff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AddStaffDialog extends DialogFragment {

    public interface Listener {
        void onSave(Staff staff);
    }

    private final Listener listener;
    private FragmentAddStaffBinding binding;
    private String documentPath = "";
    private String documentType = "";

    private ActivityResultLauncher<PickVisualMediaRequest> imagePickerLauncher;

    public AddStaffDialog(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        String internalPath = saveImageToInternalStorage(uri);
                        if (internalPath != null) {
                            documentPath = Uri.fromFile(new File(internalPath)).toString();
                            if (binding != null) {
                                binding.imgDocument.setImageURI(Uri.fromFile(new File(internalPath)));
                            }
                        }
                    }
                }
        );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = FragmentAddStaffBinding.inflate(LayoutInflater.from(requireContext()));

        // Setup Document Type Spinner
        String[] docs = {"Aadhar Card", "PAN Card"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, docs);
        binding.spDocumentType.setAdapter(adapter);

        // Upload Button
        binding.btnUploadDocument.setOnClickListener(v -> {
            imagePickerLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // Hide the original save button as we use Dialog buttons
        binding.btnSaveStaff.setVisibility(View.GONE);

        return new AlertDialog.Builder(requireContext())
                .setTitle("Add New Staff")
                .setView(binding.getRoot())
                .setPositiveButton("Save", (dialog, which) -> {
                    saveStaff();
                })
                .setNegativeButton("Cancel", null)
                .create();
    }

    private void saveStaff() {
        String name = binding.etStaffName.getText().toString().trim();
        String mobile1 = binding.etMobile1.getText().toString().trim();
        String mobile2 = binding.etMobile2.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        documentType = binding.spDocumentType.getSelectedItem().toString();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(mobile1)) {
            Toast.makeText(requireContext(), "Mobile number is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Staff staff = new Staff(name, mobile1, mobile2, address, documentPath, documentType);
        listener.onSave(staff);
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File file = new File(requireContext().getFilesDir(), "staff_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
