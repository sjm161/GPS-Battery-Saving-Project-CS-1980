package com.gps.gpsoptimizationproject;

import androidx.fragment.app.FragmentActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    //Both time variables are currently in seconds
    //On average, we can reacquire a signal in this amount of time
    final float STDTIMEALLOWANCE = 36.59f;
    //This is the amount of time the GPS needs to be off before we save power
    final float POWERSAVINGSTIME = 10f;

    //specifies who the route is going to be created for (Stephen, Matt, Mosse, driver)
    final String user = "matt";

    //Declaring objects for use
    Location currentLocation;
    private GoogleMap mMap;
    LocationManager LocM;
    LocationListener newlistener;
    TextView velocitydisplay, distancedisplay, timedisplay;
    Location destination;
    ArrayList<Location> staticRoute;
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

        try {
            logfile = new File(getApplicationContext().getFilesDir() + "/GPSLog.txt");
            fos = new FileOutputStream(logfile, true);
            Date now = new Date();
            String logstring = "APPSTART|"+now.toString() + "\n";
            fos.write(logstring.getBytes());
        } catch(FileNotFoundException e) {
            try {
                logfile.createNewFile();
                fos = new FileOutputStream(logfile);
            } catch(Exception ex) {

            }
        } catch(Exception ef) {

        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        FloatingActionButton distbut = findViewById(R.id.dist);

        staticRoute = new ArrayList<Location>();
        Bundle extras = getIntent().getExtras();
        boolean select = extras.getBoolean("routeSelect");
        if(select == true) {
            createHomeRoute();
        } else {
            createPittRoute();
        }

        /*destination = new Location("");
        destination.setLatitude(40.444396);
        destination.setLongitude(-79.954794);*/
        //Setup the listener for the floating button
        distbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                    destination = staticRoute.get(ListI);
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
        setNewLocationListener();
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

    //creates route to Pitt
    private void createPittRoute() {
        if(user.equals("test")) {
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

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
        } else if(user.equals("mosse")) {
            Location one = new Location("");
            one.setLatitude(40.473427);
            one.setLongitude(-79.911090);

            Location two = new Location("");
            two.setLatitude(40.470195);
            two.setLongitude(-79.911648);

            Location three = new Location("");
            three.setLatitude(40.469281);
            three.setLongitude(-79.912592);

            Location four = new Location("");
            four.setLatitude(40.468563);
            four.setLongitude(-79.916626);

            Location five = new Location("");
            five.setLatitude(40.468800);
            five.setLongitude(-79.917835);

            Location six = new Location("");
            six.setLatitude(40.460807);
            six.setLongitude(-79.922612);

            Location seven = new Location("");
            seven.setLatitude(40.460575);
            seven.setLongitude(-79.923014);

            Location eight = new Location("");
            eight.setLatitude(40.459226);
            eight.setLongitude(-79.922089);

            Location nine = new Location("");
            nine.setLatitude(40.459039);
            nine.setLongitude(-79.924525);

            Location ten = new Location("");
            ten.setLatitude(40.458368);
            ten.setLongitude(-79.927332);

            Location eleven = new Location("");
            eleven.setLatitude(40.456369);
            eleven.setLongitude(-79.930510);

            Location twelve = new Location("");
            twelve.setLatitude(40.455084);
            twelve.setLongitude(-79.931894);

            Location thirteen = new Location("");
            thirteen.setLatitude(40.452736);
            thirteen.setLongitude(-79.936728);

            Location fourteen = new Location("");
            fourteen.setLatitude(40.451297);
            fourteen.setLongitude(-79.940955);

            Location fifteen = new Location("");
            fifteen.setLatitude(40.450489);
            fifteen.setLongitude(-79.945032);

            Location sixteen = new Location("");
            sixteen.setLatitude(40.447974);
            sixteen.setLongitude(-79.947687);

            Location seventeen = new Location("");
            seventeen.setLatitude(40.447345);
            seventeen.setLongitude(-79.947440);

            Location eighteen = new Location("");
            eighteen.setLatitude(40.447067);
            eighteen.setLongitude(-79.947634);

            Location nineteen = new Location("");
            nineteen.setLatitude(40.446716);
            nineteen.setLongitude(-79.951474);

            Location twenty = new Location("");
            twenty.setLatitude(40.442517);
            twenty.setLongitude(-79.957461);

            Location twentyone = new Location("");
            twentyone.setLatitude(40.441831);
            twentyone.setLongitude(-79.956195);

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
            staticRoute.add(five);
            staticRoute.add(six);
            staticRoute.add(seven);
            staticRoute.add(eight);
            staticRoute.add(nine);
            staticRoute.add(ten);
            staticRoute.add(eleven);
            staticRoute.add(twelve);
            staticRoute.add(thirteen);
            staticRoute.add(fourteen);
            staticRoute.add(fifteen);
            staticRoute.add(sixteen);
            staticRoute.add(seventeen);
            staticRoute.add(eighteen);
            staticRoute.add(nineteen);
            staticRoute.add(twenty);
            staticRoute.add(twentyone);
        } else if(user.equals("driver")) {
            Location one = new Location("");
            one.setLatitude(40.505380);
            one.setLongitude(-79.924719);

            Location two = new Location("");
            two.setLatitude(40.504662);
            two.setLongitude(-79.923664);

            Location three = new Location("");
            three.setLatitude(40.504288);
            three.setLongitude(-79.923376);

            Location four = new Location("");
            four.setLatitude(40.504687);
            four.setLongitude(-79.922622);

            Location five = new Location("");
            five.setLatitude(40.505061);
            five.setLongitude(-79.922419);

            Location six = new Location("");
            six.setLatitude(40.504802);
            six.setLongitude( -79.920952);

            Location seven = new Location("");
            seven.setLatitude(40.504841);
            seven.setLongitude(-79.920338);

            Location eight = new Location("");
            eight.setLatitude(40.505441);
            eight.setLongitude(-79.918783);

            Location nine = new Location("");
            nine.setLatitude(40.506032);
            nine.setLongitude(-79.918394);

            Location ten = new Location("");
            ten.setLatitude(40.506156);
            ten.setLongitude(-79.918000);

            Location eleven = new Location("");
            eleven.setLatitude(40.506066);
            eleven.setLongitude(-79.917633);

            Location twelve = new Location("");
            twelve.setLatitude(40.504010);
            twelve.setLongitude(-79.919374);

            Location thirteen = new Location("");
            thirteen.setLatitude(40.503504);
            thirteen.setLongitude(-79.920034);

            Location fourteen = new Location("");
            fourteen.setLatitude(40.502960);
            fourteen.setLongitude(-79.920918);

            Location fifteen = new Location("");
            fifteen.setLatitude(40.502262);
            fifteen.setLongitude(-79.921538);

            Location sixteen = new Location("");
            sixteen.setLatitude(40.501555);
            sixteen.setLongitude(-79.922884);

            Location seventeen = new Location("");
            seventeen.setLatitude(40.501037);
            seventeen.setLongitude(-79.923600);

            Location eighteen = new Location("");
            eighteen.setLatitude(40.500848);
            eighteen.setLongitude(-79.924062);

            Location nineteen = new Location("");
            nineteen.setLatitude(40.500673);
            nineteen.setLongitude(-79.924225);

            Location twenty = new Location("");
            twenty.setLatitude(40.497297);
            twenty.setLongitude(-79.925425);

            Location twentyone = new Location("");
            twentyone.setLatitude(40.496571);
            twentyone.setLongitude(-79.926633);

            Location twentytwo = new Location("");
            twentytwo.setLatitude(40.496299);
            twentytwo.setLongitude(-79.926580);

            Location twentythree = new Location("");
            twentythree.setLatitude(40.496402);
            twentythree.setLongitude(-79.925995);

            Location twentyfour = new Location("");
            twentyfour.setLatitude(40.496427);
            twentyfour.setLongitude(-79.925399);

            Location twentyfive = new Location("");
            twentyfive.setLatitude(40.494646);
            twentyfive.setLongitude(-79.925290);

            Location twentysix = new Location("");
            twentysix.setLatitude(40.494748);
            twentysix.setLongitude(-79.923533);

            Location twentyseven = new Location("");
            twentyseven.setLatitude(40.493134);
            twentyseven.setLongitude( -79.911484);

            Location twentyeight = new Location("");
            twentyeight.setLatitude(40.492776);
            twentyeight.setLongitude(-79.910667);

            Location twentynine = new Location("");
            twentynine.setLatitude(40.492384);
            twentynine.setLongitude(-79.910490);

            Location thirty = new Location("");
            thirty.setLatitude(40.486523);
            thirty.setLongitude(-79.913340);

            Location thirtyone = new Location("");
            thirtyone.setLatitude(40.486087);
            thirtyone.setLongitude(-79.913620);

            Location thirtytwo = new Location("");
            thirtytwo.setLatitude(40.486271);
            thirtytwo.setLongitude(-79.914975);

            Location thirtythree = new Location("");
            thirtythree.setLatitude(40.486503);
            thirtythree.setLongitude(-79.915159);

            Location thirtyfour = new Location("");
            thirtyfour.setLatitude(40.486570);
            thirtyfour.setLongitude(-79.914765);

            Location thirtyfive = new Location("");
            thirtyfive.setLatitude(40.485221);
            thirtyfive.setLongitude(-79.909418);

            Location thirtysix = new Location("");
            thirtysix.setLatitude(40.484810);
            thirtysix.setLongitude(-79.908489);

            Location thirtyseven = new Location("");
            thirtyseven.setLatitude(40.484438);
            thirtyseven.setLongitude(-79.908190);

            Location thirtyeight = new Location("");
            thirtyeight.setLatitude(40.483698);
            thirtyeight.setLongitude(-79.907904);

            Location thirtynine = new Location("");
            thirtynine.setLatitude(40.482504);
            thirtynine.setLongitude(-79.908044);

            Location forty = new Location("");
            forty.setLatitude(40.480646);
            forty.setLongitude(-79.907841);

            Location fortyone = new Location("");
            fortyone.setLatitude(40.479302);
            fortyone.setLongitude(-79.907847);

            Location fortytwo = new Location("");
            fortytwo.setLatitude(40.471237);
            fortytwo.setLongitude(-79.908559);

            Location fortythree = new Location("");
            fortythree.setLatitude(40.467464);
            fortythree.setLongitude(-79.908775);

            Location fortyfour = new Location("");
            fortyfour.setLatitude(40.463211);
            fortyfour.setLongitude(-79.905827);

            Location fortyfive = new Location("");
            fortyfive.setLatitude(40.462541);
            fortyfive.setLongitude(-79.905589);

            Location fortysix = new Location("");
            fortysix.setLatitude(40.461681);
            fortysix.setLongitude(-79.905798);

            Location fortyseven = new Location("");
            fortyseven.setLatitude(40.456749);
            fortyseven.setLongitude(-79.909474);

            Location fortyeight = new Location("");
            fortyeight.setLatitude(40.454036);
            fortyeight.setLongitude(-79.912367);

            Location fortynine = new Location("");
            fortynine.setLatitude(40.452700);
            fortynine.setLongitude(-79.922037);

            Location fifty = new Location("");
            fifty.setLatitude(40.450534);
            fifty.setLongitude(-79.927498);

            Location fiftyone = new Location("");
            fiftyone.setLatitude(40.447884);
            fiftyone.setLongitude(-79.935777);

            Location fiftytwo = new Location("");
            fiftytwo.setLatitude(40.447307);
            fiftytwo.setLongitude(-79.944174);

            Location fiftythree = new Location("");
            fiftythree.setLatitude(40.448675);
            fiftythree.setLongitude(-79.944220);

            Location fiftyfour = new Location("");
            fiftyfour.setLatitude(40.451344);
            fiftyfour.setLongitude(-79.945430);

            Location fiftyfive = new Location("");
            fiftyfive.setLatitude(40.449809);
            fiftyfive.setLongitude(-79.950745);

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
            staticRoute.add(five);
            staticRoute.add(six);
            staticRoute.add(seven);
            staticRoute.add(eight);
            staticRoute.add(nine);
            staticRoute.add(ten);
            staticRoute.add(eleven);
            staticRoute.add(twelve);
            staticRoute.add(thirteen);
            staticRoute.add(fourteen);
            staticRoute.add(fifteen);
            staticRoute.add(sixteen);
            staticRoute.add(seventeen);
            staticRoute.add(eighteen);
            staticRoute.add(nineteen);
            staticRoute.add(twenty);
            staticRoute.add(twentyone);
            staticRoute.add(twentytwo);
            staticRoute.add(twentythree);
            staticRoute.add(twentyfour);
            staticRoute.add(twentyfive);
            staticRoute.add(twentysix);
            staticRoute.add(twentyseven);
            staticRoute.add(twentyeight);
            staticRoute.add(twentynine);
            staticRoute.add(thirty);
            staticRoute.add(thirtyone);
            staticRoute.add(thirtytwo);
            staticRoute.add(thirtythree);
            staticRoute.add(thirtyfour);
            staticRoute.add(thirtyfive);
            staticRoute.add(thirtysix);
            staticRoute.add(thirtyseven);
            staticRoute.add(thirtyeight);
            staticRoute.add(thirtynine);
            staticRoute.add(forty);
            staticRoute.add(fortyone);
            staticRoute.add(fortytwo);
            staticRoute.add(fortythree);
            staticRoute.add(fortyfour);
            staticRoute.add(fortyfive);
            staticRoute.add(fortysix);
            staticRoute.add(fortyseven);
            staticRoute.add(fortyeight);
            staticRoute.add(fortynine);
            staticRoute.add(fifty);
            staticRoute.add(fiftyone);
            staticRoute.add(fiftytwo);
            staticRoute.add(fiftythree);
            staticRoute.add(fiftyfour);
            staticRoute.add(fiftyfive);
        } else if(user.equals("stephen")) {

        } else if(user.equals("matt")) {
            //Detect if it's the T
            if(GlobalVars.Transportation == "T"){

            }
            //Did I set it to be a bus instead?
            else if(GlobalVars.Transportation == "61"){
                //Fifth and Sixth Avenue
                Location one = new Location("");
                one.setLatitude(40.438941);
                one.setLongitude(-79.994737);
                //Sixth and  Forbes
                Location two = new Location("");
                two.setLatitude(40.438036);
                two.setLongitude(-79.994348);
                //Forbes and Birmingham
                Location three = new Location("");
                three.setLatitude(40.437065);
                three.setLongitude(-79.972453);
                //Forbes Stops bending
                Location four = new Location("");
                four.setLatitude(40.435949);
                four.setLongitude(-79.964750);
                //Forbes and David Lawrence Hall
                Location five = new Location("");
                five.setLatitude(40.442669);
                five.setLongitude(-79.955434);
                //Final destination - Forbes and Bigelow
                Location six = new Location("");
                six.setLatitude(40.443157);
                six.setLongitude(-79.953597);
                //Add the newly constructed routes
                staticRoute.add(one);
                staticRoute.add(two);
                staticRoute.add(three);
                staticRoute.add(four);
                staticRoute.add(five);
                staticRoute.add(six);
            }
            else if(GlobalVars.Transportation == "71"){
                //Fifth and Sixth Avenue
                Location one = new Location("");
                one.setLatitude(40.438941);
                one.setLongitude(-79.994737);
                //Sixth and  Forbes
                Location two = new Location("");
                two.setLatitude(40.438036);
                two.setLongitude(-79.994348);
                //Forbes and Jumonville
                Location three = new Location("");
                three.setLatitude(40.437787);
                three.setLongitude(-79.977697);
                //Fifth and Jumonville
                Location four = new Location("");
                four.setLatitude(40.438228);
                four.setLongitude(-79.977649);
                //Fifth and Kirkpatrick
                Location five = new Location("");
                five.setLatitude(40.437917);
                five.setLongitude(-79.973414);
                //Fifth close to Brenham
                Location six = new Location("");
                six.setLatitude(40.436445);
                six.setLongitude(-79.968761);
                //Fifth and Blvd of the Allies
                Location seven = new Location("");
                seven.setLatitude(40.436576);
                seven.setLongitude(-79.966310);

                //Fifth Robinson
                Location eight = new Location("");
                eight.setLatitude(40.437646);
                eight.setLongitude(-79.965113);

                //Fifth Dunseith
                Location nine = new Location("");
                nine.setLatitude(40.438347);
                nine.setLongitude(-79.963134);

                //Final - Fifth and Bigelow
                Location ten = new Location("");
                ten.setLatitude(40.444408);
                ten.setLongitude(-79.954845);
                //Add the newly created route
                staticRoute.add(one);
                staticRoute.add(two);
                staticRoute.add(three);
                staticRoute.add(four);
                staticRoute.add(five);
                staticRoute.add(six);
                staticRoute.add(seven);
                staticRoute.add(eight);
                staticRoute.add(nine);
                staticRoute.add(ten);
            }

        }
    }

    //creates route to home
    private void createHomeRoute() {
        if(user.equals("test")) {
            Location one = new Location("");
            one.setLatitude(40.445179);
            one.setLongitude(-79.950357);

            Location two = new Location("");
            two.setLatitude(40.445065);
            two.setLongitude(-79.951263);

            Location three = new Location("");
            three.setLatitude(40.443918);
            three.setLongitude(-79.950732);

            Location four = new Location("");
            four.setLatitude(40.443162);
            four.setLongitude(-79.953543);

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
        } else if(user.equals("mosse")) {
            Location one = new Location("");
            one.setLatitude(40.441845);
            one.setLongitude(-79.956239);

            Location two = new Location("");
            two.setLatitude(40.441955);
            two.setLongitude(-79.956426);

            Location three = new Location("");
            three.setLatitude(40.442645);
            three.setLongitude(-79.955466);

            Location four = new Location("");
            four.setLatitude(40.444442);
            four.setLongitude(-79.948675);

            Location five = new Location("");
            five.setLatitude(40.446928);
            five.setLongitude(-79.949013);

            Location six = new Location("");
            six.setLatitude(40.447080);
            six.setLongitude(-79.947648);

            Location seven = new Location("");
            seven.setLatitude(40.447287);
            seven.setLongitude(-79.947414);

            Location eight = new Location("");
            eight.setLatitude(40.447989);
            eight.setLongitude(-79.947679);

            Location nine = new Location("");
            nine.setLatitude(40.450500);
            nine.setLongitude(-79.945023);

            Location ten = new Location("");
            ten.setLatitude(40.451304);
            ten.setLongitude(-79.940930);

            Location eleven = new Location("");
            eleven.setLatitude(40.452761);
            eleven.setLongitude(-79.936714);

            Location twelve = new Location("");
            twelve.setLatitude(40.455113);
            twelve.setLongitude(-79.931859);

            Location thirteen = new Location("");
            thirteen.setLatitude(40.456358);
            thirteen.setLongitude(-79.930534);

            Location fourteen = new Location("");
            fourteen.setLatitude(40.458362);
            fourteen.setLongitude(-79.927358);

            Location fifteen = new Location("");
            fifteen.setLatitude(40.459047);
            fifteen.setLongitude(-79.924499);

            Location sixteen = new Location("");
            sixteen.setLatitude(40.459072);
            sixteen.setLongitude(-79.923802);

            Location seventeen = new Location("");
            seventeen.setLatitude(40.459251);
            seventeen.setLongitude(-79.922713);

            Location eighteen = new Location("");
            eighteen.setLatitude(40.459190);
            eighteen.setLongitude(-79.922085);

            Location nineteen = new Location("");
            nineteen.setLatitude(40.459807);
            nineteen.setLongitude(-79.922584);

            Location twenty = new Location("");
            twenty.setLatitude(40.460550);
            twenty.setLongitude(-79.922997);

            Location twentyone = new Location("");
            twentyone.setLatitude(40.460720);
            twentyone.setLongitude(-79.922688);

            Location twentytwo = new Location("");
            twentytwo.setLatitude(40.462902);
            twentytwo.setLongitude(-79.921293);

            Location twentythree = new Location("");
            twentythree.setLatitude(40.463173);
            twentythree.setLongitude(-79.921033);

            Location twentyfour = new Location("");
            twentyfour.setLatitude(40.464936);
            twentyfour.setLongitude(-79.919976);

            Location twentyfive = new Location("");
            twentyfive.setLatitude(40.466037);
            twentyfive.setLongitude(-79.919287);

            Location twentysix = new Location("");
            twentysix.setLatitude(40.468835);
            twentysix.setLongitude(-79.917803);

            Location twentyseven = new Location("");
            twentyseven.setLatitude(40.468525);
            twentyseven.setLongitude(-79.916628);

            Location twentyeight = new Location("");
            twentyeight.setLatitude(40.469247);
            twentyeight.setLongitude(-79.912611);

            Location twentynine = new Location("");
            twentynine.setLatitude(40.469627);
            twentynine.setLongitude(-79.911940);

            Location thirty = new Location("");
            thirty.setLatitude(40.470202);
            thirty.setLongitude(-79.911602);

            Location thirtyone = new Location("");
            thirtyone.setLatitude(40.473442);
            thirtyone.setLongitude(-79.911092);

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
            staticRoute.add(five);
            staticRoute.add(six);
            staticRoute.add(seven);
            staticRoute.add(eight);
            staticRoute.add(nine);
            staticRoute.add(ten);
            staticRoute.add(eleven);
            staticRoute.add(twelve);
            staticRoute.add(thirteen);
            staticRoute.add(fourteen);
            staticRoute.add(fifteen);
            staticRoute.add(sixteen);
            staticRoute.add(seventeen);
            staticRoute.add(eighteen);
            staticRoute.add(nineteen);
            staticRoute.add(twenty);
            staticRoute.add(twentyone);
            staticRoute.add(twentytwo);
            staticRoute.add(twentythree);
            staticRoute.add(twentyfour);
            staticRoute.add(twentyfive);
            staticRoute.add(twentysix);
            staticRoute.add(twentyseven);
            staticRoute.add(twentyeight);
            staticRoute.add(twentynine);
            staticRoute.add(thirty);
            staticRoute.add(thirtyone);
        } else if(user.equals("driver")) {
            Location one = new Location("");
            one.setLatitude(40.450092);
            one.setLongitude(-79.950885);

            Location two = new Location("");
            two.setLatitude(40.449805);
            two.setLongitude(-79.950725);

            Location three = new Location("");
            three.setLatitude(40.469247);
            three.setLongitude(-79.912611);

            Location four = new Location("");
            four.setLatitude(40.451407);
            four.setLongitude(-79.945441);

            Location five = new Location("");
            five.setLatitude(40.452175);
            five.setLongitude(-79.941463);

            Location six = new Location("");
            six.setLatitude(40.4447717);
            six.setLongitude(-79.938835);

            Location seven = new Location("");
            seven.setLatitude(40.447922);
            seven.setLongitude(-79.935758);

            Location eight = new Location("");
            eight.setLatitude(40.450545);
            eight.setLongitude(-79.927512);

            Location nine = new Location("");
            nine.setLatitude(40.452727);
            nine.setLongitude(-79.921930);

            Location ten = new Location("");
            ten.setLatitude(40.454016);
            ten.setLongitude(-79.912438);

            Location eleven = new Location("");
            eleven.setLatitude(40.456784);
            eleven.setLongitude(-79.909434);

            Location twelve = new Location("");
            twelve.setLatitude(40.461690);
            twelve.setLongitude(-79.905818);

            Location thirteen = new Location("");
            thirteen.setLatitude(40.462433);
            thirteen.setLongitude(-79.905609);

            Location fourteen = new Location("");
            fourteen.setLatitude(40.463282);
            fourteen.setLongitude(-79.905840);

            Location fifteen = new Location("");
            fifteen.setLatitude(40.466922);
            fifteen.setLongitude(-79.908500);

            Location sixteen = new Location("");
            sixteen.setLatitude(40.467681);
            sixteen.setLongitude(-79.908782);

            Location seventeen = new Location("");
            seventeen.setLatitude(40.479791);
            seventeen.setLongitude(-79.907803);

            Location eighteen = new Location("");
            eighteen.setLatitude(40.482198);
            eighteen.setLongitude(-79.908039);

            Location nineteen = new Location("");
            nineteen.setLatitude(40.484263);
            nineteen.setLongitude(-79.907824);

            Location twenty = new Location("");
            twenty.setLatitude(40.485152);
            twenty.setLongitude(-79.909219);

            //stop on washington blv entrance curve

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
            staticRoute.add(five);
            staticRoute.add(six);
            staticRoute.add(seven);
            staticRoute.add(eight);
            staticRoute.add(nine);
            staticRoute.add(ten);
            staticRoute.add(eleven);
            staticRoute.add(twelve);
            staticRoute.add(thirteen);
            staticRoute.add(fourteen);
            staticRoute.add(fifteen);
            staticRoute.add(sixteen);
            staticRoute.add(seventeen);
            staticRoute.add(eighteen);
            staticRoute.add(nineteen);
            staticRoute.add(twenty);
        } else if(user.equals("stephen")) {

        } else if(user.equals("matt")) {
            if(GlobalVars.Transportation == "T"){

            }
            else if(GlobalVars.Transportation == "Bus"){
                //First - Fifth and Atwood
                Location one = new Location("");
                one.setLatitude(40.441770);
                one.setLongitude(-79.958493);

                //Fifth Dunseith
                Location two = new Location("");
                two.setLatitude(40.438347);
                two.setLongitude(-79.963134);

                //Fifth Robinson
                Location three = new Location("");
                three.setLatitude(40.437646);
                three.setLongitude(-79.965113);

                //Fifth and Blvd of the Allies
                Location four = new Location("");
                four.setLatitude(40.436576);
                four.setLongitude(-79.966310);

                //Fifth close to Brenham
                Location five = new Location("");
                five.setLatitude(40.436445);
                five.setLongitude(-79.968761);

                //Fifth and Kirkpatrick
                Location six = new Location("");
                six.setLatitude(40.437917);
                six.setLongitude(-79.973414);

                Location seven = new Location("");
                seven.setLatitude(40.438936);
                seven.setLongitude(-79.994712);

                staticRoute.add(one);
                staticRoute.add(two);
                staticRoute.add(three);
                staticRoute.add(four);
                staticRoute.add(five);
                staticRoute.add(six);
                staticRoute.add(seven);
            }

        } 
    }

    private void navigation(){

        Intent intent = new Intent("Intersection Test");
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);

        try {
            destination = staticRoute.get(1);
            LocM.addProximityAlert(staticRoute.get(1).getLatitude(), staticRoute.get(1).getLongitude(), 15, -1, pendingIntent);
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
    private void setNewLocationListener(){
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
                    float radius = 40;
                    if(location.distanceTo(destination) <= radius) {
                        //We reached the destination radius - no need to test if we overshot the coordinate
                        testOvershot = false;
                        //We have reached our destination set a new destination
                        if(setDestination()) {
                            //Calculate time to the new destination
                            float time;

                            time = (calcDistance(location, destination) - radius) / location.getSpeed();


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
                            //If the distance changed more than 5 meters - a significant change
                            if(curDistance - firstDistance > 5f){
                                testOvershot = false;
                                //We overshot our destination since we increased distance by meters - acquire the next intersection
                                if(setDestination()) {
                                    //Log that we over shot and by how many meters
                                    try{
                                        Date now = new Date();

                                        String logstring = "OVERSHOT|" + curDistance + "|" + location.getLatitude() + "|" + location.getLongitude() + "|" + now.toString()  +"\n";
                                        fos.write(logstring.getBytes());
                                    }catch(Exception e){

                                    }
                                    //Calculate time to the new destination

                                    float time;
                                    time = (calcDistance(location, destination) - radius) / location.getSpeed();


                                    try{
                                        Date now = new Date();

                                        String logstring = "LOCOFF|" + location.getLatitude() + "|" + location.getLongitude() + "|" + now.toString()  +"\n";
                                        fos.write(logstring.getBytes());
                                    }catch(Exception e){

                                    }
                                    calcGPSTurnOff(time);
                                }
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
        if(ListI == staticRoute.size()) {
            destMarker.remove();
            navigation = false;
            ListI = 0;
            //Set the two text boxes relevant only to a destination to off
            distancedisplay.setText("");
            timedisplay.setText("");
            try{
                //Log that the user has reached their destination
                Date now = new Date();
                String logstring = "NAVSTOP|FINALDEST|" + now.toString() +"\n";

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
            destination = staticRoute.get(ListI);
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
        if(null == beforeEnable) {
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