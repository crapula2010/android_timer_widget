package com.timers.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ResetConfirmActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int appWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        String timerId = TimerData.getTimerIdForWidget(this, appWidgetId);
        if (timerId == null) {
            finish();
            return;
        }

        String timerName = TimerData.getTimerName(this, timerId);

        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_confirm_title)
                .setMessage(getString(R.string.reset_confirm_message, timerName))
                .setNegativeButton(R.string.reset_confirm_negative, (dialog, which) -> finish())
                .setPositiveButton(R.string.reset_confirm_positive, (dialog, which) -> {
                    TimerData.resetTimer(this, timerId);
                    TimerWidgetProvider.updateWidgetUI(this, appWidgetId);
                    finish();
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }
}
