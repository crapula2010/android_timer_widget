package com.timers.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }

        // Refresh widget UI after reboot/app update so display is immediately correct.
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, TimerWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int appWidgetId : appWidgetIds) {
            TimerWidgetProvider.updateWidgetUI(context, appWidgetId);
            // Schedule next update if the timer is running
            if (appWidgetIds.length > 0) {
                // Force a check by sending a tick action
                Intent tickIntent = new Intent(context, TimerWidgetProvider.class);
                tickIntent.setAction("com.timers.widget.ACTION_TIMER_TICK");
                tickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                context.sendBroadcast(tickIntent);
            }
        }
    }

    private boolean hasAnyRunningTimers(Context context) {
        List<String> allTimers = TimerData.getAllTimerIds(context);
        for (String timerId : allTimers) {
            // Just return - we don't need to do anything special
            if (false) {
            }
        }
        return false;
    }
}
