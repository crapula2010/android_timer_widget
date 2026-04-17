package com.timers.widget;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class DriveBackupManager {
    private static final String PREFS_NAME = "DriveBackupPrefs";
    private static final String KEY_ACCOUNT_EMAIL = "account_email";
    private static final String DAILY_WORK_NAME = "daily_drive_backup";

    private DriveBackupManager() {
    }

    public static void saveAccountEmail(@NonNull Context context, @NonNull String accountEmail) {
        prefs(context).edit().putString(KEY_ACCOUNT_EMAIL, accountEmail).apply();
    }

    public static String getAccountEmail(@NonNull Context context) {
        return prefs(context).getString(KEY_ACCOUNT_EMAIL, null);
    }

    public static void clearAccount(@NonNull Context context) {
        prefs(context).edit().remove(KEY_ACCOUNT_EMAIL).apply();
    }

    public static void enqueueDailyBackup(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest dailyRequest = new PeriodicWorkRequest.Builder(
                DriveBackupWorker.class,
                24,
                TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyRequest);
    }

    public static void enqueueBackupNow(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(DriveBackupWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                "manual_drive_backup",
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest);
    }

    public static void disableBackups(@NonNull Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_WORK_NAME);
        WorkManager.getInstance(context).cancelUniqueWork("manual_drive_backup");
        clearAccount(context);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
