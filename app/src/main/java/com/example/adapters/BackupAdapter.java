package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.models.BackupFile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.ViewHolder> {

    private List<BackupFile> fileList;
    private OnBackupClickListener listener;

    public interface OnBackupClickListener {
        void onClick(BackupFile file);
        void onLongClick(BackupFile file);
    }

    public BackupAdapter(List<BackupFile> fileList, OnBackupClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_backup_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BackupFile backupFile = fileList.get(position);
        java.io.File file = backupFile.getFile();

        // File name
        holder.tvFileName.setText(file.getName());

        // File size
        String size = getFileSize(file.length());
        holder.tvFileSize.setText(size);

        // File date
        String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(new Date(file.lastModified()));
        holder.tvFileDate.setText(date);

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(backupFile);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onLongClick(backupFile);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return fileList != null ? fileList.size() : 0;
    }

    private String getFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return (size / 1024) + " KB";
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvFileSize, tvFileDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvFileDate = itemView.findViewById(R.id.tvFileDate);
        }
    }
}