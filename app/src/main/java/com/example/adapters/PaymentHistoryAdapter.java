package com.example.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.ItemPaymentHistoryBinding;
import com.example.models.Payment;

import java.util.ArrayList;
import java.util.List;

public class PaymentHistoryAdapter
        extends RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder> {

    private List<Payment> payments = new ArrayList<>();

    public void setData(List<Payment> payments) {

        this.payments =
                payments != null
                        ? payments
                        : new ArrayList<>();

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        ItemPaymentHistoryBinding binding =
                ItemPaymentHistoryBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        holder.bind(payments.get(position));
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemPaymentHistoryBinding binding;

        ViewHolder(ItemPaymentHistoryBinding binding) {

            super(binding.getRoot());

            this.binding = binding;
        }

        void bind(Payment payment) {

            binding.txtMonth.setText(
                    payment.getMonth());

            binding.txtAmount.setText(
                    "₹" + payment.getAmount());

            binding.txtStatus.setText(
                    payment.getStatus());

            if ("Paid".equals(payment.getStatus())) {

                binding.txtStatus.setTextColor(
                        Color.GREEN);

            } else {

                binding.txtStatus.setTextColor(
                        Color.RED);

            }
        }
    }
}