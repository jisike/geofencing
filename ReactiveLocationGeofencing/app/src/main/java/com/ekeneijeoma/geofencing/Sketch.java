package com.ekeneijeoma.geofencing;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.view.Gravity;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import processing.core.PApplet;
import processing.core.PFont;
import processing.data.JSONArray;
import processing.data.JSONObject;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by ekeneijeoma on 1/28/16.
 */

public class Sketch extends PApplet {
    Context context;

    ReactiveLocationProvider locationProvider;
    Subscription subscription;

    Location lastLocation = new Location("test");

    Location geofenceSearchLocation = new Location("test");
    float geofenceSearchRadius = 1069 * .1f; //1mi = 1065meters

    ArrayList<Geofence> geofenceList = new ArrayList();
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
        LocationRequest request = LocationRequest.create() //standard GMS LocationRequest
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(100);

        locationProvider = new ReactiveLocationProvider(context);
        subscription = locationProvider.getUpdatedLocation(request)
                .subscribe(new Action1<Location>() {
                    @Override
                    public void call(Location location) {
                        updateLocation(location);
                    }
                });
    }

    private void stopLocation() {
        subscription.unsubscribe();
    }

    void updateLocation(Location location) {
//        println("updateLocation");

        lastLocation = location;

        checkLocations();

        first = false;
    }

    void loadGeofences() {
        println("loadGeofences");
        JSONArray jsons = JSONArray.parse(join(loadStrings("locations.json"), ""));

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

    void startGeofences() {
        println("startGeofences");

        final GeofencingRequest geofencingRequest = createGeofencingRequest();
        if (geofencingRequest == null) return;

        final PendingIntent pendingIntent = createNotificationBroadcastPendingIntent();

        locationProvider
                .addGeofences(pendingIntent, geofencingRequest)
                .subscribe(new Action1<Status>() {
                    @Override
                    public void call(Status addGeofenceResult) {
                        sendToast("Geofence added, success: " + addGeofenceResult.isSuccess(), Toast.LENGTH_SHORT);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                        sendToast("Error adding geofence.", Toast.LENGTH_SHORT);
                    }
                });
    }

    void stopGeofences() {
        println("stopGeofences");

        locationProvider.removeGeofences(createNotificationBroadcastPendingIntent()).subscribe(new Action1<Status>() {
            @Override
            public void call(Status status) {
                sendToast("Geofences removed", Toast.LENGTH_SHORT);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                sendToast("Error removing geofences", Toast.LENGTH_SHORT);
            }
        });

        geofenceList = new ArrayList<>();
    }

    void updateGeofences() {
        if (first || lastLocation.distanceTo(geofenceSearchLocation) >= geofenceSearchRadius) {
            println("updateGeoFences");

//            stopGeofences();

            geofenceSearchLocation = new Location(lastLocation);
            geofenceList = new ArrayList<>();

            for (GeofenceLocation i : geofenceLocationList) {
                if (i.distance < geofenceSearchRadius) {
                    geofenceList.add(createGeofence(i));
                }
            }

//            println(geofenceList);

            if(geofenceList.size() >0 )
            startGeofences();
        }
    }

    Geofence createGeofence(GeofenceLocation i) {
        String id = i.name.replace(" ", "");
        double lat = i.location.getLatitude();
        double lng = i.location.getLongitude();

        Geofence g = new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(lat, lng, geofenceRadius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        return g;
    }

    private GeofencingRequest createGeofencingRequest() {
//        println(geofenceList);
        try {
            return new GeofencingRequest.Builder()
                    .addGeofences(geofenceList)
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .build();
        } catch (NumberFormatException ex) {
            sendToast("Error parsing input.", Toast.LENGTH_SHORT);
            return null;
        }
    }

    private PendingIntent createNotificationBroadcastPendingIntent() {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, GeofenceBroadcastReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    void sendToast(String text, int duration) {
        Toast t = Toast.makeText(context, text, duration);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
        t.show();
    }
}

