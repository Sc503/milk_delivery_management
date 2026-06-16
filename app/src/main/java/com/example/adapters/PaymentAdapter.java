package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.models.Customer;
import com.example.models.Payment;

import java.util.ArrayList;
import java.util.List;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Customer customer);
    }

    private final OnItemClickListener listener;

    // Displayed list
    private List<Customer> customers = new ArrayList<>();

    // Original list for search/filter
    private List<Customer> allCustomers = new ArrayList<>();

    // STEP 9.5.3
    private List<Customer> filteredCustomers = new ArrayList<>();

    public PaymentAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    // STEP 9.4.5
    public void setData(List<Customer> customers) {

        this.allCustomers = customers != null
                ? customers
                : new ArrayList<>();

        this.customers = new ArrayList<>(this.allCustomers);

        notifyDataSetChanged();
    }

    // STEP 9.5.4
    public void setFilteredData(List<Customer> customers) {

        this.customers = customers != null
                ? customers
                : new ArrayList<>();

        notifyDataSetChanged();
    }

    // STEP 9.4.6
    public void filter(String query) {

        customers.clear();

        if (query == null || query.trim().isEmpty()) {

            customers.addAll(allCustomers);

        } else {

            query = query.toLowerCase();

            for (Customer customer : allCustomers) {

                if (customer.getName() != null
                        && customer.getName()
                        .toLowerCase()
                        .contains(query)) {

                    customers.add(customer);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);

        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull PaymentViewHolder holder,
            int position) {

        Customer customer = customers.get(position);

        holder.bind(customer);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(customer);
            }
        });
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {

        private final TextView text1;
        private final TextView text2;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);

            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }

        public void bind(Customer customer) {

            text1.setText(customer.getName());

            text2.setText(
                    "Rate: "
                            + customer.getMilkRate()
                            + " | Qty: "
                            + customer.getMilkQuantity()
            );
        }
    }
}