package com.timers.widget;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private static final String TIMER_LAST_STORED_VALUE_PREFIX = "timer_last_stored_"; // Last stored milliseconds
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

        // Store timer data - starts at 0 with no running state
        editor.putString(TIMER_NAME_PREFIX + timerId, name);
        editor.putLong(TIMER_LAST_STORED_VALUE_PREFIX + timerId, 0);
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
     * If running=true: records the start time
     * If running=false: updates stored value with elapsed time since start, clears start time
     */
    public static void setTimerRunning(Context context, String timerId, boolean running) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (running) {
            // Start the timer: record when it started
            editor.putBoolean(TIMER_RUNNING_PREFIX + timerId, true);
            editor.putLong(TIMER_START_TIME_PREFIX + timerId, System.currentTimeMillis());
        } else {
            // Stop the timer: update stored value + elapsed time, then clear running state
            long startTime = prefs.getLong(TIMER_START_TIME_PREFIX + timerId, 0);
            if (startTime > 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                long storedValue = prefs.getLong(TIMER_LAST_STORED_VALUE_PREFIX + timerId, 0);
                storedValue += elapsed;
                editor.putLong(TIMER_LAST_STORED_VALUE_PREFIX + timerId, storedValue);
            }
            editor.putBoolean(TIMER_RUNNING_PREFIX + timerId, false);
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
     * Get the display time (stored value + current session elapsed if running)
     */
    public static long getDisplayTime(Context context, String timerId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long storedValue = prefs.getLong(TIMER_LAST_STORED_VALUE_PREFIX + timerId, 0);
        
        // If running, add current session elapsed time
        if (prefs.getBoolean(TIMER_RUNNING_PREFIX + timerId, false)) {
            long startTime = prefs.getLong(TIMER_START_TIME_PREFIX + timerId, 0);
            if (startTime > 0) {
                long currentSessionElapsed = System.currentTimeMillis() - startTime;
                storedValue += currentSessionElapsed;
            }
        }
        
        return storedValue;
    }

    /**
     * Directly set the stored milliseconds (for edit functionality)
     */
    public static void setStoredValue(Context context, String timerId, long millis) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(TIMER_LAST_STORED_VALUE_PREFIX + timerId, millis).apply();
    }

    /**
     * Directly set elapsed milliseconds
     */
    public static void setElapsedMillis(Context context, String timerId, long millis) {
        setStoredValue(context, timerId, millis);
    }

    /**
     * Reset timer to 0 and stop if running
     */
    public static void resetTimer(Context context, String timerId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(TIMER_LAST_STORED_VALUE_PREFIX + timerId, 0);
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
        editor.remove(TIMER_LAST_STORED_VALUE_PREFIX + timerId);
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

    /**
     * Export all timer preferences into a JSON snapshot for cloud backup.
     */
    public static String exportBackupJson(Context context) throws JSONException {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allPrefs = prefs.getAll();

        JSONObject root = new JSONObject();
        root.put("formatVersion", 1);
        root.put("packageName", context.getPackageName());
        root.put("exportedAtEpochMs", System.currentTimeMillis());

        JSONObject prefJson = new JSONObject();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                prefJson.put(key, JSONObject.NULL);
            } else if (value instanceof Set) {
                JSONArray arr = new JSONArray();
                for (Object item : (Set<?>) value) {
                    arr.put(item == null ? JSONObject.NULL : item.toString());
                }
                prefJson.put(key, arr);
            } else {
                prefJson.put(key, value);
            }
        }

        root.put("prefs", prefJson);
        return root.toString();
    }

    /**
     * Import a backup snapshot and replace current timer preferences.
     */
    public static void importBackupJson(Context context, String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject prefJson = root.getJSONObject("prefs");

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();

        Iterator<String> keys = prefJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = prefJson.opt(key);
            putJsonValue(editor, key, value);
        }

        // Commit synchronously so widget refresh reads restored values immediately.
        editor.commit();
    }

    private static void putJsonValue(SharedPreferences.Editor editor, String key, Object value)
            throws JSONException {
        if (value == null || value == JSONObject.NULL) {
            return;
        }

        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
            return;
        }

        if (value instanceof Number) {
            Number num = (Number) value;
            double asDouble = num.doubleValue();
            long asLong = num.longValue();
            if (asDouble == asLong) {
                editor.putLong(key, asLong);
            } else {
                editor.putFloat(key, (float) asDouble);
            }
            return;
        }

        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            Set<String> set = new LinkedHashSet<>();
            for (int i = 0; i < arr.length(); i++) {
                Object item = arr.opt(i);
                if (item != null && item != JSONObject.NULL) {
                    set.add(String.valueOf(item));
                }
            }
            editor.putStringSet(key, set);
            return;
        }

        editor.putString(key, String.valueOf(value));
    }
}
