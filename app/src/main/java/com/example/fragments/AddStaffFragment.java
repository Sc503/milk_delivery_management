package com.example.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.databinding.FragmentAddStaffBinding;
import com.example.models.Staff;
import com.example.viewmodel.MilkViewModel;

import android.net.Uri;
import android.widget.ArrayAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

public class AddStaffFragment extends Fragment {

    private FragmentAddStaffBinding binding;
    private MilkViewModel viewModel;

    private String documentPath = "";
    private String documentType = "";

    private Uri selectedImageUri;

    private final ActivityResultLauncher<PickVisualMediaRequest> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    uri -> {

                        if (uri != null) {

                            String internalPath = saveImageToInternalStorage(uri);
                            if (internalPath != null) {
                                documentPath = Uri.fromFile(new File(internalPath)).toString();
                                binding.imgDocument.setImageURI(Uri.fromFile(new File(internalPath)));
                                selectedImageUri = Uri.fromFile(new File(internalPath));
                            } else {
                                Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

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

    public AddStaffFragment() {
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentAddStaffBinding.inflate(inflater, container, false);

        viewModel =
                new ViewModelProvider(requireActivity())
                        .get(MilkViewModel.class);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        binding.btnSaveStaff.setOnClickListener(v -> saveStaff());

        String[] docs = {
                "Aadhar Card",
                "PAN Card"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        docs
                );

        binding.spDocumentType.setAdapter(adapter);

        binding.btnUploadDocument.setOnClickListener(v -> {

            imagePickerLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());

        });

        // Document upload code STEP 8 मध्ये येईल
    }

    private void saveStaff() {

        String name =
                binding.etStaffName.getText().toString().trim();

        String mobile1 =
                binding.etMobile1.getText().toString().trim();

        String mobile2 =
                binding.etMobile2.getText().toString().trim();

        String address =
                binding.etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {

            binding.etStaffName.setError("Enter Name");
            return;
        }

        if (TextUtils.isEmpty(mobile1)) {

            binding.etMobile1.setError("Enter Mobile Number");
            return;
        }

        documentType =
                binding.spDocumentType
                        .getSelectedItem()
                        .toString();

        Staff staff = new Staff(
                name,
                mobile1,
                mobile2,
                address,
                documentPath,
                documentType
        );

        // STEP 7 नंतर repository मध्ये add करू
        viewModel.insertStaff(staff);
        Toast.makeText(
                requireContext(),
                "Staff Saved Successfully",
                Toast.LENGTH_SHORT
        ).show();

        clearFields();
    }

    private void clearFields() {

        binding.etStaffName.setText("");
        binding.etMobile1.setText("");
        binding.etMobile2.setText("");
        binding.etAddress.setText("");

        // Spinner reset
        binding.spDocumentType.setSelection(0);

        // Image reset
        binding.imgDocument.setImageDrawable(null);

        // URI reset
        selectedImageUri = null;

        // Document path reset
        documentPath = "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}