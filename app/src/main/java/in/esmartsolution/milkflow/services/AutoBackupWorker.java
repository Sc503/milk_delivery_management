package in.esmartsolution.milkflow.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import in.esmartsolution.milkflow.backup.BackupManager;

public class AutoBackupWorker extends Worker {

    public AutoBackupWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {

        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        boolean success =
                BackupManager.createBackup(getApplicationContext());

        if (success) {
            return Result.success();
        }

        return Result.retry();
    }
}