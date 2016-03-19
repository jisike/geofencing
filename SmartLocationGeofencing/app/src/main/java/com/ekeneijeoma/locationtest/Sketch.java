package com.ekeneijeoma.locationtest;


import android.content.Context;
import android.location.Location;
import android.view.Gravity;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import io.nlopez.smartlocation.OnGeofencingTransitionListener;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.geofencing.model.GeofenceModel;
import io.nlopez.smartlocation.geofencing.utils.TransitionGeofence;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;
import processing.core.PApplet;
import processing.core.PFont;
import processing.data.JSONArray;
import processing.data.JSONObject;

/**
 * Created by ekeneijeoma on 1/28/16.
 */

public class Sketch extends PApplet implements OnGeofencingTransitionListener, OnLocationUpdatedListener {
    Context context;

    LocationGooglePlayServicesProvider provider;

    TransitionGeofence lastGeofence = null;
    Location lastLocation = new Location("test");

    Location geofenceSearchLocation = new Location("test");
    float geofenceSearchRadius = 1069 * .1f; //1mi = 1065meters

    ArrayList<String> geofenceIdList = new ArrayList();
    int geofenceRadius = 50;

    ArrayList<GeofenceLocation> geofenceLocationList = new ArrayList<>();
    GeofenceLocation lastGeofenceLocation = null;

    PFont font;
    int fontSize = 64;

    boolean first = true;

    @Override
    public void settings() {
        fullScreen();
    }

    @Override
    public void setup() {
        context = this.getActivity().getApplicationContext();

        font = createFont("SansSerif-Bold", fontSize);

        loadGeofences();

        startLocation();
    }

    public void draw() {
        background(255);

        fill(0);
        textFont(font);
        textAlign(CENTER);

        drawLocations();
    }

    private void startLocation() {
        println("startLocation");
        provider = new LocationGooglePlayServicesProvider();
        provider.setCheckLocationSettings(true);

        SmartLocation smartLocation = new SmartLocation.Builder(context)
                .logging(true)
                .build();

        smartLocation.location(provider)
                .config(LocationParams.NAVIGATION)
                .start(this);

        SmartLocation.with(context).geofencing().start(this);
    }

    private void stopLocation() {
        println("stopLocation");

        SmartLocation.with(context).location().stop();
        SmartLocation.with(context).geofencing().stop();
    }

    void updateLocation(Location location) {
//        println("updateLocation");

        lastLocation = location;

        checkLocations();

        first = false;
    }

    void loadGeofences() {
        println("loadGeofences");
        JSONArray jsons = JSONArray.parse(join(loadStrings("crashes.json"), ""));

        for (int i = 0; i < jsons.size(); i++) {
            JSONObject json = jsons.getJSONObject(i);
            geofenceLocationList.add(createLocation(json));
        }
    }

    GeofenceLocation createLocation(JSONObject json) {
        GeofenceLocation i = new GeofenceLocation();

        i.name = json.getString("name");
        i.location.setLatitude(json.getDouble("lat"));
        i.location.setLongitude(json.getDouble("lng"));
        i.distance = 0;

        return i;
    }

    void checkLocations() {
//        PApplet.println("checkLocations");

        boolean found = false;

        for (GeofenceLocation intersection : geofenceLocationList) {
            if (intersection.checkInside(lastLocation, geofenceRadius)) {
                found = true;

                if (lastGeofenceLocation == null || (lastGeofenceLocation != null && !lastGeofenceLocation.equals(intersection))) {
//                    println("diy entering intersection");
                    lastGeofenceLocation = intersection;

//                    sendToast("entering " + lastGeofenceLocation.name, Toast.LENGTH_LONG);
                }
            }
        }

        if (!found) {
            if (lastGeofenceLocation != null) {
//                println("diy exiting intersection");
//                sendToast("exiting " + lastGeofenceLocation.name, Toast.LENGTH_LONG);

                lastGeofenceLocation = null;
            }
        }

        updateGeofences();
    }

    void drawLocations() {
        Collections.sort(geofenceLocationList, new Comparator<GeofenceLocation>() {
            public int compare(GeofenceLocation i1, GeofenceLocation i2) {
                return Float.compare(i1.distance, i2.distance);
            }
        });

        int itemW = width;
        int itemH = 120;
        int itemYGap = 5;

        int marginLeft = 50;
        int marginRight = 50;

        pushMatrix();
        translate(0, itemH + itemYGap);
        textFont(font, fontSize);

        fill(255);
        rect(0, 0, itemW, itemH);

        fill(0);
        textAlign(LEFT, CENTER);
        text(lastLocation.getLatitude() + ", " + lastLocation.getLongitude() + "/" + lastLocation.getAccuracy(), marginLeft, itemH / 2);

//        textAlign(RIGHT, CENTER);
//        text((int) locationChange, itemW - marginRight, itemH / 2);

        for (int i = 0; i < geofenceLocationList.size(); i++) {
            GeofenceLocation intersection = geofenceLocationList.get(i);

            float itemX = 0;
            float itemY = (itemH + itemYGap) * (i + 1);

            pushMatrix();
            translate(itemX, itemY);

            fill((intersection.equals(lastGeofenceLocation)) ? color(0, 255, 0) : 255);
            rect(0, 0, itemW, itemH);

            String left = intersection.name;
            fill(0);
            textAlign(LEFT, CENTER);
            text(left, marginLeft, itemH / 2);

            String right = (int) intersection.distance + "";
            textAlign(RIGHT, CENTER);
            text((right), itemW - marginRight, itemH / 2);//convert to mi

            popMatrix();
        }
        popMatrix();
    }

    void updateGeofences() {
        if (first || lastLocation.distanceTo(geofenceSearchLocation) >= geofenceSearchRadius) {
            println("updateGeoFences");

//            stopGeofences();

            geofenceSearchLocation = new Location(lastLocation);
            geofenceIdList = new ArrayList<>();

            for (GeofenceLocation i : geofenceLocationList) {
                println(i.distance);
                if (i.distance < geofenceSearchRadius) {
                    GeofenceModel g = createGeofence(i);
                    SmartLocation.with(context).geofencing().add(g);

                    geofenceIdList.add(g.getRequestId());
                }
            }

//            println(geofenceIdList);

//            if(geofenceList.size() >0 )
//                startGeofences();
        }
    }

    GeofenceModel createGeofence(GeofenceLocation i) {
        String id = i.name.replace(" ", "");
        double lat = i.location.getLatitude();
        double lng = i.location.getLongitude();

        GeofenceModel g = new GeofenceModel.Builder(id)
                .setTransition(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLatitude(lat)
                .setLongitude(lng)
                .setRadius(geofenceRadius)
                .build();

        return g;
    }

    public void onConnected() {
        println("asdfsd");
    }

    public void onLocationUpdated(Location location) {
        updateLocation(location);
    }

    public void onGeofenceTransition(TransitionGeofence geofence) {
        println("onGeofenceTransition");
        lastGeofence = geofence;

        String text = geofenceType(lastGeofence.getTransitionType()) + " " + lastGeofence.getGeofenceModel().getRequestId();

        sendToast(text, Toast.LENGTH_LONG);
    }

    String geofenceType(int type) {
        switch (type) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "entering";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "exiting";
            default:
                return "dwelling in";
        }
    }

    void sendToast(String text, int duration) {
        Toast t = Toast.makeText(context, text, duration);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
        t.show();
    }
}