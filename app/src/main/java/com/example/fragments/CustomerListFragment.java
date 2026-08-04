package com.example.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import androidx.appcompat.app.AlertDialog;
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


                if (!"Owner".equals(currentUserType) && !"Staff".equals(currentUserType)) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Access Denied", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String currentStatus = deliveryStatusMap != null && deliveryStatusMap.containsKey(customer.getId())
                        ? deliveryStatusMap.get(customer.getId()) : "Pending";

                if ("Pending".equalsIgnoreCase(currentStatus)) {
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
                } else if ("Delivered".equalsIgnoreCase(currentStatus)) {
                    showUndeliverDialog(customer);
                }
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


    @Override
    public void onResume() {
        super.onResume();
        loadCustomers();
        Log.d("CustomerListFragment", "🔄 Refreshing customer list");
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

    private void showUndeliverDialog(Customer customer) {
        new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Undeliver Confirmation")
                .setMessage("Are you sure you want to mark this delivery as UNDELIVERED?\n\n" +
                        "Customer: " + customer.getName() + "\n" +
                        "Mobile: " + customer.getMobile())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("✅ Yes, Undeliver", (dialog, which) -> {
                    // Undeliver logic
                    String today = DateUtils.getTodayDateString();

                    viewModel.markDeliveryPending(
                            customer.getId(),
                            today,
                            () -> {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "⏳ Marked as Undelivered", Toast.LENGTH_SHORT).show();
                                });
                                loadCustomers();
                            }
                    );
                })
                .setNegativeButton("❌ No, Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Action cancelled", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("📞 Call Customer", (dialog, which) -> {
                    // Call customer directly from dialog
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customer.getMobile()));
                    startActivity(intent);
                })
                .show();
    }
}