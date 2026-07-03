package com.example.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;

import java.util.List;

public class CalendarGridAdapter extends RecyclerView.Adapter<CalendarGridAdapter.DayViewHolder> {

    private final List<CalendarDay> daysList;
    private final OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public static class CalendarDay {
        public String dateString;
        public int dayNumber;
        public String status;
        public boolean isToday;

        public CalendarDay(String dateString, int dayNumber, String status, boolean isToday) {
            this.dateString = dateString;
            this.dayNumber = dayNumber;
            this.status = status;
            this.isToday = isToday;
        }
    }

    public CalendarGridAdapter(List<CalendarDay> daysList, OnDayClickListener listener) {
        this.daysList = daysList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDay day = daysList.get(position);
        holder.bind(day, listener);
    }

    @Override
    public int getItemCount() {
        return daysList.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtDayNumber;
        private final TextView txtDayStatus;
        private final LinearLayout root;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDayNumber = itemView.findViewById(R.id.txt_day_number);
            txtDayStatus = itemView.findViewById(R.id.txt_day_status);
            root = itemView.findViewById(R.id.day_cell_root);
        }

        public void bind(final CalendarDay day, final OnDayClickListener listener) {
            if (day.dayNumber == 0) {
                // Empty padding cell
                txtDayNumber.setText("");
                txtDayStatus.setText("");
                root.setClickable(false);
                root.setBackgroundColor(Color.TRANSPARENT);
                txtDayNumber.setBackgroundResource(0);
            } else {
                txtDayNumber.setText(String.valueOf(day.dayNumber));
                root.setClickable(true);

                // Today highlighting
                if (day.isToday) {
                    txtDayNumber.setTextColor(Color.WHITE);
                    txtDayNumber.setBackgroundResource(R.drawable.circle_today_solid);
                } else {
                    txtDayNumber.setTextColor(Color.BLACK);
                    txtDayNumber.setBackgroundResource(0);
                }

                // Status mapping
                if ("Delivered".equalsIgnoreCase(day.status)) {
                    txtDayStatus.setText("✓");
                    txtDayStatus.setTextColor(root.getContext().getResources().getColor(R.color.delivered_green));
                } else if ("Pending".equalsIgnoreCase(day.status)) {
                    txtDayStatus.setText("✗");
                    txtDayStatus.setTextColor(root.getContext().getResources().getColor(R.color.pending_red));
                } else {
                    txtDayStatus.setText("");
                }

                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDayClick(day);
                    }
                });
            }
        }
    }
}