package com.example.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adapters.PaymentHistoryAdapter;
import com.example.databinding.FragmentPaymentHistoryBinding;
import com.example.viewmodel.MilkViewModel;

public class PaymentHistoryFragment extends Fragment {

    private FragmentPaymentHistoryBinding binding;

    // STEP 9.6.6
    private long customerId;

    // STEP 9.7.8
    private PaymentHistoryAdapter adapter;

    private MilkViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding =
                FragmentPaymentHistoryBinding.inflate(
                        inflater,
                        container,
                        false);

        if (getArguments() != null) {

            customerId =
                    getArguments()
                            .getLong("customerId");

        }

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

        binding.rvPaymentHistory.setLayoutManager(
                new LinearLayoutManager(getContext()));

        // STEP 9.7.8
        adapter = new PaymentHistoryAdapter();

        binding.rvPaymentHistory.setAdapter(adapter);

        new Thread(() -> {

            var list =
                    viewModel.getPaymentHistory(
                            customerId);

            requireActivity().runOnUiThread(() ->
                    adapter.setData(list));

        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}