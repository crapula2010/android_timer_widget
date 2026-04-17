package com.timers.widget;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DriveBackupWorker extends Worker {

    public DriveBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String accountEmail = DriveBackupManager.getAccountEmail(getApplicationContext());
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            return Result.failure();
        }

        try {
            DriveUploader.uploadBackupJson(getApplicationContext(), accountEmail);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
