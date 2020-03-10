/* TODO
*   Add the users alarm data to a database and display it in the AlarmSystem table
*   - Still need to be able to delete and edit alarms (could use alarmAdd UI to edit?) from database and AlarmManager
*   - Alarm data won't display in the TableLayout properly
*   Google News API?
*   Try to get alarms to display on the homepage
*   Add alarm icon scale animation in AlarmActivity
*   Fix weather temp and wind speed
*   Add weather icons
*   Card title texts bold? i.e. UI improvements
*   Alarm snooze? if not maybe add a message saying the snooze button isn't good for you
*   Diagrams (class/flow/wireframe) and report
*   Internal commentary
*  */

package com.app.today;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.HandlerThread;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;
import android.widget.Toolbar;
import com.androdocs.httprequest.HttpRequest;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    //Used to determine which permission we're asking for
    static final int MY_PERMISSIONS_REQUEST_READ_CALENDAR = 0;
    static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    //UI attributes
    TextView lastWUpdateTxt, forecastTxt, highsLowsTxt, temperatureTxt, windTxt, calTitle, newsTitle;
    ImageView alarmMore;
    TableLayout calTable, newsTable;
    Button button;

    //Used to get and store weather details
    private List<String> weatherDetails = new ArrayList<>();
    private static final String weatherAPI = "2a2d2e85e492fe3c92b568f4fe3ce854";

    //Used to store calendar events
    private List<Event> calendar = new ArrayList<>();

    //Used to store news headlines
    private HeadlineReceiver headlineReceiver = new HeadlineReceiver();

    //Used to verify Firebase sign in
    private static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    protected FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Acquires the current user and determines whether they are logged in or not
        user = mAuth.getCurrentUser();
        if(user == null) {
            //If not, go to sign in page
            Intent alarmActivity = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(alarmActivity);
            finish();
        } else {
            //If they are, begin initializing the homepage

            //Used to add an options menu to the ActionBar
            Toolbar toolbar = findViewById(R.id.action_logOut);
            setActionBar(toolbar);

            Log.i("! user is signed in", Objects.requireNonNull(user.getEmail()));

            //Initialize UI attributes
            lastWUpdateTxt = findViewById(R.id.lastWUpdate);
            forecastTxt = findViewById(R.id.forecast);
            highsLowsTxt = findViewById(R.id.highsLows);
            temperatureTxt = findViewById(R.id.temperature);
            windTxt = findViewById(R.id.windSpeed);
            calTitle = findViewById(R.id.calTitle);
            calTable = findViewById(R.id.calTable);
            alarmMore = findViewById(R.id.alarmMore);
            newsTitle = findViewById(R.id.newsTitle);
            newsTable = findViewById(R.id.newsTable);

            //Request required permissions then begin UI operations
            if(reqPermission(MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION))
                new weatherTask().execute();
            if(reqPermission(MY_PERMISSIONS_REQUEST_READ_CALENDAR))
                updateCalendarView();
            new headlineReceiver().execute();

            //If the user clicks the Alarms "+", go to AlarmSystem
            alarmMore.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent alarmActivity = new Intent(MainActivity.this, AlarmSystem.class);
                    startActivity(alarmActivity);
                }
            });
            button = findViewById(R.id.button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent alarmActivity = new Intent(MainActivity.this, AlarmActivity.class);
                    startActivity(alarmActivity);
                    finish();
                }
            });
        }
    }

    //Uses our 'menu.xml' file to define the ActionBar menu and pushes it to the display
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //Defines the actions for each option in the action bar menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //It receives a value passed on-click of the ActionBar menu and determines what to do by using a switch case
        switch (item.getItemId()) {
            case R.id.action_logOut:
                //Signs the user out and returns them to the sign in page
                mAuth.signOut();
                Intent signIn = new Intent(MainActivity.this, SignInActivity.class);
                startActivity(signIn);
                finish();
                return true;
            case R.id.action_refreshWeather:
                //Refreshes the weather (requests location access beforehand)
                if(reqPermission(MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)) {
                    new weatherTask().execute();
                    Toast.makeText(MainActivity.this, "! INFO: If refresh fails, there's no new weather data to pull or the API request limit for today has been reached", Toast.LENGTH_LONG).show();
                }
            case R.id.action_refreshCalendar:
                //Refreshes the calendar
                if(reqPermission(MY_PERMISSIONS_REQUEST_READ_CALENDAR))
                    updateCalendarView();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //AsyncTask for getting the weather, this is used to determine actions for before (onPreExecute) and after (onPostExecute) another
    //action (doInBackground, which as the name implies can be done in the background)
    class weatherTask extends AsyncTask<String, Void, String> {
        //Attributes defined globally for this subclass so they can be accessed by inner classes
        private double longitude, latitude;

        //onPreExecute instructs the UI to show that the weather is loading
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            updateWeatherView(View.VISIBLE, View.GONE, View.GONE, View.GONE);
        }

        //doInBackground gets the users location then uses it to retrieve the weather in their location
        @Override
        protected String doInBackground(String... args) {
            final LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    longitude = location.getLongitude();
                    latitude = location.getLatitude();
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(String provider) {}
                @Override
                public void onProviderDisabled(String provider) {}
            };
            //The GPS permission is required for the completion of weather retrieval
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //Creates an instance of the location service and retrieves the last know location
                //We do this so the system can determine later if there's a location update, and if
                //there is, request a weather update for the new location
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); //Location location =
                HandlerThread t = new HandlerThread("handlerThread");
                t.start();
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener, t.getLooper());
                //t.quit(); <-- causes a dead thread warning, critical?
                return HttpRequest.excuteGet("https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&APPID=" + weatherAPI);
            }
            return null;
        }

        //onPostExecute basically handles the weather response:
        @Override
        protected void onPostExecute(String result) {
            //If there's weather data to use, i.e. the .json response isn't empty, try to update the UI too
            //Else show the error message (in the catch)
            try {
                //Retrieves the .json result
                JSONObject jsonObj = new JSONObject(result);

                //Organises the results into suitable Objects
                JSONObject main = jsonObj.getJSONObject("main");
                JSONObject wind = jsonObj.getJSONObject("wind");
                JSONObject weather = jsonObj.getJSONArray("weather").getJSONObject(0);
                long updatedAt = jsonObj.getLong("dt");

                //Add the objects main values into a list of strings to be displayed
                weatherDetails.add(weather.getString("description"));
                weatherDetails.add(main.getString("temp") + "°C");
                weatherDetails.add(main.getString("temp_min") + "°C min - " + main.getString("temp_max") + "°C max");
                weatherDetails.add(wind.getString("speed") + " mph winds");
                weatherDetails.add("last updated: " + new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(new Date(updatedAt * 1000)));

                //If .json response isn't empty, display the response in the suitable UI views
                //Else throw an empty .json message
                if(!weatherDetails.isEmpty()) {
                    forecastTxt.setText(weatherDetails.get(0).toUpperCase());
                    temperatureTxt.setText(weatherDetails.get(1));
                    highsLowsTxt.setText(weatherDetails.get(2));
                    windTxt.setText(weatherDetails.get(3));
                    lastWUpdateTxt.setText(weatherDetails.get(4));
                    updateWeatherView(View.GONE, View.VISIBLE, View.VISIBLE, View.GONE);
                } else
                    throw new JSONException("Weather JSON empty... was HttpRequest unsuccessful?");
            } catch (JSONException e) {
                Log.e("? weather JSONException", ".json empty?", e);
                updateWeatherView(View.GONE, View.VISIBLE, View.GONE, View.VISIBLE);
            }
        }
    }

    //Used to hide/show certain views based on the parameters received
    private void updateWeatherView(int load, int card, int group, int error) {
        findViewById(R.id.weatherLoad).setVisibility(load);
        findViewById(R.id.weatherCard).setVisibility(card);
        findViewById(R.id.weatherStats).setVisibility(group);
        findViewById(R.id.weatherError).setVisibility(error);
    }

    //Used to handle the response from the CalendarContentResolver
    private void updateCalendarView() {
        //First, clear the table of calendar events in the UI and any currently stored events
        calTable.removeAllViews();
        if(calendar != null)
            calendar.clear();

        //Create a CalendarContentResolver to query and return the events of the calendar
        CalendarContentResolver resolver = new CalendarContentResolver();
        calendar = resolver.getCalendar(this);

        //If the calendar permission was granted, then this should continue
        //Else show error message
        if(calendar != null) {
            //If there are events happening today, add them to the UI table
            //Else show there are no events today
            if(!calendar.isEmpty()) {
                calTitle.setText(R.string.calTitle);
                for(Event event : calendar) {
                    //Creates a new TableRow to be added to the table
                    TableRow eventRow = new TableRow(getApplicationContext());

                    //Creates and defines a TextView to display an event
                    TextView eventTxt = new TextView(getApplicationContext());
                    String output;
                    //If the current event is not happening all day, display its start and end times
                    //Else display it as an all day event
                    if(!event.isAllDay())
                        output = event.getTitle() + " | " + resolver.getTimeString(event.getBegin().getTime()) + "-" + resolver.getTimeString(event.getEnd().getTime());
                    else
                        output = event.getTitle() + " | All day";
                    eventTxt.setText(output);
                    eventTxt.setTextSize(14);
                    //Define the layout parameters and margins of the event text view
                    //LayoutParameters are basically the width, height and weight (number of lines) of the display
                    TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1.0f);
                    params.setMargins(0, 12, 0, 8);
                    eventTxt.setLayoutParams(params);

                    //Adds the text to the row, then the row to the table with the layout parameters
                    eventRow.addView(eventTxt);
                    calTable.addView(eventRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                }
            } else
                calTitle.setText(R.string.calEmpty);
        }
        else
            calTitle.setText(R.string.calError);
    }

    //
    class headlineReceiver extends AsyncTask<String, Void, List<String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            updateHeadlinesView(View.VISIBLE, View.GONE, View.GONE);
        }
        @Override
        protected List<String> doInBackground(String... strings) {
            List<String> headlines;
            headlines = headlineReceiver.getHeadlines();
            return headlines;
        }
        @Override
        protected void onPostExecute(List<String> result) {
            if(result != null) {
                int i = 0;
                for(String headline : result) {
                    if(!(headline.equals("Corrections and clarifications") || headline.equals("This week’s correction | For the record"))) {
                        i++;
                        Log.i("headline", String.valueOf(i));
                        TableRow newsRow = new TableRow(getApplicationContext());
                        TextView titleTxt = new TextView(getApplicationContext());
                        String output = i + ". " + headline;
                        titleTxt.setText(output);
                        titleTxt.setTextSize(14);
                        titleTxt.setPadding(0, 12, 0, 0);
                        titleTxt.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1.0f));
                        newsRow.addView(titleTxt);
                        newsTable.addView(newsRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                    }
                }
                newsTitle.setText(R.string.newsTitle);
                updateHeadlinesView(View.GONE, View.VISIBLE, View.VISIBLE);
            } else {
                newsTitle.setText(R.string.newsError);
                updateHeadlinesView(View.GONE, View.VISIBLE, View.GONE);
            }
        }
    }
    private void updateHeadlinesView(int load, int card, int table) {
        findViewById(R.id.newsLoad).setVisibility(load);
        findViewById(R.id.newsCard).setVisibility(card);
        newsTable.setVisibility(table);
    }
    private boolean reqPermission(int p) {
        switch(p) {
            case MY_PERMISSIONS_REQUEST_READ_CALENDAR:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                    /*if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALENDAR)) {
                    } else { */
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, MY_PERMISSIONS_REQUEST_READ_CALENDAR);
                } else { return true; }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    return true;
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    /*if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    } else { */
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                } else { return true; }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    return true;
        }
        return false;
    }
}