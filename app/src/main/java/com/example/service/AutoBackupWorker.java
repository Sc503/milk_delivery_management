package com.example.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.backup.BackupManager;

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