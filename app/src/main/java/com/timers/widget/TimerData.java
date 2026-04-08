package com.timers.widget;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

/**
 * Utility class for managing stopwatch/usage timer data storage and retrieval.
 * Supports persistent timer instances across widget lifecycle.
 * These are count-up timers for tracking accumulated usage (e.g., cartridge playtime).
 */
public class TimerData {
    private static final String PREFS_NAME = "TimerWidgetPrefs";
    private static final String GLOBAL_TIMERS_LIST = "global_timers_list"; // JSON list of all timers
    private static final String TIMER_ID_TO_WIDGET_ID = "timer_id_to_widget_"; // Map timer ID to widget ID
    private static final String WIDGET_ID_TO_TIMER_ID = "widget_id_to_timer_"; // Map widget ID to timer ID
    
    // Timer-specific keys
    private static final String TIMER_NAME_PREFIX = "timer_name_";
    private static final String TIMER_ELAPSED_MILLIS_PREFIX = "timer_elapsed_millis_"; // Total accumulated time
    private static final String TIMER_RUNNING_PREFIX = "timer_running_";
    private static final String TIMER_START_TIME_PREFIX = "timer_start_time_"; // When timer was started (epoch ms)
    private static final String TIMER_CREATED_PREFIX = "timer_created_"; // When timer was created

    /**
     * Create a new timer and return its ID
     */
    public static String createTimer(Context context, String name) {
        String timerId = UUID.randomUUID().toString();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Store timer data - starts at 0 elapsed time
        editor.putString(TIMER_NAME_PREFIX + timerId, name);
        editor.putLong(TIMER_ELAPSED_MILLIS_PREFIX + timerId, 0);
        editor.putBoolean(TIMER_RUNNING_PREFIX + timerId, false);
        editor.putLong(TIMER_CREATED_PREFIX + timerId, System.currentTimeMillis());

        // Add to global list
        String currentList = prefs.getString(GLOBAL_TIMERS_LIST, "");
        if (currentList.isEmpty()) {
            editor.putString(GLOBAL_TIMERS_LIST, timerId);
        } else {
            editor.putString(GLOBAL_TIMERS_LIST, currentList + "," + timerId);
        }

        editor.apply();
        return timerId;
    }

    /**
     * Get all available timers
     */
    public static List<String> getAllTimerIds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String list = prefs.getString(GLOBAL_TIMERS_LIST, "");
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(list.split(","));
    }

    /**
     * Link widget to timer
     */
    public static void linkWidgetToTimer(Context context, int appWidgetId, String timerId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(WIDGET_ID_TO_TIMER_ID + appWidgetId, timerId)
                .putString(TIMER_ID_TO_WIDGET_ID + timerId, String.valueOf(appWidgetId))
                .apply();
    }

    /**
     * Get timer ID for a widget
     */
    public static String getTimerIdForWidget(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(WIDGET_ID_TO_TIMER_ID + appWidgetId, null);
    }

    /**
     * Get timer name
     */
    public static String getTimerName(Context context, String timerId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(TIMER_NAME_PREFIX + timerId, "Timer");
    }

    /**
     * Set timer name
     */
    public static void setTimerName(Context context, String timerId, String name) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(TIMER_NAME_PREFIX + timerId, name).apply();
    }

    /**
     * Set if timer is running
     */
    public static void setTimerRunning(Context context, String timerId, boolean running) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(TIMER_RUNNING_PREFIX + timerId, running);
        
        if (running) {
            // Record when timer started
            editor.putLong(TIMER_START_TIME_PREFIX + timerId, System.currentTimeMillis());
        } else {
            // When stopped, calculate elapsed time and add to total
            long startTime = prefs.getLong(TIMER_START_TIME_PREFIX + timerId, 0);
            if (startTime > 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                long totalElapsed = prefs.getLong(TIMER_ELAPSED_MILLIS_PREFIX + timerId, 0);
                totalElapsed += elapsed;
                editor.putLong(TIMER_ELAPSED_MILLIS_PREFIX + timerId, totalElapsed);
            }
            editor.remove(TIMER_START_TIME_PREFIX + timerId);
        }
        
        editor.apply();
    }

    /**
     * Get if timer is running
     */
    public static boolean isTimerRunning(Context context, String timerId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(TIMER_RUNNING_PREFIX + timerId, false);
    }

    /**
     * Get calculated elapsed time (accounts for current session if running)
     */
    public static long getElapsedMillis(Context context, String timerId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long totalElapsed = prefs.getLong(TIMER_ELAPSED_MILLIS_PREFIX + timerId, 0);
        
        // If running, add current session elapsed time
        if (prefs.getBoolean(TIMER_RUNNING_PREFIX + timerId, false)) {
            long startTime = prefs.getLong(TIMER_START_TIME_PREFIX + timerId, 0);
            if (startTime > 0) {
                long currentSessionElapsed = System.currentTimeMillis() - startTime;
                totalElapsed += currentSessionElapsed;
            }
        }
        
        return totalElapsed;
    }

    /**
     * Directly set elapsed milliseconds
     */
    public static void setElapsedMillis(Context context, String timerId, long millis) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(TIMER_ELAPSED_MILLIS_PREFIX + timerId, millis).apply();
    }

    /**
     * Reset timer to 0
     */
    public static void resetTimer(Context context, String timerId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(TIMER_ELAPSED_MILLIS_PREFIX + timerId, 0);
        editor.putBoolean(TIMER_RUNNING_PREFIX + timerId, false);
        editor.remove(TIMER_START_TIME_PREFIX + timerId);
        editor.apply();
    }

    /**
     * Delete timer (removes it from widget but keeps in global list)
     */
    public static void unlinkWidget(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String timerId = prefs.getString(WIDGET_ID_TO_TIMER_ID + appWidgetId, null);
        
        if (timerId != null) {
            prefs.edit()
                    .remove(WIDGET_ID_TO_TIMER_ID + appWidgetId)
                    .remove(TIMER_ID_TO_WIDGET_ID + timerId)
                    .apply();
        }
    }

    /**
     * Delete timer completely (from global list)
     */
    public static void deleteTimer(Context context, String timerId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Remove from global list
        String list = prefs.getString(GLOBAL_TIMERS_LIST, "");
        List<String> timers = new ArrayList<>(Arrays.asList(list.split(",")));
        timers.remove(timerId);
        if (timers.isEmpty()) {
            editor.remove(GLOBAL_TIMERS_LIST);
        } else {
            editor.putString(GLOBAL_TIMERS_LIST, String.join(",", timers));
        }
        
        // Remove timer data
        editor.remove(TIMER_NAME_PREFIX + timerId);
        editor.remove(TIMER_ELAPSED_MILLIS_PREFIX + timerId);
        editor.remove(TIMER_RUNNING_PREFIX + timerId);
        editor.remove(TIMER_START_TIME_PREFIX + timerId);
        editor.remove(TIMER_CREATED_PREFIX + timerId);
        editor.remove(TIMER_ID_TO_WIDGET_ID + timerId);
        
        editor.apply();
    }

    /**
     * Convert milliseconds to formatted time string (HH:MM:SS)
     * Supports up to 9999:59:59
     */
    public static String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        // Cap at 9999:59:59
        if (hours > 9999) hours = 9999;

        return String.format("%04d:%02d:%02d", hours, minutes, seconds);
    }
}
