package in.esmartsolution.milkflow.adapters;



import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import in.esmartsolution.milkflow.R;
import in.esmartsolution.milkflow.models.BackupFile;

import java.io.File;
import java.util.List;

public class BackupFileAdapter extends RecyclerView.Adapter<BackupFileAdapter.ViewHolder> {

    private final Context context;
    private final List<BackupFile> files;
    private final OnFileClickListener listener;

    public interface OnFileClickListener {
        void onFileClick(File file);
    }

    public BackupFileAdapter(Context context, List<BackupFile> files, OnFileClickListener listener) {
        this.context = context;
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_backup_file, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (files == null || position >= files.size()) {
            return;
        }

        BackupFile backupFile = files.get(position);
        if (backupFile == null) {
            return;
        }

        File file = backupFile.getFile();
        if (file == null) {
            return;
        }

        // ✅ Null-safe text setting
        if (holder.tvFileName != null) {
            holder.tvFileName.setText(backupFile.getName() != null ? backupFile.getName() : "Unknown");
        }

        if (holder.tvFileSize != null) {
            holder.tvFileSize.setText(backupFile.getFormattedSize() != null ? backupFile.getFormattedSize() : "0 B");
        }

        if (holder.tvFileDate != null) {
            holder.tvFileDate.setText(backupFile.getFormattedDate() != null ? backupFile.getFormattedDate() : "Unknown");
        }

        if (holder.tvFileType != null) {
            holder.tvFileType.setText(backupFile.getExtension() != null ? backupFile.getExtension() : "FILE");
        }

        if (holder.ivFileIcon != null) {
            if (backupFile.getType() == BackupFile.Type.MY_BACKUP) {
                holder.ivFileIcon.setImageResource(R.drawable.ic_file);
            } else {
                holder.ivFileIcon.setImageResource(R.drawable.ic_check_circle);
            }
        }

        if (holder.itemView != null) {
            if (backupFile.getType() == BackupFile.Type.MY_BACKUP) {
                holder.itemView.setBackgroundResource(R.drawable.bg_backup_my);
            } else {
                holder.itemView.setBackgroundResource(R.drawable.bg_backup_received);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClick(file);
            }
        });
    }


    @Override
    public int getItemCount() {
        return files != null ? files.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvFileSize, tvFileDate, tvFileType;
        ImageView ivFileIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tv_backup_file_name);
            tvFileSize = itemView.findViewById(R.id.tv_backup_file_size);
            tvFileDate = itemView.findViewById(R.id.tv_backup_file_date);
            tvFileType = itemView.findViewById(R.id.tv_backup_file_type);
            ivFileIcon = itemView.findViewById(R.id.iv_backup_file_icon);
        }
    }
}