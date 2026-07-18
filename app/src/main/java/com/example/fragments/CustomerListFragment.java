package com.example.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.activities.EditCustomer_Activity;
import com.example.adapters.CustomerCardAdapter;
import com.example.databinding.FragmentCustomerListBinding;
import com.example.models.Customer;
import com.example.models.DeliveryWithStaff;
import com.example.utils.DateUtils;
import com.example.viewmodel.MilkViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerListFragment extends Fragment {

    private FragmentCustomerListBinding binding;
    private MilkViewModel viewModel;
    private CustomerCardAdapter adapter;
    private final List<Customer> customerList = new ArrayList<>();

    private Map<Long, String> deliveryStatusMap = new HashMap<>();
    private Map<Long, String> staffNameMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MilkViewModel.class);

        // Setup RecyclerView
        adapter = new CustomerCardAdapter(customerList, new CustomerCardAdapter.CustomerCardListener() {
            @Override
            public void onCall(Customer customer) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customer.getMobile()));
                startActivity(intent);
            }

            @Override
            public void onNavigate(Customer customer) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + customer.getLatitude() + "," + customer.getLongitude());
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }

            @Override
            public void onDeliver(Customer customer) {
                String today = DateUtils.getTodayDateString();
                String nowTime = DateUtils.getCurrentTimeString();

                SharedPreferences prefs = requireContext().getSharedPreferences("UserSession", MODE_PRIVATE);
                long staffId = prefs.getLong("staff_id", 0);
                String staffName = prefs.getString("staff_name", "Staff");

                String currentUserType = requireContext()
                        .getSharedPreferences("UserSession", MODE_PRIVATE)
                        .getString("userType", "");

                if (!"Owner".equals(currentUserType)) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Access Denied", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                viewModel.deliverCustomer(
                        customer.getId(),
                        today,
                        nowTime,
                        staffId,
                        staffName,
                        () -> {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "✅ Delivered by " + staffName, Toast.LENGTH_SHORT).show();
                            });
                            loadCustomers();
                        }
                );
            }

            @Override
            public void onEdit(Customer customer) {
                //  Open EditCustomer_Activity
                Intent intent = new Intent(getContext(), EditCustomer_Activity.class);
                intent.putExtra("CUSTOMER_ID", customer.getId());
                startActivity(intent);
            }
        });

        binding.recyclerCustomerList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerCustomerList.setAdapter(adapter);

        // Load customers
        loadCustomers();
    }

    private void loadCustomers() {
        String today = DateUtils.getTodayDateString();

        viewModel.getRepository().getExecutor().execute(() -> {
            List<Customer> customers = viewModel.getAllCustomersSync();

            Map<Long, String> statusMap = new HashMap<>();
            Map<Long, String> staffMap = new HashMap<>();

            if (customers != null) {
                for (Customer customer : customers) {
                    DeliveryWithStaff delivery = viewModel.getDeliveryWithStaff(customer.getId(), today);
                    if (delivery != null) {
                        statusMap.put(customer.getId(), delivery.status);
                        if (delivery.staffName != null && !delivery.staffName.isEmpty()) {
                            staffMap.put(customer.getId(), delivery.staffName);
                        } else if (delivery.staffId > 0) {
                            staffMap.put(customer.getId(), "Staff ID: " + delivery.staffId);
                        } else {
                            staffMap.put(customer.getId(), "Not assigned");
                        }
                    } else {
                        statusMap.put(customer.getId(), "Pending");
                        staffMap.put(customer.getId(), "Not assigned");
                    }
                }
            }

            final Map<Long, String> finalStatusMap = statusMap;
            final Map<Long, String> finalStaffMap = staffMap;
            final List<Customer> finalCustomers = customers;

            requireActivity().runOnUiThread(() -> {
                customerList.clear();
                if (finalCustomers != null && !finalCustomers.isEmpty()) {
                    customerList.addAll(finalCustomers);
                }
                deliveryStatusMap = finalStatusMap;
                staffNameMap = finalStaffMap;
                adapter.updateData(customerList, deliveryStatusMap, staffNameMap);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}