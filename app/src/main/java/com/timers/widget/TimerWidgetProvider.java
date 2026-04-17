package com.timers.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * The main AppWidgetProvider for the Timer Widget
 * Simplified design: all state stored in SharedPreferences, widget just displays
 */
public class TimerWidgetProvider extends AppWidgetProvider {

    public static void refreshAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, TimerWidgetProvider.class)
        );

        TimerWidgetProvider provider = new TimerWidgetProvider();
        for (int appWidgetId : appWidgetIds) {
            updateWidgetUI(context, appWidgetId);
            provider.scheduleNextUpdateIfRunning(context, appWidgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidgetUI(context, appWidgetId);
            scheduleNextUpdateIfRunning(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
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
            // Timer tick - just update the display
            updateWidgetUI(context, appWidgetId);
            scheduleNextUpdateIfRunning(context, appWidgetId);
        }
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
        long displayTime = TimerData.getDisplayTime(context, timerId);
        boolean isRunning = TimerData.isTimerRunning(context, timerId);

        // Set timer name
        view.setTextViewText(R.id.timer_name, timerName);

        // Set timer display
        String timeDisplay = TimerData.formatTime(displayTime);
        view.setTextViewText(R.id.timer_display, timeDisplay);

        // Set button text
        view.setTextViewText(R.id.btn_start_stop, isRunning ? "Stop" : "Start");

        // Set up button click intents with unique request codes
        int uniqueIdBase = appWidgetId * 3;
        
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
                context, uniqueIdBase, startStopIntent, flags);
        android.app.PendingIntent resetPendingIntent = android.app.PendingIntent.getActivity(
                context, uniqueIdBase + 1, resetIntent, flags);
        android.app.PendingIntent editPendingIntent = android.app.PendingIntent.getBroadcast(
                context, uniqueIdBase + 2, editIntent, flags);

        view.setOnClickPendingIntent(R.id.btn_start_stop, startStopPendingIntent);
        view.setOnClickPendingIntent(R.id.btn_reset, resetPendingIntent);
        view.setOnClickPendingIntent(R.id.btn_edit, editPendingIntent);

        // Update widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(appWidgetId, view);
    }

    /**
     * Schedule the next update in 1 second if timer is running
     */
    private void scheduleNextUpdateIfRunning(Context context, int appWidgetId) {
        String timerId = TimerData.getTimerIdForWidget(context, appWidgetId);
        if (timerId == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent tickIntent = new Intent(context, TimerWidgetProvider.class);
        tickIntent.setAction("com.timers.widget.ACTION_TIMER_TICK");
        tickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE;
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context, appWidgetId + 20000, tickIntent, flags);

        // Cancel any existing alarm for this widget
        alarmManager.cancel(pendingIntent);

        // If timer is running, schedule next update in 1 second
        if (TimerData.isTimerRunning(context, timerId)) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000,
                    pendingIntent
            );
        }
    }

    /**
     * Handle start/stop button click
     */
    private void handleStartStop(Context context, int appWidgetId) {
        String timerId = TimerData.getTimerIdForWidget(context, appWidgetId);
        if (timerId == null) return;

        boolean isRunning = TimerData.isTimerRunning(context, timerId);

        if (isRunning) {
            // Stop the timer - this updates stored value
            TimerData.setTimerRunning(context, timerId, false);
        } else {
            // Start the timer - record start time
            TimerData.setTimerRunning(context, timerId, true);
        }

        // Update UI immediately to provide feedback
        updateWidgetUI(context, appWidgetId);
        scheduleNextUpdateIfRunning(context, appWidgetId);
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
        String timerId = TimerData.getTimerIdForWidget(context, appWidgetId);
        if (timerId == null) return;

        // Stop timer if running before opening edit
        if (TimerData.isTimerRunning(context, timerId)) {
            TimerData.setTimerRunning(context, timerId, false);
        }

        Intent configIntent = new Intent(context, TimerConfigActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        configIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(configIntent);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            // Cancel any pending alarms for this widget
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                Intent tickIntent = new Intent(context, TimerWidgetProvider.class);
                tickIntent.setAction("com.timers.widget.ACTION_TIMER_TICK");
                int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE;
                android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                        context, appWidgetId + 20000, tickIntent, flags);
                alarmManager.cancel(pendingIntent);
            }
            
            // Unlink widget from timer but keep timer data
            TimerData.unlinkWidget(context, appWidgetId);
        }
    }
}
