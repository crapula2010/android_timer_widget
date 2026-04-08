package com.timers.widget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Background service that manages stopwatch/usage timer tick updates
 * Persists across app lifecycle and calculates elapsed time on resume
 */
public class TimerService extends Service {
    private static final long TICK_INTERVAL = 100; // Update every 100ms for smooth display
    private Handler handler;
    private Runnable tickRunnable;
    private Set<String> activeTimers = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        
        // Resume any running timers from storage on service creation (after app restart)
        resumeRunningTimers();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            String timerId = intent.getStringExtra("timer_id");

            if ("com.timers.widget.ACTION_TIMER_START".equals(action) && timerId != null) {
                activeTimers.add(timerId);
                startTickUpdates();
            }
        }
        return START_STICKY;
    }

    /**
     * Resume any timers that were running before app was stopped
     */
    private void resumeRunningTimers() {
        List<String> allTimers = TimerData.getAllTimerIds(this);
        for (String timerId : allTimers) {
            if (TimerData.isTimerRunning(this, timerId)) {
                activeTimers.add(timerId);
            }
        }
        
        if (!activeTimers.isEmpty()) {
            startTickUpdates();
        }
    }

    private void startTickUpdates() {
        if (tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
        }

        tickRunnable = new Runnable() {
            @Override
            public void run() {
                boolean anyRunning = false;
                
                for (String timerId : new HashSet<>(activeTimers)) {
                    if (TimerData.isTimerRunning(TimerService.this, timerId)) {
                        anyRunning = true;
                        
                        // For count-up timers, just update display
                        // The actual elapsed time is calculated on-the-fly in TimerData.getElapsedMillis()
                        updateWidgetsForTimer(timerId);
                    } else {
                        activeTimers.remove(timerId);
                    }
                }

                // Schedule next tick if any timer is running
                if (anyRunning) {
                    handler.postDelayed(this, TICK_INTERVAL);
                } else {
                    stopSelf();
                }
            }
        };

        handler.post(tickRunnable);
    }

    /**
     * Update all widgets that are using this timer
     */
    private void updateWidgetsForTimer(String timerId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, TimerWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int appWidgetId : appWidgetIds) {
            String widgetTimerId = TimerData.getTimerIdForWidget(this, appWidgetId);
            if (timerId.equals(widgetTimerId)) {
                TimerWidgetProvider.updateWidgetUI(this, appWidgetId);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
        }
    }
}
