package com.app.today;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import android.icu.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AlarmSystem extends AppCompatActivity {
    ImageView alarmBack, alarmAdd, alarmSave;
    CardView alarmsCard, addCard;
    CheckBox chkMon, chkTues, chkWed, chkThurs, chkFri, chkSat, chkSun;
    EditText hour, minute, alarmLabel;
    TextView alarmEmpty;
    ProgressBar alarmLoad;
    ConstraintLayout addGroup;
    TableLayout alarmTable;

    Button button;

    AlarmManager alarmManager;
    PendingIntent alarmSender;

    //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    //DatabaseUtils alarms = new DatabaseUtils(AlarmSystem.this); //SQLite
    DatabaseUtils alarms = new DatabaseUtils();
    List<Alarm> alarmList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_system);
        alarmsCard = findViewById(R.id.alarmsCard);
        addCard = findViewById(R.id.addCard);
        alarmBack = findViewById(R.id.alarmBack);
        alarmAdd = findViewById(R.id.alarmAdd);
        alarmTable = findViewById(R.id.alarmTable);
        alarmEmpty = findViewById(R.id.alarmEmpty);
        alarmLoad = findViewById(R.id.alarmLoad);
        alarmSave = findViewById(R.id.alarmSave);
        addGroup = findViewById(R.id.addGroup);
        chkMon = findViewById(R.id.chkMon);
        chkTues = findViewById(R.id.chkTues);
        chkWed = findViewById(R.id.chkWed);
        chkThurs = findViewById(R.id.chkThurs);
        chkFri = findViewById(R.id.chkFri);
        chkSat = findViewById(R.id.chkSat);
        chkSun = findViewById(R.id.chkSun);
        hour = findViewById(R.id.hour);
        minute = findViewById(R.id.minute);
        alarmLabel = findViewById(R.id.alarmLabel);

        button = findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new alarmRetrieve().execute();
            }
        });

        try {
            new alarmRetrieve().execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        alarmBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearAddUI();
                Intent mainReturn = new Intent(AlarmSystem.this, MainActivity.class);
                //mainReturn.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                startActivity(mainReturn);
                finish();
            }
        });
        alarmAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(addCard.getVisibility() == View.GONE)
                    addCard.setVisibility(View.VISIBLE);
                    //change add button image to close/X
                else
                    addCard.setVisibility(View.GONE);
            }
        });
        hour.addTextChangedListener(new TextWatcher() {
            int hr;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() > 0) {
                    hr = Integer.parseInt(s.toString());
                    if(!(hr >= 0 && hr <= 23 && s.length() <= 2)) {
                        if(s.length() != 0)
                            hour.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                        else
                            hour.getText().clear();
                }
                else
                    hr = 0;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        minute.addTextChangedListener(new TextWatcher() {
            int min;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() > 0) {
                    min = Integer.parseInt(s.toString());
                    if(!(min >= 0 && min <= 59 && s.length() <= 2)) {
                        if(s.length() != 0)
                            minute.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                        else
                            minute.getText().clear();
                    }
                }
                else
                    min = 0;
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        alarmLabel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() > 100) {
                    alarmLabel.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                    Toast.makeText(AlarmSystem.this, "Label may consist of 100 characters max", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        alarmSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!(hour.getText().toString().equals("") || minute.getText().toString().equals(""))) {
                    Calendar validationDate = DateUtilities.buildTime(Integer.parseInt(hour.getText().toString()), Integer.parseInt(minute.getText().toString()), 0, 0); //Calendar.getInstance();
                    long alarmTime = validationDate.getTimeInMillis();
                    long now = System.currentTimeMillis();

                    Log.i("? time comparison", "is " + now + " > " + alarmTime + "?");
                    Log.i("? date == ", validationDate.getTime().toString());

                    if (now > alarmTime && !(chkMon.isChecked() || chkTues.isChecked() || chkWed.isChecked() || chkThurs.isChecked() || chkFri.isChecked() || chkSat.isChecked() || chkSun.isChecked())) {
                        Toast.makeText(getApplicationContext(), "Cannot set an alarm for a past time...", Toast.LENGTH_LONG).show();
                    } else {
                        //UITime = hour.getText() + ":" + minute.getText();
                        Alarm alarm = createAlarm(hour.getText().toString(), minute.getText().toString(), alarmLabel.getText().toString());
                        scheduleAlarm(alarm, alarmTime);
                        clearAddUI();
                    }
                }
                else {
                    if(hour.getText().toString().equals(""))
                        hour.setHintTextColor(Color.RED);
                    if(minute.getText().toString().equals(""))
                        minute.setHintTextColor(Color.RED);
                    Toast.makeText(getApplicationContext(), "Both Hour and Minute must be defined...", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    class alarmRetrieve extends AsyncTask<String, Void, List<Alarm>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            alarmTable.removeAllViews();
            updateAlarms(View.GONE, View.VISIBLE, View.GONE);
        }
        @Override
        protected List<Alarm> doInBackground(String... strings) {
            return alarms.get();
        }
        @Override
        protected void onPostExecute(List<Alarm> s) {
            super.onPostExecute(s);
            if(s != null) {
                for(Alarm alarm : s) {
                    TableRow alarmRow = new TableRow(getApplicationContext());
                    alarmRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    TextView timeTxt = new TextView(getApplicationContext());
                    timeTxt.setText(alarm.getTime());
                    timeTxt.setTextSize(20);
                    TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                    //params.setMargins(30, 15, 0, 8);
                    timeTxt.setLayoutParams(params);
                    alarmRow.addView(timeTxt);
                    //createRow(alarm.getTime(), alarm.getLabel(), alarm.getDays());
                    alarmTable.addView(alarmRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                }
                updateAlarms(View.VISIBLE, View.GONE, View.GONE);
            }
            else
                updateAlarms(View.GONE, View.GONE, View.VISIBLE);
        }
    }
    private void updateAlarms(int table, int load, int error) {
        alarmLoad.setVisibility(load);
        alarmsCard.setVisibility(table);
        alarmEmpty.setVisibility(error);
    }
    private Alarm createAlarm(String hour, String minute, String label) {
        /*while(true) {
            int id = new Random().nextInt(10);
            if(!alarms.has(id))
                break;
        }*/
        String days = "";
        String UITime = hour + ":" + minute;
        String id = alarms.newKey();
        if (!(alarmLabel.getText().toString().equals("")))
            label = alarmLabel.getText().toString();
        if(chkMon.isChecked() || chkTues.isChecked() || chkWed.isChecked() || chkThurs.isChecked() || chkFri.isChecked() || chkSat.isChecked() || chkSun.isChecked()) {
            if (chkMon.isChecked())
                days = days + "," + Calendar.MONDAY;
            if (chkTues.isChecked())
                days = days + "," + Calendar.TUESDAY;
            if (chkWed.isChecked())
                days = days + "," + Calendar.WEDNESDAY;
            if (chkThurs.isChecked())
                days = days + "," + Calendar.THURSDAY;
            if (chkFri.isChecked())
                days = days + "," + Calendar.FRIDAY;
            if (chkSat.isChecked())
                days = days + "," + Calendar.SATURDAY;
            if (chkSun.isChecked())
                days = days + "," + Calendar.SUNDAY;
        }
        return new Alarm(id, days, label, UITime);
    }
    private void clearAddUI() {
        hour.getText().clear();
        minute.getText().clear();
        if(chkMon.isChecked())
            chkMon.toggle();
        if(chkTues.isChecked())
            chkTues.toggle();
        if(chkWed.isChecked())
            chkWed.toggle();
        if(chkThurs.isChecked())
            chkThurs.toggle();
        if(chkFri.isChecked())
            chkFri.toggle();
        if(chkSat.isChecked())
            chkSat.toggle();
        if(chkSun.isChecked())
            chkSun.toggle();
        alarmLabel.getText().clear();
        addCard.setVisibility(View.GONE);
    }
    private void scheduleAlarm(Alarm alarm, long alarmTime) {

        /* perhaps the best way to do this is set the alarm to trigger every day, then once that time in the
        * day has come, pull the alarm matching the ID we want from the database and check if it is due
        * to ring on the current day. */

        /*Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        calendar.setTimeInMillis(alarmTime);*/
        /* Check we aren't setting it in the past which would trigger it to fire instantly
        if(calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 7);
        }*/

        // Set this to whatever you were planning to do at the given time
        String id = alarm.getId();
        Intent alarmIntent = new Intent(this, AlarmRing.class);
        alarmIntent.putExtra("alarmID", id);
        alarmIntent.setAction("com.app.today.FireAlarm");
        alarmSender = PendingIntent.getBroadcast(this.getApplicationContext(), 0, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
        //upon ring, transition to alarm activity
        //perhaps pull an id and ring the alarm matching that id, alarms could be stored
        //in a database, plus it might be easier to view and delete them this way?



        Log.i("? attempted to invoke AlarmManager with ID", String.valueOf(id));
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        assert alarmManager != null;
        //sets alarm -> alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarmTime, AlarmManager.INTERVAL_DAY, alarmSender); //AlarmManager.INTERVAL_DAY * 7 <-- changed from a week to a day to fit proposal

        //Currently this will fire the alarm every day, but if you want it to ring on set days every week, it will ring corresponding to the amount of days
        //you checked at the same time. So say you checked 4 days, it will fire 4 alarms at the same time every day. Either set the interval back to 7 so
        //the alarm for that day doesn't fire until next week, or only schedule it if it hasn't already been added to the database, i.e. alarm ID doesn't
        //already exist

        alarms.store(alarm);
        new alarmRetrieve().execute();
    }

    /* Possible alarm operators

    public void cancelPeriodicSchedule(PendingIntent sender) {
        if (am != null) {
            if (sender != null) {
                am.cancel(sender);
                sender.cancel();
            }
        }
        // Deactivate Broadcast Receiver to stop receiving broadcasts
        deactivateBroadcastreceiver();
    }
    private void activateBroadcastReceiver() {
        PackageManager pm = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, AlarmReceiver.class);
        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        Toast.makeText(context, "activated", Toast.LENGTH_LONG).show();
    }
    private void deactivateBroadcastreceiver() {
        PackageManager pm = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, AlarmReceiver.class);
        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        Toast.makeText(context, "cancelled", Toast.LENGTH_LONG).show();
    }*/
}