package com.example.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.activities.LoginActivity;
import com.example.databinding.FragmentSettingsBinding;
import com.example.viewmodel.MilkViewModel;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private MilkViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding = FragmentSettingsBinding.inflate(
                inflater,
                container,
                false);

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

        // Clear Database
        binding.btnClearCache.setOnClickListener(v -> {

            viewModel.getRepository()
                    .getExecutor()
                    .execute(() -> {

                        com.example.database.AppDatabase
                                .getInstance(requireContext())
                                .clearAllTables();

                        if (getActivity() != null) {

                            getActivity().runOnUiThread(() ->

                                    Toast.makeText(
                                            getContext(),
                                            "Local database reset successfully!",
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                        }
                    });

        });

        // Logout
        binding.btnLogout.setOnClickListener(v -> {

            requireActivity()
                    .getSharedPreferences(
                            "UserSession",
                            requireActivity().MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            Intent intent =
                    new Intent(
                            requireActivity(),
                            LoginActivity.class);

            startActivity(intent);

            requireActivity().finish();

        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}