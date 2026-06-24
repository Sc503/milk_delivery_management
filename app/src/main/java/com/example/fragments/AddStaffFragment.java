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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class AddStaffFragment extends Fragment {

    private FragmentAddStaffBinding binding;
    private MilkViewModel viewModel;

    private String documentPath = "";
    private String documentType = "";

    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {

                        if (uri != null) {

                            selectedImageUri = uri;

                            binding.imgDocument.setImageURI(uri);

                            documentPath = uri.toString();
                        }
                    });

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

            imagePickerLauncher.launch("image/*");

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}