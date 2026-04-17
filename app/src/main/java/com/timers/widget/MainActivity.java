package com.timers.widget;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;

public class MainActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9101;

    private GoogleSignInClient googleSignInClient;
    private TextView backupStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        backupStatus = findViewById(R.id.backup_status);
        Button connectBackupButton = findViewById(R.id.btn_connect_backup);
        Button backupNowButton = findViewById(R.id.btn_backup_now);
        Button restoreLatestButton = findViewById(R.id.btn_restore_latest);
        Button disableBackupButton = findViewById(R.id.btn_disable_backup);

        connectBackupButton.setOnClickListener(v -> connectAndEnableBackup());
        backupNowButton.setOnClickListener(v -> backupNow());
        restoreLatestButton.setOnClickListener(v -> restoreLatestBackup());
        disableBackupButton.setOnClickListener(v -> disableBackup());

        refreshBackupStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_SIGN_IN) {
            return;
        }

        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException.class);
            if (account == null || account.getEmail() == null) {
                Toast.makeText(this, "Google account email was not available", Toast.LENGTH_LONG).show();
                return;
            }

            DriveBackupManager.saveAccountEmail(this, account.getEmail());
            DriveBackupManager.enqueueDailyBackup(this);
            Toast.makeText(this, "Daily Drive backup enabled", Toast.LENGTH_LONG).show();
            refreshBackupStatus();
        } catch (ApiException e) {
            if (e.getStatusCode() == 10) {
                Toast.makeText(this, R.string.signin_failed_10, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Sign-in failed: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void connectAndEnableBackup() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        Scope driveScope = new Scope(DriveScopes.DRIVE_FILE);

        if (account != null && GoogleSignIn.hasPermissions(account, driveScope) && account.getEmail() != null) {
            DriveBackupManager.saveAccountEmail(this, account.getEmail());
            DriveBackupManager.enqueueDailyBackup(this);
            Toast.makeText(this, "Daily Drive backup enabled", Toast.LENGTH_LONG).show();
            refreshBackupStatus();
            return;
        }

        startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    private void backupNow() {
        String accountEmail = DriveBackupManager.getAccountEmail(this);
        if (accountEmail == null || accountEmail.isEmpty()) {
            Toast.makeText(this, "Connect Google Drive first", Toast.LENGTH_LONG).show();
            return;
        }

        DriveBackupManager.enqueueBackupNow(this);
        Toast.makeText(this, R.string.backup_queued, Toast.LENGTH_LONG).show();
    }

    private void disableBackup() {
        DriveBackupManager.disableBackups(this);
        googleSignInClient.signOut();
        Toast.makeText(this, R.string.backup_disabled, Toast.LENGTH_LONG).show();
        refreshBackupStatus();
    }

    private void restoreLatestBackup() {
        String accountEmail = DriveBackupManager.getAccountEmail(this);
        if (accountEmail == null || accountEmail.isEmpty()) {
            Toast.makeText(this, "Connect Google Drive first", Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            try {
                String json = DriveUploader.downloadLatestBackupJson(this, accountEmail);
                if (json == null || json.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.restore_none, Toast.LENGTH_LONG).show());
                    return;
                }

                TimerData.importBackupJson(this, json);
                TimerWidgetProvider.refreshAllWidgets(this);
                runOnUiThread(() -> Toast.makeText(this, R.string.restore_done, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                String message = e.getMessage() == null ? "unknown error" : e.getMessage();
                runOnUiThread(() -> Toast.makeText(
                        this,
                        getString(R.string.restore_failed, message),
                        Toast.LENGTH_LONG
                ).show());
            }
        }).start();
    }

    private void refreshBackupStatus() {
        String accountEmail = DriveBackupManager.getAccountEmail(this);
        if (accountEmail == null || accountEmail.isEmpty()) {
            backupStatus.setText(R.string.backup_status_not_connected);
        } else {
            backupStatus.setText(getString(R.string.backup_status_connected, accountEmail));
        }
    }
}
