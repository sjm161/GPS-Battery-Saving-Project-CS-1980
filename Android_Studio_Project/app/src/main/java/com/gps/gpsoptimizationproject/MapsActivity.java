package com.gps.gpsoptimizationproject;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    //Both time variables are currently in seconds
    //On average, we can reacquire a signal in this amount of time
    final float STDTIMEALLOWANCE = 36.59f;
    //This is the amount of time the GPS needs to be off before we save power
    final float POWERSAVINGSTIME = 10f;

    //Declaring objects for use
    Location currentLocation;
    private GoogleMap mMap;
    LocationManager LocM;
    LocationListener newlistener;
    TextView velocitydisplay, distancedisplay, timedisplay;
    Location destination;
    ArrayList<Location> staticroute;
    int ListI = 0;
    boolean navigation = false, logGPSOnLatLng = false, testOvershot = false, firstOvershot=false;
    Marker destMarker;
    File logfile = null;
    FileOutputStream fos = null;
    String beforeEnable = "";
    Runnable GPSON = null;
    Handler h = new Handler();
    float firstDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        //Declare the textboxs within the context of the code
        velocitydisplay = findViewById(R.id.VelocityView);
        distancedisplay = findViewById(R.id.DistanceView);
        timedisplay = findViewById(R.id.TimeView);

        try{
            logfile = new File(getApplicationContext().getFilesDir() + "/GPSLog.txt");
            fos = new FileOutputStream(logfile, true);
            Date now = new Date();
            String logstring = "APPSTART|"+now.toString() + "\n";
            fos.write(logstring.getBytes());
        }catch(FileNotFoundException e){
            try {
                logfile.createNewFile();
                fos = new FileOutputStream(logfile);
            }catch(Exception ex){

            }
        }catch(Exception ef){

        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        FloatingActionButton distbut = findViewById(R.id.dist);

        staticroute = new ArrayList<Location>();
        createstaticroute();

        /*destination = new Location("");
        destination.setLatitude(40.444396);
        destination.setLongitude(-79.954794);*/
        //Setup the listener for the floating button
        distbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //calcDistance(destination);
                //navigation();
                navigation = !navigation;
                if(navigation){
                    try{
                        //Log when the user starts the navigational app
                        Date now = new Date();
                        String logstring = "NAVSTART|" + now.toString() + "\n";
                        fos.write("-------------------------------------------------------\n".getBytes());
                        fos.write(logstring.getBytes());
                    }catch(Exception e){
                        velocitydisplay.setText(e.getMessage());
                    }
                    //We started navigation - so start a new run
                    ListI = 0;
                    distancedisplay.setText("0 m");
                    timedisplay.setText("0 s");
                    //Acquire the new destination
                    destination = staticroute.get(ListI);
                    //Add it to the map
                    LatLng destLL = new LatLng(destination.getLatitude(),destination.getLongitude());
                    MarkerOptions destMarkerOptions = new MarkerOptions().position(destLL).title("Next Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                    //Add it to the map and save it in an object that we can use to remove it
                    destMarker = mMap.addMarker(destMarkerOptions);

                }
                else{
                    //We disabled navigation - remove the current marker
                    destMarker.remove();
                    distancedisplay.setText("");
                    timedisplay.setText("");
                    try{
                        //Log that the user stopped navigation manually
                        Date now = new Date();
                        String logstring = "NAVSTOP|MANUAL|" + now.toString() + "\n";

                        fos.write(logstring.getBytes());
                        fos.write("-------------------------------------------------------\n".getBytes());
                    }catch(Exception e){
                        velocitydisplay.setText(e.getMessage());
                    }
                }
            }
        });
        setnewLocationListener();
        //Declare a location Manager
        LocM = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            //LocM.requestSingleUpdate(LocationManager.GPS_PROVIDER, null);
            LocM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, newlistener);
        } catch (SecurityException e){
            velocitydisplay.setText(e.getMessage());
        }
        //create a new Runnable that will call the turn GPS On method after a certain amount of time that is inserted into the handler
        GPSON = new Runnable() {
            @Override
            public void run() {
                //turnGpsOn(getApplicationContext());
                logGPSOn();
            }
        };
    }

    private void createstaticroute(){

        Location one = new Location("");
        one.setLatitude(40.443162);
        one.setLongitude(-79.953543);

        Location two = new Location("");
        two.setLatitude(40.443918);
        two.setLongitude(-79.950732);

        Location three = new Location("");
        three.setLatitude(40.445065);
        three.setLongitude(-79.951263);

        Location four = new Location("");
        four.setLatitude(40.445179);
        four.setLongitude(-79.950357);

        staticroute.add(one);
        staticroute.add(two);
        staticroute.add(three);
        staticroute.add(four);
    }
    /*protected void createLocationRequest(){
        LocationRequest LocR = LocationRequest.create();

    }*/

    private void navigation(){

        Intent intent = new Intent("Intersection Test");
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);

        try {
            destination = staticroute.get(1);
            LocM.addProximityAlert(staticroute.get(1).getLatitude(), staticroute.get(1).getLongitude(), 15, -1, pendingIntent);
        }catch(SecurityException e){
            timedisplay.setText(e.getMessage());
        }
    }

    private void calcDistance(Location dest) {
        try {
            LocationManager tempmanager;
            //tempmanager.requestSingleUpdate( LocationManager.GPS_PROVIDER, new MyLocationListenerGPS(), null );
            Location cur = LocM.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //Location cur = new Location("");
            //cur.setLongitude(-79.953829);
            //cur.setLatitude(40.442469);
            if(cur == null) {
                distancedisplay.setText("Cur is null");
            } else {
                float distance = cur.distanceTo(dest);
                distancedisplay.setText(String.valueOf(distance) + " || " + cur.getLatitude() + " || " + cur.getLongitude());
            }
        }catch(SecurityException se) {
            velocitydisplay.setText(se.getMessage());
        }
        catch (Exception e){
            velocitydisplay.setText(e.getMessage());
        }
    }
    private float calcDistance(Location cur, Location dest) {
        try {
            //Location cur = LocM.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //Location cur = new Location("");
            //cur.setLongitude(-79.953829);
            //cur.setLatitude(40.442469);
            if(cur == null) {
                distancedisplay.setText("Cur is null");
                return 0f;
            } else {
                float distance = cur.distanceTo(dest);
                //distancedisplay.setText(String.valueOf(distance) + " || " + cur.getLatitude() + " || " + cur.getLongitude());

                distancedisplay.setText(String.valueOf(distance) + " m");
                return distance;
            }
        } catch (Exception e){
            velocitydisplay.setText(e.getMessage());
        }
        return 0f;
    }
    //This function creates a location listener - which handles detecting location changes- we don't
    //have a need for the other functions - as they are not as efficient as we want them to be and don't do what we need them to do
    private void setnewLocationListener(){
        newlistener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                velocitydisplay.setText("Trying to get velocity");
                if(location.hasSpeed()) {

                    velocitydisplay.setText(String.valueOf(location.getSpeed()) + " m/s");
                    //If we are navigating - calculate distance to the next destination
                    if(navigation) {
                        float time = calcDistance(location, destination) / location.getSpeed();
                        timedisplay.setText(String.valueOf(time) + " s");
                        try{
                            Date now = new Date();
                            String logstring = "CURLOC|" + location.getLatitude() + "|" + location.getLongitude() + "|" + location.getSpeed() +"m/s|" + location.distanceTo(destination) + "m|" + now.toString()  +"\n";
                            fos.write(logstring.getBytes());
                        }catch(Exception e){
                            logGPSOnLatLng = true;
                        }
                    }
                }
                if(navigation){
                    if(location.distanceTo(destination) <= 20) {
                        //We reached the destination radius - no need to test if we overshot the coordinate
                        testOvershot = false;
                        //We have reached our destination set a new destination
                        if(setDestination()) {
                            //Calculate time to the new destination
                            float time = calcDistance(location, destination) / location.getSpeed();

                            try{
                                Date now = new Date();

                                String logstring = "LOCOFF|" + location.getLatitude() + "|" + location.getLongitude() + "|" + now.toString()  +"\n";
                                fos.write(logstring.getBytes());
                            }catch(Exception e){

                            }
                            calcGPSTurnOff(time);
                        }
                    }
                    //We are not withing the target destination - see if we need to test for overshooting
                    else if(testOvershot){
                        //Acquire the first distance to destination and use that to determine if we overshot our destination
                        if(firstOvershot) {
                            firstDistance = location.distanceTo(destination);
                            firstOvershot = false;
                        }
                        //We have a first distance and the location changed again - measure
                        else{
                            //Get the current distance
                            float curDistance = location.distanceTo(destination);
                            //Since the users' location fluctuates ever so slightly even when they're standing still - we have to account for that, can't blindly compare if one is greater than the other
                            //If the distance changed more than 3 meters - a significant change
                            if(curDistance - firstDistance > 5f){
                                testOvershot = false;
                            }
                            //If we're gaining distance by more than a meter
                            else if(curDistance - firstDistance < -5f){
                                //Just end the testing here - we're still moving closer to the destination so we did not overshoot
                                testOvershot = false;
                            }
                            //If neither of those occured - do nothing - keep measuring the distance


                        }

                    }
                }
                //Check if we just turned the GPS On and need to log the first posible set of coordinates
                if(logGPSOnLatLng){
                    logGPSOnLatLng = false;
                    try{
                        Date now = new Date();
                        String logstring = "LOCON|" + location.getLatitude() + "|" + location.getLongitude() + "|" + now.toString()  + "\n";
                        fos.write(logstring.getBytes());
                    }catch(Exception e){
                        logGPSOnLatLng = true;
                    }
                }



            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }
    //A function for handling the acquisition of the next position in a route
    private boolean setDestination(){
        ListI++;
        //Check if the user reached their destination
        if(ListI == staticroute.size()) {
            destMarker.remove();
            navigation = false;
            ListI = 0;
            //Set the two text boxes relevant only to a destination to off
            distancedisplay.setText("");
            timedisplay.setText("");
            try{
                //Log that the user has reached their destination
                Date now = new Date();
                String logstring = "NAVSTOP|FINALDEST|" + now.toString();

                fos.write(logstring.getBytes());
                fos.write("-------------------------------------------------------\n".getBytes());
            }catch(Exception e){
                velocitydisplay.setText(e.getMessage());
            }
            return false;
        }
        else {
            //Remove the current marker from the map
            destMarker.remove();
            //Acquire the new destination
            destination = staticroute.get(ListI);
            //Create a marker at that point
            LatLng destLL = new LatLng(destination.getLatitude(),destination.getLongitude());
            MarkerOptions destMarkerOptions = new MarkerOptions().position(destLL).title("Next Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            //Add it to the map and save it in an object that we can use to remove it
            destMarker = mMap.addMarker(destMarkerOptions);
            return true;
        }
    }
    private void calcGPSTurnOff(float time){
        float turnOffTime = time - STDTIMEALLOWANCE;
        //If the odds of getting a new fix are lower than we like, don't bother shutting it off at all
        if(turnOffTime <= 0){
            try{
                //Log that the time was too small for standard deviation of reacquiring signal
                Date now = new Date();
                String logstring = "GPSNOTOFF|UNDERSTD|" + turnOffTime + "|" + now.toString() + "\n";
                fos.write(logstring.getBytes());
            }catch(Exception e){
                velocitydisplay.setText(e.getMessage());
            }
        }
        //Conversely, if the time we're shutting it off for is too small for any noticeable savings
        else if(turnOffTime <= POWERSAVINGSTIME){
            try{
                //Log that the time was too small for standard deviation of reacquiring signal
                Date now = new Date();
                String logstring = "GPSNOTOFF|UNDERPOWERSAVINGS|" + turnOffTime + "|" + now.toString() + "\n";
                fos.write(logstring.getBytes());
            }catch(Exception e){
                velocitydisplay.setText(e.getMessage());
            }
        }
        //We have enough time. Turn the GPS off and then back on again
        else{
            try{
                //Log that the time was too small for standard deviation of reacquiring signal
                Date now = new Date();
                String logstring = "ETAOFF|" + turnOffTime +"|" + now.toString() + "\n";
                fos.write(logstring.getBytes());
            }catch(Exception e){
                velocitydisplay.setText(e.getMessage());
            }
            //Shut it off
            //turnGpsOff(getApplicationContext());
            logGPSOff();
            //Convert from seconds to milliseconds for the runnable
            long milliseconds = (long)(turnOffTime * 1000);
            //Call the GPS ON Method after
            h.postDelayed(GPSON, milliseconds);
        }
    }
    private void logGPSOff(){
        Date now = new Date();
        String logstring = "GPSLOFF|" + now.toString() + "\n";
        try {
            fos.write(logstring.getBytes());
        }catch(Exception e){
            velocitydisplay.setText(e.getMessage());
        }
    }
    private void logGPSOn(){
        Date now = new Date();
        String logstring = "GPSLON|" + now.toString() + "\n";
        try {
            fos.write(logstring.getBytes());
        }catch(Exception e){
            velocitydisplay.setText(e.getMessage());
        }
        logGPSOnLatLng = true;
        testOvershot = true;
        firstOvershot = true;
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    //@RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        //Now to get the user's live speed

        // Add a marker in Sydney and move the camera
      /*  LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
        
    }
    private void turnGpsOff (Context context) {
        //LocationServices.SettingsApi
        if (null == beforeEnable) {
            String str = Settings.Secure.getString (context.getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if (null == str) {
                str = "";
            } else {
                String[] list = str.split (",");
                str = "";
                int j = 0;
                for (int i = 0; i < list.length; i++) {
                    if (!list[i].equals("network") && !list[i].equals("gps")) {
                        if (j > 0) {
                            str += ",";
                        }
                        str += list[i];
                        j++;
                    }
                }
                beforeEnable = str;
                //GPSText.setText(str);
            }
        }
        try {
            Settings.Secure.putString (context.getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                    beforeEnable);
        } catch(Exception e) {
            //Let's add in some error checking - lets set one of the text views to an error message
            //for exception handling
            velocitydisplay.setText(e.getMessage());
        }
        Date now = new Date();
        String logstring = "GPSOFF|" + now.toString() + "\n";
        try {
            fos.write(logstring.getBytes());
        }catch(Exception e){
            velocitydisplay.setText(e.getMessage());
        }
    }
    //Same function from MainActivity for turning the GPS ON
    private void turnGpsOn (Context context) {

        try {
            Settings.Secure.putString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, "network,gps");
        }
        catch (Exception e){
            velocitydisplay.setText(e.getMessage());
        }
        Date now = new Date();
        String logstring = "GPSON|" + now.toString() + "\n";
        try {
            fos.write(logstring.getBytes());
        }catch(Exception e){
            velocitydisplay.setText(e.getMessage());
        }
        //We set the GPS to on, get the first coordinates that we can
        logGPSOnLatLng = true;
        testOvershot = true;
        firstOvershot = true;
    }
}
