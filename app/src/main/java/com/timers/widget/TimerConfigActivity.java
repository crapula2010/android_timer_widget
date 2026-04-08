package com.timers.widget;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration activity shown when user adds a new widget instance
 * Offers to restore existing stopwatch timers or create a new one
 */
public class TimerConfigActivity extends AppCompatActivity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private String selectedTimerId = null;
    private EditText editTimerName;
    private EditText editHours;
    private EditText editMinutes;
    private EditText editSeconds;
    private ListView timerListView;
    private List<String> availableTimerIds;
    private List<String> availableTimerNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent cancelValue = new Intent();
        cancelValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, cancelValue);

        setContentView(R.layout.timer_edit_dialog);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        cancelValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        initializeUI();
    }

    private void initializeUI() {
        editTimerName = findViewById(R.id.edit_timer_name);
        editHours = findViewById(R.id.edit_hours);
        editMinutes = findViewById(R.id.edit_minutes);
        editSeconds = findViewById(R.id.edit_seconds);
        timerListView = findViewById(R.id.timer_list);

        // Load available timers
        availableTimerIds = TimerData.getAllTimerIds(this);
        availableTimerNames = new ArrayList<>();
        for (String timerId : availableTimerIds) {
            availableTimerNames.add(TimerData.getTimerName(this, timerId));
        }

        // Check if this widget already has a linked timer (editing existing widget)
        String existingTimerId = TimerData.getTimerIdForWidget(this, appWidgetId);
        
        if (existingTimerId != null) {
            // Edit mode - show current timer details
            selectedTimerId = existingTimerId;
            editTimerName.setText(TimerData.getTimerName(this, existingTimerId));
            
            long elapsedMillis = TimerData.getElapsedMillis(this, existingTimerId);
            long totalSeconds = elapsedMillis / 1000;
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            editHours.setText(String.valueOf(hours));
            editMinutes.setText(String.valueOf(minutes));
            editSeconds.setText(String.valueOf(seconds));
            
            // Hide list view in edit mode
            timerListView.setVisibility(android.view.View.GONE);
        } else if (!availableTimerIds.isEmpty()) {
            // New widget - show list of available timers to restore
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_single_choice,
                    availableTimerNames);
            timerListView.setAdapter(adapter);
            timerListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            
            timerListView.setOnItemClickListener((parent, view, position, id) -> {
                selectedTimerId = availableTimerIds.get(position);
                // Load timer details
                loadTimerDetails(selectedTimerId);
            });
        } else {
            // No existing timers - hide list
            timerListView.setVisibility(android.view.View.GONE);
        }

        Button saveButton = findViewById(R.id.btn_dialog_save);
        Button cancelButton = findViewById(R.id.btn_dialog_cancel);

        saveButton.setOnClickListener(v -> saveTimerConfig());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void loadTimerDetails(String timerId) {
        editTimerName.setText(TimerData.getTimerName(this, timerId));
        
        long elapsedMillis = TimerData.getElapsedMillis(this, timerId);
        long totalSeconds = elapsedMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        editHours.setText(String.valueOf(hours));
        editMinutes.setText(String.valueOf(minutes));
        editSeconds.setText(String.valueOf(seconds));
    }

    private void saveTimerConfig() {
        String name = editTimerName.getText().toString().trim();
        if (name.isEmpty()) {
            name = "Timer";
        }

        // Parse elapsed time values
        long hours = parseInput(editHours.getText().toString(), 0, 9999);
        long minutes = parseInput(editMinutes.getText().toString(), 0, 59);
        long seconds = parseInput(editSeconds.getText().toString(), 0, 59);

        // Validate
        if (minutes > 59) minutes = 59;
        if (seconds > 59) seconds = 59;

        // Convert to milliseconds
        long elapsedMillis = (hours * 3600 + minutes * 60 + seconds) * 1000;

        String timerId = selectedTimerId;
        if (timerId == null) {
            // Create new timer starting at 0
            timerId = TimerData.createTimer(this, name);
            // If user manually set a value, apply it
            if (elapsedMillis > 0) {
                TimerData.setElapsedMillis(this, timerId, elapsedMillis);
            }
        } else {
            // Update existing timer
            TimerData.setTimerName(this, timerId, name);
            TimerData.setElapsedMillis(this, timerId, elapsedMillis);
        }

        // Link widget to timer
        TimerData.linkWidgetToTimer(this, appWidgetId, timerId);

        // Update widget UI
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        TimerWidgetProvider.updateWidgetUI(this, appWidgetId);

        // Return to home screen
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private long parseInput(String input, long min, long max) {
        try {
            long value = Long.parseLong(input);
            if (value < min) return min;
            if (value > max) return max;
            return value;
        } catch (NumberFormatException e) {
            return min;
        }
    }
}
