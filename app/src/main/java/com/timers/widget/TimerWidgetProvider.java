package com.timers.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * The main AppWidgetProvider for the Timer Widget
 */
public class TimerWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidgetUI(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        if ("com.timers.widget.ACTION_TIMER_START".equals(action)) {
            handleStartStop(context, appWidgetId);
        } else if ("com.timers.widget.ACTION_TIMER_RESET".equals(action)) {
            handleReset(context, appWidgetId);
        } else if ("com.timers.widget.ACTION_TIMER_EDIT".equals(action)) {
            handleEdit(context, appWidgetId);
        } else if ("com.timers.widget.ACTION_TIMER_TICK".equals(action)) {
            updateWidgetUI(context, appWidgetId);
        }

        super.onReceive(context, intent);
    }

    /**
     * Update the widget UI with current timer state
     */
    public static void updateWidgetUI(Context context, int appWidgetId) {
        RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.timer_widget_layout);

        // Get timer ID for this widget
        String timerId = TimerData.getTimerIdForWidget(context, appWidgetId);
        if (timerId == null) {
            return; // Widget not configured yet
        }

        // Get current timer data
        String timerName = TimerData.getTimerName(context, timerId);
        long elapsedMillis = TimerData.getElapsedMillis(context, timerId);
        boolean isRunning = TimerData.isTimerRunning(context, timerId);

        // Set timer name
        view.setTextViewText(R.id.timer_name, timerName);

        // Set timer display (accumulated usage)
        String timeDisplay = TimerData.formatTime(elapsedMillis);
        view.setTextViewText(R.id.timer_display, timeDisplay);

        // Set button text
        view.setTextViewText(R.id.btn_start_stop, isRunning ? "Stop" : "Start");

        // Set up button click intents
        Intent startStopIntent = new Intent(context, TimerWidgetProvider.class);
        startStopIntent.setAction("com.timers.widget.ACTION_TIMER_START");
        startStopIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        Intent resetIntent = new Intent(context, ResetConfirmActivity.class);
        resetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent editIntent = new Intent(context, TimerWidgetProvider.class);
        editIntent.setAction("com.timers.widget.ACTION_TIMER_EDIT");
        editIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE;

        android.app.PendingIntent startStopPendingIntent = android.app.PendingIntent.getBroadcast(
                context, appWidgetId, startStopIntent, flags);
        android.app.PendingIntent resetPendingIntent = android.app.PendingIntent.getActivity(
                context, appWidgetId + 5000, resetIntent, flags);
        android.app.PendingIntent editPendingIntent = android.app.PendingIntent.getBroadcast(
                context, appWidgetId + 10000, editIntent, flags);

        view.setOnClickPendingIntent(R.id.btn_start_stop, startStopPendingIntent);
        view.setOnClickPendingIntent(R.id.btn_reset, resetPendingIntent);
        view.setOnClickPendingIntent(R.id.btn_edit, editPendingIntent);

        // Update widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(appWidgetId, view);
    }

    /**
     * Handle start/stop button click
     */
    private void handleStartStop(Context context, int appWidgetId) {
        String timerId = TimerData.getTimerIdForWidget(context, appWidgetId);
        if (timerId == null) return;

        boolean isRunning = TimerData.isTimerRunning(context, timerId);

        if (isRunning) {
            // Stop the timer
            TimerData.setTimerRunning(context, timerId, false);
        } else {
            // Start the timer
            TimerData.setTimerRunning(context, timerId, true);
            // Start the background service
            Intent serviceIntent = new Intent(context, TimerService.class);
            serviceIntent.setAction("com.timers.widget.ACTION_TIMER_START");
            serviceIntent.putExtra("timer_id", timerId);
            context.startService(serviceIntent);
        }

        updateWidgetUI(context, appWidgetId);
    }

    /**
     * Handle reset button click
     */
    private void handleReset(Context context, int appWidgetId) {
        String timerId = TimerData.getTimerIdForWidget(context, appWidgetId);
        if (timerId == null) return;

        TimerData.resetTimer(context, timerId);
        updateWidgetUI(context, appWidgetId);
    }

    /**
     * Handle edit button click
     */
    private void handleEdit(Context context, int appWidgetId) {
        Intent configIntent = new Intent(context, TimerConfigActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        configIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(configIntent);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            // Unlink widget from timer but keep timer data
            TimerData.unlinkWidget(context, appWidgetId);
        }
    }
}
